package com.example.newtv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.newtv.R
import com.example.newtv.engine.Program
import com.example.newtv.ui.MainUiState
import com.example.newtv.ui.components.NostalgiaButton
import com.example.newtv.ui.components.NostalgiaPanel
import com.example.newtv.ui.components.NostalgiaWindow
import com.example.newtv.ui.components.OsdBanner
import com.example.newtv.ui.components.TileRow
import com.example.newtv.ui.components.rememberInitialFocus
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ScheduleScreen(
    uiState: MainUiState,
    onBack: () -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    var selected by remember { mutableStateOf<Program?>(null) }
    val firstFocus = rememberInitialFocus()
    NostalgiaWindow(
        title = stringResource(R.string.schedule_title),
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        NostalgiaPanel(height = 360.dp) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(uiState.schedule) { index, item ->
                    val timeLabel = formatter.format(item.startTime.atZone(ZoneId.systemDefault()))
                    TileRow(
                        title = item.show.title,
                        subtitle = stringResource(
                            R.string.program_time_episode, timeLabel, item.season, item.episode
                        ),
                        accentSeed = item.show.id,
                        onClick = { selected = item },
                        modifier = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier
                    )
                }
            }
        }
        selected?.let { program ->
            OsdBanner(
                text = stringResource(
                    R.string.program_airs_at,
                    program.show.title,
                    program.season,
                    program.episode,
                    formatter.format(program.startTime.atZone(ZoneId.systemDefault())),
                    program.channel.number
                )
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        NostalgiaButton(text = stringResource(R.string.back), onClick = onBack)
    }
}
