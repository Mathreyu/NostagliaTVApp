package com.example.newtv.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StateDao {
    @Query("SELECT * FROM channel_state WHERE channelId = :channelId")
    suspend fun getChannelState(channelId: String): ChannelStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChannelState(state: ChannelStateEntity)

    @Query("SELECT * FROM episode_state WHERE showId = :showId")
    suspend fun getEpisodeState(showId: String): EpisodeStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEpisodeState(state: EpisodeStateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentEpisode(recent: RecentEpisodeEntity)

    @Query(
        "SELECT * FROM recent_episodes WHERE channelId = :channelId " +
            "ORDER BY playedAt DESC LIMIT :limit"
    )
    suspend fun getRecentEpisodes(channelId: String, limit: Int): List<RecentEpisodeEntity>
}
