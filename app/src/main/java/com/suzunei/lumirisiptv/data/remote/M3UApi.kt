package com.suzunei.lumirisiptv.data.remote

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

interface M3UApi {

    /**
     * Fetches the raw M3U body as a String. Use `Cache-Control: no-cache` when refreshing
     * to bypass any intermediate caches (CDN + OkHttp).
     */
    @GET
    suspend fun fetchPlaylist(
        @Url url: String,
        @Header("Cache-Control") cacheControl: String = "no-cache",
    ): String
}
