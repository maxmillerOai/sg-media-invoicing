package org.example.project

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.example.project.data.openDatabase
import org.example.project.db.AppDatabase
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

/** Koin module providing the SQLite-backed [AppDatabase] for the desktop app. */
fun desktopDataModule(): Module = module {
    single<AppDatabase> {
        val dir = File(System.getProperty("user.home"), ".sgmedia").apply { mkdirs() }
        val dbFile = File(dir, "sgmedia.db")
        // Creates the schema for a new DB and migrates an older one (preserving data).
        openDatabase(JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}"))
    }
}
