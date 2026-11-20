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
import androidx.compose.ui.unit.dp
import com.example.newtv.R
import com.example.newtv.ui.MainUiState
import com.example.newtv.ui.RuleField
import com.example.newtv.ui.Spacing
import com.example.newtv.ui.Steps
import com.example.newtv.ui.components.HelpLine
import com.example.newtv.ui.components.NostalgiaButton
import com.example.newtv.ui.components.NostalgiaWindow
import com.example.newtv.ui.components.OsdBanner
import com.example.newtv.ui.components.OsdTone
import com.example.newtv.ui.components.ScreenScaffold
import com.example.newtv.ui.components.StepperRow
import com.example.newtv.ui.components.rememberInitialFocus
import com.example.newtv.ui.formatMinuteOfDay

/** Cable-box style setup menu for a channel's schedule rule. */
@Composable
fun RuleEditorScreen(
    uiState: MainUiState,
    onStep: (RuleField, Int) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val editor = uiState.ruleEditor ?: return
    val firstFocus = rememberInitialFocus()
    ScreenScaffold(kidSafe = uiState.kidSafeEnabled, spacing = Spacing.S) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            NostalgiaWindow(
                title = stringResource(R.string.rule_editor_title),
                modifier = Modifier.width(MenuWidth),
                contentSpacing = Spacing.S,
                scrollable = true,
                footer = {
                    if (editor.error != null) OsdBanner(text = editor.error, tone = OsdTone.Error)
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.M)) {
                        NostalgiaButton(text = stringResource(R.string.save), onClick = onSave)
                        NostalgiaButton(text = stringResource(R.string.cancel), onClick = onCancel, secondary = true)
                    }
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.S)) {
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
                }
                if (editor.error == null) HelpLine(stringResource(R.string.rule_editor_help))
            }
        }
    }
}

/** Width of the centred setup-menu window on a 960 dp wide TV. */
internal val MenuWidth = 760.dp
