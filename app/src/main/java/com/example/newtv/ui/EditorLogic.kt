package com.example.newtv.ui

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class RuleField { START, END, SLOT_MINUTES, NO_REPEAT }

enum class RuleError { START_END_RANGE, START_EQUALS_END, SLOT_LENGTH, NO_REPEAT_RANGE }

enum class ChannelError { NAME_REQUIRED, AGE_RANGE }

data class RuleEditorState(
    val channelId: String,
    val startMinute: Int,
    val endMinute: Int,
    val noRepeatWindow: Int,
    val slotMinutes: Int,
    val error: String? = null
)

data class ChannelEditorState(
    val channelId: String?,
    val name: String,
    val ageRating: Int,
    val kidSafeOnly: Boolean,
    val isNew: Boolean,
    /** Read-only channel number for existing channels. */
    val number: Int? = null,
    val error: String? = null
)

/** Allowed values and stepping for the TV-native steppers. */
object Steps {
    const val TIME_STEP_MINUTES = 15
    const val MINUTES_PER_DAY = 1440

    /** Start of day offsets: 00:00 .. 23:45. */
    val StartTimes: List<Int> = (0 until MINUTES_PER_DAY step TIME_STEP_MINUTES).toList()

    /** End offsets: 00:15 .. 24:00 included (24:00 = 1440), plus 00:00 for overnight rules. */
    val EndTimes: List<Int> = (0..MINUTES_PER_DAY step TIME_STEP_MINUTES).toList()

    val SlotMinutes: List<Int> = listOf(15, 30, 45, 60, 90, 120)
    val NoRepeat: List<Int> = (1..50).toList()
    val AgeRatings: List<Int> = (0..21).toList()

    /**
     * Next (direction > 0) or previous (direction < 0) value of the sorted [values] relative to
     * [current], wrapping around. [current] need not be in the list.
     */
    fun step(values: List<Int>, current: Int, direction: Int): Int {
        require(values.isNotEmpty())
        return if (direction >= 0) {
            values.firstOrNull { it > current } ?: values.first()
        } else {
            values.lastOrNull { it < current } ?: values.last()
        }
    }
}

fun RuleEditorState.stepped(field: RuleField, direction: Int): RuleEditorState = when (field) {
    RuleField.START -> copy(startMinute = Steps.step(Steps.StartTimes, startMinute, direction), error = null)
    RuleField.END -> copy(endMinute = Steps.step(Steps.EndTimes, endMinute, direction), error = null)
    RuleField.SLOT_MINUTES -> copy(slotMinutes = Steps.step(Steps.SlotMinutes, slotMinutes, direction), error = null)
    RuleField.NO_REPEAT -> copy(noRepeatWindow = Steps.step(Steps.NoRepeat, noRepeatWindow, direction), error = null)
}

fun RuleEditorState.validate(): RuleError? = when {
    startMinute !in 0..Steps.MINUTES_PER_DAY || endMinute !in 0..Steps.MINUTES_PER_DAY ->
        RuleError.START_END_RANGE
    startMinute >= Steps.MINUTES_PER_DAY -> RuleError.START_END_RANGE
    startMinute == endMinute -> RuleError.START_EQUALS_END
    slotMinutes !in 5..240 -> RuleError.SLOT_LENGTH
    noRepeatWindow !in 1..50 -> RuleError.NO_REPEAT_RANGE
    else -> null
}

fun ChannelEditorState.steppedAge(direction: Int): ChannelEditorState =
    copy(ageRating = Steps.step(Steps.AgeRatings, ageRating, direction), error = null)

fun ChannelEditorState.validate(): ChannelError? = when {
    name.isBlank() -> ChannelError.NAME_REQUIRED
    ageRating !in 0..21 -> ChannelError.AGE_RANGE
    else -> null
}

private val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

/** Minute-of-day as "h:mm a". 1440 (24:00) renders as 12:00 AM; callers may add a suffix. */
fun formatMinuteOfDay(minute: Int, locale: Locale = Locale.getDefault()): String =
    LocalTime.ofSecondOfDay((minute.coerceIn(0, Steps.MINUTES_PER_DAY) % Steps.MINUTES_PER_DAY) * 60L)
        .format(TimeFormat.withLocale(locale))
