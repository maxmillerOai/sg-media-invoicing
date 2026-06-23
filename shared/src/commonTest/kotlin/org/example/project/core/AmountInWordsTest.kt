package org.example.project.core

import kotlin.test.Test
import kotlin.test.assertEquals

class AmountInWordsTest {

    @Test
    fun spells_invoice_total() {
        assertEquals(
            "Cent vingt-huit mille soixante-six dinars algériens",
            amountInWordsDZD(Money.ofDinars(128_066)),
        )
    }

    @Test
    fun french_special_tens() {
        assertEquals("Soixante et onze dinars algériens", amountInWordsDZD(Money.ofDinars(71)))
        assertEquals("Quatre-vingts dinars algériens", amountInWordsDZD(Money.ofDinars(80)))
        assertEquals("Quatre-vingt-un dinars algériens", amountInWordsDZD(Money.ofDinars(81)))
        assertEquals("Quatre-vingt-dix-sept dinars algériens", amountInWordsDZD(Money.ofDinars(97)))
    }

    @Test
    fun hundreds_plural_rules() {
        assertEquals("Deux cents dinars algériens", amountInWordsDZD(Money.ofDinars(200)))
        assertEquals("Deux cent un dinars algériens", amountInWordsDZD(Money.ofDinars(201)))
    }

    @Test
    fun singular_and_centimes() {
        assertEquals("Un dinar algérien", amountInWordsDZD(Money.ofDinars(1)))
        assertEquals("Zéro dinar algérien", amountInWordsDZD(Money.ZERO))
        assertEquals(
            "Cent vingt-cinq dinars algériens et cinquante centimes",
            amountInWordsDZD(Money.ofMajor(125.50)),
        )
    }
}
