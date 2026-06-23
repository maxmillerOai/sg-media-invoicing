package org.example.project.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.core.Money
import org.example.project.db.AppDatabase
import org.example.project.domain.model.CatalogItem

/** SQLDelight-backed storage for the services/products catalog. */
class CatalogRepository(private val db: AppDatabase) {

    private val q get() = db.catalogQueries

    suspend fun list(): List<CatalogItem> = withContext(Dispatchers.Default) {
        q.selectAll().executeAsList().map {
            CatalogItem(
                id = it.id,
                name = it.name,
                unit = it.unit,
                defaultPriceHT = Money(it.defaultPriceMinor),
                defaultVatPct = it.defaultVatPct,
                description = it.description,
            )
        }
    }

    suspend fun save(item: CatalogItem): Long = withContext(Dispatchers.Default) {
        if (item.id == 0L) {
            db.transactionWithResult {
                q.insert(item.name, item.unit, item.defaultPriceHT.amountMinor, item.defaultVatPct, item.description)
                q.lastInsertedId().executeAsOne()
            }
        } else {
            q.update(item.name, item.unit, item.defaultPriceHT.amountMinor, item.defaultVatPct, item.description, item.id)
            item.id
        }
    }

    suspend fun delete(id: Long): Unit = withContext(Dispatchers.Default) { q.deleteById(id) }
}
