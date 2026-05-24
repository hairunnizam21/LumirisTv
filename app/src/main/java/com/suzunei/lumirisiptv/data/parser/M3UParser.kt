package com.suzunei.lumirisiptv.data.parser

import com.suzunei.lumirisiptv.data.util.DirectUrlHelper
import com.suzunei.lumirisiptv.domain.model.Category
import com.suzunei.lumirisiptv.domain.model.Channel

/**
 * Parses the project's M3U playlist into ordered categories.
 *
 * Two formats are supported simultaneously:
 *  1. The project's custom format using `=== Category Name ===` delimiters. Each subsequent
 *     entry block (separated by blank lines) has the shape:
 *       Channel Name
 *       <logo URL>            (optional — may be missing or invalid)
 *       <stream URL>          (required — first detected video/manifest URL wins)
 *       <KID:KEY hex>         (optional — ClearKey DRM, used with DASH)
 *  2. Standard `#EXTM3U` / `#EXTINF` lines. The `group-title` attribute (or the most recent
 *     `=== Category ===` delimiter) is used as the category.
 *
 * Robustness rule: if an item has a valid stream URL but the logo is missing/empty/invalid,
 * the channel IS still included with `logoUrl = null` so the UI can render a placeholder.
 *
 * Google Drive share URLs are converted to direct download URLs at parse time so the player
 * can buffer them without redirects breaking.
 */
class M3UParser {

    private val drmKeyRegex = Regex("""^[a-fA-F0-9]{32}:[a-fA-F0-9]{32}$""")
    private val urlScheme = Regex("""^(https?|rtsp|rtmp)://""", RegexOption.IGNORE_CASE)
    private val imageExtensions = listOf(
        ".png", ".jpg", ".jpeg", ".webp", ".gif", ".bmp", ".svg", ".ico",
    )
    private val streamExtensions = listOf(
        ".mpd", ".m3u8", ".m3u", ".mp4", ".m4v", ".mkv", ".webm", ".ts", ".mov", ".flv",
        ".mp3", ".aac", ".flac", ".ogg", ".wav",
    )

    fun parse(raw: String): List<Category> {
        val orderedCategoryNames = mutableListOf<String>()
        val channelsByCategory = linkedMapOf<String, MutableList<Channel>>()
        var currentCategory: String? = null
        val buffer = mutableListOf<String>()
        // For #EXTINF style: the most recent EXTINF line attributes (name + logo + group-title).
        var pendingExtInf: ExtInf? = null

        fun ensureCategory(name: String) {
            if (name !in channelsByCategory) {
                channelsByCategory[name] = mutableListOf()
                orderedCategoryNames.add(name)
            }
        }

        fun flushBuffer() {
            if (buffer.isEmpty()) return
            val category = currentCategory
            val channel = parseEntry(buffer.toList(), category)
            buffer.clear()
            if (channel != null) {
                ensureCategory(channel.categoryName)
                channelsByCategory[channel.categoryName]!!.add(channel)
            }
        }

        for (rawLine in raw.lineSequence()) {
            val line = rawLine.trim().trimEnd('\u0000')
            when {
                line.isEmpty() -> {
                    flushBuffer()
                }
                isCategoryDelimiter(line) -> {
                    flushBuffer()
                    currentCategory = line.trim('=').trim().ifBlank { currentCategory }
                }
                line.startsWith("#EXTM3U", ignoreCase = true) -> {
                    // header — ignore
                }
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    flushBuffer()
                    pendingExtInf = parseExtInf(line)
                }
                line.startsWith("#KODIPROP", ignoreCase = true) ||
                    line.startsWith("#EXTVLCOPT", ignoreCase = true) -> {
                    // Pass-through: capture ClearKey-style props for the pending EXTINF entry.
                    pendingExtInf?.let { it.extras.add(line) }
                }
                line.startsWith("#") -> {
                    // any other M3U directive
                }
                pendingExtInf != null && urlScheme.containsMatchIn(line) -> {
                    val info = pendingExtInf!!
                    pendingExtInf = null
                    val cat = info.groupTitle ?: currentCategory ?: DEFAULT_CATEGORY
                    ensureCategory(cat)
                    val streamUrl = DirectUrlHelper.convert(line)
                    val drmKey = info.extras
                        .mapNotNull { extractClearKey(it) }
                        .firstOrNull()
                    val channel = Channel(
                        id = stableId(info.name, streamUrl),
                        name = info.name,
                        logoUrl = info.logo?.takeIf { isImageUrl(it) },
                        streamUrl = streamUrl,
                        categoryName = cat,
                        drmKey = drmKey,
                    )
                    channelsByCategory[cat]!!.add(channel)
                }
                else -> {
                    buffer.add(line)
                }
            }
        }
        flushBuffer()

        return orderedCategoryNames
            .map { name -> Category(name = name, channels = channelsByCategory[name].orEmpty().toList()) }
            .filter { it.channels.isNotEmpty() }
    }

    private fun parseEntry(lines: List<String>, category: String?): Channel? {
        if (lines.isEmpty()) return null
        val effectiveCategory = category ?: DEFAULT_CATEGORY

        var name: String? = null
        var logoUrl: String? = null
        var streamUrl: String? = null
        var drmKey: String? = null

        for (line in lines) {
            when {
                isClearKey(line) -> drmKey = drmKey ?: line
                isImageUrl(line) -> logoUrl = logoUrl ?: line
                isStreamUrl(line) -> streamUrl = streamUrl ?: line
                isAnyUrl(line) -> {
                    // Unknown URL — treat as stream if we don't have one, otherwise as logo if missing.
                    when {
                        streamUrl == null -> streamUrl = line
                        logoUrl == null -> logoUrl = line
                    }
                }
                else -> {
                    if (name == null) name = line
                }
            }
        }

        val finalStream = streamUrl ?: return null
        val finalName = name?.ifBlank { null } ?: "Untitled"
        val converted = DirectUrlHelper.convert(finalStream)
        return Channel(
            id = stableId(finalName, converted),
            name = finalName,
            // Robustness: keep null when logo is missing/invalid so the UI can show a placeholder.
            logoUrl = logoUrl?.takeIf { isImageUrl(it) },
            streamUrl = converted,
            categoryName = effectiveCategory,
            drmKey = drmKey,
        )
    }

    private fun isCategoryDelimiter(line: String): Boolean =
        line.startsWith("===") && line.endsWith("===") && line.length > 6

    private fun isImageUrl(line: String): Boolean {
        if (!isAnyUrl(line)) return false
        val pathOnly = line.lowercase().substringBefore('?')
        return imageExtensions.any { pathOnly.endsWith(it) || pathOnly.contains("$it/") } ||
            pathOnly.contains("/image/") || pathOnly.contains("/logo/") ||
            pathOnly.contains("/poster/") || pathOnly.contains("/img/") ||
            pathOnly.contains("image-resizer")
    }

    private fun isStreamUrl(line: String): Boolean {
        if (!isAnyUrl(line)) return false
        val lower = line.lowercase()
        val pathOnly = lower.substringBefore('?')
        val query = lower.substringAfter('?', missingDelimiterValue = "")
        if (streamExtensions.any { pathOnly.endsWith(it) || pathOnly.contains("$it/") || pathOnly.contains("$it?") }) return true
        if (DirectUrlHelper.isShareUrl(line)) return true
        if (lower.contains("/dash/") || lower.contains("/hls/") || lower.contains("manifest")) return true
        if (query.contains("format=m3u8") || query.contains("format=mpd") ||
            query.contains("type=hls") || query.contains("type=dash")) return true
        return false
    }

    private fun isAnyUrl(line: String): Boolean = urlScheme.containsMatchIn(line)

    private fun isClearKey(line: String): Boolean = drmKeyRegex.matches(line)

    private fun extractClearKey(prop: String): String? {
        // Common shapes:
        //   #KODIPROP:inputstream.adaptive.license_key=KID:KEY
        //   #KODIPROP:inputstream.adaptive.license_key={"keys":[{"kty":"oct","k":"BASE64URL","kid":"BASE64URL"}], ...}
        val eq = prop.indexOf('=')
        if (eq < 0) return null
        val value = prop.substring(eq + 1).trim()
        return if (drmKeyRegex.matches(value)) value else null
    }

    private fun stableId(name: String, streamUrl: String): String =
        "${name.lowercase()}|${streamUrl.lowercase()}"

    private data class ExtInf(
        val name: String,
        val logo: String?,
        val groupTitle: String?,
        val extras: MutableList<String> = mutableListOf(),
    )

    private fun parseExtInf(line: String): ExtInf {
        // Format: #EXTINF:-1 tvg-logo="..." group-title="..." tvg-id="...",Display Name
        val afterColon = line.substringAfter(":", missingDelimiterValue = "")
        val commaIdx = afterColon.indexOf(',')
        val (attrPart, display) = if (commaIdx >= 0) {
            afterColon.substring(0, commaIdx) to afterColon.substring(commaIdx + 1).trim()
        } else {
            afterColon to ""
        }
        val attrs = ATTR_REGEX.findAll(attrPart).associate {
            it.groupValues[1].lowercase() to it.groupValues[2]
        }
        val name = display.ifBlank { attrs["tvg-name"] ?: "Untitled" }
        val logo = attrs["tvg-logo"]?.takeIf { it.isNotBlank() }
        val group = attrs["group-title"]?.takeIf { it.isNotBlank() }
        return ExtInf(name = name, logo = logo, groupTitle = group)
    }

    companion object {
        private const val DEFAULT_CATEGORY = "Uncategorized"
        private val ATTR_REGEX = Regex("""([a-zA-Z0-9_\-]+)="([^"]*)"""")
    }
}
