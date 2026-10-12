package com.cardlock.app.data.crypto

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeystoreHelper {

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    fun getOrCreateBiometricKey(alias: String = BIOMETRIC_KEY_ALIAS): SecretKey {
        if (keyStore.containsAlias(alias)) {
            val entry = keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry
            if (entry?.secretKey != null) {
                return entry.secretKey
            }
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val builder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG
            )
        }

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    fun getInitializedCipherForEncryption(alias: String = BIOMETRIC_KEY_ALIAS): Cipher {
        val entry = keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, entry.secretKey)
        return cipher
    }

    fun getInitializedCipherForDecryption(alias: String = BIOMETRIC_KEY_ALIAS, iv: ByteArray): Cipher {
        val entry = keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry
        val cipher = Cipher.getInstance(AES_GCM)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, entry.secretKey, spec)
        return cipher
    }

    fun encryptWithCipher(cipher: Cipher, data: ByteArray): Pair<ByteArray, ByteArray> {
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data)
        return Pair(iv, encryptedData)
    }

    fun decryptWithCipher(cipher: Cipher, encryptedData: ByteArray): ByteArray {
        return cipher.doFinal(encryptedData)
    }

    fun removeKey(alias: String = BIOMETRIC_KEY_ALIAS) {
        if (keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias)
        }
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val BIOMETRIC_KEY_ALIAS = "CardlockBiometricKey"
        private const val AES_GCM = "AES/GCM/NoPadding"
    }
}
