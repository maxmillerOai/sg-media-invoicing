package org.example.project.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import org.example.project.db.AppDatabase

/**
 * Opens [AppDatabase] on a raw [SqlDriver] (desktop's JDBC driver), creating the schema for a
 * fresh database and running SQLDelight migrations for an older one — using `PRAGMA user_version`
 * to track the on-disk version. This is what lets schema changes preserve existing data.
 *
 * Android/iOS drivers manage create/migrate themselves, so they don't call this.
 */
fun openDatabase(driver: SqlDriver): AppDatabase {
    val schema = AppDatabase.Schema
    val current = driver.executeQuery(
        identifier = null,
        sql = "PRAGMA user_version;",
        mapper = { cursor -> QueryResult.Value(if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L) },
        parameters = 0,
    ).value

    when {
        current == 0L -> {
            val alreadyHasTables = driver.executeQuery(
                identifier = null,
                sql = "SELECT name FROM sqlite_master WHERE type='table' AND name='InvoiceEntity';",
                mapper = { cursor -> QueryResult.Value(cursor.next().value) },
                parameters = 0,
            ).value
            if (alreadyHasTables != true) schema.create(driver)
            driver.execute(null, "PRAGMA user_version = ${schema.version};", 0)
        }
        current < schema.version -> {
            schema.migrate(driver, current, schema.version)
            driver.execute(null, "PRAGMA user_version = ${schema.version};", 0)
        }
    }
    return AppDatabase(driver)
}
