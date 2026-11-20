package com.example.newtv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.newtv.R
import com.example.newtv.security.KidSafePinManager
import com.example.newtv.ui.components.ChannelChangeOverlay
import com.example.newtv.ui.components.NostalgiaBackground
import com.example.newtv.ui.components.NostalgiaWindow
import com.example.newtv.ui.components.ParentalLockPad
import com.example.newtv.ui.screens.ChannelEditorScreen
import com.example.newtv.ui.screens.ChannelScreen
import com.example.newtv.ui.screens.HomeScreen
import com.example.newtv.ui.screens.LineupEditorScreen
import com.example.newtv.ui.screens.ShowEditorScreen
import com.example.newtv.ui.screens.RuleEditorScreen
import com.example.newtv.ui.screens.ScheduleScreen

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val pinOpen = uiState.pinDialog != null

    // Back: cancel the PIN pad first, then pop the stack; at the Home root the system handles it.
    BackHandler(enabled = pinOpen || uiState.backStack.size > 1) {
        if (pinOpen) viewModel.dismissPinDialog() else viewModel.navigateBack()
    }

    NewTvTheme {
        NostalgiaBackground {
            Box(Modifier.fillMaxSize()) {
                // Screens are not composed while the PIN pad is up so focus cannot leak under the
                // scrim; they restore their own focus when the pad closes.
                if (!pinOpen) {
                    ScreenContent(uiState, viewModel)
                }

                uiState.pinDialog?.let { dialog ->
                    PinOverlay(
                        state = dialog,
                        onDigit = viewModel::pinDigit,
                        onBackspace = viewModel::pinBackspace,
                        onSubmit = viewModel::submitPin,
                        onCancel = viewModel::dismissPinDialog
                    )
                }

                ChannelChangeOverlay(trigger = uiState.channelChangeCount)
            }
        }
    }
}

@Composable
private fun ScreenContent(uiState: MainUiState, viewModel: MainViewModel) {
    when (val screen = uiState.screen) {
        Screen.Home -> HomeScreen(
            uiState = uiState,
            onChannelSelected = viewModel::openChannel,
            onChannelFocused = viewModel::onChannelFocused,
            onEnableKidSafe = viewModel::requestEnableKidSafe,
            onExitKidSafe = viewModel::requestExitKidSafe,
            onAddChannel = { viewModel.openChannelEditor() },
            onParentSettings = { viewModel.requestParentAction(ParentAction.AddChannel) }
        )
        is Screen.Channel -> ChannelScreen(
            uiState = uiState,
            channelId = screen.channelId,
            onBack = { viewModel.navigateBack() },
            onOpenSchedule = viewModel::openSchedule,
            onPlayNow = viewModel::playNow,
            onParentSettings = { viewModel.requestParentAction(ParentAction.EditChannel(it)) },
            onEditRules = viewModel::openRuleEditor,
            onEditChannel = viewModel::openChannelEditor,
            onEditLineup = viewModel::openLineupEditor,
            onInstallProvider = viewModel::openProviderInstall,
            onDismissMessage = viewModel::dismissMessage,
            onChannelUp = viewModel::channelUp,
            onChannelDown = viewModel::channelDown
        )
        is Screen.Schedule -> ScheduleScreen(
            uiState = uiState,
            channelId = screen.channelId,
            onBack = { viewModel.navigateBack() }
        )
        is Screen.RuleEditor -> RuleEditorScreen(
            uiState = uiState,
            onStep = viewModel::stepRuleEditor,
            onSave = viewModel::saveRuleEditor,
            onCancel = viewModel::cancelRuleEditor
        )
        is Screen.LineupEditor -> LineupEditorScreen(
            uiState = uiState,
            onToggle = viewModel::toggleLineupShow,
            onNewShow = { viewModel.openShowEditor(null) },
            onEditShow = viewModel::openShowEditor,
            onSave = viewModel::saveLineup,
            onCancel = viewModel::cancelLineupEditor
        )
        is Screen.ShowEditor -> ShowEditorScreen(
            uiState = uiState,
            onTitleChange = viewModel::updateShowTitle,
            onContentIdChange = viewModel::updateShowContentId,
            onStepProvider = viewModel::stepShowProvider,
            onStepSeasons = viewModel::stepShowSeasons,
            onStepEpisodes = viewModel::stepShowEpisodes,
            onStepAgeRating = viewModel::stepShowAgeRating,
            onStepLink = viewModel::stepShowLink,
            onSave = viewModel::saveShowEditor,
            onCancel = viewModel::cancelShowEditor,
            onDelete = viewModel::requestDeleteShow
        )
        Screen.ChannelEditor -> ChannelEditorScreen(
            uiState = uiState,
            onNameChange = viewModel::updateChannelName,
            onStepAgeRating = viewModel::stepChannelAgeRating,
            onToggleKidSafe = viewModel::toggleChannelKidSafeOnly,
            onEditRules = viewModel::openRuleEditor,
            onEditLineup = viewModel::openLineupEditor,
            onSave = viewModel::saveChannelEditor,
            onCancel = viewModel::cancelChannelEditor
        )
    }
}

private val PinWindowWidth = 820.dp

/** Full-screen scrim with the parental lock keypad on a setup-menu window. */
@Composable
internal fun PinOverlay(
    state: PinDialogState,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit
) {
    val creating = state.mode == PinDialogMode.CREATE
    val title: String
    val message: String
    when {
        creating && state.confirming -> {
            title = stringResource(R.string.pin_confirm_title)
            message = stringResource(R.string.pin_confirm_message)
        }
        creating -> {
            title = stringResource(R.string.pin_create_title)
            message = stringResource(
                R.string.pin_create_message, KidSafePinManager.MIN_LENGTH, KidSafePinManager.MAX_LENGTH
            )
        }
        else -> {
            title = stringResource(R.string.pin_title)
            message = stringResource(
                if (state.pendingAction != null) R.string.pin_parent_message else R.string.pin_exit_message
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .padding(horizontal = Spacing.SafeHorizontal, vertical = Spacing.SafeVertical),
        contentAlignment = Alignment.Center
    ) {
        NostalgiaWindow(
            title = stringResource(R.string.pin_window_title),
            modifier = Modifier.width(PinWindowWidth)
        ) {
            ParentalLockPad(
                title = title,
                message = message,
                digits = state.digits.length,
                maxDigits = KidSafePinManager.MAX_LENGTH,
                error = state.error,
                busy = state.busy,
                onDigit = onDigit,
                onBackspace = onBackspace,
                onSubmit = onSubmit,
                onCancel = onCancel
            )
        }
    }
}
