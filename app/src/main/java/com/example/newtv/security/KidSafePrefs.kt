package com.example.newtv.security

import android.content.Context
import java.util.Base64

/**
 * SharedPreferences-backed store for kid-safe state. Only a salted hash of the PIN is kept.
 * Security-relevant writes use commit() so a force-stop cannot drop them.
 */
class KidSafePrefs(context: Context) : PinStorage {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val encoder = Base64.getEncoder()
    private val decoder = Base64.getDecoder()

    /** Persisted so restarting the app does not silently leave kid-safe mode. */
    var kidSafeEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_ENABLED, value).commit()
        }

    override fun readPin(): StoredPin? {
        val salt = prefs.getString(KEY_SALT, null) ?: return null
        val hash = prefs.getString(KEY_HASH, null) ?: return null
        val iterations = prefs.getInt(KEY_ITERATIONS, 0)
        if (iterations <= 0) return null
        return StoredPin(decoder.decode(salt), decoder.decode(hash), iterations)
    }

    override fun writePin(pin: StoredPin) {
        prefs.edit()
            .putString(KEY_SALT, encoder.encodeToString(pin.salt))
            .putString(KEY_HASH, encoder.encodeToString(pin.hash))
            .putInt(KEY_ITERATIONS, pin.iterations)
            .commit()
    }

    override var failedAttempts: Int
        get() = prefs.getInt(KEY_FAILED, 0)
        set(value) {
            prefs.edit().putInt(KEY_FAILED, value).commit()
        }

    override var lockedUntilMillis: Long
        get() = prefs.getLong(KEY_LOCKED_UNTIL, 0L)
        set(value) {
            prefs.edit().putLong(KEY_LOCKED_UNTIL, value).commit()
        }

    private companion object {
        const val PREFS_NAME = "kid_safe"
        const val KEY_ENABLED = "enabled"
        const val KEY_SALT = "pin_salt"
        const val KEY_HASH = "pin_hash"
        const val KEY_ITERATIONS = "pin_iterations"
        const val KEY_FAILED = "failed_attempts"
        const val KEY_LOCKED_UNTIL = "locked_until"
    }
}
