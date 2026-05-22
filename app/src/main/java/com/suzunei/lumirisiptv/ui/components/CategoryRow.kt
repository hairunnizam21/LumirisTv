package com.suzunei.lumirisiptv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.suzunei.lumirisiptv.domain.model.Category
import com.suzunei.lumirisiptv.domain.model.Channel

@Composable
fun CategoryRow(
    category: Category,
    selectedChannelId: String?,
    onChannelClick: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = category.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            items(category.channels, key = { it.id }) { channel ->
                ChannelCard(
                    channel = channel,
                    selected = channel.id == selectedChannelId,
                    onClick = { onChannelClick(channel) },
                )
            }
        }
    }
}
