package com.example.newtv.ui

import com.example.newtv.data.Provider
import com.example.newtv.data.ShowEntity
import com.example.newtv.resolver.CustomLink
import com.example.newtv.resolver.LinkChoice
import com.example.newtv.resolver.LinkChoices
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
    val error: String? = null,
    /** Number of shows currently in this channel's lineup (existing channels). */
    val showCount: Int = 0
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
    val Seasons: List<Int> = (1..30).toList()
    val EpisodesPerSeason: List<Int> = (1..60).toList()

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

enum class LineupError { EMPTY }

enum class ShowError { TITLE_REQUIRED, CONTENT_ID_REQUIRED, SEASONS_RANGE, EPISODES_RANGE, AGE_RANGE }

/** Lineup of one channel being edited: the whole library plus which shows are switched on. */
data class LineupEditorState(
    val channelId: String,
    val shows: List<ShowEntity>,
    val selected: Set<String>,
    val error: String? = null
)

fun LineupEditorState.toggled(showId: String): LineupEditorState =
    copy(selected = if (showId in selected) selected - showId else selected + showId, error = null)

/** A channel must keep at least one show. */
fun LineupEditorState.validate(): LineupError? =
    if (selected.none { id -> shows.any { it.id == id } }) LineupError.EMPTY else null

/** Show being created ([showId] null) or edited. */
data class ShowEditorState(
    val showId: String?,
    val title: String = "",
    val provider: Provider = Provider.entries.first(),
    val seasonCount: Int = 1,
    val episodeCount: Int = 10,
    val ageRating: Int = 7,
    val contentId: String = "",
    val linkChoice: LinkChoice = LinkChoice.TITLE_PAGE,
    /** Patterns the show carried that match no provider template; null for new/standard shows. */
    val custom: CustomLink? = null,
    val confirmingDelete: Boolean = false,
    val error: String? = null
) {
    val isNew: Boolean get() = showId == null
    val linkChoices: List<LinkChoice> get() = LinkChoices.available(provider, custom)

    companion object {
        fun from(show: ShowEntity) = ShowEditorState(
            showId = show.id,
            title = show.title,
            provider = show.provider,
            seasonCount = show.seasonCount,
            episodeCount = show.episodeCount,
            ageRating = show.ageRating,
            contentId = show.contentId,
            linkChoice = LinkChoices.detect(show),
            custom = LinkChoices.customOf(show)
        )
    }
}

private fun ShowEditorState.edited(block: ShowEditorState.() -> ShowEditorState): ShowEditorState =
    block().copy(error = null, confirmingDelete = false)

fun ShowEditorState.withTitle(value: String) = edited { copy(title = value) }

fun ShowEditorState.withContentId(value: String) = edited { copy(contentId = value) }

fun ShowEditorState.steppedProvider(direction: Int): ShowEditorState = edited {
    val all = Provider.entries
    val next = all[Math.floorMod(all.indexOf(provider) + if (direction >= 0) 1 else -1, all.size)]
    copy(provider = next, linkChoice = LinkChoices.coerce(linkChoice, next, custom))
}

fun ShowEditorState.steppedSeasons(direction: Int) =
    edited { copy(seasonCount = Steps.step(Steps.Seasons, seasonCount, direction)) }

fun ShowEditorState.steppedEpisodes(direction: Int) =
    edited { copy(episodeCount = Steps.step(Steps.EpisodesPerSeason, episodeCount, direction)) }

fun ShowEditorState.steppedAge(direction: Int) =
    edited { copy(ageRating = Steps.step(Steps.AgeRatings, ageRating, direction)) }

fun ShowEditorState.steppedLink(direction: Int) =
    edited { copy(linkChoice = LinkChoices.step(linkChoice, provider, custom, direction)) }

fun ShowEditorState.validate(): ShowError? = when {
    title.isBlank() -> ShowError.TITLE_REQUIRED
    contentId.isBlank() -> ShowError.CONTENT_ID_REQUIRED
    seasonCount !in 1..30 -> ShowError.SEASONS_RANGE
    episodeCount !in 1..60 -> ShowError.EPISODES_RANGE
    ageRating !in 0..21 -> ShowError.AGE_RANGE
    else -> null
}

/** The entity to store; [id] is the existing id or a freshly generated one. */
fun ShowEditorState.toEntity(id: String): ShowEntity {
    val (titlePattern, episodePattern) = LinkChoices.patterns(linkChoice, provider, custom)
    return ShowEntity(
        id = id,
        title = title.trim(),
        provider = provider,
        deepLinkPattern = titlePattern,
        contentId = contentId.trim(),
        seasonCount = seasonCount,
        episodeCount = episodeCount,
        ageRating = ageRating,
        episodeLinkPattern = episodePattern
    )
}

/** Lower-case id slug: runs of non-alphanumerics become one underscore. */
fun slugify(text: String, fallback: String): String =
    text.trim().lowercase().replace("[^a-z0-9]+".toRegex(), "_").trim('_').ifBlank { fallback }

/** [base], or base_2, base_3 ... the first one not in [taken]. */
fun uniqueId(base: String, taken: Set<String>): String {
    if (base !in taken) return base
    var suffix = 2
    while ("${base}_$suffix" in taken) suffix += 1
    return "${base}_$suffix"
}

private val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

/** Minute-of-day as "h:mm a". 1440 (24:00) renders as 12:00 AM; callers may add a suffix. */
fun formatMinuteOfDay(minute: Int, locale: Locale = Locale.getDefault()): String =
    LocalTime.ofSecondOfDay((minute.coerceIn(0, Steps.MINUTES_PER_DAY) % Steps.MINUTES_PER_DAY) * 60L)
        .format(TimeFormat.withLocale(locale))
