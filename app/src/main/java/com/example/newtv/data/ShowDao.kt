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

    @Query("SELECT * FROM shows WHERE id = :showId")
    suspend fun getShow(showId: String): ShowEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShows(shows: List<ShowEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannelShows(crossRefs: List<ChannelShowCrossRef>)
}
