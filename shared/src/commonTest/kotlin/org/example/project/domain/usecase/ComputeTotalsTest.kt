package org.example.project.domain.usecase

import kotlinx.datetime.LocalDate
import org.example.project.core.Money
import org.example.project.domain.model.CompanyProfile
import org.example.project.domain.model.DocumentType
import org.example.project.domain.model.Invoice
import org.example.project.domain.model.LineItem
import org.example.project.domain.model.PaymentMode
import kotlin.test.Test
import kotlin.test.assertEquals

class ComputeTotalsTest {

    private val compute = ComputeTotals()

    @Test
    fun single_line_no_discount_bank_transfer() {
        val breakdown = compute.compute(
            lines = listOf(
                LineItem("Design", qty = 2.0, unitPriceHT = Money.ofDinars(1_000), vatPct = 19.0),
            ),
            paymentMode = PaymentMode.BANK_TRANSFER,
        )
        assertEquals(Money.ofDinars(2_000), breakdown.totalHT)
        assertEquals(Money.ZERO, breakdown.totalDiscount)
        assertEquals(Money.ofDinars(2_000), breakdown.vatBase)
        assertEquals(Money.ofDinars(380), breakdown.vatAmount)
        assertEquals(Money.ZERO, breakdown.stampTax)        // not cash
        assertEquals(Money.ofDinars(2_380), breakdown.totalTTC)
    }

    @Test
    fun discount_and_cash_stamp_tax() {
        val breakdown = compute.compute(
            lines = listOf(
                LineItem("Print run", qty = 1.0, unitPriceHT = Money.ofDinars(1_000), discountPct = 10.0, vatPct = 19.0),
            ),
            paymentMode = PaymentMode.CASH,
        )
        assertEquals(Money.ofDinars(1_000), breakdown.totalHT)
        assertEquals(Money.ofDinars(100), breakdown.totalDiscount)
        assertEquals(Money.ofDinars(900), breakdown.vatBase)
        assertEquals(Money.ofDinars(171), breakdown.vatAmount)
        // TTC before stamp = 1071 DA -> 11 slices * 1% -> 11 DA
        assertEquals(Money.ofDinars(11), breakdown.stampTax)
        assertEquals(Money.ofDinars(1_082), breakdown.totalTTC)
    }

    @Test
    fun multiple_lines_sum_correctly() {
        val breakdown = compute.compute(
            lines = listOf(
                LineItem("A", qty = 3.0, unitPriceHT = Money.ofDinars(500), vatPct = 19.0),  // 1500
                LineItem("B", qty = 2.0, unitPriceHT = Money.ofDinars(250), vatPct = 9.0),    // 500
            ),
            paymentMode = PaymentMode.BANK_TRANSFER,
        )
        assertEquals(Money.ofDinars(2_000), breakdown.totalHT)
        assertEquals(Money.ofDinars(2_000), breakdown.vatBase)
        // 1500*19% = 285 ; 500*9% = 45 -> 330
        assertEquals(Money.ofDinars(330), breakdown.vatAmount)
        assertEquals(Money.ofDinars(2_330), breakdown.totalTTC)
    }

    @Test
    fun vat_exempt_company_charges_no_vat() {
        val invoice = Invoice(
            number = "F-2026-001",
            type = DocumentType.INVOICE,
            clientId = 1,
            issueDate = LocalDate(2026, 1, 1),
            paymentMode = PaymentMode.BANK_TRANSFER,
            lines = listOf(LineItem("Service", qty = 2.0, unitPriceHT = Money.ofDinars(1_000))),
        )
        val company = CompanyProfile(name = "SG Media", vatExempt = true)
        val breakdown = compute(invoice, company)
        assertEquals(Money.ofDinars(2_000), breakdown.vatBase)
        assertEquals(Money.ZERO, breakdown.vatAmount)
        assertEquals(Money.ofDinars(2_000), breakdown.totalTTC)
    }

    @Test
    fun personal_mode_invoice_computes_totals() {
        // Personal-mode documents have no B2B legal fields, but totals are computed identically.
        val invoice = Invoice(
            number = "F-2026-002",
            type = DocumentType.INVOICE,
            clientId = 2,
            issueDate = LocalDate(2026, 1, 2),
            paymentMode = PaymentMode.CASH,
            lines = listOf(LineItem("Freelance work", qty = 1.0, unitPriceHT = Money.ofDinars(10_000))),
        )
        val breakdown = compute(invoice)
        assertEquals(Money.ofDinars(10_000), breakdown.vatBase)
        assertEquals(Money.ofDinars(1_900), breakdown.vatAmount)        // 19%
        // TTC before stamp = 11 900 DA -> 119 slices * 1% -> 119 DA
        assertEquals(Money.ofDinars(119), breakdown.stampTax)
        assertEquals(Money.ofDinars(12_019), breakdown.totalTTC)
    }
}
