package com.suzunei.lumirisiptv.ui.player

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import com.suzunei.lumirisiptv.data.util.StreamTypeDetector
import com.suzunei.lumirisiptv.domain.model.Channel

/**
 * Builds a Media3 [MediaItem] from a [Channel]. Handles MIME hinting and optional ClearKey DRM
 * (typical for DASH manifests in this playlist).
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
            val (kidHex, keyHex) = kidKey.split(":", limit = 2)
                .let { it[0] to it.getOrElse(1) { "" } }
            if (kidHex.isNotBlank() && keyHex.isNotBlank()) {
                val licenseJson = buildClearKeyLicense(kidHex, keyHex)
                builder.setDrmConfiguration(
                    MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID)
                        .setKeySetId(licenseJson.toByteArray(Charsets.UTF_8))
                        .build(),
                )
            }
        }

        return builder.build()
    }

    /**
     * Builds the JSON ClearKey license body required by ExoPlayer's LocalMediaDrmCallback.
     * KID/key are converted from hex to base64-url (no padding) per the ClearKey spec.
     */
    private fun buildClearKeyLicense(kidHex: String, keyHex: String): String {
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
