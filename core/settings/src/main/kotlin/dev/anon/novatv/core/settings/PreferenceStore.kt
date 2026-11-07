package dev.anon.novatv.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.novaPreferences by preferencesDataStore(name = "novatv_prefs")

class PreferenceStore(private val dataStore: DataStore<Preferences>) {

    val activeProfileId: Flow<Long> = dataStore.data.map { it[ACTIVE_PROFILE] ?: DEFAULT_PROFILE_ID }

    suspend fun setActiveProfile(id: Long) {
        dataStore.edit { it[ACTIVE_PROFILE] = id }
    }

    companion object {
        const val DEFAULT_PROFILE_ID = 1L
        private val ACTIVE_PROFILE = longPreferencesKey("active_profile_id")

        fun create(context: Context): PreferenceStore =
            PreferenceStore(context.applicationContext.novaPreferences)
    }
}
