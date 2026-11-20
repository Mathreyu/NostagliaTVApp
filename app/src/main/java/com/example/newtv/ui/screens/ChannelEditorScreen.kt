package com.example.newtv.ui.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
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
import com.example.newtv.ui.components.StepperRow
import com.example.newtv.ui.components.ToggleRow
import com.example.newtv.ui.components.TvTextField
import com.example.newtv.ui.components.ValueRow
import com.example.newtv.ui.components.rememberInitialFocus

/** Cable-box style setup menu for adding or editing a channel. */
@Composable
fun ChannelEditorScreen(
    uiState: MainUiState,
    onNameChange: (String) -> Unit,
    onStepAgeRating: (Int) -> Unit,
    onToggleKidSafe: () -> Unit,
    onEditRules: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val editor = uiState.channelEditor ?: return
    val firstFocus = rememberInitialFocus()
    ScreenScaffold(kidSafe = uiState.kidSafeEnabled, spacing = Spacing.S) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            NostalgiaWindow(
                title = stringResource(
                    if (editor.isNew) R.string.channel_editor_add_title else R.string.channel_editor_edit_title
                ),
                modifier = Modifier.width(MenuWidth).verticalScroll(rememberScrollState()),
                contentSpacing = Spacing.S
            ) {
                TvTextField(
                    label = stringResource(R.string.channel_name),
                    value = editor.name,
                    onValueChange = onNameChange,
                    placeholder = stringResource(R.string.channel_name_placeholder),
                    modifier = Modifier.focusRequester(firstFocus)
                )
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.S)) {
                    ValueRow(
                        label = stringResource(R.string.channel_number),
                        valueText = if (editor.number != null) {
                            stringResource(R.string.channel_number_value, editor.number)
                        } else {
                            stringResource(R.string.channel_number_auto)
                        }
                    )
                    StepperRow(
                        label = stringResource(R.string.age_rating),
                        valueText = stringResource(R.string.age_rating_value, editor.ageRating),
                        onDecrement = { onStepAgeRating(-1) },
                        onIncrement = { onStepAgeRating(+1) }
                    )
                    ToggleRow(
                        label = stringResource(R.string.kid_safe_only),
                        valueText = stringResource(if (editor.kidSafeOnly) R.string.on else R.string.off),
                        onClick = onToggleKidSafe
                    )
                }
                if (editor.error != null) {
                    OsdBanner(text = editor.error, tone = OsdTone.Error)
                } else {
                    HelpLine(stringResource(R.string.channel_editor_help))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.M)) {
                    NostalgiaButton(text = stringResource(R.string.save), onClick = onSave)
                    NostalgiaButton(text = stringResource(R.string.cancel), onClick = onCancel, secondary = true)
                    if (editor.channelId != null) {
                        NostalgiaButton(
                            text = stringResource(R.string.edit_rules),
                            onClick = { onEditRules(editor.channelId) },
                            secondary = true
                        )
                    }
                }
            }
        }
    }
}
