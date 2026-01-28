package com.example.newtv.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.newtv.data.AppDatabase
import com.example.newtv.data.ChannelEntity
import com.example.newtv.data.RotationMode
import com.example.newtv.data.ScheduleRuleEntity
import com.example.newtv.data.TvRepository
import com.example.newtv.engine.Program
import com.example.newtv.engine.ProgrammingEngine
import com.example.newtv.playback.PlaybackOrchestrator
import com.example.newtv.resolver.ContentResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val screen: Screen = Screen.Home,
    val channels: List<ChannelEntity> = emptyList(),
    val currentProgram: Program? = null,
    val schedule: List<Program> = emptyList(),
    val kidSafeEnabled: Boolean = false,
    val providerAvailable: Boolean? = null,
    val message: String? = null,
    val ruleEditor: RuleEditorState? = null,
    val channelEditor: ChannelEditorState? = null
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
    private val kidSafePin = "1234"

    private val _uiState = MutableStateFlow(MainUiState())
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
            val channel = ChannelEntity(
                id = resolvedId,
                name = editor.name.trim(),
                ageRating = ageRating ?: 0,
                kidSafeOnly = editor.kidSafeOnly
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
    }

    fun enableKidSafe() {
        _uiState.update { it.copy(kidSafeEnabled = true) }
        updateChannelList()
    }

    fun disableKidSafe(pin: String): Boolean {
        val allowed = pin == kidSafePin
        if (allowed) {
            _uiState.update { it.copy(kidSafeEnabled = false) }
            updateChannelList()
        }
        return allowed
    }

    fun playNow(activity: Activity, channelId: String) {
        viewModelScope.launch {
            val program = engine.getProgramNow(channelId) ?: return@launch
            val intent = resolver.resolveIntent(program.show, program.season, program.episode)
            if (intent != null) {
                orchestrator.startPlayback(activity, intent, program)
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
            val providerAvailable = program?.show?.provider?.let { resolver.isProviderAvailable(it) }
            _uiState.update {
                it.copy(
                    currentProgram = program,
                    schedule = schedule,
                    providerAvailable = providerAvailable
                )
            }
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
