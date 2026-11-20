package com.example.newtv.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.newtv.data.AppDatabase
import com.example.newtv.data.ChannelEntity
import com.example.newtv.data.ChannelPalette
import com.example.newtv.data.RotationMode
import com.example.newtv.data.ScheduleRuleEntity
import com.example.newtv.data.TvRepository
import com.example.newtv.engine.Program
import com.example.newtv.engine.ProgrammingEngine
import com.example.newtv.playback.PlaybackOrchestrator
import com.example.newtv.resolver.ContentResolver
import com.example.newtv.security.KidSafePinManager
import com.example.newtv.security.KidSafePrefs
import com.example.newtv.security.PinCheck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainUiState(
    val screen: Screen = Screen.Home,
    val channels: List<ChannelEntity> = emptyList(),
    val currentProgram: Program? = null,
    val schedule: List<Program> = emptyList(),
    val nowPlaying: Map<String, Program> = emptyMap(),
    val kidSafeEnabled: Boolean = false,
    val providerAvailable: Boolean? = null,
    val message: String? = null,
    val ruleEditor: RuleEditorState? = null,
    val channelEditor: ChannelEditorState? = null,
    val pinDialog: PinDialogState? = null
)

enum class PinDialogMode { CREATE, VERIFY }

data class PinDialogState(
    val mode: PinDialogMode,
    val error: String? = null,
    val busy: Boolean = false
)

sealed class Screen {
    data object Home : Screen()
    data class Channel(val channelId: String) : Screen()
    data class Schedule(val channelId: String) : Screen()
    data class RuleEditor(val channelId: String) : Screen()
    data object ChannelEditor : Screen()
}

data class RuleEditorState(
    val channelId: String,
    val startMinute: String,
    val endMinute: String,
    val noRepeatWindow: String,
    val slotMinutes: String,
    val error: String? = null
)

data class ChannelEditorState(
    val channelId: String?,
    val name: String,
    val ageRating: String,
    val kidSafeOnly: Boolean,
    val isNew: Boolean,
    val error: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TvRepository(AppDatabase.getInstance(application))
    private val resolver = ContentResolver(application)
    private val engine = ProgrammingEngine(repository, resolver)
    private val orchestrator = PlaybackOrchestrator(application)
    private val kidSafePrefs = KidSafePrefs(application)
    private val pinManager = KidSafePinManager(kidSafePrefs)

    private val _uiState = MutableStateFlow(MainUiState(kidSafeEnabled = kidSafePrefs.kidSafeEnabled))
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    private var allChannels: List<ChannelEntity> = emptyList()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.seedIfEmpty()
        }
        viewModelScope.launch {
            repository.observeChannels().collectLatest { channels ->
                allChannels = channels
                updateChannelList()
                refreshNowPlaying()
            }
        }
    }

    fun openChannel(channelId: String) {
        _uiState.update { it.copy(screen = Screen.Channel(channelId)) }
        refreshChannel(channelId)
    }

    fun openSchedule(channelId: String) {
        _uiState.update { it.copy(screen = Screen.Schedule(channelId)) }
        refreshChannel(channelId)
    }

    fun openRuleEditor(channelId: String) {
        viewModelScope.launch {
            val rule = repository.getRules(channelId).firstOrNull() ?: defaultRule(channelId)
            _uiState.update {
                it.copy(
                    screen = Screen.RuleEditor(channelId),
                    ruleEditor = RuleEditorState(
                        channelId = channelId,
                        startMinute = rule.startMinute.toString(),
                        endMinute = rule.endMinute.toString(),
                        noRepeatWindow = rule.noRepeatWindow.toString(),
                        slotMinutes = rule.slotMinutes.toString()
                    )
                )
            }
        }
    }

    fun openChannelEditor(channelId: String? = null) {
        viewModelScope.launch {
            val channel = channelId?.let { repository.getChannel(it) }
            _uiState.update {
                it.copy(
                    screen = Screen.ChannelEditor,
                    channelEditor = ChannelEditorState(
                        channelId = channel?.id,
                        name = channel?.name.orEmpty(),
                        ageRating = channel?.ageRating?.toString() ?: "7",
                        kidSafeOnly = channel?.kidSafeOnly ?: false,
                        isNew = channel == null
                    )
                )
            }
        }
    }

    fun updateChannelEditor(
        name: String? = null,
        ageRating: String? = null,
        kidSafeOnly: Boolean? = null
    ) {
        _uiState.update { state ->
            val editor = state.channelEditor ?: return@update state
            state.copy(
                channelEditor = editor.copy(
                    name = name ?: editor.name,
                    ageRating = ageRating ?: editor.ageRating,
                    kidSafeOnly = kidSafeOnly ?: editor.kidSafeOnly,
                    error = null
                )
            )
        }
    }

    fun saveChannelEditor() {
        val editor = _uiState.value.channelEditor ?: return
        val ageRating = editor.ageRating.toIntOrNull()
        val error = when {
            editor.name.isBlank() -> "Channel name is required."
            ageRating == null || ageRating !in 0..21 -> "Age rating must be 0–21."
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(channelEditor = editor.copy(error = error)) }
            return
        }

        viewModelScope.launch {
            val resolvedId = editor.channelId ?: generateChannelId(editor.name)
            val existing = editor.channelId?.let { repository.getChannel(it) }
            val number = existing?.number?.takeIf { it > 0 }
                ?: (repository.getMaxChannelNumber() + 1)
            val accent = existing?.accentColor ?: ChannelPalette.forNumber(number)
            val channel = ChannelEntity(
                id = resolvedId,
                name = editor.name.trim(),
                ageRating = ageRating ?: 0,
                kidSafeOnly = editor.kidSafeOnly,
                number = number,
                accentColor = accent
            )
            repository.upsertChannel(channel)
            val existingRule = repository.getRules(resolvedId).firstOrNull()
            if (existingRule == null) {
                repository.upsertRule(defaultRule(resolvedId))
            }
            _uiState.update { it.copy(screen = Screen.Channel(resolvedId), channelEditor = null) }
            refreshChannel(resolvedId)
        }
    }

    fun cancelChannelEditor() {
        val channelId = _uiState.value.channelEditor?.channelId
        _uiState.update {
            it.copy(
                screen = channelId?.let { id -> Screen.Channel(id) } ?: Screen.Home,
                channelEditor = null
            )
        }
        if (channelId != null) {
            refreshChannel(channelId)
        }
    }

    fun updateRuleEditor(
        startMinute: String? = null,
        endMinute: String? = null,
        noRepeatWindow: String? = null,
        slotMinutes: String? = null
    ) {
        _uiState.update { state ->
            val editor = state.ruleEditor ?: return@update state
            state.copy(
                ruleEditor = editor.copy(
                    startMinute = startMinute ?: editor.startMinute,
                    endMinute = endMinute ?: editor.endMinute,
                    noRepeatWindow = noRepeatWindow ?: editor.noRepeatWindow,
                    slotMinutes = slotMinutes ?: editor.slotMinutes,
                    error = null
                )
            )
        }
    }

    fun saveRuleEditor() {
        val editor = _uiState.value.ruleEditor ?: return
        val startMinute = editor.startMinute.toIntOrNull()
        val endMinute = editor.endMinute.toIntOrNull()
        val noRepeat = editor.noRepeatWindow.toIntOrNull()
        val slotMinutes = editor.slotMinutes.toIntOrNull()

        val error = when {
            startMinute == null || endMinute == null || noRepeat == null || slotMinutes == null ->
                "Please enter valid numbers."
            startMinute !in 0..1440 || endMinute !in 0..1440 ->
                "Start and end minutes must be 0–1440."
            noRepeat < 1 -> "No-repeat window must be at least 1."
            slotMinutes !in 5..240 ->
                "Slot minutes must be between 5 and 240."
            else -> null
        }

        if (error != null) {
            _uiState.update { it.copy(ruleEditor = editor.copy(error = error)) }
            return
        }

        viewModelScope.launch {
            repository.upsertRule(
                ScheduleRuleEntity(
                    id = "${editor.channelId}_default",
                    channelId = editor.channelId,
                    startMinute = startMinute ?: 0,
                    endMinute = endMinute ?: 0,
                    noRepeatWindow = noRepeat ?: 0,
                    slotMinutes = slotMinutes ?: 0,
                    rotationMode = RotationMode.ROUND_ROBIN
                )
            )
            _uiState.update { it.copy(screen = Screen.Channel(editor.channelId)) }
            refreshChannel(editor.channelId)
        }
    }

    fun cancelRuleEditor() {
        val channelId = _uiState.value.ruleEditor?.channelId ?: return
        _uiState.update { it.copy(screen = Screen.Channel(channelId), ruleEditor = null) }
        refreshChannel(channelId)
    }

    fun backToHome() {
        _uiState.update { it.copy(screen = Screen.Home) }
        refreshNowPlaying()
    }

    /** Enables kid-safe mode, asking the parent to create a PIN first if none exists. */
    fun requestEnableKidSafe() {
        if (pinManager.hasPin()) {
            setKidSafe(true)
        } else {
            _uiState.update { it.copy(pinDialog = PinDialogState(PinDialogMode.CREATE)) }
        }
    }

    fun requestExitKidSafe() {
        _uiState.update { it.copy(pinDialog = PinDialogState(PinDialogMode.VERIFY)) }
    }

    fun dismissPinDialog() {
        if (_uiState.value.pinDialog?.busy == true) return
        _uiState.update { it.copy(pinDialog = null) }
    }

    fun submitPin(pin: String, confirmation: String) {
        val dialog = _uiState.value.pinDialog ?: return
        if (dialog.busy) return
        when (dialog.mode) {
            PinDialogMode.CREATE -> createPin(pin, confirmation)
            PinDialogMode.VERIFY -> verifyPin(pin)
        }
    }

    private fun createPin(pin: String, confirmation: String) {
        val error = when {
            !KidSafePinManager.isValidPin(pin) ->
                "PIN must be ${KidSafePinManager.MIN_LENGTH}–${KidSafePinManager.MAX_LENGTH} digits."
            pin != confirmation -> "PINs don't match."
            else -> null
        }
        if (error != null) {
            setPinDialogError(error)
            return
        }
        setPinDialogBusy()
        viewModelScope.launch {
            withContext(Dispatchers.Default) { pinManager.setPin(pin) }
            _uiState.update { it.copy(pinDialog = null) }
            setKidSafe(true)
        }
    }

    private fun verifyPin(pin: String) {
        setPinDialogBusy()
        viewModelScope.launch {
            when (val result = withContext(Dispatchers.Default) { pinManager.verify(pin) }) {
                PinCheck.Success, PinCheck.NoPinSet -> {
                    _uiState.update { it.copy(pinDialog = null) }
                    setKidSafe(false)
                }
                is PinCheck.Incorrect -> setPinDialogError(
                    "Incorrect PIN. ${result.attemptsLeft} attempt(s) left."
                )
                is PinCheck.LockedOut -> setPinDialogError(
                    "Too many attempts. Try again in ${(result.retryInMillis + 999) / 1000} s."
                )
            }
        }
    }

    private fun setPinDialogBusy() {
        _uiState.update { it.copy(pinDialog = it.pinDialog?.copy(busy = true, error = null)) }
    }

    private fun setPinDialogError(message: String) {
        _uiState.update { it.copy(pinDialog = it.pinDialog?.copy(busy = false, error = message)) }
    }

    private fun setKidSafe(enabled: Boolean) {
        kidSafePrefs.kidSafeEnabled = enabled
        _uiState.update { it.copy(kidSafeEnabled = enabled) }
        updateChannelList()
    }

    fun playNow(activity: Activity, channelId: String) {
        viewModelScope.launch {
            val program = engine.getProgramNow(channelId) ?: return@launch
            val intent = resolver.resolveIntent(program.show, program.season, program.episode)
            if (intent != null) {
                orchestrator.startPlayback(activity, intent, program)
                engine.recordPlayed(program)
                _uiState.update { it.copy(currentProgram = program, message = null) }
            } else {
                _uiState.update {
                    it.copy(
                        message = "${program.show.provider.displayName} is not available. Install it to watch.",
                        currentProgram = program
                    )
                }
            }
        }
    }

    fun handleReturnFromPlayback(activity: Activity) {
        if (!orchestrator.shouldAutoPlay()) return
        orchestrator.consumeAutoPlayFlag()
        val channelId = orchestrator.getLastChannelId() ?: return
        viewModelScope.launch {
            val program = engine.getNextProgram(channelId) ?: return@launch
            val intent = resolver.resolveIntent(program.show, program.season, program.episode)
            if (intent != null) {
                orchestrator.startPlayback(activity, intent, program)
                engine.recordPlayed(program)
                _uiState.update { it.copy(currentProgram = program, message = null) }
            } else {
                _uiState.update {
                    it.copy(
                        message = "${program.show.provider.displayName} is not available. Install it to watch.",
                        currentProgram = program
                    )
                }
            }
        }
    }

    fun openProviderInstall(activity: Activity) {
        val provider = _uiState.value.currentProgram?.show?.provider ?: return
        val intent = resolver.buildMarketIntent(provider) ?: return
        activity.startActivity(intent)
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun refreshChannel(channelId: String) {
        viewModelScope.launch {
            val program = engine.getProgramNow(channelId)
            val schedule = engine.getSchedulePreview(channelId)
            val nowPlaying = engine.getNowPlaying(allChannels.map { it.id })
            val providerAvailable = program?.show?.provider?.let { resolver.isProviderAvailable(it) }
            _uiState.update {
                it.copy(
                    currentProgram = program,
                    schedule = schedule,
                    nowPlaying = nowPlaying,
                    providerAvailable = providerAvailable
                )
            }
        }
    }

    private fun refreshNowPlaying() {
        viewModelScope.launch {
            val nowPlaying = engine.getNowPlaying(allChannels.map { it.id })
            _uiState.update { it.copy(nowPlaying = nowPlaying) }
        }
    }

    private fun updateChannelList() {
        val kidSafe = _uiState.value.kidSafeEnabled
        val filtered = if (kidSafe) {
            allChannels.filter { it.kidSafeOnly || it.ageRating <= 7 }
        } else {
            allChannels
        }
        _uiState.update { it.copy(channels = filtered) }
    }

    private fun defaultRule(channelId: String): ScheduleRuleEntity {
        return ScheduleRuleEntity(
            id = "${channelId}_default",
            channelId = channelId,
            startMinute = 0,
            endMinute = 1440,
            noRepeatWindow = 4,
            slotMinutes = 30,
            rotationMode = RotationMode.ROUND_ROBIN
        )
    }

    private suspend fun generateChannelId(name: String): String {
        val base = name.trim().lowercase().replace("[^a-z0-9]+".toRegex(), "_").trim('_')
            .ifBlank { "channel" }
        var candidate = base
        var suffix = 2
        while (repository.getChannel(candidate) != null) {
            candidate = "${base}_$suffix"
            suffix += 1
        }
        return candidate
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
