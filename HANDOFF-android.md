# SG Media Invoicing — Android handoff

Entry point for a **new session focused on building/running the Android APK**. The app is the same
Kotlin Multiplatform + Compose codebase that ships on Windows (MSI/ZIP) and macOS (DMG via CI).
**Android builds on Windows** — no Mac/cloud needed. Android is a *secondary* target (see limits below).

## Build the APK (on this Windows machine)
Build env (this Claude sandbox needs both):
- `JAVA_HOME` = Android Studio JBR: `C:\Program Files\Android\Android Studio\jbr`
- `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:/no_afunix_xyz` (AF_UNIX selector workaround)
- Android SDK at `C:\Users\Informatics\AppData\Local\Android\Sdk` (already in `local.properties`).

Debug APK:
```
./gradlew.bat :androidApp:assembleDebug
```
Output: `androidApp/build/outputs/apk/debug/androidApp-debug.apk` (debug-signed — installs for testing).
Release (unsigned) APK: `:androidApp:assembleRelease` (needs a keystore to sign for the Play Store).

## Run it
- **Easiest:** open the project in **Android Studio** → select the `androidApp` run config → pick a
  device/emulator → click **Run ▶**.
- **Emulator (CLI):** start an AVD, then `adb install -r androidApp-debug.apk`.
- **Physical phone:** enable Developer Options + USB debugging, connect, `adb install -r <apk>`,
  then launch "SG Media Invoicing".
- The Android SDK ships `adb` and `emulator` under `…\Android\Sdk\platform-tools` / `\emulator`.

## Android-specific behavior (important)
- **Desktop-only features are no-ops on Android**: PDF/XLSX/DOCX export, print, and the file-dialog
  **backup/restore** all rely on desktop (AWT/OpenPDF/POI). `MainActivity` wires only
  `androidDataModule(...)`, so `InvoiceExporter`/`BackupManager` fall back to `Noop`. Everything else
  works: dashboard, invoices (create/edit/status/payments), clients, catalog, settings, login.
- **Login**: per-device DB, so first launch shows first-run **"Créer votre accès"** (set your own creds).
- **Weather works** (Open-Meteo + OkHttp; `INTERNET` permission is in `androidApp/src/main/AndroidManifest.xml`).
- **Storage**: SQLite via the SQLDelight **android-driver**, in the app's private data dir.
- **Config**: `applicationId = org.example.project`, `minSdk 24`, `compileSdk/targetSdk 36`, `versionName 1.0`.

## Optional polish
- The launcher icon is still the **default Android robot** (`androidApp/src/main/res/mipmap-*`).
  To brand it, replace those with the SG logo (source art: `desktopApp/icons/sg_icon.png` or
  `shared/.../drawable/sg_logo.png`) — generate adaptive-icon densities or use Android Studio's
  Image Asset wizard.

## Known risk
AGP is a preview (`agp = 9.0.1` in `gradle/libs.versions.toml`). If `assembleDebug` fails, it's most
likely an Android SDK/AGP mismatch — check the error and install the matching
`sdkmanager "platforms;android-36"` / build-tools.

## Suggested first prompt for the new session
> "Read HANDOFF-android.md. Build the Android debug APK and help me run it on an emulator (or my
> phone). Fix any Android/SDK build issues."
