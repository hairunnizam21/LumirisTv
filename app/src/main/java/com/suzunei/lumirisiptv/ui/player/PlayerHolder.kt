package com.suzunei.lumirisiptv.ui.player

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.suzunei.lumirisiptv.data.remote.NetworkModule
import com.suzunei.lumirisiptv.domain.model.Channel

/**
 * Thin wrapper around an ExoPlayer instance that handles lifecycle (pause on background, release
 * on destroy), shared OkHttp data source, and seamless channel switching.
 */
class PlayerHolder(context: Context) {

    private val httpDataSourceFactory = OkHttpDataSource.Factory(NetworkModule.okHttpClient)
        .setUserAgent("LumirisIPTV/1.0 (Android)")

    private val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

    private val mediaSourceFactory = DefaultMediaSourceFactory(context)
        .setDataSourceFactory(dataSourceFactory)

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(mediaSourceFactory)
        .build()
        .apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }

    private var currentMediaId: String? = null

    /** Sets [channel] as the active stream. No-ops if the same channel is already playing. */
    fun play(channel: Channel) {
        if (currentMediaId == channel.id) return
        currentMediaId = channel.id
        val item = MediaItemFactory.build(channel)
        player.setMediaItem(item)
        player.prepare()
        player.playWhenReady = true
    }

    fun onPause() {
        player.playWhenReady = false
    }

    fun onResume() {
        player.playWhenReady = true
    }

    fun release() {
        currentMediaId = null
        player.release()
    }
}
