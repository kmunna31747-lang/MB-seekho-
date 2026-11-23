package com.example.data.local

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object CryptoUtils {
    private val secureRandom = SecureRandom()
    private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private const val PBKDF2_ITERATIONS = 65_536
    private const val PBKDF2_KEY_LENGTH_BITS = 256

    fun normalizeCode(rawCode: String): String =
        rawCode.trim().uppercase()

    fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun generateSaltHex(byteCount: Int = 16): String {
        val bytes = ByteArray(byteCount)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashOwnerPassphrase(passphrase: String, saltHex: String): String {
        val spec = PBEKeySpec(
            passphrase.toCharArray(),
            saltHex.toByteArray(Charsets.UTF_8),
            PBKDF2_ITERATIONS,
            PBKDF2_KEY_LENGTH_BITS
        )
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun verifyOwnerPassphrase(passphrase: String, saltHex: String, expectedHashHex: String): Boolean {
        val actual = hashOwnerPassphrase(passphrase, saltHex)
        return constantTimeEquals(actual, expectedHashHex)
    }

    fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) {
            diff = diff or (a[i].code xor b[i].code)
        }
        return diff == 0
    }

    /**
     * Generates a cryptographically strong new access code matching MB Seekho format:
     * Student: MBST-XXXX-XXXX
     * Teacher: MBTC-XXXX-XXXX
     */
    fun generateRawAccessCode(prefix: String): String {
        fun chunk(len: Int): String = buildString(len) {
            repeat(len) {
                append(CODE_ALPHABET[secureRandom.nextInt(CODE_ALPHABET.length)])
            }
        }
        return "$prefix-${chunk(4)}-${chunk(4)}"
    }

    fun generateUniqueUid(prefix: String): String {
        val bytes = ByteArray(8)
        secureRandom.nextBytes(bytes)
        val hex = bytes.joinToString("") { "%02x".format(it) }.uppercase()
        return "${prefix}_$hex"
    }

    fun escapeCsvField(value: String?): String {
        val safe = value.orEmpty()
        return if (safe.contains(',') || safe.contains('"') || safe.contains('\n')) {
            "\"${safe.replace("\"", "\"\"")}\""
        } else {
            safe
        }
    }
}
