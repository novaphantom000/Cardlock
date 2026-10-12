package com.cardlock.app.data.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val AES_GCM_ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_SIZE_BITS = 256
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val GCM_IV_LENGTH_BYTES = 12
    const val PBKDF2_ITERATIONS = 120_000
    const val SALT_LENGTH_BYTES = 16

    private val secureRandom = SecureRandom()

    fun generateRandomBytes(length: Int): ByteArray {
        val bytes = ByteArray(length)
        secureRandom.nextBytes(bytes)
        return bytes
    }

    /**
     * Derives a 256-bit AES key from a passphrase char array and salt using PBKDF2-HMAC-SHA256.
     */
    fun deriveKey(passphrase: CharArray, salt: ByteArray, iterations: Int = PBKDF2_ITERATIONS): SecretKey {
        val spec = PBEKeySpec(passphrase, salt, iterations, KEY_SIZE_BITS)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        spec.clearPassword()
        val secretKey = SecretKeySpec(keyBytes, "AES")
        zeroizeByteArray(keyBytes)
        return secretKey
    }

    /**
     * Encrypts plaintext bytes using AES-256-GCM.
     * Output format: [12 bytes IV][Ciphertext + 16 bytes GCM Tag]
     */
    fun encrypt(plainText: ByteArray, secretKey: SecretKey): ByteArray {
        val iv = generateRandomBytes(GCM_IV_LENGTH_BYTES)
        val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)

        val cipherText = cipher.doFinal(plainText)
        val result = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, result, 0, iv.size)
        System.arraycopy(cipherText, 0, result, iv.size, cipherText.size)
        return result
    }

    /**
     * Decrypts ciphertext bytes using AES-256-GCM.
     * Expects payload format: [12 bytes IV][Ciphertext + 16 bytes GCM Tag]
     */
    fun decrypt(encryptedPayload: ByteArray, secretKey: SecretKey): ByteArray {
        require(encryptedPayload.size > GCM_IV_LENGTH_BYTES) { "Invalid payload length" }

        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        System.arraycopy(encryptedPayload, 0, iv, 0, GCM_IV_LENGTH_BYTES)

        val cipherTextSize = encryptedPayload.size - GCM_IV_LENGTH_BYTES
        val cipherText = ByteArray(cipherTextSize)
        System.arraycopy(encryptedPayload, GCM_IV_LENGTH_BYTES, cipherText, 0, cipherTextSize)

        val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)

        return cipher.doFinal(cipherText)
    }

    /**
     * Overload for String encryption helper
     */
    fun encryptString(plainText: String, secretKey: SecretKey): ByteArray {
        val bytes = plainText.toByteArray(Charsets.UTF_8)
        val encrypted = encrypt(bytes, secretKey)
        zeroizeByteArray(bytes)
        return encrypted
    }

    /**
     * Overload for String decryption helper
     */
    fun decryptString(encryptedPayload: ByteArray, secretKey: SecretKey): String {
        val decryptedBytes = decrypt(encryptedPayload, secretKey)
        val result = String(decryptedBytes, Charsets.UTF_8)
        zeroizeByteArray(decryptedBytes)
        return result
    }

    /**
     * Securely zero out a CharArray in memory.
     */
    fun zeroizeCharArray(array: CharArray) {
        array.fill('\u0000')
    }

    /**
     * Securely zero out a ByteArray in memory.
     */
    fun zeroizeByteArray(array: ByteArray) {
        array.fill(0)
    }
}
