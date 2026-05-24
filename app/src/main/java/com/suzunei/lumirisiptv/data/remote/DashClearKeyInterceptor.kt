package com.suzunei.lumirisiptv.data.remote

import android.util.Log
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * OkHttp interceptor that injects a ClearKey [ContentProtection] entry — with a synthesized PSSH
 * box — next to the existing CENC one in DASH manifests.
 *
 * Why it exists: many IPTV providers (e.g. perfecttv.net) ship DASH manifests with only a Widevine
 * ContentProtection element, even though they hand out plain ClearKey hex KIDs/keys in the
 * playlist. Without ClearKey [SchemeData] in the parsed manifest, Media3's
 * [androidx.media3.exoplayer.drm.DefaultDrmSessionManager.acquireSession] throws
 * `MissingSchemeDataException` because [getSchemeDatas] requires non-null scheme data.
 *
 * The fix:
 *   1. Match the existing `urn:mpeg:dash:mp4protection:2011` ContentProtection (which carries the
 *      `default_KID`).
 *   2. Append a sibling `<ContentProtection schemeIdUri="urn:uuid:e2719d58-…" …>` element with a
 *      synthesized ClearKey PSSH box (version 0, JSON payload `{"kids":["<b64url(KID)>"],"type":"temporary"}`).
 *
 * Robustness rules:
 *   - Manifests up to 50 MB are rewritten; larger payloads pass through untouched to avoid OOM.
 *   - Malformed KIDs (non-canonical length, stripped leading zeros, etc.) are normalized when
 *     possible and **skipped silently** when they can't be repaired — the request never crashes.
 *
 * Media3 now finds matching ClearKey scheme data, hands it to
 * [com.suzunei.lumirisiptv.ui.player.LumirisDrmSessionManagerProvider] which uses
 * [androidx.media3.exoplayer.drm.LocalMediaDrmCallback] (so the actual license JSON — stored in
 * `MediaItem.DrmConfiguration.keySetId` — is returned without any network round-trip).
 */
class DashClearKeyInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        val path = request.url.encodedPath.lowercase()
        if (!isManifestPath(path)) return response
        if (!response.isSuccessful) return response

        val body = response.body ?: return response

        // Avoid buffering huge manifests into memory. If the server advertises a content length
        // larger than the cap, just pass the response through. Unknown lengths (-1) are buffered
        // below with a second-level check on the actual byte count.
        val contentLength = body.contentLength()
        if (contentLength > MAX_MANIFEST_BYTES) return response

        val raw = try {
            body.string()
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to read manifest body — passing through", t)
            return response
        }

        val rewritten = if (raw.length.toLong() > MAX_MANIFEST_BYTES) {
            raw
        } else {
            try {
                rewrite(raw)
            } catch (t: Throwable) {
                // Never break playback because of a rewrite error. Log and fall back to the raw
                // manifest — ExoPlayer may still play it (Widevine path, etc.).
                Log.w(TAG, "ClearKey rewrite failed — serving original manifest", t)
                raw
            }
        }

        val contentType = body.contentType()
            ?: "application/dash+xml".toMediaTypeOrNull()
        val newBody = rewritten.toByteArray(Charsets.UTF_8).toResponseBody(contentType)
        return response.newBuilder().body(newBody).build()
    }

    internal fun rewrite(xml: String): String {
        if (xml.contains(CLEARKEY_UUID, ignoreCase = true)) return xml // already has ClearKey
        return CENC_PROTECTION_REGEX.replace(xml) { match ->
            val rawKid = match.groupValues[1]
            val original = match.value
            val normalizedKid = normalizeKid(rawKid)
                ?: return@replace original.also {
                    Log.w(TAG, "Skipping ClearKey injection: cannot normalize KID '$rawKid'")
                }
            val psshBase64 = try {
                buildClearKeyPssh(normalizedKid)
            } catch (t: Throwable) {
                Log.w(TAG, "Skipping ClearKey injection: PSSH build failed for KID '$rawKid'", t)
                return@replace original
            }
            val element = buildString {
                append("\n        <ContentProtection schemeIdUri=\"urn:uuid:")
                append(CLEARKEY_UUID)
                append("\" value=\"ClearKey1.0\" default_KID=\"")
                append(rawKid)
                append("\">\n          <cenc:pssh xmlns:cenc=\"urn:mpeg:cenc:2013\">")
                append(psshBase64)
                append("</cenc:pssh>\n        </ContentProtection>")
            }
            "$original$element"
        }
    }

    /**
     * Returns a canonical 32-character hex KID (no dashes) or `null` if the input cannot be
     * coerced into a valid 16-byte identifier.
     *
     * Handles the common producer quirks observed in the wild:
     *   - UUID-form `8-4-4-4-12` strings where a leading zero was stripped from one or more
     *     segments (e.g. `912760c4-9eb-5aff-3e06-0422c502f410`).
     *   - Plain hex strings missing a couple of leading zeros (e.g. `9eb...410` → 30 chars).
     *   - Mixed-case hex.
     *
     * If the input is structurally invalid (non-hex chars, segments longer than the canonical
     * size, etc.) we return `null` rather than guessing — the manifest is left untouched and the
     * player can still attempt the Widevine path.
     */
    internal fun normalizeKid(raw: String): String? {
        val trimmed = raw.trim().trim('"', '\'').replace("\\s+".toRegex(), "")
        if (trimmed.isEmpty()) return null

        val hex = if (trimmed.contains('-')) {
            val segments = trimmed.split('-')
            if (segments.size != UUID_SEGMENT_LENGTHS.size) return null
            val padded = StringBuilder(32)
            for (i in segments.indices) {
                val seg = segments[i]
                val expected = UUID_SEGMENT_LENGTHS[i]
                if (seg.length > expected) return null
                if (!seg.all { it.isHexDigit() }) return null
                padded.append(seg.padStart(expected, '0'))
            }
            padded.toString()
        } else {
            if (!trimmed.all { it.isHexDigit() }) return null
            if (trimmed.length > 32) return null
            trimmed.padStart(32, '0')
        }

        return if (hex.length == 32) hex.lowercase() else null
    }

    /**
     * Build a **CENC v1** PSSH box (ISO/IEC 23001-7) listing the KID(s) in binary form.
     * Android's ClearKey CDM (`InitDataParser::parsePssh`) only accepts v1 cenc PSSH for the
     * `urn:uuid:e2719d58-…` scheme — it ignores the JSON-in-PSSH-data format used by the W3C
     * EME license request body. Layout:
     *
     * ```
     *   uint32 size       // total box size in bytes
     *   char[4] 'pssh'
     *   uint8  version=1
     *   uint8[3] flags=0
     *   uint8[16] SystemID  // ClearKey UUID
     *   uint32 KID_count
     *   uint8[16][KID_count] KIDs
     *   uint32 DataSize=0
     * ```
     */
    @OptIn(ExperimentalEncodingApi::class)
    internal fun buildClearKeyPssh(kidHex: String): String {
        val kidBytes = hexToBytes(kidHex)
        require(kidBytes.size == 16) { "KID must be 16 bytes, got ${kidBytes.size}" }

        val boxSize = 4 + 4 + 4 + CLEARKEY_UUID_BYTES.size + 4 + kidBytes.size + 4
        val buf = ByteBuffer.allocate(boxSize).order(ByteOrder.BIG_ENDIAN)
        buf.putInt(boxSize)
        buf.put(byteArrayOf('p'.code.toByte(), 's'.code.toByte(), 's'.code.toByte(), 'h'.code.toByte()))
        buf.put(1.toByte()) // version 1
        buf.put(byteArrayOf(0, 0, 0)) // flags
        buf.put(CLEARKEY_UUID_BYTES)
        buf.putInt(1) // KID count
        buf.put(kidBytes)
        buf.putInt(0) // DataSize = 0
        return Base64.encode(buf.array())
    }

    private fun hexToBytes(hex: String): ByteArray {
        val out = ByteArray(hex.length / 2)
        for (i in out.indices) {
            val hi = Character.digit(hex[i * 2], 16)
            val lo = Character.digit(hex[i * 2 + 1], 16)
            out[i] = ((hi shl 4) or lo).toByte()
        }
        return out
    }

    private fun isManifestPath(lowerPath: String): Boolean {
        val pathOnly = lowerPath.substringBefore('?')
        return pathOnly.endsWith(".mpd") ||
            pathOnly.endsWith(".mpd.xml") ||
            pathOnly.contains("/manifest")
    }

    private fun Char.isHexDigit(): Boolean =
        this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

    private companion object {
        const val TAG = "DashClearKeyInterceptor"
        const val CLEARKEY_UUID = "e2719d58-a985-b3c9-781a-b030af78d30e"

        /** Hard cap on manifest size we'll buffer + rewrite. Anything bigger passes through. */
        const val MAX_MANIFEST_BYTES = 50L * 1024L * 1024L // 50 MB

        val UUID_SEGMENT_LENGTHS = intArrayOf(8, 4, 4, 4, 12)

        val CLEARKEY_UUID_BYTES = byteArrayOf(
            0xE2.toByte(), 0x71.toByte(), 0x9D.toByte(), 0x58.toByte(),
            0xA9.toByte(), 0x85.toByte(), 0xB3.toByte(), 0xC9.toByte(),
            0x78.toByte(), 0x1A.toByte(), 0xB0.toByte(), 0x30.toByte(),
            0xAF.toByte(), 0x78.toByte(), 0xD3.toByte(), 0x0E.toByte(),
        )

        /** Matches `<ContentProtection ... schemeIdUri="urn:mpeg:dash:mp4protection:2011" ... default_KID="…" .../>`. */
        val CENC_PROTECTION_REGEX = Regex(
            """<ContentProtection[^>]*schemeIdUri="urn:mpeg:dash:mp4protection:2011"[^>]*default_KID="([^"]+)"[^>]*/?>""",
            RegexOption.IGNORE_CASE,
        )
    }
}
