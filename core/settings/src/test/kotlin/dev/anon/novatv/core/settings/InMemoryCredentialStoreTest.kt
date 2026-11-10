package dev.anon.novatv.core.settings

import dev.anon.novatv.core.model.Credentials
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InMemoryCredentialStoreTest {

    @Test
    fun keepsCredentialsForSession() = runTest {
        val store = InMemoryCredentialStore()

        store.put(3L, Credentials("user", "pass"))

        assertEquals(Credentials("user", "pass"), store.get(3L))
    }

    @Test
    fun removesCredentialsAndSourceUrlTogether() = runTest {
        val store = InMemoryCredentialStore()
        store.put(3L, Credentials("user", "pass"))
        store.putSourceUrl(3L, "https://example/list.m3u")

        store.remove(3L)

        assertNull(store.get(3L))
        assertNull(store.getSourceUrl(3L))
    }

    @Test
    fun sourceUrlSurvivesSourceUrlRemovalOnly() = runTest {
        val store = InMemoryCredentialStore()
        store.put(4L, Credentials("user", "pass"))
        store.putSourceUrl(4L, "https://example/list.m3u")

        store.removeSourceUrl(4L)

        assertNull(store.getSourceUrl(4L))
        assertEquals(Credentials("user", "pass"), store.get(4L))
    }
}
