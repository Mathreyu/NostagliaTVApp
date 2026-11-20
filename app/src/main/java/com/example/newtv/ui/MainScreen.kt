package com.example.newtv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.newtv.security.KidSafePinManager
import com.example.newtv.ui.components.NostalgiaBackground
import com.example.newtv.ui.screens.ChannelEditorScreen
import com.example.newtv.ui.screens.ChannelScreen
import com.example.newtv.ui.screens.HomeScreen
import com.example.newtv.ui.screens.RuleEditorScreen
import com.example.newtv.ui.screens.ScheduleScreen

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    NewTvTheme {
        NostalgiaBackground {
            when (val screen = uiState.screen) {
                Screen.Home -> HomeScreen(
                    uiState = uiState,
                    onChannelSelected = viewModel::openChannel,
                    onEnableKidSafe = viewModel::requestEnableKidSafe,
                    onExitKidSafe = viewModel::requestExitKidSafe,
                    onAddChannel = { viewModel.openChannelEditor() }
                )
                is Screen.Channel -> ChannelScreen(
                    uiState = uiState,
                    channelId = screen.channelId,
                    onBack = viewModel::backToHome,
                    onOpenSchedule = viewModel::openSchedule,
                    onPlayNow = viewModel::playNow,
                    onEditRules = viewModel::openRuleEditor,
                    onEditChannel = viewModel::openChannelEditor,
                    onInstallProvider = viewModel::openProviderInstall,
                    onDismissMessage = viewModel::dismissMessage
                )
                is Screen.Schedule -> ScheduleScreen(
                    uiState = uiState,
                    channelId = screen.channelId,
                    onBack = viewModel::openChannel
                )
                is Screen.RuleEditor -> RuleEditorScreen(
                    uiState = uiState,
                    onStartMinuteChange = { viewModel.updateRuleEditor(startMinute = it) },
                    onEndMinuteChange = { viewModel.updateRuleEditor(endMinute = it) },
                    onNoRepeatChange = { viewModel.updateRuleEditor(noRepeatWindow = it) },
                    onSlotMinutesChange = { viewModel.updateRuleEditor(slotMinutes = it) },
                    onSave = viewModel::saveRuleEditor,
                    onCancel = viewModel::cancelRuleEditor
                )
                Screen.ChannelEditor -> ChannelEditorScreen(
                    uiState = uiState,
                    onNameChange = { viewModel.updateChannelEditor(name = it) },
                    onAgeRatingChange = { viewModel.updateChannelEditor(ageRating = it) },
                    onToggleKidSafe = { viewModel.updateChannelEditor(kidSafeOnly = it) },
                    onSave = viewModel::saveChannelEditor,
                    onCancel = viewModel::cancelChannelEditor
                )
            }

            uiState.pinDialog?.let { dialog ->
                PinDialog(
                    state = dialog,
                    onSubmit = viewModel::submitPin,
                    onCancel = viewModel::dismissPinDialog
                )
            }
        }
    }
}

@Composable
private fun PinDialog(
    state: PinDialogState,
    onSubmit: (pin: String, confirmation: String) -> Unit,
    onCancel: () -> Unit
) {
    val creating = state.mode == PinDialogMode.CREATE
    var pin by remember(state.mode) { mutableStateOf("") }
    var confirmation by remember(state.mode) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(text = if (creating) "Set a parental PIN" else "Enter PIN to exit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (creating) {
                    Text(
                        text = "Choose a ${KidSafePinManager.MIN_LENGTH}–${KidSafePinManager.MAX_LENGTH} " +
                            "digit PIN. You'll need it to leave kid-safe mode."
                    )
                }
                PinField(
                    value = pin,
                    label = "PIN",
                    isError = state.error != null,
                    enabled = !state.busy,
                    onValueChange = { pin = it }
                )
                if (creating) {
                    PinField(
                        value = confirmation,
                        label = "Confirm PIN",
                        isError = state.error != null,
                        enabled = !state.busy,
                        onValueChange = { confirmation = it }
                    )
                }
                state.error?.let { Text(text = it) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(pin, confirmation) }, enabled = !state.busy) {
                Text(text = if (creating) "Save and enable" else "Exit")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = !state.busy) {
                Text(text = "Cancel")
            }
        }
    )
}

@Composable
private fun PinField(
    value: String,
    label: String,
    isError: Boolean,
    enabled: Boolean,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            onValueChange(input.filter { it in '0'..'9' }.take(KidSafePinManager.MAX_LENGTH))
        },
        label = { Text(text = label) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        isError = isError,
        enabled = enabled,
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}
