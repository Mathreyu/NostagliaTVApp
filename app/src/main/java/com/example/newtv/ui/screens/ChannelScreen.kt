package com.example.newtv.ui.screens

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.newtv.R
import com.example.newtv.engine.Program
import com.example.newtv.ui.MainUiState
import com.example.newtv.ui.components.NostalgiaButton
import com.example.newtv.ui.components.NostalgiaWindow
import com.example.newtv.ui.components.OsdBanner
import com.example.newtv.ui.components.TileCard
import com.example.newtv.ui.components.rememberInitialFocus
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** +1 for channel up / page up, -1 for channel down / page down, null for other keys. */
internal fun channelKeyStep(keyCode: Int): Int? = when (keyCode) {
    android.view.KeyEvent.KEYCODE_CHANNEL_UP, android.view.KeyEvent.KEYCODE_PAGE_UP -> +1
    android.view.KeyEvent.KEYCODE_CHANNEL_DOWN, android.view.KeyEvent.KEYCODE_PAGE_DOWN -> -1
    else -> null
}

@Composable
fun ChannelScreen(
    uiState: MainUiState,
    channelId: String,
    onBack: () -> Unit,
    onOpenSchedule: (String) -> Unit,
    onPlayNow: (Activity, String) -> Unit,
    onParentSettings: (String) -> Unit,
    onEditRules: (String) -> Unit,
    onEditChannel: (String) -> Unit,
    onInstallProvider: (Activity) -> Unit,
    onDismissMessage: () -> Unit,
    onChannelUp: () -> Unit,
    onChannelDown: () -> Unit
) {
    val activity = LocalActivity.current
    val program = uiState.currentProgram
    val channel = uiState.channels.firstOrNull { it.id == channelId }
    val formatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    var selectedUpNext by remember(channelId) { mutableStateOf<Program?>(null) }
    val playFocus = rememberInitialFocus(key = channelId)
    NostalgiaWindow(
        title = stringResource(R.string.channel_title),
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .onPreviewKeyEvent { event ->
                val step = channelKeyStep(event.key.nativeKeyCode)
                if (step == null) {
                    false
                } else {
                    if (event.type == KeyEventType.KeyDown) {
                        if (step > 0) onChannelUp() else onChannelDown()
                    }
                    true
                }
            },
    ) {
        if (channel != null) {
            Text(
                text = stringResource(R.string.channel_heading, channel.number, channel.name),
                style = MaterialTheme.typography.titleLarge
            )
        }
        if (uiState.message != null) {
            Text(text = uiState.message)
            NostalgiaButton(text = stringResource(R.string.dismiss), onClick = onDismissMessage)
        }
        if (program != null) {
            val startLabel = formatter.format(program.startTime.atZone(ZoneId.systemDefault()))
            val endLabel = formatter.format(program.endTime.atZone(ZoneId.systemDefault()))
            Text(text = stringResource(R.string.program_time_range, startLabel, endLabel))
            Text(
                text = stringResource(
                    R.string.program_title_episode, program.show.title, program.season, program.episode
                )
            )
            Text(text = stringResource(R.string.provider_line, program.show.provider.displayName))
            if (uiState.providerAvailable == false && activity != null) {
                NostalgiaButton(
                    text = stringResource(R.string.install_provider, program.show.provider.displayName),
                    onClick = { onInstallProvider(activity) }
                )
            }
        } else {
            Text(text = stringResource(R.string.no_program))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NostalgiaButton(
                text = stringResource(R.string.play_now),
                onClick = {
                    if (activity != null) {
                        onPlayNow(activity, channelId)
                    }
                },
                modifier = Modifier.focusRequester(playFocus)
            )
            NostalgiaButton(
                text = stringResource(R.string.schedule),
                onClick = { onOpenSchedule(channelId) }
            )
            if (uiState.kidSafeEnabled) {
                NostalgiaButton(
                    text = stringResource(R.string.parent_settings),
                    onClick = { onParentSettings(channelId) }
                )
            } else {
                NostalgiaButton(
                    text = stringResource(R.string.edit_rules),
                    onClick = { onEditRules(channelId) }
                )
                NostalgiaButton(
                    text = stringResource(R.string.edit_channel),
                    onClick = { onEditChannel(channelId) }
                )
            }
            NostalgiaButton(text = stringResource(R.string.back), onClick = onBack)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(R.string.up_next), style = MaterialTheme.typography.titleLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(uiState.schedule) { item ->
                val timeLabel = formatter.format(item.startTime.atZone(ZoneId.systemDefault()))
                TileCard(
                    title = item.show.title,
                    subtitle = stringResource(
                        R.string.program_time_episode, timeLabel, item.season, item.episode
                    ),
                    onClick = { selectedUpNext = item },
                    width = 240.dp,
                    height = 140.dp,
                    accentSeed = "${item.show.id}-${item.episode}"
                )
            }
        }
        selectedUpNext?.let { next ->
            OsdBanner(
                text = stringResource(
                    R.string.program_airs_at,
                    next.show.title,
                    next.season,
                    next.episode,
                    formatter.format(next.startTime.atZone(ZoneId.systemDefault())),
                    next.channel.number
                )
            )
        }
    }
}
