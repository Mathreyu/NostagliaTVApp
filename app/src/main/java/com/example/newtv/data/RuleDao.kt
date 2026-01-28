package com.example.newtv.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RuleDao {
    @Query("SELECT * FROM schedule_rules WHERE channelId = :channelId")
    suspend fun getRulesForChannel(channelId: String): List<ScheduleRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<ScheduleRuleEntity>)
}
