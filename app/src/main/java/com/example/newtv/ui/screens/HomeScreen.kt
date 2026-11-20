package com.example.newtv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.newtv.R
import com.example.newtv.ui.MainUiState
import com.example.newtv.ui.components.NostalgiaButton
import com.example.newtv.ui.components.NostalgiaWindow
import com.example.newtv.ui.components.SectionTitle
import com.example.newtv.ui.components.TileCard
import com.example.newtv.ui.components.rememberInitialFocus
import com.example.newtv.ui.components.tryRequestFocus

@Composable
fun HomeScreen(
    uiState: MainUiState,
    onChannelSelected: (String) -> Unit,
    onChannelFocused: (String) -> Unit,
    onEnableKidSafe: () -> Unit,
    onExitKidSafe: () -> Unit,
    onAddChannel: () -> Unit,
    onParentSettings: () -> Unit
) {
    val channels = uiState.channels
    val featuredState = rememberLazyListState()
    val tileFocus = remember { FocusRequester() }
    val buttonFocus = rememberInitialFocus(enabled = channels.isEmpty())

    // Restore focus to the last focused channel (or the first one) when Home appears.
    val restoreIndex = channels.indexOfFirst { it.id == uiState.lastFocusedChannelId }.coerceAtLeast(0)
    LaunchedEffect(channels.isNotEmpty()) {
        if (channels.isNotEmpty()) {
            featuredState.scrollToItem(restoreIndex)
            withFrameNanos { }
            tileFocus.tryRequestFocus()
        }
    }

    NostalgiaWindow(
        title = stringResource(R.string.home_title),
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        SectionTitle(title = stringResource(R.string.home_featured))
        LazyRow(
            state = featuredState,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(8.dp)
        ) {
            items(channels, key = { it.id }) { channel ->
                val isRestoreTarget = channel.id == channels[restoreIndex].id
                TileCard(
                    title = channel.name,
                    subtitle = stringResource(R.string.home_on_now),
                    onClick = { onChannelSelected(channel.id) },
                    width = 360.dp,
                    height = 200.dp,
                    accentSeed = channel.id,
                    modifier = Modifier
                        .then(if (isRestoreTarget) Modifier.focusRequester(tileFocus) else Modifier)
                        .onFocusChanged { if (it.hasFocus) onChannelFocused(channel.id) }
                )
            }
        }
        SectionTitle(title = stringResource(R.string.home_channels))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(8.dp)
        ) {
            items(channels, key = { it.id }) { channel ->
                TileCard(
                    title = channel.name,
                    subtitle = stringResource(R.string.home_tune_in),
                    onClick = { onChannelSelected(channel.id) },
                    width = 300.dp,
                    height = 170.dp,
                    accentSeed = "channel-${channel.id}",
                    modifier = Modifier.onFocusChanged { if (it.hasFocus) onChannelFocused(channel.id) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (uiState.kidSafeEnabled) {
                NostalgiaButton(
                    text = stringResource(R.string.exit_kid_safe),
                    onClick = onExitKidSafe,
                    modifier = Modifier.focusRequester(buttonFocus)
                )
                NostalgiaButton(
                    text = stringResource(R.string.parent_settings),
                    onClick = onParentSettings,
                    secondary = true
                )
            } else {
                NostalgiaButton(
                    text = stringResource(R.string.enable_kid_safe),
                    onClick = onEnableKidSafe,
                    modifier = Modifier.focusRequester(buttonFocus)
                )
                NostalgiaButton(
                    text = stringResource(R.string.add_channel),
                    onClick = onAddChannel,
                    secondary = true
                )
            }
        }
    }
}
