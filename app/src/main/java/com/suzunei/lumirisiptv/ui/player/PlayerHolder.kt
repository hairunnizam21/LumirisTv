package com.suzunei.lumirisiptv.ui.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import com.suzunei.lumirisiptv.data.remote.NetworkModule
import com.suzunei.lumirisiptv.domain.model.Channel

/**
 * Thin wrapper around an [ExoPlayer] instance that handles lifecycle (pause on background, release
 * on destroy), shared OkHttp data source, ClearKey DRM, and seamless channel switching.
 *
 * The player is optimized for live/IPTV usage: aggressive buffering (15 s pre-buffer, 60 s max),
 * automatic retry on transient I/O failures, and a single instance shared across channel switches
 * so seek-back and surface reuse are cheap.
 */
@OptIn(UnstableApi::class)
class PlayerHolder(context: Context) {

    private val httpDataSourceFactory = OkHttpDataSource.Factory(NetworkModule.okHttpClient)
        .setUserAgent(USER_AGENT)
        .setDefaultRequestProperties(
            mapOf(
                "Accept" to "*/*",
                "Accept-Language" to "en-US,en;q=0.9",
            ),
        )

    private val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

    private val mediaSourceFactory = DefaultMediaSourceFactory(context)
        .setDataSourceFactory(dataSourceFactory)
        .setDrmSessionManagerProvider(LumirisDrmSessionManagerProvider())
        .setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(MAX_RETRIES))

    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            /* minBufferMs */ 15_000,
            /* maxBufferMs */ 60_000,
            /* bufferForPlaybackMs */ 1_500,
            /* bufferForPlaybackAfterRebufferMs */ 4_000,
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()

    private val audioAttributes = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
        .setUsage(C.USAGE_MEDIA)
        .build()

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(mediaSourceFactory)
        .setLoadControl(loadControl)
        .setSeekBackIncrementMs(10_000L)
        .setSeekForwardIncrementMs(10_000L)
        .build()
        .apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
            setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)
            setHandleAudioBecomingNoisy(true)
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    // Transient I/O errors (e.g. CDN hiccups) — kick the player back into action.
                    if (error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                        error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                        error.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED
                    ) {
                        seekToDefaultPosition()
                        prepare()
                    }
                }
            })
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

    private companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13; LumirisIPTV) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        const val MAX_RETRIES = 5
    }
}
