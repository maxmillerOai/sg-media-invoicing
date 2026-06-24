import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.shared)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)

    // Document export (Desktop): PDF via OpenPDF, XLSX/DOCX via Apache POI.
    implementation(libs.openpdf)
    implementation(libs.poi.ooxml)

    // DI + local database driver (Desktop).
    implementation(libs.koin.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.sqldelight.runtime)
    implementation(libs.sqldelight.sqlite.driver)
}

compose.desktop {
    application {
        mainClass = "org.example.project.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "SG Media Invoicing"
            packageVersion = "1.0.0"
            description = "SG Media — Facturation"
            vendor = "GHN-TECH"
            copyright = "© 2026 GHN-TECH. Tous droits réservés."
            // Bundle all JDK modules so the SQLite driver (java.sql) and POI work in the app image.
            includeAllModules = true

            windows {
                shortcut = true          // create a Desktop shortcut on install
                menu = true              // add a Start-menu entry
                menuGroup = "SG Media"
                perUserInstall = true    // installs without admin rights
                // Stable GUID so future versions upgrade in place instead of installing side-by-side.
                upgradeUuid = "7C9F3B2E-5A4D-4E61-9B8C-2D1F6A0E5C34"
                iconFile.set(project.file("icons/sg_icon.ico"))
            }
            macOS {
                bundleID = "com.sgmedia.invoicing"
                iconFile.set(project.file("icons/sg_icon.icns"))
            }
            linux {
                iconFile.set(project.file("icons/sg_icon.png"))
            }
        }
    }
}