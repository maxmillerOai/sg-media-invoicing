package org.example.project.domain.usecase

import kotlinx.datetime.LocalDate
import org.example.project.core.Money
import org.example.project.domain.model.DocumentStatus

/**
 * The status to DISPLAY, derived from the user-set [stored] status plus payment facts.
 * Payment state wins over the stored value (a fully paid document reads PAID even if the
 * user left it "Sent"); an unpaid issued document past its [dueDate] reads OVERDUE.
 */
fun effectiveStatus(
    stored: DocumentStatus,
    ttc: Money,
    paid: Money,
    dueDate: LocalDate?,
    today: LocalDate,
): DocumentStatus = when {
    stored == DocumentStatus.CANCELLED -> DocumentStatus.CANCELLED
    ttc.amountMinor > 0 && paid >= ttc -> DocumentStatus.PAID
    paid.amountMinor > 0 -> DocumentStatus.PARTIAL
    stored == DocumentStatus.DRAFT -> DocumentStatus.DRAFT
    dueDate != null && dueDate < today -> DocumentStatus.OVERDUE
    else -> stored // SENT / ISSUED
}

/** Amount still owed: 0 for drafts, cancelled, or paid documents. */
fun outstanding(stored: DocumentStatus, ttc: Money, paid: Money): Money {
    if (stored == DocumentStatus.DRAFT || stored == DocumentStatus.CANCELLED || stored == DocumentStatus.PAID) return Money.ZERO
    val remaining = ttc - paid
    return if (remaining.amountMinor > 0) remaining else Money.ZERO
}
