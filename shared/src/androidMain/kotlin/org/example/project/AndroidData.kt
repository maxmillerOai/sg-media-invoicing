package org.example.project

import android.content.Context
import app.cash.sqldelight.db.synchronous
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.example.project.db.AppDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

/** Koin module providing the SQLite-backed [AppDatabase] on Android (needs a [Context]). */
fun androidDataModule(context: Context): Module = module {
    single<AppDatabase> {
        AppDatabase(AndroidSqliteDriver(AppDatabase.Schema.synchronous(), context, "sgmedia.db"))
    }
}
