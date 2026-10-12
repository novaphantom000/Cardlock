package com.cardlock.app

import com.cardlock.app.util.CardMasker
import org.junit.Assert.*
import org.junit.Test

class CardMaskerTest {

    @Test
    fun testBrandDetection() {
        assertEquals(CardMasker.CardBrand.VISA, CardMasker.detectBrand("4111222233334444"))
        assertEquals(CardMasker.CardBrand.MASTERCARD, CardMasker.detectBrand("5100000000000000"))
        assertEquals(CardMasker.CardBrand.AMEX, CardMasker.detectBrand("378282246310005"))
        assertEquals(CardMasker.CardBrand.DISCOVER, CardMasker.detectBrand("6011000000000000"))
        assertEquals(CardMasker.CardBrand.UNKNOWN, CardMasker.detectBrand("0000000000000000"))
    }

    @Test
    fun testCardMasking() {
        assertEquals("•••• •••• •••• 4444", CardMasker.getMaskedCardNumber("4111222233334444"))
        assertEquals("•••• •••••• •0005", CardMasker.getMaskedCardNumber("378282246310005"))
    }

    @Test
    fun testCardFormatting() {
        assertEquals("4111 2222 3333 4444", CardMasker.formatCardNumber("4111222233334444"))
        assertEquals("3782 822463 10005", CardMasker.formatCardNumber("378282246310005"))
    }
}
