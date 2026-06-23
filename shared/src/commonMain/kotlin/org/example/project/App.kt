package org.example.project

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import org.example.project.data.BackupManager
import org.example.project.data.LocalBackupManager
import org.example.project.data.NoopBackupManager
import org.example.project.di.appModule
import org.example.project.export.InvoiceExporter
import org.example.project.export.LocalInvoiceExporter
import org.example.project.export.NoopInvoiceExporter
import org.example.project.presentation.i18n.Language
import org.example.project.presentation.i18n.LocalLanguage
import org.example.project.presentation.i18n.LocalStrings
import org.example.project.presentation.auth.AuthGate
import org.example.project.presentation.i18n.stringsFor
import org.example.project.presentation.navigation.AppScaffold
import org.example.project.presentation.theme.AgencyTheme
import org.koin.compose.KoinApplication
import org.koin.core.module.Module

@Composable
@Preview
fun App(
    exporter: InvoiceExporter = NoopInvoiceExporter,
    backupManager: BackupManager = NoopBackupManager,
    platformModules: List<Module> = emptyList(),
) {
    KoinApplication(application = {
        modules(buildList { add(appModule); addAll(platformModules) })
    }) {
        var dark by remember { mutableStateOf(false) }
        var language by remember { mutableStateOf(Language.FR) }
        val layoutDir = if (language.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
        CompositionLocalProvider(
            LocalInvoiceExporter provides exporter,
            LocalBackupManager provides backupManager,
            LocalLanguage provides language,
            LocalStrings provides stringsFor(language),
            LocalLayoutDirection provides layoutDir,
        ) {
            AgencyTheme(darkTheme = dark) {
                AuthGate {
                    AppScaffold(
                        darkTheme = dark,
                        onToggleTheme = { dark = !dark },
                        language = language,
                        onSelectLanguage = { language = it },
                    )
                }
            }
        }
    }
}
