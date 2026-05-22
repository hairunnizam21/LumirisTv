package com.suzunei.lumirisiptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.suzunei.lumirisiptv.domain.model.Channel

@Composable
fun ChannelCard(
    channel: Channel,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    Column(
        modifier = modifier
            .width(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            val logo = channel.logoUrl
            if (!logo.isNullOrBlank()) {
                SubcomposeAsyncImage(
                    model = logo,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                    loading = { PlaceholderIcon() },
                    error = { PlaceholderIcon() },
                )
            } else {
                PlaceholderIcon()
            }
        }
        Text(
            text = channel.name,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun PlaceholderIcon() {
    Icon(
        imageVector = Icons.Filled.Tv,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    )
}
