package org.example.project

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.example.project.export.DesktopInvoiceExporter

fun main() {
    val backup = DesktopBackupManager()
    backup.applyPendingImport()  // restore a staged backup before the DB is opened
    backup.autoBackupIfStale()   // daily safety-net copy on launch

    application {
        val windowState = rememberWindowState(placement = WindowPlacement.Maximized)
        Window(
            onCloseRequest = { backup.backupNow("exit"); exitApplication() },
            state = windowState,
            title = "SG MEDIA PROD",
        ) {
            App(
                exporter = DesktopInvoiceExporter(),
                backupManager = backup,
                platformModules = listOf(desktopDataModule()),
            )
        }
    }
}
