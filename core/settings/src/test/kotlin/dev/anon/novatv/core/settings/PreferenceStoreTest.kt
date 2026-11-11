package dev.anon.novatv.core.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class PreferenceStoreTest {

    private lateinit var dir: File
    private lateinit var store: PreferenceStore

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("novatv-prefs").toFile()
        store = PreferenceStore(
            PreferenceDataStoreFactory.create(
                produceFile = { File(dir, "test.preferences_pb") },
            ),
        )
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun activeProfileDefaultsToFirstProfile() = runBlocking {
        assertEquals(1L, store.activeProfileId.first())
    }

    @Test
    fun activeProfilePersists() = runBlocking {
        store.setActiveProfile(3L)
        assertEquals(3L, store.activeProfileId.first())
    }

    /**
     * Yedek yalnız görünüm ve oynatma tercihlerini taşır. Kayıt klasörü bir
     * SAF ağaç URI'i (başka cihazda anlamsız), EPG damgası kaynak kimliği
     * taşır; ikisi de `applySettings` tarafından hiç okunmuyordu.
     */
    @Test
    fun yedekCihazaBagliTercihleriTasimaz() = runBlocking {
        store.setDarkTheme(dark = true)
        store.setTypeScale(1.15f)
        store.setClock24h(enabled = true)
        store.setRecordingFolder(uri = "content://tree/primary%3AKayitlar")
        store.setEpgLastSync(playlistId = 7L, at = 1_700_000_000_000L)
        store.setActiveProfile(3L)

        val exported = store.exportValues()

        assertEquals("true", exported["appearance_dark"])
        assertEquals("1.15", exported["appearance_type_scale"])
        assertEquals("true", exported["appearance_clock_24h"])
        assertFalse(exported.containsKey("recording_folder"))
        assertFalse(exported.keys.any { it.startsWith("epg_last_sync_") })
        // Profil kimliği profil listesiyle birlikte anlam kazanır; tek başına
        // taşınmaz.
        assertFalse(exported.containsKey("active_profile_id"))
    }
}
