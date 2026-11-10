package dev.anon.novatv.core.settings

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dev.anon.novatv.core.model.Credentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EncryptedCredentialStore(
    context: Context,
    fileName: String = DEFAULT_FILE,
) : CredentialStore {

    private val prefs: SharedPreferences =
        openOrRecover(context.applicationContext, fileName)

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
            .remove(sourceUrlKey(playlistId))
            .apply()
    }

    override suspend fun putSourceUrl(playlistId: Long, url: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(sourceUrlKey(playlistId), url).apply()
    }

    override suspend fun getSourceUrl(playlistId: Long): String? = withContext(Dispatchers.IO) {
        prefs.getString(sourceUrlKey(playlistId), null)
    }

    override suspend fun removeSourceUrl(playlistId: Long) = withContext(Dispatchers.IO) {
        prefs.edit().remove(sourceUrlKey(playlistId)).apply()
    }

    private fun userKey(id: Long) = "playlist_${id}_user"
    private fun passKey(id: Long) = "playlist_${id}_pass"
    private fun sourceUrlKey(id: Long) = "playlist_${id}_source"

    private fun openOrRecover(context: Context, fileName: String): SharedPreferences =
        runCatching { create(context, fileName) }.getOrElse { error ->
            // Yedekten geri yükleme veya anahtar kaybı: dosya eski anahtarla
            // şifrelenmiş, yeni anahtar çözemiyor. Dosyayı at, yeniden kur.
            Log.w(TAG, "Şifreli depo açılamadı, sıfırlanıyor: ${error.javaClass.simpleName}")
            context.getSharedPreferences(fileName, Context.MODE_PRIVATE).edit().clear().commit()
            create(context, fileName)
        }

    private fun create(context: Context, fileName: String): SharedPreferences =
        EncryptedSharedPreferences.create(
            context,
            fileName,
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

    companion object {

        private const val TAG = "CredentialStore"
        private const val DEFAULT_FILE = "novatv_credentials"

        /**
         * Şifreli depo hiç kurulamazsa uygulamanın açılmaya devam etmesi için
         * bellek içi depoya düşer; kimlik bilgisi kaybı oturumla sınırlı kalır.
         */
        fun open(context: Context, fileName: String = DEFAULT_FILE): CredentialStore =
            runCatching { EncryptedCredentialStore(context, fileName) }.getOrElse { error ->
                Log.e(TAG, "Şifreli depo kurulamadı, belleğe düşüldü: ${error.javaClass.simpleName}")
                InMemoryCredentialStore()
            }
    }
}
