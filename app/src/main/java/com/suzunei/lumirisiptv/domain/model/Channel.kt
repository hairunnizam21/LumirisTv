package com.suzunei.lumirisiptv.domain.model

/**
 * A single playable item in the playlist.
 *
 * @param id stable identifier derived from name + streamUrl (used for diffing across refreshes
 *           to preserve player continuity).
 * @param name human-readable channel title (e.g. "TV1", "Comic 8 2025").
 * @param logoUrl optional logo / thumbnail URL. May be null/blank/invalid — the UI MUST still
 *                render the card with a placeholder icon and remain clickable.
 * @param streamUrl normalized stream URL (Google Drive share links are pre-converted to direct
 *                  download URLs by the repository).
 * @param categoryName the category this channel belongs to (e.g. "TV Malaysia", "Movie").
 * @param drmKey optional ClearKey DRM key in `KID:KEY` hex format. When present, the player
 *               attaches a ClearKey DRM config (typically used with DASH manifests).
 */
data class Channel(
    val id: String,
    val name: String,
    val logoUrl: String?,
    val streamUrl: String,
    val categoryName: String,
    val drmKey: String? = null,
)
