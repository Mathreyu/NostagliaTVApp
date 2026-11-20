package com.example.newtv.ui

import android.app.Activity
import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.newtv.R
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
import com.example.newtv.security.KidSafePolicy
import com.example.newtv.security.KidSafePrefs
import com.example.newtv.security.PinCheck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainUiState(
    /** Navigation back stack; Home is the root and the last element is the visible screen. */
    val backStack: List<Screen> = BackStack.Initial,
    /** Channels visible right now (already kid-safe filtered), ordered by number. */
    val channels: List<ChannelEntity> = emptyList(),
    val currentProgram: Program? = null,
    val schedule: List<Program> = emptyList(),
    val nowPlaying: Map<String, Program> = emptyMap(),
    val kidSafeEnabled: Boolean = false,
    val providerAvailable: Boolean? = null,
    val message: String? = null,
    val ruleEditor: RuleEditorState? = null,
    val channelEditor: ChannelEditorState? = null,
    val pinDialog: PinDialogState? = null,
    /** True while a parent has unlocked editing (PIN verified) during kid-safe mode. */
    val editingUnlocked: Boolean = false,
    val lastFocusedChannelId: String? = null,
    /** Incremented on every channel up/down so the UI can play the channel-change effect. */
    val channelChangeCount: Int = 0
) {
    val screen: Screen get() = backStack.last()
}

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
    private var channelListJob: Job? = null

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

    private fun str(@StringRes id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)

    private val kidSafe: Boolean get() = _uiState.value.kidSafeEnabled
    private val maxShowRating: Int? get() = KidSafePolicy.maxShowRating(kidSafe)

    // ---- navigation ----

    /**
     * Pushes [screen]. Editors can only be opened through [openRuleEditor] / [openChannelEditor]
     * (they need editor state), and channels hidden by kid-safe mode are refused.
     */
    fun navigate(screen: Screen) {
        if (screen.isEditor) return
        val channelId = screen.ownerChannelId
        if (channelId != null && !isChannelVisible(channelId)) return
        _uiState.update { it.withStack(BackStack.push(it.backStack, screen)) }
        afterStackChange()
    }

    /** Pops one screen. Returns false (nothing done) when already at the Home root. */
    fun navigateBack(): Boolean {
        val popped = BackStack.pop(_uiState.value.backStack) ?: return false
        _uiState.update { it.withStack(popped) }
        afterStackChange()
        return true
    }

    fun openChannel(channelId: String) = navigate(Screen.Channel(channelId))

    fun openSchedule(channelId: String) = navigate(Screen.Schedule(channelId))

    fun onChannelFocused(channelId: String) {
        if (_uiState.value.lastFocusedChannelId != channelId) {
            _uiState.update { it.copy(lastFocusedChannelId = channelId) }
        }
    }

    /** Switches to the next-higher channel number (wrapping) while on a channel screen. */
    fun channelUp() = changeChannel(+1)

    /** Switches to the next-lower channel number (wrapping) while on a channel screen. */
    fun channelDown() = changeChannel(-1)

    private fun changeChannel(step: Int) {
        val state = _uiState.value
        val current = state.screen as? Screen.Channel ?: return
        if (state.pinDialog != null) return
        val target = ChannelNavigation.adjacent(state.channels, current.channelId, step) ?: return
        if (target.id == current.channelId) return
        _uiState.update {
            it.copy(
                backStack = BackStack.replaceTop(it.backStack, Screen.Channel(target.id)),
                currentProgram = null,
                schedule = emptyList(),
                providerAvailable = null,
                message = null,
                channelChangeCount = it.channelChangeCount + 1
            )
        }
        refreshChannel(target.id)
    }

    // ---- editors (refused in kid-safe mode unless a parent unlocked them) ----

    private fun canEdit(): Boolean = !kidSafe || _uiState.value.editingUnlocked

    fun openRuleEditor(channelId: String) {
        if (!canEdit()) return
        openRuleEditorInternal(channelId, unlock = false)
    }

    fun openChannelEditor(channelId: String? = null) {
        if (!canEdit()) return
        openChannelEditorInternal(channelId, unlock = false)
    }

    private fun openRuleEditorInternal(channelId: String, unlock: Boolean) {
        viewModelScope.launch {
            val rule = repository.getRules(channelId).firstOrNull() ?: defaultRule(channelId)
            _uiState.update {
                it.withStack(BackStack.push(it.backStack, Screen.RuleEditor(channelId))).copy(
                    ruleEditor = RuleEditorState(
                        channelId = channelId,
                        startMinute = rule.startMinute,
                        endMinute = rule.endMinute,
                        noRepeatWindow = rule.noRepeatWindow,
                        slotMinutes = rule.slotMinutes
                    ),
                    editingUnlocked = it.editingUnlocked || unlock
                )
            }
        }
    }

    private fun openChannelEditorInternal(channelId: String?, unlock: Boolean) {
        viewModelScope.launch {
            val channel = channelId?.let { repository.getChannel(it) }
            _uiState.update {
                it.withStack(BackStack.push(it.backStack, Screen.ChannelEditor)).copy(
                    channelEditor = ChannelEditorState(
                        channelId = channel?.id,
                        name = channel?.name.orEmpty(),
                        ageRating = channel?.ageRating ?: 7,
                        kidSafeOnly = channel?.kidSafeOnly ?: false,
                        isNew = channel == null,
                        number = channel?.number?.takeIf { n -> n > 0 }
                    ),
                    editingUnlocked = it.editingUnlocked || unlock
                )
            }
        }
    }

    fun updateChannelName(name: String) {
        _uiState.update { state ->
            val editor = state.channelEditor ?: return@update state
            state.copy(channelEditor = editor.copy(name = name, error = null))
        }
    }

    fun stepChannelAgeRating(direction: Int) {
        _uiState.update { state ->
            val editor = state.channelEditor ?: return@update state
            state.copy(channelEditor = editor.steppedAge(direction))
        }
    }

    fun toggleChannelKidSafeOnly() {
        _uiState.update { state ->
            val editor = state.channelEditor ?: return@update state
            state.copy(channelEditor = editor.copy(kidSafeOnly = !editor.kidSafeOnly, error = null))
        }
    }

    fun saveChannelEditor() {
        if (!canEdit()) return
        val editor = _uiState.value.channelEditor ?: return
        val problem = editor.validate()
        if (problem != null) {
            val message = when (problem) {
                ChannelError.NAME_REQUIRED -> str(R.string.error_channel_name_required)
                ChannelError.AGE_RANGE -> str(R.string.error_age_rating_range)
            }
            _uiState.update { it.copy(channelEditor = editor.copy(error = message)) }
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
                ageRating = editor.ageRating,
                kidSafeOnly = editor.kidSafeOnly,
                number = number,
                accentColor = accent
            )
            repository.upsertChannel(channel)
            if (repository.getRules(resolvedId).firstOrNull() == null) {
                repository.upsertRule(defaultRule(resolvedId))
            }
            _uiState.update { state ->
                val popped = BackStack.pop(state.backStack) ?: BackStack.Initial
                // A brand-new channel opens straight away, unless kid-safe hides it (no shows yet).
                val next = if (editor.isNew && !state.kidSafeEnabled) {
                    BackStack.replaceTop(state.backStack, Screen.Channel(resolvedId))
                } else {
                    popped
                }
                state.withStack(next)
            }
            afterStackChange()
        }
    }

    fun cancelChannelEditor() {
        if (_uiState.value.screen == Screen.ChannelEditor) navigateBack()
    }

    fun stepRuleEditor(field: RuleField, direction: Int) {
        _uiState.update { state ->
            val editor = state.ruleEditor ?: return@update state
            state.copy(ruleEditor = editor.stepped(field, direction))
        }
    }

    fun saveRuleEditor() {
        if (!canEdit()) return
        val editor = _uiState.value.ruleEditor ?: return
        val problem = editor.validate()
        if (problem != null) {
            val message = when (problem) {
                RuleError.START_END_RANGE -> str(R.string.error_rule_time_range)
                RuleError.START_EQUALS_END -> str(R.string.error_rule_start_equals_end)
                RuleError.SLOT_LENGTH -> str(R.string.error_rule_slot_length)
                RuleError.NO_REPEAT_RANGE -> str(R.string.error_rule_no_repeat)
            }
            _uiState.update { it.copy(ruleEditor = editor.copy(error = message)) }
            return
        }

        viewModelScope.launch {
            repository.upsertRule(
                ScheduleRuleEntity(
                    id = "${editor.channelId}_default",
                    channelId = editor.channelId,
                    startMinute = editor.startMinute,
                    endMinute = editor.endMinute,
                    noRepeatWindow = editor.noRepeatWindow,
                    slotMinutes = editor.slotMinutes,
                    rotationMode = RotationMode.ROUND_ROBIN
                )
            )
            _uiState.update { state ->
                state.withStack(BackStack.pop(state.backStack) ?: BackStack.Initial)
            }
            afterStackChange()
        }
    }

    fun cancelRuleEditor() {
        if (_uiState.value.screen is Screen.RuleEditor) navigateBack()
    }

    // ---- parental PIN ----

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

    /**
     * Runs [action] (an editor) once the PIN is verified, staying in kid-safe mode. Without
     * kid-safe mode no PIN is needed.
     */
    fun requestParentAction(action: ParentAction) {
        if (!kidSafe) {
            runParentAction(action)
        } else {
            _uiState.update {
                it.copy(pinDialog = PinDialogState(PinDialogMode.VERIFY, pendingAction = action))
            }
        }
    }

    private fun runParentAction(action: ParentAction) {
        val unlock = kidSafe
        when (action) {
            ParentAction.AddChannel -> openChannelEditorInternal(null, unlock)
            is ParentAction.EditChannel -> openChannelEditorInternal(action.channelId, unlock)
            is ParentAction.EditRules -> openRuleEditorInternal(action.channelId, unlock)
        }
    }

    fun dismissPinDialog() {
        if (_uiState.value.pinDialog?.busy == true) return
        _uiState.update { it.copy(pinDialog = null) }
    }

    fun pinDigit(digit: Char) {
        _uiState.update { it.copy(pinDialog = it.pinDialog?.withDigit(digit)) }
    }

    fun pinBackspace() {
        _uiState.update { it.copy(pinDialog = it.pinDialog?.withBackspace()) }
    }

    fun submitPin() {
        val dialog = _uiState.value.pinDialog ?: return
        when (val result = dialog.submit()) {
            PinSubmit.Ignore -> Unit
            is PinSubmit.Problem -> {
                val message = when (result.problem) {
                    PinProblem.INVALID_LENGTH -> str(
                        R.string.error_pin_length,
                        KidSafePinManager.MIN_LENGTH,
                        KidSafePinManager.MAX_LENGTH
                    )
                    PinProblem.MISMATCH -> str(R.string.error_pin_mismatch)
                }
                _uiState.update { it.copy(pinDialog = result.state.copy(error = message)) }
            }
            is PinSubmit.Advance -> _uiState.update { it.copy(pinDialog = result.state) }
            is PinSubmit.Create -> createPin(result.pin)
            is PinSubmit.Verify -> verifyPin(result.pin)
        }
    }

    private fun createPin(pin: String) {
        setPinDialogBusy()
        viewModelScope.launch {
            withContext(Dispatchers.Default) { pinManager.setPin(pin) }
            _uiState.update { it.copy(pinDialog = null) }
            setKidSafe(true)
        }
    }

    private fun verifyPin(pin: String) {
        val action = _uiState.value.pinDialog?.pendingAction
        setPinDialogBusy()
        viewModelScope.launch {
            when (val result = withContext(Dispatchers.Default) { pinManager.verify(pin) }) {
                PinCheck.Success, PinCheck.NoPinSet -> {
                    _uiState.update { it.copy(pinDialog = null) }
                    if (action == null) setKidSafe(false) else runParentAction(action)
                }
                is PinCheck.Incorrect -> setPinDialogError(
                    str(R.string.error_pin_incorrect, result.attemptsLeft)
                )
                is PinCheck.LockedOut -> setPinDialogError(
                    str(R.string.error_pin_locked, ((result.retryInMillis + 999) / 1000).toInt())
                )
            }
        }
    }

    private fun setPinDialogBusy() {
        _uiState.update {
            it.copy(pinDialog = it.pinDialog?.copy(busy = true, digits = "", error = null))
        }
    }

    private fun setPinDialogError(message: String) {
        _uiState.update {
            it.copy(pinDialog = it.pinDialog?.copy(busy = false, digits = "", error = message))
        }
    }

    private fun setKidSafe(enabled: Boolean) {
        kidSafePrefs.kidSafeEnabled = enabled
        _uiState.update { state ->
            var next = state.copy(kidSafeEnabled = enabled, editingUnlocked = false)
            if (enabled) {
                val allowed = allChannels.filter(KidSafePolicy::isChannelAllowed).map { it.id }.toSet()
                next = next.withStack(
                    BackStack.pruneChannels(BackStack.pruneEditors(next.backStack), allowed)
                )
            }
            next
        }
        updateChannelList()
        afterStackChange()
    }

    // ---- playback ----

    fun playNow(activity: Activity, channelId: String) {
        viewModelScope.launch {
            if (!isChannelPlayable(channelId)) return@launch
            val program = engine.getProgramNow(channelId, maxShowRating = maxShowRating) ?: return@launch
            startProgram(activity, program)
        }
    }

    fun handleReturnFromPlayback(activity: Activity) {
        if (!orchestrator.shouldAutoPlay()) return
        orchestrator.consumeAutoPlayFlag()
        val channelId = orchestrator.getLastChannelId() ?: return
        viewModelScope.launch {
            if (!isChannelPlayable(channelId)) return@launch
            val program = engine.getNextProgram(channelId, maxShowRating = maxShowRating) ?: return@launch
            startProgram(activity, program)
        }
    }

    private suspend fun isChannelPlayable(channelId: String): Boolean {
        if (!kidSafe) return true
        val channel = repository.getChannel(channelId) ?: return false
        return KidSafePolicy.isChannelAllowed(channel)
    }

    private suspend fun startProgram(activity: Activity, program: Program) {
        val intent = resolver.resolveIntent(program.show, program.season, program.episode)
        if (intent != null) {
            orchestrator.startPlayback(activity, intent, program)
            engine.recordPlayed(program)
            _uiState.update { it.copy(currentProgram = program, message = null) }
        } else {
            _uiState.update {
                it.copy(
                    message = str(R.string.provider_unavailable, program.show.provider.displayName),
                    currentProgram = program
                )
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

    // ---- data refresh ----

    private fun isChannelVisible(channelId: String): Boolean =
        !kidSafe || _uiState.value.channels.any { it.id == channelId }

    private fun afterStackChange() {
        val channelId = _uiState.value.screen.ownerChannelId
        if (channelId != null) refreshChannel(channelId) else refreshNowPlaying()
    }

    private fun refreshChannel(channelId: String) {
        viewModelScope.launch {
            val rating = maxShowRating
            val program = engine.getProgramNow(channelId, maxShowRating = rating)
            val schedule = engine.getSchedulePreview(channelId, maxShowRating = rating)
            val nowPlaying = engine.getNowPlaying(_uiState.value.channels.map { it.id }, maxShowRating = rating)
            val providerAvailable = program?.show?.provider?.let { resolver.isProviderAvailable(it) }
            _uiState.update {
                // Ignore a stale result if the user moved to another channel meanwhile.
                val shown = it.screen.ownerChannelId
                if (shown != null && shown != channelId) it
                else it.copy(
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
            val nowPlaying = engine.getNowPlaying(
                _uiState.value.channels.map { it.id },
                maxShowRating = maxShowRating
            )
            _uiState.update { it.copy(nowPlaying = nowPlaying) }
        }
    }

    /** Recomputes the visible channel list (kid-safe: channel rule AND at least one allowed show). */
    private fun updateChannelList() {
        channelListJob?.cancel()
        val snapshot = allChannels
        val kidSafeNow = kidSafe
        channelListJob = viewModelScope.launch {
            val visible = if (kidSafeNow) {
                val candidates = snapshot.filter(KidSafePolicy::isChannelAllowed)
                val shows = candidates.associate { it.id to repository.getShows(it.id) }
                KidSafePolicy.visibleChannels(snapshot, shows)
            } else {
                snapshot
            }
            _uiState.update { state ->
                var next = state.copy(channels = visible)
                if (kidSafeNow) {
                    next = next.withStack(
                        BackStack.pruneChannels(next.backStack, visible.map { it.id }.toSet())
                    )
                }
                next
            }
            afterStackChange()
        }
    }

    // ---- helpers ----

    /** Replaces the stack and drops editor state / unlock for editors no longer on it. */
    private fun MainUiState.withStack(stack: List<Screen>): MainUiState {
        val hasRule = stack.any { it is Screen.RuleEditor }
        val hasChannel = stack.any { it is Screen.ChannelEditor }
        return copy(
            backStack = stack,
            ruleEditor = ruleEditor.takeIf { hasRule },
            channelEditor = channelEditor.takeIf { hasChannel },
            editingUnlocked = editingUnlocked && (hasRule || hasChannel)
        )
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
