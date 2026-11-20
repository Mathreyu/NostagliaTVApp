package com.example.newtv.playback

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.example.newtv.engine.Program
import java.time.Instant

class PlaybackOrchestrator(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Hands playback to the provider app. Returns false, leaving the autoplay state untouched,
     * when the provider cannot be launched (for example it was uninstalled after the check).
     */
    fun startPlayback(activity: Activity, intent: Intent, program: Program): Boolean {
        try {
            activity.startActivity(intent)
        } catch (exception: ActivityNotFoundException) {
            return false
        } catch (exception: SecurityException) {
            return false
        }
        prefs.edit()
            .putString(KEY_LAST_CHANNEL, program.channel.id)
            .putLong(KEY_STARTED_AT, Instant.now().toEpochMilli())
            .putBoolean(KEY_SHOULD_AUTOPLAY, true)
            .apply()
        return true
    }

    fun shouldAutoPlay(): Boolean {
        val startedAt = prefs.getLong(KEY_STARTED_AT, 0L)
        val shouldAutoPlay = prefs.getBoolean(KEY_SHOULD_AUTOPLAY, false)
        if (!shouldAutoPlay || startedAt == 0L) return false
        val elapsed = Instant.now().toEpochMilli() - startedAt
        return elapsed >= MIN_PLAYBACK_MS
    }

    fun consumeAutoPlayFlag() {
        prefs.edit().putBoolean(KEY_SHOULD_AUTOPLAY, false).apply()
    }

    fun getLastChannelId(): String? = prefs.getString(KEY_LAST_CHANNEL, null)

    companion object {
        private const val PREFS_NAME = "playback_state"
        private const val KEY_LAST_CHANNEL = "last_channel"
        private const val KEY_STARTED_AT = "started_at"
        private const val KEY_SHOULD_AUTOPLAY = "should_autoplay"
        private const val MIN_PLAYBACK_MS = 30_000L
    }
}
