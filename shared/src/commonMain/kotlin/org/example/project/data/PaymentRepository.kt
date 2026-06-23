package org.example.project.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.example.project.core.Money
import org.example.project.db.AppDatabase
import org.example.project.domain.model.PaymentMode

/** A payment recorded against an invoice. */
data class PaymentRecord(
    val id: Long = 0,
    val invoiceId: Long,
    val amount: Money,
    val date: LocalDate,
    val method: PaymentMode,
    val note: String? = null,
)

private fun parseMethod(raw: String): PaymentMode =
    runCatching { PaymentMode.valueOf(raw) }.getOrDefault(PaymentMode.BANK_TRANSFER)

/** SQLDelight-backed storage for invoice payments. */
class PaymentRepository(private val db: AppDatabase) {

    private val pq get() = db.paymentQueries

    suspend fun forInvoice(invoiceId: Long): List<PaymentRecord> = withContext(Dispatchers.Default) {
        pq.selectForInvoice(invoiceId).executeAsList().map { e ->
            PaymentRecord(e.id, e.invoiceId, Money(e.amountMinor), LocalDate.parse(e.date), parseMethod(e.method), e.note)
        }
    }

    suspend fun add(payment: PaymentRecord): Long = withContext(Dispatchers.Default) {
        db.transactionWithResult {
            pq.insert(
                invoiceId = payment.invoiceId,
                amountMinor = payment.amount.amountMinor,
                date = payment.date.toString(),
                method = payment.method.name,
                note = payment.note,
                createdAt = payment.date.toEpochDays().toLong() * 86_400_000L,
            )
            pq.lastInsertedId().executeAsOne()
        }
    }

    suspend fun delete(id: Long): Unit = withContext(Dispatchers.Default) {
        pq.deleteById(id)
    }

    suspend fun sumForInvoice(invoiceId: Long): Money = withContext(Dispatchers.Default) {
        Money(pq.sumForInvoice(invoiceId).executeAsOne())
    }

    /** Total paid per invoice id, for computing outstanding across many documents at once. */
    suspend fun paidByInvoice(): Map<Long, Money> = withContext(Dispatchers.Default) {
        pq.paidByInvoice().executeAsList().associate { it.invoiceId to Money(it.paid) }
    }
}
