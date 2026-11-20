package com.example.newtv.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ShowDao {
    @Query(
        "SELECT shows.* FROM shows " +
            "INNER JOIN channel_shows ON shows.id = channel_shows.showId " +
            "WHERE channel_shows.channelId = :channelId"
    )
    suspend fun getShowsForChannel(channelId: String): List<ShowEntity>

    @Query("SELECT * FROM shows ORDER BY title COLLATE NOCASE, id")
    suspend fun getAllShows(): List<ShowEntity>

    @Query("SELECT * FROM shows WHERE id = :showId")
    suspend fun getShow(showId: String): ShowEntity?

    @Query("SELECT showId FROM channel_shows WHERE channelId = :channelId")
    suspend fun getShowIdsForChannel(channelId: String): List<String>

    @Query("SELECT COUNT(*) FROM channel_shows WHERE channelId = :channelId")
    suspend fun countShowsForChannel(channelId: String): Int

    @Query("SELECT channelId, COUNT(*) AS showCount FROM channel_shows GROUP BY channelId")
    suspend fun getShowCounts(): List<ChannelShowCount>

    /** Channels whose lineup consists of [showId] alone, i.e. that deleting it would empty. */
    @Query(
        "SELECT channelId FROM channel_shows WHERE showId = :showId AND channelId IN (" +
            "SELECT channelId FROM channel_shows GROUP BY channelId HAVING COUNT(*) = 1)"
    )
    suspend fun getChannelsOnlyShowing(showId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShows(shows: List<ShowEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannelShows(crossRefs: List<ChannelShowCrossRef>)

    @Query("DELETE FROM channel_shows WHERE channelId = :channelId AND showId = :showId")
    suspend fun removeShowFromChannel(channelId: String, showId: String)

    @Query("DELETE FROM channel_shows WHERE channelId = :channelId")
    suspend fun clearChannelShows(channelId: String)

    @Query("DELETE FROM channel_shows WHERE showId = :showId")
    suspend fun deleteCrossRefsForShow(showId: String)

    @Query("DELETE FROM shows WHERE id = :showId")
    suspend fun deleteShowRow(showId: String)
}
