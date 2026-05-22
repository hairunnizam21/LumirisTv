package com.suzunei.lumirisiptv.domain.model

/**
 * A logical grouping of channels (e.g. "TV Malaysia", "Anime Latest", "Movie").
 * Order is preserved from the M3U file.
 */
data class Category(
    val name: String,
    val channels: List<Channel>,
)
