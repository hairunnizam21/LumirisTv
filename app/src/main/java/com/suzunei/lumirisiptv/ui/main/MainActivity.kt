package com.suzunei.lumirisiptv.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suzunei.lumirisiptv.ui.player.PlayerHolder
import com.suzunei.lumirisiptv.ui.theme.LumirisTheme
import com.suzunei.lumirisiptv.util.applyImmersiveLandscape

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        applyImmersiveLandscape()
        setContent {
            LumirisTheme {
                MainContent(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyImmersiveLandscape()
    }
}

@Composable
private fun MainContent(viewModel: MainViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val holder = remember { PlayerHolder(context) }

    DisposableEffect(Unit) {
        onDispose { holder.release() }
    }

    LaunchedEffect(state.currentChannel?.id) {
        state.currentChannel?.let { holder.play(it) }
    }

    MainScreen(
        state = state,
        player = holder.player,
        onChannelClick = viewModel::selectChannel,
        onRefresh = viewModel::refresh,
        onErrorShown = viewModel::consumeError,
    )
}
