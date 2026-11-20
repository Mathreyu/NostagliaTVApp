package com.example.newtv.data

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
}
