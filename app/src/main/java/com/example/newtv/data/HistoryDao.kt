package com.example.newtv.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** Playback history only; the programming engine itself never reads or writes this. */
@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentEpisode(recent: RecentEpisodeEntity)

    @Query(
        "SELECT * FROM recent_episodes WHERE channelId = :channelId " +
            "ORDER BY playedAt DESC LIMIT :limit"
    )
    suspend fun getRecentEpisodes(channelId: String, limit: Int): List<RecentEpisodeEntity>

    @Query("DELETE FROM recent_episodes WHERE playedAt < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long)

    /** Keeps only the [keep] most recent rows. */
    @Query(
        "DELETE FROM recent_episodes WHERE id NOT IN (" +
            "SELECT id FROM recent_episodes ORDER BY playedAt DESC, id DESC LIMIT :keep)"
    )
    suspend fun trimToNewest(keep: Int)

    @Query("DELETE FROM recent_episodes WHERE showId = :showId")
    suspend fun deleteForShow(showId: String)
}
