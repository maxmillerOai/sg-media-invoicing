package org.example.project.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MoneyTest {

    @Test
    fun constructors_use_minor_units() {
        assertEquals(10_000, Money.ofDinars(100).amountMinor)
        assertEquals(123_456, Money.ofMajor(1234.56).amountMinor)
        assertEquals(0, Money.ZERO.amountMinor)
    }

    @Test
    fun addition_and_subtraction_are_exact() {
        assertEquals(Money.ofDinars(150), Money.ofDinars(100) + Money.ofDinars(50))
        assertEquals(Money.ofDinars(50), Money.ofDinars(100) - Money.ofDinars(50))
        assertEquals(Money(-5000), -Money.ofDinars(50))
    }

    @Test
    fun multiply_by_quantity() {
        assertEquals(Money.ofDinars(200), Money.ofDinars(100) * 2.0)
        assertEquals(Money.ofDinars(30), Money.ofMajor(10.0) * 3.0)
    }

    @Test
    fun percent_rounds_half_up() {
        assertEquals(Money.ofDinars(19), Money.ofDinars(100).percent(19.0))
        // 33.33 DA * 19% = 6.3327 DA -> 633 centimes
        assertEquals(Money(633), Money.ofMajor(33.33).percent(19.0))
    }

    @Test
    fun format_uses_grouping_and_comma() {
        assertEquals("1 234,56 DA", Money.ofMajor(1234.56).format())
        assertEquals("0,00 DA", Money.ZERO.format())
        assertEquals("-1 234,56 DA", Money.ofMajor(-1234.56).format())
        assertEquals("1 000 000,00 DA", Money.ofDinars(1_000_000).format())
        assertEquals("1 234,56", Money.ofMajor(1234.56).format(withSymbol = false))
    }

    @Test
    fun parse_accepts_common_formats() {
        assertEquals(Money(123_456), Money.parse("1234.56"))
        assertEquals(Money(123_456), Money.parse("1 234,56"))
        assertEquals(Money(123_456), Money.parse("1234,56 DA"))
        assertEquals(Money(-5000), Money.parse("-50"))
        assertNull(Money.parse(""))
        assertNull(Money.parse("abc"))
    }

    @Test
    fun comparison_and_flags() {
        assertTrue(Money.ofDinars(100) > Money.ofDinars(50))
        assertTrue(Money.ZERO.isZero)
        assertTrue(Money(-1).isNegative)
    }
}
