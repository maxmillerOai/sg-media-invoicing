package org.example.project.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.db.AppDatabase

/** Simple key/value settings store (used for login credentials, preferences, …). */
class SettingsRepository(private val db: AppDatabase) {

    private val q get() = db.settingsQueries

    suspend fun get(key: String): String? = withContext(Dispatchers.Default) {
        q.get(key).executeAsOneOrNull()
    }

    suspend fun set(key: String, value: String): Unit = withContext(Dispatchers.Default) {
        q.upsert(key, value)
    }

    companion object {
        const val KEY_AUTH_USER = "auth_user"
        const val KEY_AUTH_HASH = "auth_hash"
        const val KEY_AUTH_MASTER = "auth_master_hash"
        const val KEY_AUTH_SALT = "auth_salt"
        const val KEY_WEATHER_CITY = "weather_city"
    }
}
