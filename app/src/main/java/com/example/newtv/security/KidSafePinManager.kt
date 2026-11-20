package com.example.newtv.security

/** Persistence for the kid-safe PIN and its failed-attempt counters. */
interface PinStorage {
    fun readPin(): StoredPin?
    fun writePin(pin: StoredPin)
    var failedAttempts: Int
    var lockedUntilMillis: Long
}

sealed interface PinCheck {
    data object Success : PinCheck
    data object NoPinSet : PinCheck
    data class Incorrect(val attemptsLeft: Int) : PinCheck
    data class LockedOut(val retryInMillis: Long) : PinCheck
}

/**
 * Sets and verifies the parental PIN that guards exiting kid-safe mode.
 *
 * After [MAX_ATTEMPTS] wrong guesses, further attempts are refused for [LOCKOUT_MILLIS],
 * which makes brute-forcing a 4-digit PIN from the remote impractical.
 */
class KidSafePinManager(
    private val storage: PinStorage,
    private val hasher: PinHasher = PinHasher(),
    private val clock: () -> Long = System::currentTimeMillis
) {
    fun hasPin(): Boolean = storage.readPin() != null

    fun setPin(pin: String) {
        require(isValidPin(pin)) { "PIN must be $MIN_LENGTH–$MAX_LENGTH digits." }
        storage.writePin(hasher.create(pin))
        storage.failedAttempts = 0
        storage.lockedUntilMillis = 0L
    }

    fun verify(pin: String): PinCheck {
        val stored = storage.readPin() ?: return PinCheck.NoPinSet
        val now = clock()
        val lockedUntil = storage.lockedUntilMillis
        if (lockedUntil > now) return PinCheck.LockedOut(lockedUntil - now)

        if (hasher.matches(pin, stored)) {
            storage.failedAttempts = 0
            storage.lockedUntilMillis = 0L
            return PinCheck.Success
        }

        val failures = storage.failedAttempts + 1
        return if (failures >= MAX_ATTEMPTS) {
            storage.failedAttempts = 0
            storage.lockedUntilMillis = now + LOCKOUT_MILLIS
            PinCheck.LockedOut(LOCKOUT_MILLIS)
        } else {
            storage.failedAttempts = failures
            PinCheck.Incorrect(MAX_ATTEMPTS - failures)
        }
    }

    companion object {
        const val MIN_LENGTH = 4
        const val MAX_LENGTH = 8
        const val MAX_ATTEMPTS = 5
        const val LOCKOUT_MILLIS = 60_000L

        fun isValidPin(pin: String): Boolean =
            pin.length in MIN_LENGTH..MAX_LENGTH && pin.all { it in '0'..'9' }
    }
}
