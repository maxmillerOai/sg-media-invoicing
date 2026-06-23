package org.example.project

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import org.example.project.db.AppDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

/** Koin module providing the SQLite-backed [AppDatabase] on iOS. */
fun iosDataModule(): Module = module {
    single<AppDatabase> {
        AppDatabase(NativeSqliteDriver(AppDatabase.Schema, "sgmedia.db"))
    }
}
