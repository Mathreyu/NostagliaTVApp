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
import com.example.newtv.R
import com.example.newtv.ui.MainUiState
import com.example.newtv.ui.RuleField
import com.example.newtv.ui.Steps
import com.example.newtv.ui.components.NostalgiaButton
import com.example.newtv.ui.components.NostalgiaWindow
import com.example.newtv.ui.components.OsdBanner
import com.example.newtv.ui.components.OsdTone
import com.example.newtv.ui.components.StepperRow
import com.example.newtv.ui.components.rememberInitialFocus
import com.example.newtv.ui.formatMinuteOfDay

@Composable
fun RuleEditorScreen(
    uiState: MainUiState,
    onStep: (RuleField, Int) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val editor = uiState.ruleEditor ?: return
    val firstFocus = rememberInitialFocus()
    NostalgiaWindow(
        title = stringResource(R.string.rule_editor_title),
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        StepperRow(
            label = stringResource(R.string.rule_start),
            valueText = formatMinuteOfDay(editor.startMinute),
            onDecrement = { onStep(RuleField.START, -1) },
            onIncrement = { onStep(RuleField.START, +1) },
            modifier = Modifier.focusRequester(firstFocus)
        )
        StepperRow(
            label = stringResource(R.string.rule_end),
            valueText = formatMinuteOfDay(editor.endMinute).let {
                if (editor.endMinute >= Steps.MINUTES_PER_DAY) {
                    stringResource(R.string.end_of_day_suffix, it)
                } else {
                    it
                }
            },
            onDecrement = { onStep(RuleField.END, -1) },
            onIncrement = { onStep(RuleField.END, +1) }
        )
        StepperRow(
            label = stringResource(R.string.rule_slot_length),
            valueText = stringResource(R.string.minutes_value, editor.slotMinutes),
            onDecrement = { onStep(RuleField.SLOT_MINUTES, -1) },
            onIncrement = { onStep(RuleField.SLOT_MINUTES, +1) }
        )
        StepperRow(
            label = stringResource(R.string.rule_no_repeat),
            valueText = stringResource(R.string.airings_value, editor.noRepeatWindow),
            onDecrement = { onStep(RuleField.NO_REPEAT, -1) },
            onIncrement = { onStep(RuleField.NO_REPEAT, +1) }
        )
        if (editor.error != null) {
            OsdBanner(text = editor.error, tone = OsdTone.Error)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NostalgiaButton(text = stringResource(R.string.save), onClick = onSave)
            NostalgiaButton(text = stringResource(R.string.cancel), onClick = onCancel, secondary = true)
        }
    }
}
