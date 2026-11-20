package com.example.newtv.engine

import com.example.newtv.data.ChannelEntity
import com.example.newtv.data.RecentEpisodeEntity
import com.example.newtv.data.RotationMode
import com.example.newtv.data.ScheduleRuleEntity
import com.example.newtv.data.ShowEntity
import com.example.newtv.data.TvRepository
import com.example.newtv.resolver.DeepLinkResolver
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class Program(
    val channel: ChannelEntity,
    val show: ShowEntity,
    val season: Int,
    val episode: Int,
    val deepLink: String?,
    val startTime: Instant,
    val endTime: Instant
) {
    /** Fraction of the program elapsed at [now], clamped to 0..1. */
    fun progress(now: Instant): Float {
        val total = Duration.between(startTime, endTime).toMillis()
        if (total <= 0L) return 1f
        val elapsed = Duration.between(startTime, now).toMillis()
        return (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }
}

/**
 * Pure, deterministic programming engine: the program for a channel is a function of
 * (channel, its shows, its rules, wall-clock instant, zone). Nothing is persisted when
 * computing "now", "next" or the schedule.
 *
 * Layout: each local day is cut into slots by walking from 00:00; each slot uses the slot
 * length of the rule active at its start (rules sorted by id, first match wins, otherwise the
 * first rule, otherwise a 30-minute all-day default) and is truncated at the next rule
 * boundary or midnight. Slots get a global monotonic index (days since [ANCHOR] times slots
 * per day plus the slot's position in the day).
 *
 * Show for slot n is shows[n mod size] (shows ordered by id, round-robin). The k-th airing
 * (k = n / size) of a show maps through a per-channel offset into its episodes, so episodes
 * rotate sequentially with wrap-around. Rotation therefore guarantees no repeat within
 * min(noRepeatWindow, total episodes) airings of the same show on the channel (in fact within
 * any window shorter than the show's total episode count).
 */
class ProgrammingEngine(
    private val repository: TvRepository,
    private val resolver: DeepLinkResolver,
    private val zone: ZoneId = ZoneId.systemDefault()
) {
    suspend fun getProgramNow(
        channelId: String,
        now: Instant = Instant.now(),
        maxShowRating: Int? = null
    ): Program? = getProgramAt(channelId, now, maxShowRating)

    /**
     * All program lookups take an optional [maxShowRating]: shows whose age rating is higher are
     * excluded from the channel's rotation (kid-safe mode). A channel left with no shows has no
     * programs.
     */
    suspend fun getProgramAt(
        channelId: String,
        instant: Instant,
        maxShowRating: Int? = null
    ): Program? {
        val channel = repository.getChannel(channelId) ?: return null
        val shows = loadShows(channelId, maxShowRating)
        val rules = repository.getRules(channelId)
        return computeProgram(channel, shows, rules, instant)
    }

    /** The program starting at the current program's end time. */
    suspend fun getNextProgram(
        channelId: String,
        now: Instant = Instant.now(),
        maxShowRating: Int? = null
    ): Program? {
        val channel = repository.getChannel(channelId) ?: return null
        val shows = loadShows(channelId, maxShowRating)
        val rules = repository.getRules(channelId)
        val current = computeProgram(channel, shows, rules, now) ?: return null
        return computeProgram(channel, shows, rules, current.endTime)
    }

    /** The [count] programs after the current one: contiguous and never overlapping it. */
    suspend fun getSchedulePreview(
        channelId: String,
        now: Instant = Instant.now(),
        count: Int = 5,
        maxShowRating: Int? = null
    ): List<Program> {
        val channel = repository.getChannel(channelId) ?: return emptyList()
        val shows = loadShows(channelId, maxShowRating)
        val rules = repository.getRules(channelId)
        var cursor = computeProgram(channel, shows, rules, now)?.endTime ?: return emptyList()
        val result = ArrayList<Program>(count)
        while (result.size < count) {
            val program = computeProgram(channel, shows, rules, cursor) ?: break
            result.add(program)
            cursor = program.endTime
        }
        return result
    }

    /** Current program of each given channel (all channels when null), keyed by channel id. */
    suspend fun getNowPlaying(
        channelIds: Collection<String>? = null,
        now: Instant = Instant.now(),
        maxShowRating: Int? = null
    ): Map<String, Program> {
        val ids = channelIds ?: repository.getChannels().map { it.id }
        val result = LinkedHashMap<String, Program>()
        for (id in ids) {
            getProgramNow(id, now, maxShowRating)?.let { result[id] = it }
        }
        return result
    }

    /** Records that [program] was actually played. History only; never affects scheduling. */
    suspend fun recordPlayed(program: Program) {
        repository.addRecentEpisode(
            RecentEpisodeEntity(
                channelId = program.channel.id,
                showId = program.show.id,
                season = program.season,
                episode = program.episode,
                playedAt = Instant.now().toEpochMilli()
            )
        )
    }

    private suspend fun loadShows(channelId: String, maxShowRating: Int?): List<ShowEntity> {
        val shows = repository.getShows(channelId)
        return if (maxShowRating == null) shows else shows.filter { it.ageRating <= maxShowRating }
    }

    // ---- pure computation ----

    private fun computeProgram(
        channel: ChannelEntity,
        shows: List<ShowEntity>,
        rules: List<ScheduleRuleEntity>,
        instant: Instant
    ): Program? {
        if (shows.isEmpty()) return null
        val ordered = shows.sortedBy { it.id }
        val layout = layoutDay(rules.sortedBy { it.id }, channel.id)

        val local = instant.atZone(zone)
        val date = local.toLocalDate()
        val dayStart = date.atStartOfDay(zone)
        val nextDayStart = date.plusDays(1).atStartOfDay(zone).toInstant()
        val minuteOfDay = Duration.between(dayStart.toInstant(), instant).toMinutes()
            .coerceAtLeast(0L)

        var slotIdx = layout.indexOfLast { it.startMinute <= minuteOfDay }
        if (slotIdx < 0) slotIdx = 0
        val slot = layout[slotIdx]

        val start = minOf(dayStart.plusMinutes(slot.startMinute.toLong()).toInstant(), nextDayStart)
        val end = if (slot.endMinute >= MINUTES_PER_DAY) {
            nextDayStart
        } else {
            minOf(dayStart.plusMinutes(slot.endMinute.toLong()).toInstant(), nextDayStart)
        }

        val days = ChronoUnit.DAYS.between(ANCHOR, date)
        val n = days * layout.size + slotIdx

        val size = ordered.size
        val show = ordered[Math.floorMod(n, size.toLong()).toInt()]
        val k = Math.floorDiv(n, size.toLong())
        val (season, episode) = episodeFor(channel.id, show, k)

        return Program(
            channel = channel,
            show = show,
            season = season,
            episode = episode,
            deepLink = resolver.buildDeepLink(show, season, episode),
            startTime = start,
            endTime = end
        )
    }

    private data class Slot(val startMinute: Int, val endMinute: Int)

    /** Lays out one local day (minute offsets from midnight) as contiguous slots. */
    private fun layoutDay(sortedRules: List<ScheduleRuleEntity>, channelId: String): List<Slot> {
        val rules = sortedRules.ifEmpty { listOf(defaultRule(channelId)) }
        val boundaries = sortedSetOf(0, MINUTES_PER_DAY)
        for (r in rules) {
            boundaries.add(r.startMinute.coerceIn(0, MINUTES_PER_DAY))
            boundaries.add(r.endMinute.coerceIn(0, MINUTES_PER_DAY))
        }
        val slots = ArrayList<Slot>()
        var t = 0
        while (t < MINUTES_PER_DAY) {
            val rule = activeRule(rules, t)
            val length = if (rule.slotMinutes > 0) rule.slotMinutes else DEFAULT_SLOT_MINUTES
            val nextBoundary = boundaries.first { it > t }
            val end = minOf(t + length, nextBoundary)
            slots.add(Slot(t, end))
            t = end
        }
        return slots
    }

    private fun activeRule(rules: List<ScheduleRuleEntity>, minute: Int): ScheduleRuleEntity =
        rules.firstOrNull { matchesRule(minute, it) } ?: rules.first()

    private fun matchesRule(minute: Int, rule: ScheduleRuleEntity): Boolean =
        if (rule.startMinute <= rule.endMinute) {
            minute >= rule.startMinute && minute < rule.endMinute
        } else {
            minute >= rule.startMinute || minute < rule.endMinute
        }

    private fun defaultRule(channelId: String) = ScheduleRuleEntity(
        id = "${channelId}_default",
        channelId = channelId,
        startMinute = 0,
        endMinute = MINUTES_PER_DAY,
        noRepeatWindow = 4,
        slotMinutes = DEFAULT_SLOT_MINUTES,
        rotationMode = RotationMode.ROUND_ROBIN
    )

    private fun episodeFor(channelId: String, show: ShowEntity, k: Long): Pair<Int, Int> {
        val perSeason = show.episodeCount.coerceAtLeast(1)
        val total = (show.seasonCount.coerceAtLeast(1).toLong() * perSeason)
        val offset = fnv1a("$channelId:${show.id}") % total
        val idx = Math.floorMod(k + offset, total)
        return (idx / perSeason).toInt() + 1 to (idx % perSeason).toInt() + 1
    }

    private fun fnv1a(text: String): Long {
        var hash = 0x811C9DC5L
        for (b in text.toByteArray(Charsets.UTF_8)) {
            hash = hash xor (b.toLong() and 0xFF)
            hash = (hash * 0x01000193L) and 0xFFFFFFFFL
        }
        return hash
    }

    companion object {
        private const val MINUTES_PER_DAY = 1440
        private const val DEFAULT_SLOT_MINUTES = 30
        private val ANCHOR: LocalDate = LocalDate.of(2024, 1, 1)
    }
}
