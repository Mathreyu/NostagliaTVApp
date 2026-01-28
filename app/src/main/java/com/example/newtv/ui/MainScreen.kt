package com.example.newtv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.newtv.ui.components.NostalgiaBackground
import com.example.newtv.ui.screens.ChannelEditorScreen
import com.example.newtv.ui.screens.ChannelScreen
import com.example.newtv.ui.screens.HomeScreen
import com.example.newtv.ui.screens.RuleEditorScreen
import com.example.newtv.ui.screens.ScheduleScreen

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showPinDialog by remember { mutableStateOf(false) }
    var pinValue by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    NewTvTheme {
        NostalgiaBackground {
            when (val screen = uiState.screen) {
                Screen.Home -> HomeScreen(
                    uiState = uiState,
                    onChannelSelected = viewModel::openChannel,
                    onEnableKidSafe = viewModel::enableKidSafe,
                    onExitKidSafe = { showPinDialog = true },
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

            if (showPinDialog) {
                PinDialog(
                    pinValue = pinValue,
                    pinError = pinError,
                    onPinChange = { pinValue = it },
                    onCancel = {
                        showPinDialog = false
                        pinValue = ""
                        pinError = false
                    },
                    onConfirm = {
                        val allowed = viewModel.disableKidSafe(pinValue)
                        if (allowed) {
                            showPinDialog = false
                            pinValue = ""
                            pinError = false
                        } else {
                            pinError = true
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PinDialog(
    pinValue: String,
    pinError: Boolean,
    onPinChange: (String) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(text = "Enter PIN to exit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pinValue,
                    onValueChange = onPinChange,
                    label = { Text(text = "PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = pinError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (pinError) {
                    Text(text = "Incorrect PIN. Try again.")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Exit")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(text = "Cancel")
            }
        }
    )
}
