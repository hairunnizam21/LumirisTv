package com.suzunei.lumirisiptv.data.util

/**
 * Converts standard Google Drive sharing URLs into direct streaming URLs that ExoPlayer can buffer.
 *
 * Handles common shapes:
 *  - https://drive.google.com/file/d/FILE_ID/view?usp=sharing
 *  - https://drive.google.com/file/d/FILE_ID/preview
 *  - https://drive.google.com/open?id=FILE_ID
 *  - https://drive.google.com/uc?id=FILE_ID&export=download
 *
 * Output form:
 *  - https://docs.google.com/uc?export=download&id=FILE_ID
 *
 * Non-Drive URLs pass through unchanged.
 */
object GoogleDriveUrlHelper {

    private val FILE_D_REGEX = Regex("""/file/d/([A-Za-z0-9_\-]+)""")
    private val ID_QUERY_REGEX = Regex("""[?&]id=([A-Za-z0-9_\-]+)""")

    fun isDriveUrl(url: String): Boolean {
        if (url.isBlank()) return false
        return url.contains("drive.google.com", ignoreCase = true) ||
            url.contains("docs.google.com", ignoreCase = true)
    }

    fun convert(url: String): String {
        if (!isDriveUrl(url)) return url
        val fileId = extractFileId(url) ?: return url
        return "https://docs.google.com/uc?export=download&id=$fileId"
    }

    fun extractFileId(url: String): String? {
        FILE_D_REGEX.find(url)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }?.let { return it }
        ID_QUERY_REGEX.find(url)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }?.let { return it }
        return null
    }
}
