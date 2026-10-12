package com.cardlock.app

import com.cardlock.app.data.backup.BackupManager
import com.cardlock.app.data.model.CardItem
import org.junit.Assert.*
import org.junit.Test

class BackupRestoreTest {

    @Test
    fun testBackupExportAndImportSuccess() {
        val cards = listOf(
            CardItem(
                id = "card-1",
                title = "Primary Visa",
                cardholderName = "Jane Doe",
                cardNumber = "4111 2222 3333 4444",
                expiryMonth = "12",
                expiryYear = "28",
                cardBrand = "Visa",
                notes = "Personal account"
            ),
            CardItem(
                id = "card-2",
                title = "Work Mastercard",
                cardholderName = "Jane Doe",
                cardNumber = "5100 0000 0000 0000",
                expiryMonth = "06",
                expiryYear = "27",
                cardBrand = "Mastercard"
            )
        )

        val backupPassword = "BackupPassword99!".toCharArray()

        val backupJson = BackupManager.exportBackup(cards, backupPassword)
        assertNotNull(backupJson)
        assertTrue(backupJson.contains("CARDLOCK_ENCRYPTED_BACKUP_V1"))
        assertFalse("Backup file must not contain raw card numbers", backupJson.contains("4111 2222 3333 4444"))

        val restoredCards = BackupManager.importBackup(backupJson, backupPassword)
        assertEquals(2, restoredCards.size)
        assertEquals("Primary Visa", restoredCards[0].title)
        assertEquals("4111 2222 3333 4444", restoredCards[0].cardNumber)
        assertEquals("Work Mastercard", restoredCards[1].title)
    }

    @Test
    fun testBackupImportWithWrongPasswordFails() {
        val cards = listOf(
            CardItem(
                id = "card-1",
                title = "Travel Amex",
                cardholderName = "John Smith",
                cardNumber = "3782 822463 10005",
                expiryMonth = "08",
                expiryYear = "29",
                cardBrand = "American Express"
            )
        )

        val correctPassword = "CorrectBackupPass123".toCharArray()
        val wrongPassword = "WrongBackupPass123".toCharArray()

        val backupJson = BackupManager.exportBackup(cards, correctPassword)

        try {
            BackupManager.importBackup(backupJson, wrongPassword)
            fail("Importing with wrong password should fail decryption")
        } catch (e: Exception) {
            // Expected exception
        }
    }
}
