package com.example.newtv.ui

import com.example.newtv.data.ChannelEntity
import com.example.newtv.engine.Program
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Pure helpers for the live "now / coming up" board that the ViewModel keeps fresh. */
object LiveBoard {
    /** How often the ViewModel checks whether a slot boundary has passed. */
    const val TICK_MILLIS = 30_000L

    /** Programs after the current one kept per channel for the guide. */
    const val GUIDE_PREVIEW_COUNT = 4

    /** True when any shown program has ended at [now], i.e. a slot boundary passed. */
    fun needsRefresh(
        nowPlaying: Map<String, Program>,
        currentProgram: Program?,
        now: Instant
    ): Boolean =
        nowPlaying.values.any { !it.endTime.isAfter(now) } ||
            (currentProgram != null && !currentProgram.endTime.isAfter(now))

    /** Channels ordered by number (ties by name), as shown on Home and in the guide. */
    fun ordered(channels: List<ChannelEntity>): List<ChannelEntity> =
        channels.sortedWith(compareBy({ it.number }, { it.name }, { it.id }))

    /** Start of the guide window: [now] floored to the half hour in [zone]. */
    fun guideStart(now: Instant, zone: ZoneId): Instant {
        val local = now.atZone(zone)
        return local.withMinute(local.minute / 30 * 30).truncatedTo(ChronoUnit.MINUTES).toInstant()
    }
}
