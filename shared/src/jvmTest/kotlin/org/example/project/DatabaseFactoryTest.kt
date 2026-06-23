package org.example.project

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.example.project.core.Money
import org.example.project.data.InvoiceRepository
import org.example.project.data.SavedInvoice
import org.example.project.data.openDatabase
import org.example.project.domain.model.LineItem
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseFactoryTest {

    @Test
    fun reopening_the_database_preserves_data() = runBlocking<Unit> {
        val tmp = File.createTempFile("sgmig", ".db").also { it.delete() }
        val url = "jdbc:sqlite:${tmp.absolutePath}"

        // First open: creates schema, saves an invoice.
        val driver1 = JdbcSqliteDriver(url)
        val repo1 = InvoiceRepository(openDatabase(driver1))
        repo1.save(
            SavedInvoice(
                number = "FAC-2026-0001",
                clientName = "Acme",
                clientAddress = "Alger",
                clientNif = null,
                issueDate = LocalDate(2026, 1, 1),
                cash = false, vatExempt = false, applyRemise = true,
                lines = listOf(LineItem("Service", 1.0, "u", Money.ofDinars(1000), 0.0, 19.0)),
            ),
        )
        driver1.close()

        // Second open of the same file: must NOT recreate; data preserved.
        val driver2 = JdbcSqliteDriver(url)
        val repo2 = InvoiceRepository(openDatabase(driver2))
        assertEquals(1, repo2.list().size)
        assertEquals("Acme", repo2.list().first().clientName)
        driver2.close()

        tmp.delete()
    }
}
