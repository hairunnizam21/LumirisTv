package com.suzunei.lumirisiptv.data.util

import androidx.media3.common.MimeTypes

/**
 * Best-effort stream-type detection by URL path so ExoPlayer can pick the right extractor.
 * Falls back to `null` (auto-detect by Media3) when the URL gives no hint.
 */
object StreamTypeDetector {

    fun detectMimeType(url: String): String? {
        val lower = url.lowercase()
        val path = lower.substringBefore('?')
        return when {
            path.endsWith(".mpd") || lower.contains(".mpd") -> MimeTypes.APPLICATION_MPD
            path.endsWith(".m3u8") || lower.contains(".m3u8") -> MimeTypes.APPLICATION_M3U8
            path.endsWith(".mp4") -> MimeTypes.VIDEO_MP4
            path.endsWith(".webm") -> MimeTypes.VIDEO_WEBM
            path.endsWith(".mkv") -> MimeTypes.VIDEO_MATROSKA
            // Google Drive direct downloads have no extension; let Media3 sniff the container.
            GoogleDriveUrlHelper.isDriveUrl(url) -> null
            else -> null
        }
    }
}
