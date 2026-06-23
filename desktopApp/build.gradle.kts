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
            // Bundle all JDK modules so the SQLite driver (java.sql) and POI work in the app image.
            includeAllModules = true
        }
    }
}