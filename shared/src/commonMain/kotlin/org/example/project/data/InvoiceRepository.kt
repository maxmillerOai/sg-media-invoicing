package org.example.project.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.example.project.core.Money
import org.example.project.db.AppDatabase
import org.example.project.domain.model.DocumentStatus
import org.example.project.domain.model.LineItem
import org.example.project.domain.model.PaymentMode

/** A persisted document with its line items. */
data class SavedInvoice(
    val id: Long = 0,
    val number: String,
    val docType: String = "FACTURE PROFORMA",
    val clientName: String,
    val clientAddress: String,
    val clientNif: String?,
    val clientRc: String? = null,
    val clientNis: String? = null,
    val clientArticle: String? = null,
    val issueDate: LocalDate,
    val cash: Boolean,
    val vatExempt: Boolean,
    val applyRemise: Boolean,
    val lines: List<LineItem>,
    val status: DocumentStatus = DocumentStatus.DRAFT,
    val dueDate: LocalDate? = null,
    val paymentMethod: PaymentMode = PaymentMode.CASH,
    val chequeNumber: String? = null,
    val chequeBank: String? = null,
)

/** Tolerant parse of the stored status string; unknown values fall back to DRAFT. */
private fun parseStatus(raw: String?): DocumentStatus =
    raw?.let { runCatching { DocumentStatus.valueOf(it) }.getOrNull() } ?: DocumentStatus.DRAFT

/** Tolerant parse of the stored payment-method string; unknown values fall back to CASH. */
private fun parseMethod(raw: String?): PaymentMode =
    raw?.let { runCatching { PaymentMode.valueOf(it) }.getOrNull() } ?: PaymentMode.CASH

/** SQLDelight-backed storage for documents. */
class InvoiceRepository(private val db: AppDatabase) {

    private val iq get() = db.invoiceQueries
    private val lq get() = db.lineItemQueries

    suspend fun list(): List<SavedInvoice> = withContext(Dispatchers.Default) {
        iq.selectAll().awaitAsList().map { e ->
            SavedInvoice(
                id = e.id,
                number = e.number,
                docType = e.docType,
                clientName = e.clientName,
                clientAddress = e.clientAddress,
                clientNif = e.clientNif,
                clientRc = e.clientRc,
                clientNis = e.clientNis,
                clientArticle = e.clientArticle,
                issueDate = LocalDate.parse(e.issueDate),
                cash = e.cash != 0L,
                vatExempt = e.vatExempt != 0L,
                applyRemise = e.applyRemise != 0L,
                lines = loadLines(e.id),
                status = parseStatus(e.status),
                dueDate = e.dueDate?.let { LocalDate.parse(it) },
                paymentMethod = parseMethod(e.paymentMethod),
                chequeNumber = e.chequeNumber,
                chequeBank = e.chequeBank,
            )
        }
    }

    suspend fun load(id: Long): SavedInvoice? = withContext(Dispatchers.Default) {
        val e = iq.selectById(id).awaitAsOneOrNull() ?: return@withContext null
        SavedInvoice(
            id = e.id, number = e.number, docType = e.docType,
            clientName = e.clientName, clientAddress = e.clientAddress, clientNif = e.clientNif,
            clientRc = e.clientRc, clientNis = e.clientNis, clientArticle = e.clientArticle,
            issueDate = LocalDate.parse(e.issueDate),
            cash = e.cash != 0L, vatExempt = e.vatExempt != 0L, applyRemise = e.applyRemise != 0L,
            lines = loadLines(e.id),
            status = parseStatus(e.status),
            dueDate = e.dueDate?.let { LocalDate.parse(it) },
            paymentMethod = parseMethod(e.paymentMethod),
            chequeNumber = e.chequeNumber,
            chequeBank = e.chequeBank,
        )
    }

    private suspend fun loadLines(invoiceId: Long): List<LineItem> =
        lq.selectForInvoice(invoiceId).awaitAsList().map { l ->
            LineItem(
                designation = l.designation,
                qty = l.qty,
                unit = l.unit,
                unitPriceHT = Money(l.unitPriceMinor),
                discountPct = l.discountPct,
                vatPct = l.vatPct,
            )
        }

    suspend fun save(inv: SavedInvoice): Long = withContext(Dispatchers.Default) {
        db.transactionWithResult {
            iq.insert(
                number = inv.number,
                docType = inv.docType,
                clientName = inv.clientName,
                clientAddress = inv.clientAddress,
                clientNif = inv.clientNif,
                clientRc = inv.clientRc,
                clientNis = inv.clientNis,
                clientArticle = inv.clientArticle,
                issueDate = inv.issueDate.toString(),
                cash = if (inv.cash) 1L else 0L,
                vatExempt = if (inv.vatExempt) 1L else 0L,
                applyRemise = if (inv.applyRemise) 1L else 0L,
                createdAt = inv.issueDate.toEpochDays().toLong() * 86_400_000L,
                status = inv.status.name,
                dueDate = inv.dueDate?.toString(),
                paymentMethod = inv.paymentMethod.name,
                chequeNumber = inv.chequeNumber,
                chequeBank = inv.chequeBank,
            )
            val id = iq.lastInsertedId().awaitAsOne()
            inv.lines.forEachIndexed { i, l ->
                lq.insert(
                    invoiceId = id,
                    designation = l.designation,
                    unit = l.unit,
                    qty = l.qty,
                    unitPriceMinor = l.unitPriceHT.amountMinor,
                    discountPct = l.discountPct,
                    vatPct = l.vatPct,
                    position = i.toLong(),
                )
            }
            id
        }
    }

    /** Updates an existing document's fields and replaces its line items, preserving id, status and payments. */
    suspend fun update(inv: SavedInvoice): Unit = withContext(Dispatchers.Default) {
        db.transaction {
            iq.updateFull(
                number = inv.number,
                docType = inv.docType,
                clientName = inv.clientName,
                clientAddress = inv.clientAddress,
                clientNif = inv.clientNif,
                clientRc = inv.clientRc,
                clientNis = inv.clientNis,
                clientArticle = inv.clientArticle,
                issueDate = inv.issueDate.toString(),
                cash = if (inv.cash) 1L else 0L,
                vatExempt = if (inv.vatExempt) 1L else 0L,
                applyRemise = if (inv.applyRemise) 1L else 0L,
                dueDate = inv.dueDate?.toString(),
                paymentMethod = inv.paymentMethod.name,
                chequeNumber = inv.chequeNumber,
                chequeBank = inv.chequeBank,
                id = inv.id,
            )
            lq.deleteForInvoice(inv.id)
            inv.lines.forEachIndexed { i, l ->
                lq.insert(
                    invoiceId = inv.id,
                    designation = l.designation,
                    unit = l.unit,
                    qty = l.qty,
                    unitPriceMinor = l.unitPriceHT.amountMinor,
                    discountPct = l.discountPct,
                    vatPct = l.vatPct,
                    position = i.toLong(),
                )
            }
        }
    }

    suspend fun delete(id: Long): Unit = withContext(Dispatchers.Default) {
        db.transaction {
            db.paymentQueries.deleteForInvoice(id)
            lq.deleteForInvoice(id)
            iq.deleteById(id)
        }
    }

    suspend fun updateStatus(id: Long, status: DocumentStatus): Unit = withContext(Dispatchers.Default) {
        iq.updateStatus(status.name, id)
    }

    suspend fun updateDueDate(id: Long, dueDate: LocalDate?): Unit = withContext(Dispatchers.Default) {
        iq.updateDueDate(dueDate?.toString(), id)
    }

    /** Atomic next document number for the year and type, e.g. PRO-2026-0001. */
    suspend fun nextNumber(year: Int, prefix: String = "PRO"): String = withContext(Dispatchers.Default) {
        val base = "$prefix-$year-"
        val count = iq.countForYearPrefix("$base%").awaitAsOne()
        base + (count + 1).toString().padStart(4, '0')
    }
}
