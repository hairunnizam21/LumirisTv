package com.suzunei.lumirisiptv.data.repository

import com.suzunei.lumirisiptv.data.parser.M3UParser
import com.suzunei.lumirisiptv.data.remote.M3UApi
import com.suzunei.lumirisiptv.data.remote.NetworkModule
import com.suzunei.lumirisiptv.domain.model.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlaylistRepository(
    private val api: M3UApi = NetworkModule.m3uApi,
    private val parser: M3UParser = M3UParser(),
) {

    /**
     * Fetches the playlist from [url] and parses it into categories. When [forceRefresh] is true
     * the `Cache-Control: no-cache` header is sent so the GitHub raw response is fetched fresh.
     */
    suspend fun loadPlaylist(
        url: String = DEFAULT_PLAYLIST_URL,
        forceRefresh: Boolean = false,
    ): List<Category> = withContext(Dispatchers.IO) {
        val cacheControl = if (forceRefresh) "no-cache, no-store, max-age=0" else "no-cache"
        val body = api.fetchPlaylist(url = url, cacheControl = cacheControl)
        parser.parse(body)
    }

    companion object {
        const val DEFAULT_PLAYLIST_URL =
            "https://raw.githubusercontent.com/hairunnizam21/myiptv-playlist/refs/heads/main/animedantv.m3u"
    }
}
