package org.example.project.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.db.AppDatabase
import org.example.project.domain.model.Client

/** SQLDelight-backed storage for the client portfolio. */
class ClientRepository(private val db: AppDatabase) {

    private val q get() = db.clientQueries

    suspend fun list(): List<Client> = withContext(Dispatchers.Default) {
        q.selectAll().awaitAsList().map { it.toClient() }
    }

    suspend fun load(id: Long): Client? = withContext(Dispatchers.Default) {
        q.selectById(id).awaitAsOneOrNull()?.toClient()
    }

    /** Inserts when [Client.id] is 0, otherwise updates. Returns the row id. */
    suspend fun save(c: Client): Long = withContext(Dispatchers.Default) {
        if (c.id == 0L) {
            db.transactionWithResult {
                q.insert(
                    displayName = c.displayName, legalForm = c.legalForm, rc = c.rc, nif = c.nif, nis = c.nis,
                    articleImposition = c.articleImposition, address = c.address, city = c.city, wilaya = c.wilaya,
                    phone = c.phone, email = c.email, iban = c.iban, bankName = c.bankName,
                    isPersonal = if (c.isPersonal) 1L else 0L, notes = c.notes,
                )
                q.lastInsertedId().awaitAsOne()
            }
        } else {
            q.update(
                displayName = c.displayName, legalForm = c.legalForm, rc = c.rc, nif = c.nif, nis = c.nis,
                articleImposition = c.articleImposition, address = c.address, city = c.city, wilaya = c.wilaya,
                phone = c.phone, email = c.email, iban = c.iban, bankName = c.bankName,
                isPersonal = if (c.isPersonal) 1L else 0L, notes = c.notes, id = c.id,
            )
            c.id
        }
    }

    suspend fun delete(id: Long): Unit = withContext(Dispatchers.Default) { q.deleteById(id) }
}

private fun org.example.project.db.ClientEntity.toClient() = Client(
    id = id,
    displayName = displayName,
    legalForm = legalForm,
    rc = rc,
    nif = nif,
    nis = nis,
    articleImposition = articleImposition,
    address = address,
    city = city,
    wilaya = wilaya,
    phone = phone,
    email = email,
    iban = iban,
    bankName = bankName,
    isPersonal = isPersonal != 0L,
    notes = notes,
)

