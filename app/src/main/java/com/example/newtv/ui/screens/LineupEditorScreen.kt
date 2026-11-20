package com.example.newtv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import com.example.newtv.R
import com.example.newtv.ui.MainUiState
import com.example.newtv.ui.Spacing
import com.example.newtv.ui.components.HelpLine
import com.example.newtv.ui.components.NostalgiaButton
import com.example.newtv.ui.components.NostalgiaWindow
import com.example.newtv.ui.components.OsdBanner
import com.example.newtv.ui.components.OsdTone
import com.example.newtv.ui.components.ScreenScaffold
import com.example.newtv.ui.components.ToggleRow
import com.example.newtv.ui.components.rememberInitialFocus

/** Cable-box style setup menu: switch library shows ON or OFF for one channel. */
@Composable
fun LineupEditorScreen(
    uiState: MainUiState,
    onToggle: (String) -> Unit,
    onNewShow: () -> Unit,
    onEditShow: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val editor = uiState.lineupEditor ?: return
    val channelName = uiState.channels.firstOrNull { it.id == editor.channelId }?.name
    val firstFocus = rememberInitialFocus()
    ScreenScaffold(kidSafe = uiState.kidSafeEnabled, spacing = Spacing.S) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            NostalgiaWindow(
                title = channelName?.let { "${stringResource(R.string.lineup_editor_title)} • $it" }
                    ?: stringResource(R.string.lineup_editor_title),
                modifier = Modifier.width(MenuWidth),
                contentSpacing = Spacing.S
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.M)) {
                    NostalgiaButton(
                        text = stringResource(R.string.save),
                        onClick = onSave,
                        modifier = Modifier.focusRequester(firstFocus)
                    )
                    NostalgiaButton(text = stringResource(R.string.new_show), onClick = onNewShow, secondary = true)
                    NostalgiaButton(text = stringResource(R.string.cancel), onClick = onCancel, secondary = true)
                }
                if (editor.error != null) {
                    OsdBanner(text = editor.error, tone = OsdTone.Error)
                } else {
                    HelpLine(stringResource(R.string.lineup_editor_help))
                }
                if (editor.shows.isEmpty()) {
                    HelpLine(stringResource(R.string.lineup_empty_library))
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(Spacing.S)
                    ) {
                        items(editor.shows, key = { it.id }) { show ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.M)
                            ) {
                                Box(Modifier.weight(1f)) {
                                    ToggleRow(
                                        label = stringResource(
                                            R.string.lineup_show_label,
                                            show.title,
                                            show.provider.displayName,
                                            show.ageRating
                                        ),
                                        valueText = stringResource(
                                            if (show.id in editor.selected) R.string.on else R.string.off
                                        ),
                                        onClick = { onToggle(show.id) }
                                    )
                                }
                                NostalgiaButton(
                                    text = stringResource(R.string.edit_show),
                                    onClick = { onEditShow(show.id) },
                                    secondary = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
