package com.example.newtv.engine

import com.example.newtv.data.ChannelEntity
import com.example.newtv.data.ChannelStateEntity
import com.example.newtv.data.EpisodeStateEntity
import com.example.newtv.data.RecentEpisodeEntity
import com.example.newtv.data.RotationMode
import com.example.newtv.data.ScheduleRuleEntity
import com.example.newtv.data.ShowEntity
import com.example.newtv.data.TvRepository
import com.example.newtv.resolver.DeepLinkResolver
import java.util.ArrayDeque
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

data class Program(
    val channel: ChannelEntity,
    val show: ShowEntity,
    val season: Int,
    val episode: Int,
    val deepLink: String?,
    val startTime: Instant,
    val endTime: Instant
)

class ProgrammingEngine(
    private val repository: TvRepository,
    private val resolver: DeepLinkResolver
) {
    suspend fun getProgramNow(
        channelId: String,
        now: Instant = Instant.now()
    ): Program? = buildProgram(channelId, now, forceAdvance = false, persistState = true, previewState = null)

    suspend fun getNextProgram(
        channelId: String,
        now: Instant = Instant.now()
    ): Program? {
        val current = getProgramNow(channelId, now) ?: return null
        return buildProgram(
            channelId = channelId,
            now = current.endTime,
            forceAdvance = true,
            persistState = true,
            previewState = null
        )
    }

    suspend fun getSchedulePreview(
        channelId: String,
        now: Instant = Instant.now(),
        count: Int = 5
    ): List<Program> {
        val programs = mutableListOf<Program>()
        val shows = repository.getShows(channelId)
        if (shows.isEmpty()) return programs
        val previewState = buildPreviewState(channelId, shows)
        var current = now
        repeat(count) {
            val program = buildProgram(
                channelId = channelId,
                now = current,
                forceAdvance = true,
                persistState = false,
                previewState = previewState
            )
            if (program != null) {
                programs.add(program)
                current = program.endTime
            }
        }
        return programs
    }

    private suspend fun buildProgram(
        channelId: String,
        now: Instant,
        forceAdvance: Boolean,
        persistState: Boolean,
        previewState: PreviewState?
    ): Program? {
        val channel = repository.getChannel(channelId) ?: return null
        val shows = repository.getShows(channelId)
        if (shows.isEmpty()) return null

        val rules = repository.getRules(channelId)
        val rule = selectRule(rules, now) ?: defaultRule(channelId)
        val slotStart = alignToSlot(now, rule.slotMinutes)
        val slotEnd = slotStart.plusSeconds(rule.slotMinutes.toLong() * 60)

        val show = selectShow(shows, channelId, rule, forceAdvance, persistState, previewState, slotStart)
        val episode = selectEpisode(
            show = show,
            channelId = channelId,
            rule = rule,
            forceAdvance = forceAdvance,
            persistState = persistState,
            previewState = previewState,
            timestampMillis = slotStart.toEpochMilli()
        )
        val deepLink = resolver.buildDeepLink(show, episode.season, episode.episode)

        return Program(
            channel = channel,
            show = show,
            season = episode.season,
            episode = episode.episode,
            deepLink = deepLink,
            startTime = slotStart,
            endTime = slotEnd
        )
    }

    private fun selectRule(
        rules: List<ScheduleRuleEntity>,
        now: Instant
    ): ScheduleRuleEntity? {
        if (rules.isEmpty()) return null
        val minute = LocalTime.ofInstant(now, ZoneId.systemDefault()).toSecondOfDay() / 60
        return rules.firstOrNull { matchesRule(minute, it) } ?: rules.first()
    }

    private fun matchesRule(minute: Int, rule: ScheduleRuleEntity): Boolean {
        return if (rule.startMinute <= rule.endMinute) {
            minute in rule.startMinute until rule.endMinute
        } else {
            minute >= rule.startMinute || minute < rule.endMinute
        }
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

    private suspend fun selectShow(
        shows: List<ShowEntity>,
        channelId: String,
        rule: ScheduleRuleEntity,
        forceAdvance: Boolean,
        persistState: Boolean,
        previewState: PreviewState?,
        timestamp: Instant
    ): ShowEntity {
        val ordered = shows.sortedBy { it.title }
        val state = previewState?.lastShowId?.let { ChannelStateEntity(channelId, it, 0L) }
            ?: repository.getChannelState(channelId)
        val lastShowIndex = state?.lastShowId?.let { id ->
            ordered.indexOfFirst { it.id == id }
        } ?: -1

        val nextIndex = if (forceAdvance || lastShowIndex == -1) {
            (lastShowIndex + 1).mod(ordered.size)
        } else {
            lastShowIndex
        }

        val chosen = ordered[nextIndex]
        if (persistState) {
            repository.upsertChannelState(
                ChannelStateEntity(
                    channelId = channelId,
                    lastShowId = chosen.id,
                    lastUpdatedAt = timestamp.toEpochMilli()
                )
            )
        } else if (previewState != null) {
            previewState.lastShowId = chosen.id
        }
        return chosen
    }

    private suspend fun selectEpisode(
        show: ShowEntity,
        channelId: String,
        rule: ScheduleRuleEntity,
        forceAdvance: Boolean,
        persistState: Boolean,
        previewState: PreviewState?,
        timestampMillis: Long
    ): EpisodeSelection {
        val state = previewState?.episodeState?.get(show.id)
            ?: repository.getEpisodeState(show.id)
        val recent = previewState?.recent ?: ArrayDeque(
            repository.getRecentEpisodes(channelId, rule.noRepeatWindow)
        )
        val recentKeys = recent.map { EpisodeKey(it.showId, it.season, it.episode) }.toSet()

        var season = state?.lastSeason ?: 1
        var episode = state?.lastEpisode ?: 0

        if (forceAdvance || state == null) {
            val attemptLimit = show.episodeCount * show.seasonCount
            var attempts = 0
            do {
                val next = advanceEpisode(season, episode, show)
                season = next.season
                episode = next.episode
                attempts++
            } while (EpisodeKey(show.id, season, episode) in recentKeys && attempts < attemptLimit)
        }

        if (persistState) {
            repository.upsertEpisodeState(
                EpisodeStateEntity(
                    showId = show.id,
                    lastSeason = season,
                    lastEpisode = episode,
                    lastPlayedAt = timestampMillis
                )
            )
            repository.addRecentEpisode(
                RecentEpisodeEntity(
                    channelId = channelId,
                    showId = show.id,
                    season = season,
                    episode = episode,
                    playedAt = timestampMillis
                )
            )
        } else if (previewState != null) {
            previewState.episodeState[show.id] = EpisodeStateEntity(
                showId = show.id,
                lastSeason = season,
                lastEpisode = episode,
                lastPlayedAt = timestampMillis
            )
            previewState.recent.addFirst(
                RecentEpisodeEntity(
                    channelId = channelId,
                    showId = show.id,
                    season = season,
                    episode = episode,
                    playedAt = timestampMillis
                )
            )
            while (previewState.recent.size > rule.noRepeatWindow) {
                previewState.recent.removeLast()
            }
        }

        return EpisodeSelection(season, episode)
    }

    private fun advanceEpisode(
        season: Int,
        episode: Int,
        show: ShowEntity
    ): EpisodeSelection {
        var nextSeason = season
        var nextEpisode = episode + 1
        if (nextEpisode > show.episodeCount) {
            nextEpisode = 1
            nextSeason += 1
            if (nextSeason > show.seasonCount) {
                nextSeason = 1
            }
        }
        return EpisodeSelection(nextSeason, nextEpisode)
    }

    private data class EpisodeKey(val showId: String, val season: Int, val episode: Int)
    private data class PreviewState(
        var lastShowId: String?,
        val episodeState: MutableMap<String, EpisodeStateEntity>,
        val recent: ArrayDeque<RecentEpisodeEntity>
    )
    private data class EpisodeSelection(val season: Int, val episode: Int)

    private suspend fun buildPreviewState(
        channelId: String,
        shows: List<ShowEntity>
    ): PreviewState {
        val channelState = repository.getChannelState(channelId)
        val episodeState = shows.associate { show ->
            show.id to (repository.getEpisodeState(show.id) ?: EpisodeStateEntity(show.id, 1, 0, 0L))
        }.toMutableMap()
        val recent = ArrayDeque(repository.getRecentEpisodes(channelId, limit = 10))
        return PreviewState(channelState?.lastShowId, episodeState, recent)
    }

    private fun alignToSlot(now: Instant, slotMinutes: Int): Instant {
        val safeSlot = if (slotMinutes <= 0) 30 else slotMinutes
        val zone = ZoneId.systemDefault()
        val local = now.atZone(zone)
        val minutes = local.toLocalTime().toSecondOfDay() / 60
        val slotStartMinutes = (minutes / safeSlot) * safeSlot
        return local.toLocalDate()
            .atStartOfDay(zone)
            .plusMinutes(slotStartMinutes.toLong())
            .toInstant()
    }
}
