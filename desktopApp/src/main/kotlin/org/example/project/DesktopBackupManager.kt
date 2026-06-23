package org.example.project

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.example.project.data.BackupInfo
import org.example.project.data.BackupManager
import org.example.project.data.BackupResult
import java.awt.FileDialog
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties
import javax.swing.JFileChooser

/**
 * File-based backup/restore for the desktop app. The database is a single SQLite file
 * (`~/.sgmedia/sgmedia.db`); a backup is a timestamped copy.
 *
 * - Automatic: a copy is written on every app close and once per day on launch.
 * - Rolling: keeps the most recent [KEEP] copies locally and in the external folder.
 * - External folder (USB / D: / OneDrive…) is what survives a Windows reinstall — configurable.
 * - Restore is staged and applied on the next launch (before the DB is opened) to avoid file locks.
 */
class DesktopBackupManager : BackupManager {

    private val home = File(System.getProperty("user.home"), ".sgmedia").apply { mkdirs() }
    private val dbFile = File(home, "sgmedia.db")
    private val backupsDir = File(home, "backups").apply { mkdirs() }
    private val configFile = File(home, "backup.properties")
    private val pendingFile = File(home, "sgmedia.restore.pending")
    private val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

    private fun config(): Properties = Properties().apply {
        if (configFile.exists()) configFile.inputStream().use { load(it) }
    }

    private fun saveConfig(p: Properties) = configFile.outputStream().use { p.store(it, "SG Media backup config") }

    private fun externalFolder(): File? = config().getProperty("external.folder")?.takeIf { it.isNotBlank() }?.let(::File)

    private fun copyDb(target: File) {
        if (!dbFile.exists()) return
        target.parentFile?.mkdirs()
        dbFile.copyTo(target, overwrite = true)
    }

    private fun prune(dir: File, keep: Int = KEEP) {
        val files = dir.listFiles { f -> f.name.startsWith("sgmedia-") && f.name.endsWith(".db") }?.sortedBy { it.name } ?: return
        if (files.size > keep) files.dropLast(keep).forEach { it.delete() }
    }

    /** Copy the DB to the local backups dir (+ external folder if set). Safe to call on the EDT. */
    fun backupNow(reason: String) {
        runCatching {
            if (!dbFile.exists()) return
            val name = "sgmedia-${LocalDateTime.now().format(stamp)}.db"
            copyDb(File(backupsDir, name)); prune(backupsDir)
            externalFolder()?.let { ext ->
                runCatching { ext.mkdirs(); copyDb(File(ext, name)); prune(ext) }
            }
            val p = config()
            p.setProperty("last.backup", LocalDateTime.now().toString())
            p.setProperty("last.reason", reason)
            saveConfig(p)
        }
    }

    /** Daily safety net on launch: back up if the last one is older than ~20h. */
    fun autoBackupIfStale() {
        val last = config().getProperty("last.backup")?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
        val due = last == null || last.isBefore(LocalDateTime.now().minusHours(20))
        if (due) backupNow("daily")
    }

    /** Apply a staged restore before the DB is opened: back up the current file, then swap it in. */
    fun applyPendingImport() {
        if (!pendingFile.exists()) return
        runCatching {
            if (dbFile.exists()) copyDb(File(backupsDir, "sgmedia-${LocalDateTime.now().format(stamp)}-before-restore.db"))
            pendingFile.copyTo(dbFile, overwrite = true)
            pendingFile.delete()
        }
    }

    override suspend fun exportBackup(): BackupResult {
        if (!dbFile.exists()) return BackupResult(false, "Aucune base de données à sauvegarder.")
        val target = withContext(Dispatchers.Swing) {
            val d = FileDialog(null as java.awt.Frame?, "Exporter une sauvegarde", FileDialog.SAVE).apply {
                file = "sgmedia-backup-${LocalDateTime.now().format(stamp)}.db"
                isVisible = true
            }
            d.file?.let { File(d.directory ?: "", it) }
        } ?: return BackupResult(false, "Export annulé.")
        return withContext(Dispatchers.IO) {
            runCatching { copyDb(target); BackupResult(true, "✓ Sauvegarde enregistrée : ${target.absolutePath}") }
                .getOrElse { BackupResult(false, "Erreur : ${it.message}") }
        }
    }

    override suspend fun importBackup(): BackupResult {
        val source = withContext(Dispatchers.Swing) {
            val d = FileDialog(null as java.awt.Frame?, "Importer une sauvegarde (.db)", FileDialog.LOAD).apply { isVisible = true }
            d.file?.let { File(d.directory ?: "", it) }
        } ?: return BackupResult(false, "Import annulé.")
        if (!source.exists()) return BackupResult(false, "Fichier introuvable.")
        return withContext(Dispatchers.IO) {
            runCatching {
                source.copyTo(pendingFile, overwrite = true)
                BackupResult(true, "✓ Sauvegarde importée. Fermez et rouvrez l'application pour l'appliquer.")
            }.getOrElse { BackupResult(false, "Erreur : ${it.message}") }
        }
    }

    override suspend fun browseBackupFolder(): String? = withContext(Dispatchers.Swing) {
        val chooser = JFileChooser().apply {
            dialogTitle = "Dossier de sauvegarde externe"
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            externalFolder()?.let { if (it.exists()) currentDirectory = it }
        }
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile?.absolutePath else null
    }

    override suspend fun getBackupFolder(): String? = withContext(Dispatchers.IO) { externalFolder()?.absolutePath }

    override suspend fun setBackupFolder(path: String?): BackupResult = withContext(Dispatchers.IO) {
        runCatching {
            val p = config()
            if (path.isNullOrBlank()) {
                p.remove("external.folder")
                saveConfig(p)
                BackupResult(true, "Dossier externe retiré.")
            } else {
                val dir = File(path)
                if (!dir.exists() && !dir.mkdirs()) return@runCatching BackupResult(false, "Dossier inaccessible : $path")
                p.setProperty("external.folder", dir.absolutePath)
                saveConfig(p)
                // Immediately drop a fresh copy there so it's protected right away.
                runCatching { copyDb(File(dir, "sgmedia-${LocalDateTime.now().format(stamp)}.db")); prune(dir) }
                BackupResult(true, "✓ Dossier externe défini : ${dir.absolutePath}")
            }
        }.getOrElse { BackupResult(false, "Erreur : ${it.message}") }
    }

    override suspend fun info(): BackupInfo = withContext(Dispatchers.IO) {
        val count = backupsDir.listFiles { f -> f.name.startsWith("sgmedia-") && f.name.endsWith(".db") }?.size ?: 0
        BackupInfo(
            localBackupsDir = backupsDir.absolutePath,
            externalFolder = externalFolder()?.absolutePath,
            lastBackup = config().getProperty("last.backup"),
            count = count,
        )
    }

    companion object {
        const val KEEP = 15
    }
}
