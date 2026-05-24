package com.suzunei.lumirisiptv.data.util

/**
 * Normalizes common "share" URLs into URLs that ExoPlayer's HTTP data source can actually buffer.
 *
 * Each helper is conservative: if a URL doesn't match the expected provider pattern we return it
 * unchanged, so this is safe to call on every stream URL emerging from the M3U parser. Today we
 * cover:
 *
 *  - **Google Drive** — delegated to [GoogleDriveUrlHelper]; converts `…/file/d/<ID>/view` and
 *    friends to `https://drive.usercontent.google.com/download?id=<ID>&export=download&confirm=t`.
 *  - **Dropbox** — `https://www.dropbox.com/s/<token>/file.mp4?dl=0` →
 *    `https://dl.dropboxusercontent.com/s/<token>/file.mp4?raw=1` (Dropbox's `?dl=0` page is HTML).
 *  - **GitHub blob** — `https://github.com/<user>/<repo>/blob/<ref>/<path>` →
 *    `https://raw.githubusercontent.com/<user>/<repo>/<ref>/<path>` so the raw bytes are served.
 *  - **OneDrive `embed`/`view`** — best-effort swap to `download` action so the response is the
 *    actual file rather than an HTML embed page.
 */
object DirectUrlHelper {

    fun isShareUrl(url: String): Boolean {
        if (url.isBlank()) return false
        return GoogleDriveUrlHelper.isDriveUrl(url) ||
            url.contains("dropbox.com", ignoreCase = true) ||
            url.contains("dl.dropboxusercontent.com", ignoreCase = true) ||
            url.contains("github.com", ignoreCase = true) ||
            url.contains("1drv.ms", ignoreCase = true) ||
            url.contains("onedrive.live.com", ignoreCase = true)
    }

    /** Converts a share URL into a direct-stream URL. Returns the input unchanged when no rule applies. */
    fun convert(url: String): String {
        if (url.isBlank()) return url
        return when {
            GoogleDriveUrlHelper.isDriveUrl(url) -> GoogleDriveUrlHelper.convert(url)
            url.contains("dropbox.com", ignoreCase = true) -> convertDropbox(url)
            url.contains("github.com", ignoreCase = true) -> convertGitHubBlob(url)
            url.contains("onedrive.live.com", ignoreCase = true) -> convertOneDrive(url)
            else -> url
        }
    }

    private fun convertDropbox(url: String): String {
        // Already a direct-content host — leave alone.
        if (url.contains("dl.dropboxusercontent.com", ignoreCase = true)) return url
        // Strip any existing dl= / raw= flags then force raw=1.
        val withoutFlag = url.replace(Regex("([?&])(dl|raw)=[^&]*"), "$1").trimEnd('?', '&')
        val direct = withoutFlag.replace(
            "www.dropbox.com",
            "dl.dropboxusercontent.com",
            ignoreCase = true,
        )
        val sep = if (direct.contains('?')) "&" else "?"
        return "$direct${sep}raw=1"
    }

    private fun convertGitHubBlob(url: String): String {
        // Already raw content — leave alone.
        if (url.contains("raw.githubusercontent.com", ignoreCase = true)) return url
        val match = GITHUB_BLOB_REGEX.find(url) ?: return url
        val (user, repo, ref, path) = match.destructured
        return "https://raw.githubusercontent.com/$user/$repo/$ref/$path"
    }

    private fun convertOneDrive(url: String): String {
        // OneDrive web URLs have several shapes. The most common share format embeds a `resid` &
        // `authkey`; swapping `embed`/`view` for `download` works for personal accounts.
        return url
            .replace("action=embed", "action=download", ignoreCase = true)
            .replace("action=view", "action=download", ignoreCase = true)
    }

    private val GITHUB_BLOB_REGEX = Regex(
        """https?://github\.com/([^/]+)/([^/]+)/blob/([^/]+)/(.+)""",
        RegexOption.IGNORE_CASE,
    )
}
