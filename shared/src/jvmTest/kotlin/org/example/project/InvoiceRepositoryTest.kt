package org.example.project

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.example.project.core.Money
import org.example.project.data.InvoiceRepository
import org.example.project.data.SavedInvoice
import org.example.project.db.AppDatabase
import org.example.project.domain.model.LineItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class InvoiceRepositoryTest {

    private fun repo(): InvoiceRepository {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        return InvoiceRepository(AppDatabase(driver))
    }

    @Test
    fun save_load_number_and_delete() = runBlocking {
        val r = repo()
        assertEquals("PRO-2026-0001", r.nextNumber(2026))

        val id = r.save(
            SavedInvoice(
                number = "PRO-2026-0001",
                clientName = "Ligue de Football Professionnel",
                clientAddress = "Belouizdad, Alger",
                clientNif = "525070005361686",
                issueDate = LocalDate(2026, 6, 22),
                cash = true, vatExempt = false, applyRemise = true,
                lines = listOf(
                    LineItem("Service A", 2.0, "u", Money.ofDinars(1000), 0.0, 19.0),
                    LineItem("Service B", 1.0, "forfait", Money.ofDinars(5000), 10.0, 9.0),
                ),
            ),
        )

        val loaded = r.load(id)
        assertNotNull(loaded)
        assertEquals("Ligue de Football Professionnel", loaded.clientName)
        assertEquals("525070005361686", loaded.clientNif)
        assertEquals(true, loaded.cash)
        assertEquals(2, loaded.lines.size)
        assertEquals(Money.ofDinars(1000), loaded.lines[0].unitPriceHT)
        assertEquals(10.0, loaded.lines[1].discountPct)

        assertEquals(1, r.list().size)
        assertEquals("PRO-2026-0002", r.nextNumber(2026)) // count reflects the saved one

        r.delete(id)
        assertEquals(0, r.list().size)
    }
}
