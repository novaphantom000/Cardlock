package com.cardlock.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Data representation of a credit/debit card stored inside the encrypted vault payload.
 *
 * PRIVACY GUARANTEE:
 * Cardlock NEVER requests or stores CVV/CVC, PINs, banking passwords, or OTPs.
 */
@Serializable
data class CardItem(
    val id: String,
    val title: String,            // E.g. "Primary Checking Visa" or "Personal Amex"
    val cardholderName: String,   // E.g. "Jane Doe"
    val cardNumber: String,       // Full card number (digits and spaces)
    val expiryMonth: String,      // MM (e.g. "08")
    val expiryYear: String,       // YY or YYYY (e.g. "28" or "2028")
    val cardBrand: String,        // Visa, Mastercard, Amex, Discover, etc.
    val notes: String = "",       // Optional user notes
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Room Entity storing encrypted card data in SQLite.
 * The payload is an AES-256-GCM encrypted JSON string representation of [CardItem].
 */
@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val id: String,
    val encryptedPayload: ByteArray,
    val createdAt: Long,
    val updatedAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CardEntity

        if (id != other.id) return false
        if (!encryptedPayload.contentEquals(other.encryptedPayload)) return false
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + encryptedPayload.contentHashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        return result
    }
}

/**
 * Metadata stored in local SharedPreferences / KeyStore to verify master password
 * without storing the master password itself.
 */
data class VaultMetadata(
    val salt: ByteArray,
    val verificationHash: ByteArray, // Encrypted verification token ("CARDLOCK_VERIFY") using derived key
    val isBiometricsEnabled: Boolean = false,
    val wrappedKeyForBiometrics: ByteArray? = null,
    val biometricIv: ByteArray? = null
)
