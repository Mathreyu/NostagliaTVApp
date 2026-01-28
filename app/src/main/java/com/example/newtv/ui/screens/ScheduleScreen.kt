package com.example.newtv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.newtv.ui.MainUiState
import com.example.newtv.ui.components.NostalgiaButton
import com.example.newtv.ui.components.NostalgiaPanel
import com.example.newtv.ui.components.NostalgiaWindow
import com.example.newtv.ui.components.TileRow
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ScheduleScreen(
    uiState: MainUiState,
    channelId: String,
    onBack: (String) -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    NostalgiaWindow(
        title = "Schedule",
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        NostalgiaPanel(height = 360.dp) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(uiState.schedule) { item ->
                    val timeLabel = formatter.format(item.startTime.atZone(ZoneId.systemDefault()))
                    TileRow(
                        title = item.show.title,
                        subtitle = "$timeLabel • S${item.season}E${item.episode}",
                        accentSeed = item.show.id
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        NostalgiaButton(text = "Back", onClick = { onBack(channelId) })
    }
}
