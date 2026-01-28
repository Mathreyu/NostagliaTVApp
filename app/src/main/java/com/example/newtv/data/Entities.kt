package com.example.newtv.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val ageRating: Int,
    val kidSafeOnly: Boolean
)

@Entity(tableName = "shows")
data class ShowEntity(
    @PrimaryKey val id: String,
    val title: String,
    val provider: Provider,
    val deepLinkPattern: String,
    val contentId: String,
    val seasonCount: Int,
    val episodeCount: Int,
    val ageRating: Int
)

@Entity(tableName = "channel_shows", primaryKeys = ["channelId", "showId"])
data class ChannelShowCrossRef(
    val channelId: String,
    val showId: String
)

@Entity(tableName = "schedule_rules")
data class ScheduleRuleEntity(
    @PrimaryKey val id: String,
    val channelId: String,
    val startMinute: Int,
    val endMinute: Int,
    val noRepeatWindow: Int,
    val slotMinutes: Int,
    val rotationMode: RotationMode
)

@Entity(tableName = "channel_state")
data class ChannelStateEntity(
    @PrimaryKey val channelId: String,
    val lastShowId: String?,
    val lastUpdatedAt: Long
)

@Entity(tableName = "episode_state")
data class EpisodeStateEntity(
    @PrimaryKey val showId: String,
    val lastSeason: Int,
    val lastEpisode: Int,
    val lastPlayedAt: Long
)

@Entity(tableName = "recent_episodes")
data class RecentEpisodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelId: String,
    val showId: String,
    val season: Int,
    val episode: Int,
    val playedAt: Long
)
