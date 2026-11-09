package dev.anon.novatv.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.novaPreferences by preferencesDataStore(name = "novatv_prefs")

class PreferenceStore(private val dataStore: DataStore<Preferences>) {

    val activeProfileId: Flow<Long> = dataStore.data.map { it[ACTIVE_PROFILE] ?: DEFAULT_PROFILE_ID }

    suspend fun setActiveProfile(id: Long) {
        dataStore.edit { it[ACTIVE_PROFILE] = id }
    }

    /** Canlı TV listesi ızgara mı, liste mi — kullanıcı tercihi kalıcı. */
    val liveGrid: Flow<Boolean> = dataStore.data.map { it[LIVE_GRID] ?: false }

    suspend fun setLiveGrid(grid: Boolean) {
        dataStore.edit { it[LIVE_GRID] = grid }
    }

    /** Oynatıcı tamponu (saniye): 10 / 30 / 60. */
    val playerBufferSeconds: Flow<Int> = dataStore.data.map { it[PLAYER_BUFFER_SECONDS] ?: 30 }

    suspend fun setPlayerBufferSeconds(seconds: Int) {
        dataStore.edit { it[PLAYER_BUFFER_SECONDS] = seconds }
    }

    /** Tema, vurgu, punto, rehber ve sistem tercihleri. */
    val appearance: Flow<Appearance> = dataStore.data.map { prefs ->
        Appearance(
            dark = prefs[DARK_THEME] ?: true,
            accent = prefs[ACCENT]?.let { runCatching { Accent.valueOf(it) }.getOrNull() }
                ?: Accent.AMBER,
            typeScale = prefs[TYPE_SCALE] ?: 1f,
            clock24h = prefs[CLOCK_24H] ?: true,
            epgRowHeightDp = prefs[EPG_ROW_HEIGHT] ?: 64,
            epgWindowDays = prefs[EPG_WINDOW_DAYS] ?: 3,
            autoFrameRate = prefs[AUTO_FRAME_RATE] ?: false,
            autoStart = prefs[AUTO_START] ?: false,
            sleepMinutes = prefs[SLEEP_MINUTES] ?: 0,
        )
    }

    suspend fun setDarkTheme(dark: Boolean) = dataStore.edit { it[DARK_THEME] = dark }

    suspend fun setAccent(accent: Accent) = dataStore.edit { it[ACCENT] = accent.name }

    suspend fun setTypeScale(scale: Float) = dataStore.edit { it[TYPE_SCALE] = scale }

    suspend fun setClock24h(enabled: Boolean) = dataStore.edit { it[CLOCK_24H] = enabled }

    suspend fun setEpgRowHeight(dp: Int) = dataStore.edit { it[EPG_ROW_HEIGHT] = dp.coerceIn(40, 110) }

    suspend fun setEpgWindowDays(days: Int) =
        dataStore.edit { it[EPG_WINDOW_DAYS] = days.coerceIn(1, 7) }

    suspend fun setAutoFrameRate(enabled: Boolean) = dataStore.edit { it[AUTO_FRAME_RATE] = enabled }

    suspend fun setAutoStart(enabled: Boolean) = dataStore.edit { it[AUTO_START] = enabled }

    suspend fun setSleepMinutes(minutes: Int) = dataStore.edit { it[SLEEP_MINUTES] = minutes }

    /** Rehber senkronunun kaynak başına son başarılı zamanı. */
    fun epgLastSync(playlistId: Long): Flow<Long?> =
        dataStore.data.map { it[longPreferencesKey("epg_last_sync_$playlistId")] }

    suspend fun setEpgLastSync(playlistId: Long, at: Long) =
        dataStore.edit { it[longPreferencesKey("epg_last_sync_$playlistId")] = at }

    /** Kayıt klasörü tercihi; boşsa uygulamanın dış dizini kullanılır. */
    val recordingFolder: Flow<String?> =
        dataStore.data.map { it[RECORDING_FOLDER]?.takeIf(String::isNotBlank) }

    suspend fun setRecordingFolder(uri: String?) = dataStore.edit { prefs ->
        if (uri.isNullOrBlank()) prefs.remove(RECORDING_FOLDER) else prefs[RECORDING_FOLDER] = uri
    }

    suspend fun exportValues(): Map<String, String> =
        dataStore.data.map { prefs ->
            prefs.asMap().mapNotNull { (key, value) ->
                val text = when (value) {
                    is Boolean, is Int, is Long, is Float, is String -> value.toString()
                    else -> null
                }
                text?.let { key.name to it }
            }.toMap()
        }.first()

    companion object {
        const val DEFAULT_PROFILE_ID = 1L
        private val ACTIVE_PROFILE = longPreferencesKey("active_profile_id")
        private val LIVE_GRID = booleanPreferencesKey("live_grid")
        private val PLAYER_BUFFER_SECONDS = intPreferencesKey("player_buffer_seconds")
        private val DARK_THEME = booleanPreferencesKey("appearance_dark")
        private val ACCENT = androidx.datastore.preferences.core.stringPreferencesKey("appearance_accent")
        private val TYPE_SCALE = floatPreferencesKey("appearance_type_scale")
        private val CLOCK_24H = booleanPreferencesKey("appearance_clock_24h")
        private val EPG_ROW_HEIGHT = intPreferencesKey("appearance_epg_row_height")
        private val EPG_WINDOW_DAYS = intPreferencesKey("epg_window_days")
        private val AUTO_FRAME_RATE = booleanPreferencesKey("player_auto_frame_rate")
        private val AUTO_START = booleanPreferencesKey("system_auto_start")
        private val SLEEP_MINUTES = intPreferencesKey("system_sleep_minutes")
        private val RECORDING_FOLDER = androidx.datastore.preferences.core.stringPreferencesKey("recording_folder")

        fun create(context: Context): PreferenceStore =
            PreferenceStore(context.applicationContext.novaPreferences)
    }
}
