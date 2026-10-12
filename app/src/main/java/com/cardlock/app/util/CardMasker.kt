package com.cardlock.app.util

object CardMasker {

    enum class CardBrand(val displayName: String) {
        VISA("Visa"),
        MASTERCARD("Mastercard"),
        AMEX("American Express"),
        DISCOVER("Discover"),
        DINERS_CLUB("Diners Club"),
        JCB("JCB"),
        UNIONPAY("UnionPay"),
        UNKNOWN("Generic Card")
    }

    /**
     * Identifies card brand based on card number prefixes/IIN.
     */
    fun detectBrand(cardNumber: String): CardBrand {
        val clean = cardNumber.filter { it.isDigit() }
        if (clean.isEmpty()) return CardBrand.UNKNOWN

        return when {
            clean.startsWith("4") -> CardBrand.VISA
            clean.matches(Regex("^(5[1-5]|2[2-7]).*")) -> CardBrand.MASTERCARD
            clean.matches(Regex("^3[47].*")) -> CardBrand.AMEX
            clean.matches(Regex("^(6011|65|64[4-9]|622).*")) -> CardBrand.DISCOVER
            clean.matches(Regex("^3(0[0-5]|[68]).*")) -> CardBrand.DINERS_CLUB
            clean.matches(Regex("^(352[89]|35[3-8]).*")) -> CardBrand.JCB
            clean.startsWith("62") -> CardBrand.UNIONPAY
            else -> CardBrand.UNKNOWN
        }
    }

    /**
     * Formats card digits into standard 4-digit groups (e.g. 1234 5678 1234 5678) or 4-6-5 for Amex.
     */
    fun formatCardNumber(cardNumber: String): String {
        val clean = cardNumber.filter { it.isDigit() }
        if (clean.isEmpty()) return ""

        val brand = detectBrand(clean)
        val sb = StringBuilder()

        if (brand == CardBrand.AMEX) {
            for (i in clean.indices) {
                if (i == 4 || i == 10) sb.append(" ")
                sb.append(clean[i])
            }
        } else {
            for (i in clean.indices) {
                if (i > 0 && i % 4 == 0) sb.append(" ")
                sb.append(clean[i])
            }
        }
        return sb.toString()
    }

    /**
     * Returns masked card number showing only last 4 digits by default.
     * Example: "•••• •••• •••• 4321" or "•••• •••••• •4321" for Amex.
     */
    fun getMaskedCardNumber(cardNumber: String): String {
        val clean = cardNumber.filter { it.isDigit() }
        if (clean.length < 4) return "••••"

        val last4 = clean.takeLast(4)
        val brand = detectBrand(clean)

        return if (brand == CardBrand.AMEX) {
            "•••• •••••• •$last4"
        } else {
            "•••• •••• •••• $last4"
        }
    }
}
