package dev.anon.novatv.core.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dev.anon.novatv.core.model.Credentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EncryptedCredentialStore(
    context: Context,
    fileName: String = "novatv_credentials",
) : CredentialStore {

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context.applicationContext,
        fileName,
        MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override suspend fun put(playlistId: Long, credentials: Credentials) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putString(userKey(playlistId), credentials.username)
            .putString(passKey(playlistId), credentials.password)
            .apply()
    }

    override suspend fun get(playlistId: Long): Credentials? = withContext(Dispatchers.IO) {
        val user = prefs.getString(userKey(playlistId), null)
        val pass = prefs.getString(passKey(playlistId), null)
        if (user == null && pass == null) null else Credentials(user, pass)
    }

    override suspend fun remove(playlistId: Long) = withContext(Dispatchers.IO) {
        prefs.edit()
            .remove(userKey(playlistId))
            .remove(passKey(playlistId))
            .apply()
    }

    private fun userKey(id: Long) = "playlist_${id}_user"
    private fun passKey(id: Long) = "playlist_${id}_pass"
}
