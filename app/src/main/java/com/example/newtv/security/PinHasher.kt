package com.example.newtv.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Derives a salted PBKDF2 hash from a PIN so the PIN itself is never stored.
 *
 * Hashing is deliberately slow; call it off the main thread.
 */
class PinHasher(
    private val iterations: Int = DEFAULT_ITERATIONS,
    private val random: SecureRandom = SecureRandom()
) {
    fun newSalt(): ByteArray = ByteArray(SALT_BYTES).also(random::nextBytes)

    fun hash(pin: String, salt: ByteArray, iterations: Int = this.iterations): ByteArray {
        val chars = pin.toCharArray()
        val spec = PBEKeySpec(chars, salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            chars.fill('\u0000')
        }
    }

    /** Constant-time comparison so response timing does not leak how much of a guess matched. */
    fun matches(pin: String, stored: StoredPin): Boolean =
        MessageDigest.isEqual(hash(pin, stored.salt, stored.iterations), stored.hash)

    fun create(pin: String): StoredPin {
        val salt = newSalt()
        return StoredPin(salt = salt, hash = hash(pin, salt), iterations = iterations)
    }

    companion object {
        const val ALGORITHM = "PBKDF2WithHmacSHA256"
        const val DEFAULT_ITERATIONS = 120_000
        private const val SALT_BYTES = 16
        private const val KEY_BITS = 256
    }
}

/** A salted PIN hash plus the iteration count used, so the cost can be raised later. */
class StoredPin(val salt: ByteArray, val hash: ByteArray, val iterations: Int)
