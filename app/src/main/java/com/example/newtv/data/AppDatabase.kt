package com.example.newtv.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ChannelEntity::class,
        ShowEntity::class,
        ChannelShowCrossRef::class,
        ScheduleRuleEntity::class,
        RecentEpisodeEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao
    abstract fun showDao(): ShowDao
    abstract fun ruleDao(): RuleDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /**
         * v2 -> v3: adds channel number and accent colour, drops the obsolete
         * channel_state / episode_state tables (the engine is now stateless).
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE channels ADD COLUMN number INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE channels ADD COLUMN accentColor INTEGER NOT NULL DEFAULT 4281595647")
                // Seeded channels keep their well-known numbers and accents.
                db.execSQL("UPDATE channels SET number = 3, accentColor = ${0xFFFF4FD8} WHERE id = 'cartoons'")
                db.execSQL("UPDATE channels SET number = 7, accentColor = ${0xFF33F6FF} WHERE id = 'classic_nick'")
                db.execSQL(
                    "UPDATE channels SET number = 11, accentColor = ${0xFF8CFF5E} WHERE id = 'disney_after_school'"
                )
                // Any other channel gets the next numbers in rowid order, after 11.
                db.execSQL(
                    "UPDATE channels SET number = 11 + (" +
                        "SELECT COUNT(*) FROM channels c2 WHERE c2.rowid <= channels.rowid " +
                        "AND c2.id NOT IN ('cartoons', 'classic_nick', 'disney_after_school')) " +
                        "WHERE id NOT IN ('cartoons', 'classic_nick', 'disney_after_school')"
                )
                db.execSQL(
                    "UPDATE channels SET accentColor = CASE ((number - 1) % 5) " +
                        "WHEN 0 THEN ${0xFFFF4FD8} WHEN 1 THEN ${0xFF33F6FF} WHEN 2 THEN ${0xFF8CFF5E} " +
                        "WHEN 3 THEN ${0xFFFF9F1C} ELSE ${0xFFB388FF} END " +
                        "WHERE id NOT IN ('cartoons', 'classic_nick', 'disney_after_school')"
                )
                db.execSQL("DROP TABLE IF EXISTS channel_state")
                db.execSQL("DROP TABLE IF EXISTS episode_state")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "newtv.db")
                .addMigrations(MIGRATION_2_3)
                // Pre-release v1 schema has no migration path.
                .fallbackToDestructiveMigrationFrom(true, 1)
                .build()
        }
    }
}
