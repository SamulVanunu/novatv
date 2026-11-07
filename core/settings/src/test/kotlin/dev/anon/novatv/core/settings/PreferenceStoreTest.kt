package dev.anon.novatv.core.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
}
