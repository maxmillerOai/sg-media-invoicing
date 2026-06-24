package org.example.project

import org.example.project.db.AppDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

/** Browser-backed [AppDatabase]. Wired in a later phase (sql.js web worker). */
fun webDataModule(): Module = module {
    single<AppDatabase> { error("Web database not wired yet") }
}
