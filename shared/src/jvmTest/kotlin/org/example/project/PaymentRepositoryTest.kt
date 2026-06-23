package org.example.project

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.example.project.core.Money
import org.example.project.data.InvoiceRepository
import org.example.project.data.PaymentRecord
import org.example.project.data.PaymentRepository
import org.example.project.data.SavedInvoice
import org.example.project.db.AppDatabase
import org.example.project.domain.model.DocumentStatus
import org.example.project.domain.model.LineItem
import org.example.project.domain.model.PaymentMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PaymentRepositoryTest {

    private fun db(): AppDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        return AppDatabase(driver)
    }

    private fun invoice(number: String) = SavedInvoice(
        number = number,
        clientName = "ACME",
        clientAddress = "Alger",
        clientNif = null,
        issueDate = LocalDate(2026, 6, 22),
        cash = false, vatExempt = false, applyRemise = true,
        lines = listOf(LineItem("Service", 1.0, "u", Money.ofDinars(10_000), 0.0, 19.0)),
    )

    @Test
    fun new_invoice_defaults_to_draft_and_status_updates_persist() = runBlocking {
        val database = db()
        val invoices = InvoiceRepository(database)
        val id = invoices.save(invoice("FAC-2026-0001"))

        assertEquals(DocumentStatus.DRAFT, invoices.load(id)!!.status)
        assertEquals(null, invoices.load(id)!!.dueDate)

        invoices.updateStatus(id, DocumentStatus.SENT)
        invoices.updateDueDate(id, LocalDate(2026, 7, 22))
        val reloaded = invoices.load(id)!!
        assertEquals(DocumentStatus.SENT, reloaded.status)
        assertEquals(LocalDate(2026, 7, 22), reloaded.dueDate)
    }

    @Test
    fun save_persists_payment_method_and_cheque_details() = runBlocking {
        val invoices = InvoiceRepository(db())
        val id = invoices.save(
            invoice("FAC-2026-0010").copy(
                cash = false,
                paymentMethod = PaymentMode.CHEQUE,
                chequeNumber = "1234567",
                chequeBank = "BNA Alger",
            ),
        )
        val loaded = invoices.load(id)!!
        assertEquals(PaymentMode.CHEQUE, loaded.paymentMethod)
        assertEquals("1234567", loaded.chequeNumber)
        assertEquals("BNA Alger", loaded.chequeBank)
    }

    @Test
    fun save_persists_due_date_from_editor() = runBlocking {
        val invoices = InvoiceRepository(db())
        val id = invoices.save(invoice("FAC-2026-0009").copy(dueDate = LocalDate(2026, 8, 1)))
        assertEquals(LocalDate(2026, 8, 1), invoices.load(id)!!.dueDate)
    }

    @Test
    fun payments_persist_sum_and_cascade_on_invoice_delete() = runBlocking {
        val database = db()
        val invoices = InvoiceRepository(database)
        val payments = PaymentRepository(database)
        val id = invoices.save(invoice("FAC-2026-0002"))

        payments.add(PaymentRecord(invoiceId = id, amount = Money.ofDinars(4_000), date = LocalDate(2026, 6, 23), method = PaymentMode.BANK_TRANSFER))
        payments.add(PaymentRecord(invoiceId = id, amount = Money.ofDinars(2_500), date = LocalDate(2026, 6, 24), method = PaymentMode.CASH, note = "acompte"))

        assertEquals(Money.ofDinars(6_500), payments.sumForInvoice(id))
        assertEquals(2, payments.forInvoice(id).size)
        assertEquals(Money.ofDinars(6_500), payments.paidByInvoice()[id])

        // newest first
        val first = payments.forInvoice(id).first()
        assertEquals(LocalDate(2026, 6, 24), first.date)
        assertEquals(PaymentMode.CASH, first.method)
        assertEquals("acompte", first.note)

        // deleting one payment lowers the sum
        payments.delete(first.id)
        assertEquals(Money.ofDinars(4_000), payments.sumForInvoice(id))

        // deleting the invoice removes its remaining payments too
        invoices.delete(id)
        assertEquals(0, payments.forInvoice(id).size)
        assertNotNull(payments.paidByInvoice()) // no leftover rows for the deleted invoice
        assertEquals(null, payments.paidByInvoice()[id])
    }
}
