package com.cardlock.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.cardlock.app.data.crypto.CryptoManager
import com.cardlock.app.data.crypto.KeystoreHelper
import com.cardlock.app.data.db.CardDao
import com.cardlock.app.data.db.VaultDatabase
import com.cardlock.app.data.model.CardEntity
import com.cardlock.app.data.model.CardItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class VaultRepository(
    private val context: Context,
    private val cardDao: CardDao = VaultDatabase.getDatabase(context).cardDao(),
    private val keystoreHelper: KeystoreHelper = KeystoreHelper()
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var activeMasterKey: SecretKey? = null

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    fun isVaultInitialized(): Boolean {
        return prefs.contains(KEY_SALT) && prefs.contains(KEY_VERIFICATION)
    }

    /**
     * Initializes a new vault with the given master password.
     */
    fun setupVault(password: CharArray): Boolean {
        try {
            val salt = CryptoManager.generateRandomBytes(CryptoManager.SALT_LENGTH_BYTES)
            val derivedKey = CryptoManager.deriveKey(password, salt)

            val verifyToken = VERIFICATION_MAGIC.toByteArray(Charsets.UTF_8)
            val encryptedVerifyToken = CryptoManager.encrypt(verifyToken, derivedKey)

            prefs.edit()
                .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString(KEY_VERIFICATION, Base64.encodeToString(encryptedVerifyToken, Base64.NO_WRAP))
                .apply()

            activeMasterKey = derivedKey
            _isUnlocked.value = true
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Unlocks the vault using master password.
     */
    fun unlockWithPassword(password: CharArray): Boolean {
        val saltBase64 = prefs.getString(KEY_SALT, null) ?: return false
        val verifyBase64 = prefs.getString(KEY_VERIFICATION, null) ?: return false

        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val encryptedVerifyToken = Base64.decode(verifyBase64, Base64.NO_WRAP)

        return try {
            val derivedKey = CryptoManager.deriveKey(password, salt)
            val decryptedBytes = CryptoManager.decrypt(encryptedVerifyToken, derivedKey)
            val decryptedString = String(decryptedBytes, Charsets.UTF_8)

            if (decryptedString == VERIFICATION_MAGIC) {
                activeMasterKey = derivedKey
                _isUnlocked.value = true
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Lock vault and clear key from memory.
     */
    fun lockVault() {
        activeMasterKey = null
        _isUnlocked.value = false
    }

    /**
     * Check if biometrics is enabled.
     */
    fun isBiometricsEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRICS_ENABLED, false) &&
                prefs.contains(KEY_BIOMETRIC_WRAPPED_KEY) &&
                prefs.contains(KEY_BIOMETRIC_IV)
    }

    fun getBiometricCipherForEnable(): Cipher? {
        return try {
            keystoreHelper.getOrCreateBiometricKey()
            keystoreHelper.getInitializedCipherForEncryption()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getBiometricCipherForUnlock(): Cipher? {
        if (!isBiometricsEnabled()) return null
        val ivBase64 = prefs.getString(KEY_BIOMETRIC_IV, null) ?: return null
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        return try {
            keystoreHelper.getInitializedCipherForDecryption(iv = iv)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Enable biometrics after authenticated cipher scan.
     */
    fun enableBiometricsWithAuthenticatedCipher(authenticatedCipher: Cipher): Boolean {
        val masterKey = activeMasterKey ?: return false
        return try {
            val (iv, wrappedKey) = keystoreHelper.encryptWithCipher(authenticatedCipher, masterKey.encoded)
            prefs.edit()
                .putBoolean(KEY_BIOMETRICS_ENABLED, true)
                .putString(KEY_BIOMETRIC_WRAPPED_KEY, Base64.encodeToString(wrappedKey, Base64.NO_WRAP))
                .putString(KEY_BIOMETRIC_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Disable biometrics.
     */
    fun disableBiometrics() {
        prefs.edit()
            .putBoolean(KEY_BIOMETRICS_ENABLED, false)
            .remove(KEY_BIOMETRIC_WRAPPED_KEY)
            .remove(KEY_BIOMETRIC_IV)
            .apply()
        keystoreHelper.removeKey()
    }

    /**
     * Unlock vault using authenticated biometric cipher.
     */
    fun unlockWithAuthenticatedBiometricCipher(authenticatedCipher: Cipher): Boolean {
        if (!isBiometricsEnabled()) return false
        val wrappedKeyBase64 = prefs.getString(KEY_BIOMETRIC_WRAPPED_KEY, null) ?: return false
        val wrappedKey = Base64.decode(wrappedKeyBase64, Base64.NO_WRAP)

        return try {
            val keyBytes = keystoreHelper.decryptWithCipher(authenticatedCipher, wrappedKey)
            activeMasterKey = SecretKeySpec(keyBytes, "AES")
            _isUnlocked.value = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * CRUD: Get all cards decrypted.
     */
    suspend fun getAllCards(): List<CardItem> {
        val masterKey = activeMasterKey ?: return emptyList()
        val entities = cardDao.getAllCards()
        return entities.mapNotNull { entity ->
            try {
                val jsonString = CryptoManager.decryptString(entity.encryptedPayload, masterKey)
                Json.decodeFromString<CardItem>(jsonString)
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * CRUD: Save card (encrypt and insert/update).
     */
    suspend fun saveCard(card: CardItem): Boolean {
        val masterKey = activeMasterKey ?: return false
        return try {
            val jsonString = Json.encodeToString(card)
            val encryptedPayload = CryptoManager.encryptString(jsonString, masterKey)
            val entity = CardEntity(
                id = card.id,
                encryptedPayload = encryptedPayload,
                createdAt = card.createdAt,
                updatedAt = System.currentTimeMillis()
            )
            cardDao.insertOrUpdateCard(entity)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * CRUD: Delete card.
     */
    suspend fun deleteCard(cardId: String): Boolean {
        if (activeMasterKey == null) return false
        return try {
            cardDao.deleteCardById(cardId)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getActiveMasterKey(): SecretKey? = activeMasterKey

    companion object {
        private const val PREFS_NAME = "cardlock_prefs"
        private const val KEY_SALT = "vault_salt"
        private const val KEY_VERIFICATION = "vault_verification"
        private const val KEY_BIOMETRICS_ENABLED = "biometrics_enabled"
        private const val KEY_BIOMETRIC_WRAPPED_KEY = "biometric_wrapped_key"
        private const val KEY_BIOMETRIC_IV = "biometric_iv"
        private const val VERIFICATION_MAGIC = "CARDLOCK_VERIFY_V1"
    }
}
