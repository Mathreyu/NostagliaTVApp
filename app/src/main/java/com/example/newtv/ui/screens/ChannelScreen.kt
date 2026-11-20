package com.example.newtv.ui.screens

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.newtv.R
import com.example.newtv.engine.Program
import com.example.newtv.ui.MainUiState
import com.example.newtv.ui.NostalgiaColors
import com.example.newtv.ui.NostalgiaShapes
import com.example.newtv.ui.Spacing
import com.example.newtv.ui.components.ChannelBug
import com.example.newtv.ui.components.NoSignalCard
import com.example.newtv.ui.components.NostalgiaButton
import com.example.newtv.ui.components.OsdBanner
import com.example.newtv.ui.components.OsdHeading
import com.example.newtv.ui.components.OsdTone
import com.example.newtv.ui.components.ScreenScaffold
import com.example.newtv.ui.components.TileCard
import com.example.newtv.ui.components.rememberInitialFocus
import com.example.newtv.ui.osdTextStyle
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** +1 for channel up / page up, -1 for channel down / page down, null for other keys. */
internal fun channelKeyStep(keyCode: Int): Int? = when (keyCode) {
    android.view.KeyEvent.KEYCODE_CHANNEL_UP, android.view.KeyEvent.KEYCODE_PAGE_UP -> +1
    android.view.KeyEvent.KEYCODE_CHANNEL_DOWN, android.view.KeyEvent.KEYCODE_PAGE_DOWN -> -1
    else -> null
}

/** "Tuned in" view of one channel: what is on, how far along, and what to do about it. */
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
    onChannelDown: () -> Unit,
    zone: ZoneId = ZoneId.systemDefault()
) {
    val activity = LocalActivity.current
    val program = uiState.currentProgram
    val channel = uiState.channels.firstOrNull { it.id == channelId }
    val accent = channel?.let { Color(it.accentColor) } ?: NostalgiaColors.OsdGreen
    val formatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    var selectedUpNext by remember(channelId) { mutableStateOf<Program?>(null) }
    val firstFocus = rememberInitialFocus(key = channelId)

    ScreenScaffold(
        kidSafe = uiState.kidSafeEnabled,
        modifier = Modifier.onPreviewKeyEvent { event ->
            val step = channelKeyStep(event.key.nativeKeyCode)
            if (step == null) {
                false
            } else {
                if (event.type == KeyEventType.KeyDown) {
                    if (step > 0) onChannelUp() else onChannelDown()
                }
                true
            }
        }
    ) {
        // Channel identity + change-channel hint.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.L)) {
            if (channel != null) {
                ChannelBug(number = channel.number, accent = accent, textSize = 34)
                Text(
                    text = channel.name,
                    style = osdTextStyle(36, accent),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            Text(
                text = stringResource(R.string.channel_change_hint),
                style = osdTextStyle(22, NostalgiaColors.TextMuted, glow = false),
                maxLines = 1
            )
        }

        // The body scrolls as a safety net on very small viewports; normally everything fits.
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.M)
        ) {
        val providerName = program?.show?.provider?.displayName
        val unavailable = uiState.providerAvailable == false && providerName != null
        if (uiState.message != null || unavailable) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.M)) {
                OsdBanner(
                    text = uiState.message ?: stringResource(R.string.provider_unavailable, providerName.orEmpty()),
                    tone = OsdTone.Error,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (unavailable && activity != null) {
                    NostalgiaButton(
                        text = stringResource(R.string.install_provider, providerName.orEmpty()),
                        onClick = { onInstallProvider(activity) }
                    )
                }
                if (uiState.message != null) {
                    NostalgiaButton(
                        text = stringResource(R.string.dismiss),
                        onClick = onDismissMessage,
                        secondary = true
                    )
                }
            }
        }

        if (program != null) {
            NowPlaying(program, accent, uiState, formatter, zone)
        } else {
            NoSignalCard(
                title = stringResource(R.string.no_signal),
                message = stringResource(R.string.no_signal_no_shows),
                barsWidth = 220.dp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.M)) {
            var first = true
            @Composable
            fun action(text: String, secondary: Boolean = false, onClick: () -> Unit) {
                NostalgiaButton(
                    text = text,
                    onClick = onClick,
                    secondary = secondary,
                    modifier = if (first) Modifier.focusRequester(firstFocus) else Modifier
                )
                first = false
            }
            if (program != null) {
                action(stringResource(R.string.watch_now)) {
                    if (activity != null) onPlayNow(activity, channelId)
                }
            }
            action(stringResource(R.string.guide), secondary = true) { onOpenSchedule(channelId) }
            if (uiState.kidSafeEnabled) {
                action(stringResource(R.string.parent_settings), secondary = true) { onParentSettings(channelId) }
            } else {
                action(stringResource(R.string.edit_rules), secondary = true) { onEditRules(channelId) }
                action(stringResource(R.string.edit_channel), secondary = true) { onEditChannel(channelId) }
            }
            action(stringResource(R.string.back), secondary = true, onClick = onBack)
        }

        if (uiState.schedule.isNotEmpty()) {
            OsdHeading(stringResource(R.string.up_next))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.M),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.S, vertical = Spacing.S)
            ) {
                items(uiState.schedule) { item ->
                    val timeLabel = formatter.format(item.startTime.atZone(zone))
                    TileCard(
                        title = item.show.title,
                        subtitle = stringResource(R.string.program_time_episode, timeLabel, item.season, item.episode),
                        onClick = { selectedUpNext = item },
                        width = 232.dp,
                        height = 88.dp,
                        accentSeed = "${item.show.id}-${item.episode}",
                        accent = accent
                    )
                }
            }
        }
        selectedUpNext?.let { next ->
            OsdBanner(
                text = stringResource(
                    R.string.program_airs_at,
                    next.show.title,
                    next.season,
                    next.episode,
                    formatter.format(next.startTime.atZone(zone)),
                    next.channel.number
                )
            )
        }
        }
    }
}

@Composable
private fun NowPlaying(
    program: Program,
    accent: Color,
    uiState: MainUiState,
    formatter: DateTimeFormatter,
    zone: ZoneId
) {
    val startLabel = formatter.format(program.startTime.atZone(zone))
    val endLabel = formatter.format(program.endTime.atZone(zone))
    val minutesLeft = Duration.between(uiState.now, program.endTime).toMinutes().coerceAtLeast(0)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.S)) {
        Text(
            text = program.show.title,
            style = osdTextStyle(48, NostalgiaColors.TextPrimary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.XL), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.episode_code, program.season, program.episode),
                style = osdTextStyle(32, accent)
            )
            Text(
                text = stringResource(R.string.program_time_range, startLabel, endLabel),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.provider_line, program.show.provider.displayName),
                style = MaterialTheme.typography.titleMedium,
                color = NostalgiaColors.TextMuted
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.L)) {
            Box(
                Modifier
                    .weight(1f)
                    .height(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f), NostalgiaShapes.Small)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(program.progress(uiState.now))
                        .height(8.dp)
                        .background(accent, NostalgiaShapes.Small)
                )
            }
            Text(
                text = stringResource(R.string.minutes_left, minutesLeft.toInt()),
                style = osdTextStyle(24, NostalgiaColors.TextMuted, glow = false),
                maxLines = 1
            )
        }
    }
}
