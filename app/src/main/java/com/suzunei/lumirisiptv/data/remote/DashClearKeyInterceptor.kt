package com.suzunei.lumirisiptv.data.remote

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
        if (!path.endsWith(".mpd") && !path.endsWith(".mpd.xml")) return response
        if (!response.isSuccessful) return response

        val body = response.body ?: return response
        val raw = body.string()
        val rewritten = rewrite(raw)

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
            val kidNoDashes = rawKid.replace("-", "")
            val psshBase64 = buildClearKeyPssh(kidNoDashes)
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

    /** Build a v0 PSSH box with ClearKey SystemID and a JSON payload listing the KID. */
    @OptIn(ExperimentalEncodingApi::class)
    internal fun buildClearKeyPssh(kidHex: String): String {
        val kidBytes = hexToBytes(kidHex)
        val kidB64 = Base64.UrlSafe.encode(kidBytes).trimEnd('=')
        val json = """{"kids":["$kidB64"],"type":"temporary"}"""
        val data = json.toByteArray(Charsets.UTF_8)

        val boxSize = 4 + 4 + 4 + CLEARKEY_UUID_BYTES.size + 4 + data.size
        val buf = ByteBuffer.allocate(boxSize).order(ByteOrder.BIG_ENDIAN)
        buf.putInt(boxSize)
        buf.put(byteArrayOf('p'.code.toByte(), 's'.code.toByte(), 's'.code.toByte(), 'h'.code.toByte()))
        buf.putInt(0) // version (0) + flags (0)
        buf.put(CLEARKEY_UUID_BYTES)
        buf.putInt(data.size)
        buf.put(data)
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

    private companion object {
        const val CLEARKEY_UUID = "e2719d58-a985-b3c9-781a-b030af78d30e"
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
