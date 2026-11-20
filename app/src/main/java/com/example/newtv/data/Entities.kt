package com.example.newtv.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val ageRating: Int,
    val kidSafeOnly: Boolean,
    /** Channel number shown in the UI. */
    @ColumnInfo(defaultValue = "0") val number: Int = 0,
    /** ARGB accent colour. */
    @ColumnInfo(defaultValue = "4281595647") val accentColor: Long = ChannelPalette.DEFAULT
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
    val ageRating: Int,
    /**
     * Optional template (`{contentId}`, `{season}`, `{episode}`) that opens a single episode.
     * Null when episodes cannot be targeted; [deepLinkPattern] (the title page) is used then.
     */
    val episodeLinkPattern: String? = null
)

@Entity(
    tableName = "channel_shows",
    primaryKeys = ["channelId", "showId"],
    indices = [Index("showId")]
)
data class ChannelShowCrossRef(
    val channelId: String,
    val showId: String
)

@Entity(tableName = "schedule_rules", indices = [Index("channelId")])
data class ScheduleRuleEntity(
    @PrimaryKey val id: String,
    val channelId: String,
    val startMinute: Int,
    val endMinute: Int,
    val noRepeatWindow: Int,
    val slotMinutes: Int,
    val rotationMode: RotationMode
)

@Entity(tableName = "recent_episodes", indices = [Index(value = ["channelId", "playedAt"])])
data class RecentEpisodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelId: String,
    val showId: String,
    val season: Int,
    val episode: Int,
    val playedAt: Long
)
