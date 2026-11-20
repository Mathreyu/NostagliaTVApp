package com.example.newtv

import com.example.newtv.security.KidSafePinManager
import com.example.newtv.security.PinCheck
import com.example.newtv.security.PinHasher
import com.example.newtv.security.PinStorage
import com.example.newtv.security.StoredPin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KidSafePinManagerTest {
    private val hasher = PinHasher(iterations = 1_000)
    private lateinit var storage: InMemoryPinStorage
    private var now = 1_000_000L
    private lateinit var manager: KidSafePinManager

    @Before
    fun setUp() {
        storage = InMemoryPinStorage()
        manager = KidSafePinManager(storage, hasher) { now }
    }

    @Test
    fun noPinSetByDefault() {
        assertFalse(manager.hasPin())
        assertEquals(PinCheck.NoPinSet, manager.verify("1234"))
    }

    @Test
    fun storesHashNotPlainPin() {
        manager.setPin("4821")
        val stored = storage.readPin()!!
        assertNotEquals("4821", String(stored.hash, Charsets.UTF_8))
        assertEquals(16, stored.salt.size)
        assertEquals(32, stored.hash.size)
    }

    @Test
    fun sameValueGetsDifferentSalts() {
        val first = hasher.create("4821")
        val second = hasher.create("4821")
        assertFalse(first.salt.contentEquals(second.salt))
        assertFalse(first.hash.contentEquals(second.hash))
    }

    @Test
    fun correctPinSucceeds() {
        manager.setPin("4821")
        assertEquals(PinCheck.Success, manager.verify("4821"))
    }

    @Test
    fun wrongPinCountsDownAttempts() {
        manager.setPin("4821")
        assertEquals(PinCheck.Incorrect(4), manager.verify("0000"))
        assertEquals(PinCheck.Incorrect(3), manager.verify("1111"))
    }

    @Test
    fun locksOutAfterMaxAttemptsEvenForCorrectPin() {
        manager.setPin("4821")
        repeat(KidSafePinManager.MAX_ATTEMPTS - 1) { manager.verify("0000") }
        assertEquals(PinCheck.LockedOut(KidSafePinManager.LOCKOUT_MILLIS), manager.verify("0000"))

        now += 10_000L
        val locked = manager.verify("4821")
        assertTrue(locked is PinCheck.LockedOut)
        assertEquals(KidSafePinManager.LOCKOUT_MILLIS - 10_000L, (locked as PinCheck.LockedOut).retryInMillis)
    }

    @Test
    fun lockoutExpires() {
        manager.setPin("4821")
        repeat(KidSafePinManager.MAX_ATTEMPTS) { manager.verify("0000") }
        now += KidSafePinManager.LOCKOUT_MILLIS
        assertEquals(PinCheck.Success, manager.verify("4821"))
    }

    @Test
    fun successResetsFailureCount() {
        manager.setPin("4821")
        manager.verify("0000")
        manager.verify("0000")
        assertEquals(PinCheck.Success, manager.verify("4821"))
        assertEquals(PinCheck.Incorrect(4), manager.verify("0000"))
    }

    @Test
    fun validatesPinFormat() {
        assertTrue(KidSafePinManager.isValidPin("1234"))
        assertTrue(KidSafePinManager.isValidPin("12345678"))
        assertFalse(KidSafePinManager.isValidPin("123"))
        assertFalse(KidSafePinManager.isValidPin("123456789"))
        assertFalse(KidSafePinManager.isValidPin("12a4"))
        assertFalse(KidSafePinManager.isValidPin("١٢٣٤"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidPinOnSet() {
        manager.setPin("12")
    }

    private class InMemoryPinStorage : PinStorage {
        private var pin: StoredPin? = null
        override fun readPin(): StoredPin? = pin
        override fun writePin(pin: StoredPin) {
            this.pin = pin
        }
        override var failedAttempts: Int = 0
        override var lockedUntilMillis: Long = 0L
    }
}
