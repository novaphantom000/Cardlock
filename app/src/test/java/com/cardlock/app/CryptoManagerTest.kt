package com.cardlock.app

import com.cardlock.app.data.crypto.CryptoManager
import org.junit.Assert.*
import org.junit.Test
import javax.crypto.AEADBadTagException

class CryptoManagerTest {

    @Test
    fun testEncryptionAndDecryptionSuccess() {
        val password = "MySuperSecretPassword123!".toCharArray()
        val salt = CryptoManager.generateRandomBytes(CryptoManager.SALT_LENGTH_BYTES)
        val key = CryptoManager.deriveKey(password, salt)

        val secretMessage = "4111 2222 3333 4444"
        val encryptedPayload = CryptoManager.encryptString(secretMessage, key)

        assertNotNull(encryptedPayload)
        assertTrue(encryptedPayload.size > 12) // Includes 12-byte IV + ciphertext + tag

        val decryptedMessage = CryptoManager.decryptString(encryptedPayload, key)
        assertEquals(secretMessage, decryptedMessage)
    }

    @Test
    fun testWrongPasswordFailsDecryption() {
        val correctPassword = "CorrectPassword123".toCharArray()
        val wrongPassword = "WrongPassword123".toCharArray()

        val salt = CryptoManager.generateRandomBytes(CryptoManager.SALT_LENGTH_BYTES)
        val correctKey = CryptoManager.deriveKey(correctPassword, salt)
        val wrongKey = CryptoManager.deriveKey(wrongPassword, salt)

        val secretMessage = "Sensitive Card Data"
        val encryptedPayload = CryptoManager.encryptString(secretMessage, correctKey)

        try {
            CryptoManager.decryptString(encryptedPayload, wrongKey)
            fail("Expected AEADBadTagException or Exception when decrypting with wrong key")
        } catch (e: Exception) {
            // Expected failure (AEADBadTagException)
            assertTrue(e is AEADBadTagException || e.cause is AEADBadTagException)
        }
    }

    @Test
    fun testTamperedCiphertextFailsDecryption() {
        val password = "StrongPassword!".toCharArray()
        val salt = CryptoManager.generateRandomBytes(CryptoManager.SALT_LENGTH_BYTES)
        val key = CryptoManager.deriveKey(password, salt)

        val encryptedPayload = CryptoManager.encryptString("Secret Vault Data", key)

        // Tamper with payload byte
        encryptedPayload[encryptedPayload.size - 1] = (encryptedPayload[encryptedPayload.size - 1].toInt() xor 0xFF).toByte()

        try {
            CryptoManager.decryptString(encryptedPayload, key)
            fail("Expected exception for tampered payload")
        } catch (e: Exception) {
            // Success: authentication tag check failed
        }
    }

    @Test
    fun testZeroizeArray() {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        CryptoManager.zeroizeByteArray(bytes)
        assertArrayEquals(byteArrayOf(0, 0, 0, 0, 0), bytes)

        val chars = charArrayOf('a', 'b', 'c', 'd')
        CryptoManager.zeroizeCharArray(chars)
        assertArrayEquals(charArrayOf('\u0000', '\u0000', '\u0000', '\u0000'), chars)
    }
}
