package com.suzunei.lumirisiptv.ui.main

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.suzunei.lumirisiptv.domain.model.Channel
import com.suzunei.lumirisiptv.ui.components.CategoryRow

@OptIn(UnstableApi::class)
@Composable
fun MainScreen(
    state: MainUiState,
    player: Player,
    onChannelClick: (Channel) -> Unit,
    onRefresh: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onErrorShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onErrorShown()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(Modifier.fillMaxSize()) {
            if (!state.isFullscreen) {
                Header(
                    title = state.currentChannel?.name.orEmpty(),
                    isRefreshing = state.isRefreshing,
                    isFullscreen = state.isFullscreen,
                    onRefresh = onRefresh,
                    onToggleFullscreen = onToggleFullscreen,
                )
            }

            // Player area: ~70% when split, 100% when fullscreen.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (state.isFullscreen) 1f else PLAYER_WEIGHT)
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = true
                            controllerAutoShow = false
                            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                            setKeepContentOnPlayerReset(true)
                            this.player = player
                        }
                    },
                    update = { it.player = player },
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(onClick = onToggleFullscreen),
                )
                if (state.isFullscreen) {
                    // Floating exit-fullscreen button on top-right.
                    IconButton(
                        onClick = onToggleFullscreen,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FullscreenExit,
                            contentDescription = "Exit fullscreen",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                if (state.isLoading) {
                    CenteredProgress("Memuatkan…")
                }
            }

            if (!state.isFullscreen) {
                ChannelList(
                    state = state,
                    onChannelClick = onChannelClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(LIST_WEIGHT),
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        )
    }
}

private const val PLAYER_WEIGHT = 2.4f
private const val LIST_WEIGHT = 1f

@Composable
private fun Header(
    title: String,
    isRefreshing: Boolean,
    isFullscreen: Boolean,
    onRefresh: () -> Unit,
    onToggleFullscreen: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Suzunei IPTV",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
        )
        if (title.isNotBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp),
            )
        }
        IconButton(onClick = onToggleFullscreen) {
            Icon(
                imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                contentDescription = if (isFullscreen) "Exit fullscreen" else "Fullscreen",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp),
                )
            } else {
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelList(
    state: MainUiState,
    onChannelClick: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        LazyColumn(
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(state.categories, key = { it.name }) { category ->
                CategoryRow(
                    category = category,
                    selectedChannelId = state.currentChannel?.id,
                    onChannelClick = onChannelClick,
                )
            }
        }
        if (state.isLoading && state.categories.isEmpty()) {
            CenteredProgress("Menyediakan saluran…")
        }
    }
}

@Composable
private fun CenteredProgress(label: String) {
    Box(Modifier.fillMaxHeight().fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp),
            )
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
