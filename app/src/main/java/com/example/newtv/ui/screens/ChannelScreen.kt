package com.example.newtv.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.newtv.ui.MainUiState
import com.example.newtv.ui.components.NostalgiaButton
import com.example.newtv.ui.components.NostalgiaWindow
import com.example.newtv.ui.components.TileCard
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ChannelScreen(
    uiState: MainUiState,
    channelId: String,
    onBack: () -> Unit,
    onOpenSchedule: (String) -> Unit,
    onPlayNow: (Activity, String) -> Unit,
    onEditRules: (String) -> Unit,
    onEditChannel: (String) -> Unit,
    onInstallProvider: (Activity) -> Unit,
    onDismissMessage: () -> Unit
) {
    val activity = LocalContext.current as? Activity
    val program = uiState.currentProgram
    val formatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    NostalgiaWindow(
        title = "Now Playing",
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        if (uiState.message != null) {
            Text(text = uiState.message)
            NostalgiaButton(text = "Dismiss", onClick = onDismissMessage)
        }
        if (program != null) {
            val startLabel = formatter.format(program.startTime.atZone(ZoneId.systemDefault()))
            val endLabel = formatter.format(program.endTime.atZone(ZoneId.systemDefault()))
            Text(text = "$startLabel – $endLabel")
            Text(text = "${program.show.title} • S${program.season}E${program.episode}")
            Text(text = "Provider: ${program.show.provider.displayName}")
            if (uiState.providerAvailable == false && activity != null) {
                NostalgiaButton(
                    text = "Install ${program.show.provider.displayName}",
                    onClick = { onInstallProvider(activity) }
                )
            }
        } else {
            Text(text = "No program available")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NostalgiaButton(
                text = "Play now",
                onClick = {
                    if (activity != null) {
                        onPlayNow(activity, channelId)
                    }
                }
            )
            NostalgiaButton(text = "Schedule", onClick = { onOpenSchedule(channelId) })
            NostalgiaButton(text = "Edit rules", onClick = { onEditRules(channelId) })
            NostalgiaButton(text = "Edit channel", onClick = { onEditChannel(channelId) })
            NostalgiaButton(text = "Back", onClick = onBack)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Up next", style = MaterialTheme.typography.titleLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(uiState.schedule) { item ->
                val timeLabel = formatter.format(item.startTime.atZone(ZoneId.systemDefault()))
                TileCard(
                    title = item.show.title,
                    subtitle = "$timeLabel • S${item.season}E${item.episode}",
                    onClick = {},
                    width = 240.dp,
                    height = 140.dp,
                    accentSeed = "${item.show.id}-${item.episode}"
                )
            }
        }
    }
}
