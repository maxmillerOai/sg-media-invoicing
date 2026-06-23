package org.example.project.domain.tax

import org.example.project.core.Money
import org.example.project.domain.model.PaymentMode
import kotlin.test.Test
import kotlin.test.assertEquals

class AlgerianTaxCalculatorTest {

    private fun stampCash(da: Long) =
        AlgerianTaxCalculator.stampTax(Money.ofDinars(da), PaymentMode.CASH)

    @Test
    fun below_threshold_has_no_stamp_tax() {
        assertEquals(Money.ZERO, stampCash(0))
        assertEquals(Money.ZERO, stampCash(299))
    }

    @Test
    fun bracket_1_is_one_percent() {
        // 300 DA -> 3 slices * 1% -> 3 DA
        assertEquals(Money.ofDinars(3), stampCash(300))
        // 30 000 DA -> 300 slices * 1% -> 300 DA (upper edge of bracket 1)
        assertEquals(Money.ofDinars(300), stampCash(30_000))
    }

    @Test
    fun bracket_2_is_one_and_half_percent() {
        // 30 001 DA -> rounds up to 30 100 (301 slices) * 1.5% -> 451.50 DA
        assertEquals(Money.ofMajor(451.50), stampCash(30_001))
        // 100 000 DA -> 1000 slices * 1.5% -> 1500 DA (upper edge of bracket 2)
        assertEquals(Money.ofDinars(1_500), stampCash(100_000))
    }

    @Test
    fun bracket_3_is_two_percent() {
        // 100 001 DA -> rounds up to 100 100 (1001 slices) * 2% -> 2002 DA
        assertEquals(Money.ofDinars(2_002), stampCash(100_001))
    }

    @Test
    fun stamp_tax_only_applies_to_cash() {
        assertEquals(Money.ZERO, AlgerianTaxCalculator.stampTax(Money.ofDinars(50_000), PaymentMode.BANK_TRANSFER))
        assertEquals(Money.ZERO, AlgerianTaxCalculator.stampTax(Money.ofDinars(50_000), PaymentMode.CHEQUE))
        assertEquals(Money.ZERO, AlgerianTaxCalculator.stampTax(Money.ofDinars(50_000), PaymentMode.CARD))
    }

    @Test
    fun vat_computes_standard_rate() {
        assertEquals(Money.ofDinars(19), AlgerianTaxCalculator.vat(Money.ofDinars(100), 19.0))
        assertEquals(Money.ZERO, AlgerianTaxCalculator.vat(Money.ofDinars(100), 0.0))
    }
}
