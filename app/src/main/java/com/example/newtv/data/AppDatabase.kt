package com.example.newtv.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ChannelEntity::class,
        ShowEntity::class,
        ChannelShowCrossRef::class,
        ScheduleRuleEntity::class,
        ChannelStateEntity::class,
        EpisodeStateEntity::class,
        RecentEpisodeEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao
    abstract fun showDao(): ShowDao
    abstract fun ruleDao(): RuleDao
    abstract fun stateDao(): StateDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "newtv.db")
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
