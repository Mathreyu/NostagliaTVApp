package com.example.newtv.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels ORDER BY number, name")
    fun observeChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels ORDER BY number, name")
    suspend fun getChannels(): List<ChannelEntity>

    @Query("SELECT * FROM channels WHERE id = :channelId")
    suspend fun getChannel(channelId: String): ChannelEntity?

    @Query("SELECT COALESCE(MAX(number), 0) FROM channels")
    suspend fun getMaxNumber(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)
}
