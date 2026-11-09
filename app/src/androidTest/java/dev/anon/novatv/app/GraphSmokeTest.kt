package dev.anon.novatv.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GraphSmokeTest {

    @Test
    fun preferencesAndGroupsEmit() = runBlocking {
        val graph = (InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as NovaApplication).graph

        val profile = withTimeout(10_000) { graph.preferences.activeProfileId.first() }
        assertEquals(1L, profile)

        val grid = withTimeout(10_000) { graph.preferences.liveGrid.first() }

        val groups = withTimeout(10_000) { graph.channels.groups().first() }
        println("NOVA profile=$profile grid=$grid groups=${groups.size}")
    }
}
