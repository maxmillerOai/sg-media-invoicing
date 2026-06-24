# SG Media Invoicing — macOS handoff

Kotlin Multiplatform + Compose **desktop invoicing app** (Algeria: TVA 19% + droit de timbre,
RC/NIF/NIS). This file is the entry point for a **new session focused on building/testing the macOS app**.
The app is feature-complete and ships on Windows already (MSI + ZIP). The only open goal here is
producing and testing the **macOS `.dmg`**.

## Current state (done)
Dashboard (KPIs, revenue chart, analog clock, live weather via Open-Meteo, recent activity,
upcoming deadlines), invoices (create/edit, statuses Brouillon/Envoyée/Payée/Partiel/En retard,
payments, payment method incl. cheque), clients, catalog, settings. Exports PDF/XLSX/DOCX + print
(logo cleaned, footer = name → Siège social → IDs). FR/AR/EN with Arabic RTL. Local login (AuthGate).
Dates display DD/MM/YYYY. **Automatic + manual DB backups** with optional external folder.
Storage = SQLite at `~/.sgmedia/sgmedia.db` (schema v3).

## The macOS goal
Build `SG Media Invoicing.app` / `.dmg`. **A `.dmg` can only be built on macOS** (jpackage can't
cross-compile from Windows), which is why this is a separate, Mac-side effort.

### Option A — GitHub Actions (recommended; no Mac needed locally)
A workflow already exists: `.github/workflows/build-desktop.yml`.
1. Push this repo to GitHub.
2. Actions tab → "Build desktop installers" → runs on push or via "Run workflow".
3. Download the **`SG-Media-Invoicing-macOS`** artifact → it contains the `.dmg`.

### Option B — build locally on a Mac
Prereqs: **JDK 21** (Temurin), **Android SDK** with `ANDROID_HOME` set and `platforms;android-36`
installed (the `shared` module has an Android target that's configured even for a desktop build).
Then:
```
chmod +x ./gradlew
./gradlew :desktopApp:packageDmg --no-daemon --no-configuration-cache --stacktrace
```
Output: `desktopApp/build/compose/binaries/main/dmg/*.dmg`.
(`:desktopApp:createDistributable` makes a runnable `.app` without the DMG wrapper.)

## App icon (already wired)
`desktopApp/icons/sg_icon.{ico,icns,png}` = the SG emblem (white) on the brand violet→cyan gradient.
`desktopApp/build.gradle.kts` sets `macOS.iconFile = icons/sg_icon.icns`, so the `.dmg`/app picks it up
automatically. The `.icns` was generated with Pillow on Windows — if the mac build ever rejects it,
regenerate on the runner from the PNG: `sips -s format icns icons/sg_icon.png --out icons/sg_icon.icns`.

## Important macOS notes
- **DO NOT** use the Windows-only hack `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=…` — that was a
  sandbox workaround on the Windows dev box. On a real Mac/CI, build normally.
- **Login on a fresh Mac**: the DB is per-machine (`~/.sgmedia`), so first launch shows
  **"Créer votre accès"** (first-run setup) — you set your own user + password + master password there.
  (The Windows box had seeded creds `admin` / `SgMedia2026`, master `Master2026`; those do NOT carry to the Mac.)
- **Gatekeeper**: the `.dmg`/app is **unsigned** (no Apple Developer ID). First open:
  right-click the app → **Open**, or `xattr -dr com.apple.quarantine "/Applications/SG Media Invoicing.app"`.
  For distribution without warnings you'd need an Apple Developer account ($99/yr) to sign + notarize.
- **Known risk**: Android Gradle Plugin is a preview (`agp = 9.0.1` in `gradle/libs.versions.toml`).
  If the Mac/CI build fails during Android configuration, install the matching SDK component
  (`sdkmanager "platforms;android-36"`) or report the exact error to adjust the workflow.
- Toolchain auto-provisions Amazon Corretto 21 via `gradle/gradle-daemon-jvm.properties` (needs network).

## Where things live
- DB + backups: `~/.sgmedia/sgmedia.db`, `~/.sgmedia/backups/`, config `~/.sgmedia/backup.properties`.
- Desktop entry: `desktopApp/src/main/kotlin/org/example/project/main.kt`.
- Backup engine: `desktopApp/.../DesktopBackupManager.kt`.
- Build config: `desktopApp/build.gradle.kts` (`compose.desktop { … targetFormats(Dmg, Msi, Deb) }`).

## Suggested first prompt for the new session
> "Read HANDOFF-macos.md. I want to build and test the macOS .dmg. Walk me through pushing to GitHub
> and running the workflow (or building locally on this Mac), and fix any macOS/Android-SDK build issues."
