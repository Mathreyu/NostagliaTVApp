package com.example.newtv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.newtv.R
import com.example.newtv.ui.MainUiState
import com.example.newtv.ui.NostalgiaColors
import com.example.newtv.ui.components.NostalgiaButton
import com.example.newtv.ui.components.NostalgiaWindow
import com.example.newtv.ui.components.OsdBanner
import com.example.newtv.ui.components.OsdTone
import com.example.newtv.ui.components.StepperRow
import com.example.newtv.ui.components.TvTextField
import com.example.newtv.ui.components.rememberInitialFocus

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
    NostalgiaWindow(
        title = stringResource(
            if (editor.isNew) R.string.channel_editor_add_title else R.string.channel_editor_edit_title
        ),
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        TvTextField(
            label = stringResource(R.string.channel_name),
            value = editor.name,
            onValueChange = onNameChange,
            placeholder = stringResource(R.string.channel_name_placeholder),
            modifier = Modifier.focusRequester(firstFocus)
        )
        Text(
            text = stringResource(R.string.channel_number) + ": " + if (editor.number != null) {
                stringResource(R.string.channel_number_value, editor.number)
            } else {
                stringResource(R.string.channel_number_auto)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = NostalgiaColors.TextMuted
        )
        StepperRow(
            label = stringResource(R.string.age_rating),
            valueText = stringResource(R.string.age_rating_value, editor.ageRating),
            onDecrement = { onStepAgeRating(-1) },
            onIncrement = { onStepAgeRating(+1) }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NostalgiaButton(
                text = stringResource(
                    if (editor.kidSafeOnly) R.string.kid_safe_only_on else R.string.kid_safe_only_off
                ),
                onClick = onToggleKidSafe
            )
            if (editor.channelId != null) {
                NostalgiaButton(
                    text = stringResource(R.string.edit_rules),
                    onClick = { onEditRules(editor.channelId) },
                    secondary = true
                )
            }
        }
        if (editor.error != null) {
            OsdBanner(text = editor.error, tone = OsdTone.Error)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NostalgiaButton(text = stringResource(R.string.save), onClick = onSave)
            NostalgiaButton(text = stringResource(R.string.cancel), onClick = onCancel, secondary = true)
        }
    }
}
