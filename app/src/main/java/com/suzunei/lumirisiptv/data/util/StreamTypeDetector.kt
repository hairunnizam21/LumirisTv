package com.suzunei.lumirisiptv.data.util

import androidx.media3.common.MimeTypes

/**
 * Best-effort stream-type detection by URL so ExoPlayer can pick the right extractor.
 *
 * The detector inspects both the URL path **and** the query string, because many CDNs serve
 * manifests with no file extension (e.g. `…/manifest?type=hls`) or hide the real path behind a
 * token (e.g. `…/master?format=m3u8&token=…`). For URLs that give no hint at all, returns `null`
 * so Media3 falls back to its built-in container/codec sniffing.
 */
object StreamTypeDetector {

    fun detectMimeType(url: String): String? {
        if (url.isBlank()) return null
        val lower = url.lowercase()
        val path = lower.substringBefore('?')
        val query = lower.substringAfter('?', missingDelimiterValue = "")

        return when {
            // --- DASH ---
            path.endsWith(".mpd") || path.endsWith(".mpd.xml") -> MimeTypes.APPLICATION_MPD
            path.contains(".mpd/") || path.contains(".mpd?") -> MimeTypes.APPLICATION_MPD
            query.contains("format=mpd") || query.contains("type=dash") -> MimeTypes.APPLICATION_MPD
            path.contains("/dash/") || path.contains("manifest.mpd") -> MimeTypes.APPLICATION_MPD

            // --- HLS ---
            path.endsWith(".m3u8") || path.endsWith(".m3u") -> MimeTypes.APPLICATION_M3U8
            path.contains(".m3u8/") || path.contains(".m3u8?") -> MimeTypes.APPLICATION_M3U8
            query.contains("format=m3u8") || query.contains("type=hls") -> MimeTypes.APPLICATION_M3U8
            path.contains("/hls/") || path.endsWith("/master") || path.endsWith("/playlist") -> MimeTypes.APPLICATION_M3U8

            // --- Progressive / file containers ---
            path.endsWith(".mp4") || path.endsWith(".m4v") -> MimeTypes.VIDEO_MP4
            path.endsWith(".webm") -> MimeTypes.VIDEO_WEBM
            path.endsWith(".mkv") -> MimeTypes.VIDEO_MATROSKA
            path.endsWith(".ts") -> MimeTypes.VIDEO_MP2T
            path.endsWith(".mov") -> MimeTypes.VIDEO_MP4
            path.endsWith(".mp3") -> MimeTypes.AUDIO_MPEG
            path.endsWith(".aac") -> MimeTypes.AUDIO_AAC
            path.endsWith(".flac") -> MimeTypes.AUDIO_FLAC

            // Direct-download providers (Drive, Dropbox, etc.) — let Media3 sniff the container
            // since the URL has no extension hint.
            DirectUrlHelper.isShareUrl(url) -> null
            else -> null
        }
    }
}
