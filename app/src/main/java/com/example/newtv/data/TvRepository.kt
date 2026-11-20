package com.example.newtv.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

open class TvRepository(protected val database: AppDatabase?) {

    open fun observeChannels(): Flow<List<ChannelEntity>> =
        requireNotNull(database).channelDao().observeChannels()

    open suspend fun getChannels(): List<ChannelEntity> =
        requireNotNull(database).channelDao().getChannels()

    open suspend fun seedIfEmpty() {
        val db = requireNotNull(database)
        if (db.channelDao().getChannels().isNotEmpty()) return
        SeedData.seed(db)
    }

    open suspend fun getMaxChannelNumber(): Int =
        requireNotNull(database).channelDao().getMaxNumber()

    open suspend fun getChannel(channelId: String): ChannelEntity? =
        requireNotNull(database).channelDao().getChannel(channelId)

    open suspend fun upsertChannel(channel: ChannelEntity) {
        requireNotNull(database).channelDao().insertChannels(listOf(channel))
    }

    open suspend fun getShows(channelId: String): List<ShowEntity> =
        requireNotNull(database).showDao().getShowsForChannel(channelId)

    open suspend fun getRules(channelId: String): List<ScheduleRuleEntity> =
        requireNotNull(database).ruleDao().getRulesForChannel(channelId)

    open suspend fun upsertRule(rule: ScheduleRuleEntity) {
        requireNotNull(database).ruleDao().insertRules(listOf(rule))
    }

    open suspend fun addRecentEpisode(recent: RecentEpisodeEntity) {
        requireNotNull(database).historyDao().insertRecentEpisode(recent)
    }

    open suspend fun getRecentEpisodes(channelId: String, limit: Int): List<RecentEpisodeEntity> =
        requireNotNull(database).historyDao().getRecentEpisodes(channelId, limit)

    // ---- show library and lineups ----

    open suspend fun getAllShows(): List<ShowEntity> =
        requireNotNull(database).showDao().getAllShows()

    open suspend fun getShow(showId: String): ShowEntity? =
        requireNotNull(database).showDao().getShow(showId)

    open suspend fun upsertShow(show: ShowEntity) {
        requireNotNull(database).showDao().insertShows(listOf(show))
    }

    /** Deletes the show together with its lineup entries and play history. */
    open suspend fun deleteShow(showId: String) {
        val db = requireNotNull(database)
        db.withTransaction {
            db.showDao().deleteCrossRefsForShow(showId)
            db.historyDao().deleteForShow(showId)
            db.showDao().deleteShowRow(showId)
        }
    }

    /** Channels that deleting [showId] would leave without any show. */
    open suspend fun getChannelsOnlyShowing(showId: String): List<String> =
        requireNotNull(database).showDao().getChannelsOnlyShowing(showId)

    open suspend fun getShowIds(channelId: String): Set<String> =
        requireNotNull(database).showDao().getShowIdsForChannel(channelId).toSet()

    open suspend fun countShows(channelId: String): Int =
        requireNotNull(database).showDao().countShowsForChannel(channelId)

    open suspend fun getShowCounts(): Map<String, Int> =
        requireNotNull(database).showDao().getShowCounts().associate { it.channelId to it.showCount }

    open suspend fun addShowToChannel(channelId: String, showId: String) {
        requireNotNull(database).showDao().insertChannelShows(listOf(ChannelShowCrossRef(channelId, showId)))
    }

    open suspend fun removeShowFromChannel(channelId: String, showId: String) {
        requireNotNull(database).showDao().removeShowFromChannel(channelId, showId)
    }

    /** Replaces the channel's lineup with exactly [showIds]. */
    open suspend fun setChannelShows(channelId: String, showIds: Collection<String>) {
        val db = requireNotNull(database)
        db.withTransaction {
            db.showDao().clearChannelShows(channelId)
            db.showDao().insertChannelShows(showIds.map { ChannelShowCrossRef(channelId, it) })
        }
    }

    // ---- history housekeeping ----

    /** Drops history older than [HistoryRetention.MAX_AGE_DAYS] and anything beyond [HistoryRetention.MAX_ROWS]. */
    open suspend fun pruneHistory(nowMillis: Long) {
        val db = requireNotNull(database)
        db.withTransaction {
            db.historyDao().deleteOlderThan(HistoryRetention.cutoffMillis(nowMillis))
            db.historyDao().trimToNewest(HistoryRetention.MAX_ROWS)
        }
    }
}
