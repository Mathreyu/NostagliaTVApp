package com.example.newtv.data

import java.time.Duration

/** Retention policy for the recent-episodes history table. */
object HistoryRetention {
    const val MAX_AGE_DAYS = 30L
    const val MAX_ROWS = 500

    /** Rows with `playedAt` strictly before the returned epoch-millis are expired. */
    fun cutoffMillis(nowMillis: Long, maxAgeDays: Long = MAX_AGE_DAYS): Long =
        nowMillis - Duration.ofDays(maxAgeDays).toMillis()
}
