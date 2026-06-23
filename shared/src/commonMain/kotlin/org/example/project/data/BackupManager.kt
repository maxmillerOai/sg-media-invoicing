package org.example.project.data

import androidx.compose.runtime.staticCompositionLocalOf

data class BackupResult(val ok: Boolean, val message: String)

data class BackupInfo(
    val localBackupsDir: String,
    val externalFolder: String?,
    val lastBackup: String?,
    val count: Int,
)

/**
 * Manual + automatic database backup/restore. The whole app state is a single SQLite file,
 * so a backup is just a copy of it. Implemented on desktop; no-op elsewhere.
 */
interface BackupManager {
    /** Save a copy of the database to a user-chosen location. */
    suspend fun exportBackup(): BackupResult

    /** Pick a backup file to restore; applied on next launch. */
    suspend fun importBackup(): BackupResult

    /** Open a folder picker for the external (USB/cloud) auto-backup destination. */
    suspend fun browseBackupFolder(): String?

    suspend fun getBackupFolder(): String?
    suspend fun setBackupFolder(path: String?): BackupResult
    suspend fun info(): BackupInfo
}

object NoopBackupManager : BackupManager {
    override suspend fun exportBackup() = BackupResult(false, "Sauvegarde disponible sur Desktop.")
    override suspend fun importBackup() = BackupResult(false, "Restauration disponible sur Desktop.")
    override suspend fun browseBackupFolder(): String? = null
    override suspend fun getBackupFolder(): String? = null
    override suspend fun setBackupFolder(path: String?) = BackupResult(false, "Non supporté.")
    override suspend fun info() = BackupInfo("", null, null, 0)
}

val LocalBackupManager = staticCompositionLocalOf<BackupManager> { NoopBackupManager }
