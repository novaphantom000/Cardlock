package com.cardlock.app.data.backup

import com.cardlock.app.data.crypto.CryptoManager
import com.cardlock.app.data.model.CardItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Base64

@Serializable
data class BackupPayload(
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val cards: List<CardItem>
)

@Serializable
data class EncryptedBackupFile(
    val format: String = "CARDLOCK_ENCRYPTED_BACKUP_V1",
    val saltBase64: String,
    val encryptedDataBase64: String
)

object BackupManager {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    /**
     * Exports cards into an encrypted JSON backup string using a password-derived key.
     */
    fun exportBackup(cards: List<CardItem>, backupPassword: CharArray): String {
        val payload = BackupPayload(cards = cards)
        val plainJson = json.encodeToString(payload)

        val salt = CryptoManager.generateRandomBytes(CryptoManager.SALT_LENGTH_BYTES)
        val backupKey = CryptoManager.deriveKey(backupPassword, salt)

        val encryptedBytes = CryptoManager.encryptString(plainJson, backupKey)

        val backupFile = EncryptedBackupFile(
            saltBase64 = Base64.getEncoder().encodeToString(salt),
            encryptedDataBase64 = Base64.getEncoder().encodeToString(encryptedBytes)
        )

        return json.encodeToString(backupFile)
    }

    /**
     * Imports cards from an encrypted JSON backup string using password.
     * Throws exception if wrong password or payload tampered.
     */
    fun importBackup(backupJsonContent: String, backupPassword: CharArray): List<CardItem> {
        val backupFile = json.decodeFromString<EncryptedBackupFile>(backupJsonContent)
        require(backupFile.format == "CARDLOCK_ENCRYPTED_BACKUP_V1") { "Invalid backup format" }

        val salt = Base64.getDecoder().decode(backupFile.saltBase64)
        val encryptedData = Base64.getDecoder().decode(backupFile.encryptedDataBase64)

        val backupKey = CryptoManager.deriveKey(backupPassword, salt)
        val decryptedJson = CryptoManager.decryptString(encryptedData, backupKey)

        val payload = json.decodeFromString<BackupPayload>(decryptedJson)
        return payload.cards
    }
}
