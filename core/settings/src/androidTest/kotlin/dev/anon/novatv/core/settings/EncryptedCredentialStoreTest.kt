package dev.anon.novatv.core.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedCredentialStoreTest {

    @Test
    fun storesReadsAndRemovesCredentials() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = EncryptedCredentialStore(context, fileName = "novatv_credentials_test")

        store.put(42L, Credentials("demo-user", "demo-pass"))
        assertEquals(Credentials("demo-user", "demo-pass"), store.get(42L))

        store.remove(42L)
        assertNull(store.get(42L))
    }
}
