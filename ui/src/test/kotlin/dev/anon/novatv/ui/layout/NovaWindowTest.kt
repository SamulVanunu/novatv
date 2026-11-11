package dev.anon.novatv.ui.layout

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class NovaWindowTest {

    @Test
    fun `599 dp compact`() {
        assertEquals(NovaWindow.Compact, windowClass(599))
    }

    @Test
    fun `600 dp medium`() {
        assertEquals(NovaWindow.Medium, windowClass(600))
    }

    @Test
    fun `839 dp medium`() {
        assertEquals(NovaWindow.Medium, windowClass(839))
    }

    @Test
    fun `840 dp expanded`() {
        assertEquals(NovaWindow.Expanded, windowClass(840))
    }

    @Test
    fun `spacing compact 16-8`() {
        val spacing = spacingFor(NovaWindow.Compact)
        assertEquals(16.dp, spacing.screen)
        assertEquals(8.dp, spacing.gap)
    }

    @Test
    fun `spacing medium 24-12`() {
        val spacing = spacingFor(NovaWindow.Medium)
        assertEquals(24.dp, spacing.screen)
        assertEquals(12.dp, spacing.gap)
    }

    @Test
    fun `spacing expanded 32-16`() {
        val spacing = spacingFor(NovaWindow.Expanded)
        assertEquals(32.dp, spacing.screen)
        assertEquals(16.dp, spacing.gap)
    }
}
