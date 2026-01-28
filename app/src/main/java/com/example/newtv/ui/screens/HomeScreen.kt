package com.example.newtv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.newtv.ui.MainUiState
import com.example.newtv.ui.components.NostalgiaButton
import com.example.newtv.ui.components.NostalgiaWindow
import com.example.newtv.ui.components.SectionTitle
import com.example.newtv.ui.components.TileCard

@Composable
fun HomeScreen(
    uiState: MainUiState,
    onChannelSelected: (String) -> Unit,
    onEnableKidSafe: () -> Unit,
    onExitKidSafe: () -> Unit,
    onAddChannel: () -> Unit
) {
    NostalgiaWindow(
        title = "Home",
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        SectionTitle(title = "Featured")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(8.dp)) {
            items(uiState.channels) { channel ->
                TileCard(
                    title = channel.name,
                    subtitle = "On now",
                    onClick = { onChannelSelected(channel.id) },
                    width = 360.dp,
                    height = 200.dp,
                    accentSeed = channel.id
                )
            }
        }
        SectionTitle(title = "Channels")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(8.dp)) {
            items(uiState.channels) { channel ->
                TileCard(
                    title = channel.name,
                    subtitle = "Tune in",
                    onClick = { onChannelSelected(channel.id) },
                    width = 300.dp,
                    height = 170.dp,
                    accentSeed = "channel-${channel.id}"
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (uiState.kidSafeEnabled) {
                NostalgiaButton(text = "Exit kid-safe mode", onClick = onExitKidSafe)
            } else {
                NostalgiaButton(text = "Enable kid-safe mode", onClick = onEnableKidSafe)
            }
            NostalgiaButton(text = "Add channel", onClick = onAddChannel)
        }
    }
}
