package org.example.project.domain.usecase

import kotlinx.datetime.LocalDate
import org.example.project.core.Money
import org.example.project.domain.model.DocumentStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class DocumentStateTest {

    private val today = LocalDate(2026, 6, 23)
    private val ttc = Money.ofDinars(10_000)

    @Test
    fun draft_stays_draft_and_owes_nothing() {
        assertEquals(DocumentStatus.DRAFT, effectiveStatus(DocumentStatus.DRAFT, ttc, Money.ZERO, null, today))
        assertEquals(Money.ZERO, outstanding(DocumentStatus.DRAFT, ttc, Money.ZERO))
    }

    @Test
    fun sent_unpaid_is_outstanding_for_full_amount() {
        assertEquals(DocumentStatus.SENT, effectiveStatus(DocumentStatus.SENT, ttc, Money.ZERO, null, today))
        assertEquals(ttc, outstanding(DocumentStatus.SENT, ttc, Money.ZERO))
    }

    @Test
    fun partial_payment_reads_partial_and_owes_remainder() {
        val paid = Money.ofDinars(4_000)
        assertEquals(DocumentStatus.PARTIAL, effectiveStatus(DocumentStatus.SENT, ttc, paid, null, today))
        assertEquals(Money.ofDinars(6_000), outstanding(DocumentStatus.SENT, ttc, paid))
    }

    @Test
    fun full_payment_reads_paid_and_owes_nothing() {
        assertEquals(DocumentStatus.PAID, effectiveStatus(DocumentStatus.SENT, ttc, ttc, null, today))
        assertEquals(Money.ZERO, outstanding(DocumentStatus.PAID, ttc, ttc))
    }

    @Test
    fun overpayment_still_reads_paid_and_owes_nothing() {
        val over = Money.ofDinars(12_000)
        assertEquals(DocumentStatus.PAID, effectiveStatus(DocumentStatus.SENT, ttc, over, null, today))
        assertEquals(Money.ZERO, outstanding(DocumentStatus.SENT, ttc, over))
    }

    @Test
    fun past_due_unpaid_reads_overdue() {
        val due = LocalDate(2026, 6, 1)
        assertEquals(DocumentStatus.OVERDUE, effectiveStatus(DocumentStatus.SENT, ttc, Money.ZERO, due, today))
    }

    @Test
    fun future_due_unpaid_stays_sent() {
        val due = LocalDate(2026, 7, 1)
        assertEquals(DocumentStatus.SENT, effectiveStatus(DocumentStatus.SENT, ttc, Money.ZERO, due, today))
    }

    @Test
    fun cancelled_overrides_everything() {
        assertEquals(DocumentStatus.CANCELLED, effectiveStatus(DocumentStatus.CANCELLED, ttc, ttc, null, today))
        assertEquals(Money.ZERO, outstanding(DocumentStatus.CANCELLED, ttc, Money.ZERO))
    }

    @Test
    fun manually_marked_paid_owes_nothing_even_without_payments() {
        assertEquals(DocumentStatus.PAID, effectiveStatus(DocumentStatus.PAID, ttc, Money.ZERO, null, today))
        assertEquals(Money.ZERO, outstanding(DocumentStatus.PAID, ttc, Money.ZERO))
    }
}
