package com.suzunei.lumirisiptv.ui.player

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import com.suzunei.lumirisiptv.data.util.StreamTypeDetector
import com.suzunei.lumirisiptv.domain.model.Channel

/**
 * Builds a Media3 [MediaItem] from a [Channel]. Handles MIME hinting and optional ClearKey DRM
 * (typical for DASH manifests in this playlist).
 *
 * The ClearKey license response (W3C-compliant JSON) is carried on the [MediaItem.DrmConfiguration]
 * as `keySetId` raw bytes. [LumirisDrmSessionManagerProvider] later reads those bytes and feeds
 * them to a [androidx.media3.exoplayer.drm.LocalMediaDrmCallback] — no network round-trip happens.
 */
object MediaItemFactory {

    fun build(channel: Channel): MediaItem {
        val builder = MediaItem.Builder()
            .setUri(channel.streamUrl)
            .setMediaId(channel.id)

        StreamTypeDetector.detectMimeType(channel.streamUrl)?.let { mime ->
            builder.setMimeType(mime)
        }

        channel.drmKey?.let { kidKey ->
            val parts = kidKey.split(":", limit = 2)
            val kidHex = parts.getOrNull(0).orEmpty()
            val keyHex = parts.getOrNull(1).orEmpty()
            if (kidHex.isNotBlank() && keyHex.isNotBlank()) {
                val license = buildClearKeyLicense(kidHex, keyHex).toByteArray(Charsets.UTF_8)
                builder.setDrmConfiguration(
                    MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID)
                        .setKeySetId(license)
                        .setMultiSession(false)
                        .build(),
                )
            }
        }

        return builder.build()
    }

    /**
     * Builds the JSON ClearKey license body that ExoPlayer's `LocalMediaDrmCallback` expects.
     * KID/key are hex in the playlist; the JWK Set spec requires base64-url-without-padding.
     */
    internal fun buildClearKeyLicense(kidHex: String, keyHex: String): String {
        val kid = base64Url(hexToBytes(kidHex))
        val key = base64Url(hexToBytes(keyHex))
        return """{"keys":[{"kty":"oct","k":"$key","kid":"$kid"}],"type":"temporary"}"""
    }

    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.replace("-", "")
        val out = ByteArray(clean.length / 2)
        for (i in out.indices) {
            val hi = Character.digit(clean[i * 2], 16)
            val lo = Character.digit(clean[i * 2 + 1], 16)
            out[i] = ((hi shl 4) or lo).toByte()
        }
        return out
    }

    private fun base64Url(bytes: ByteArray): String =
        android.util.Base64.encodeToString(
            bytes,
            android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP,
        )
}
