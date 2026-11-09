package dev.anon.novatv.app

import android.content.Context
import android.net.Uri
import dev.anon.novatv.core.model.Profile
import dev.anon.novatv.core.settings.Accent
import dev.anon.novatv.core.settings.PreferenceStore
import dev.anon.novatv.core.sync.ProfileRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Görünüm ve profil yedeği.
 *
 * Kaynak adresleri, kullanıcı adları ve PIN özetleri bilerek dışarıda kalır;
 * yedek dosyası bir cihazdan diğerine taşınsa bile kimlik bilgisi taşımaz.
 */
class BackupManager(
    private val context: Context,
    private val preferences: PreferenceStore,
    private val profiles: ProfileRepository,
) {

    suspend fun backup(target: Uri?): String = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("app", "novatv")
            put("version", 1)
            put("createdAt", System.currentTimeMillis())
            put("settings", JSONObject(preferences.exportValues()))
            put(
                "profiles",
                JSONArray().apply {
                    profiles.observeAll().first().forEach { profile ->
                        put(
                            JSONObject().apply {
                                put("name", profile.name)
                                put("isKids", profile.isKids)
                                profile.maxRating?.let { put("maxRating", it) }
                            },
                        )
                    }
                },
            )
        }
        val text = payload.toString(2)
        if (target != null) {
            context.contentResolver.openOutputStream(target)?.use { stream ->
                stream.write(text.toByteArray(Charsets.UTF_8))
            } ?: return@withContext "Yedek yazılamadı"
            "Yedek alındı"
        } else {
            val file = defaultFile()
            file.parentFile?.mkdirs()
            file.writeText(text)
            "Yedek: ${file.absolutePath}"
        }
    }

    suspend fun restore(source: Uri?): String = withContext(Dispatchers.IO) {
        val text = if (source != null) {
            context.contentResolver.openInputStream(source)?.use { stream ->
                stream.readBytes().toString(Charsets.UTF_8)
            }
        } else {
            defaultFile().takeIf { it.exists() }?.readText()
        } ?: return@withContext "Yedek dosyası bulunamadı"

        val root = runCatching { JSONObject(text) }.getOrNull()
            ?: return@withContext "Yedek okunamadı"
        if (root.optString("app") != "novatv") return@withContext "Bu bir NovaTV yedeği değil"

        val settings = root.optJSONObject("settings") ?: JSONObject()
        applySettings(settings)

        var added = 0
        val names = profiles.observeAll().first().map { it.name }.toMutableSet()
        val list = root.optJSONArray("profiles") ?: JSONArray()
        for (index in 0 until list.length()) {
            val item = list.optJSONObject(index) ?: continue
            val name = item.optString("name").trim()
            if (name.isBlank() || !names.add(name)) continue
            runCatching {
                profiles.add(
                    name = name,
                    pin = null,
                    maxRating = item.optString("maxRating").takeIf { it.isNotBlank() },
                    isKids = item.optBoolean("isKids"),
                )
            }.onSuccess { added++ }
        }

        "Yedek geri yüklendi" + if (added > 0) " · $added profil eklendi" else ""
    }

    fun defaultFile(): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, "novatv-backup.json")

    private suspend fun applySettings(settings: JSONObject) {
        if (settings.has("appearance_dark")) {
            preferences.setDarkTheme(settings.optBoolean("appearance_dark", true))
        }
        settings.optString("appearance_accent").takeIf { it.isNotBlank() }?.let { name ->
            runCatching { Accent.valueOf(name) }.getOrNull()?.let { preferences.setAccent(it) }
        }
        if (settings.has("appearance_type_scale")) {
            preferences.setTypeScale(settings.optDouble("appearance_type_scale", 1.0).toFloat())
        }
        if (settings.has("appearance_clock_24h")) {
            preferences.setClock24h(settings.optBoolean("appearance_clock_24h", true))
        }
        if (settings.has("appearance_epg_row_height")) {
            preferences.setEpgRowHeight(settings.optInt("appearance_epg_row_height", 64))
        }
        if (settings.has("epg_window_days")) {
            preferences.setEpgWindowDays(settings.optInt("epg_window_days", 3))
        }
        if (settings.has("player_auto_frame_rate")) {
            preferences.setAutoFrameRate(settings.optBoolean("player_auto_frame_rate", false))
        }
        if (settings.has("system_auto_start")) {
            preferences.setAutoStart(settings.optBoolean("system_auto_start", false))
        }
        if (settings.has("system_sleep_minutes")) {
            preferences.setSleepMinutes(settings.optInt("system_sleep_minutes", 0))
        }
    }

    /** Profil listesindeki gizli alanları dışa vurmamak için yardımcı. */
    fun sanitize(profile: Profile): Profile = profile.copy(pinHash = null)
}
