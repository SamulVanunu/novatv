package dev.anon.novatv.feature.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AutoFrameRateTest {

    private val tvModes = listOf(
        DisplayModeOption(id = 1, refreshRateHz = 50f),
        DisplayModeOption(id = 2, refreshRateHz = 60f),
        DisplayModeOption(id = 3, refreshRateHz = 24f),
        DisplayModeOption(id = 4, refreshRateHz = 23.976f),
    )

    @Test
    fun picksNearestModeForFilmFrameRate() {
        assertEquals(4, AutoFrameRate.selectMode(tvModes, 23.976f)?.id)
    }

    @Test
    fun picksNearestModeWithinTolerance() {
        assertEquals(3, AutoFrameRate.selectMode(tvModes, 24.5f)?.id)
    }

    @Test
    fun refusesModeOutsideTolerance() {
        assertNull(AutoFrameRate.selectMode(tvModes, 30f))
    }

    @Test
    fun ignoresInvalidFrameRateAndModes() {
        assertNull(AutoFrameRate.selectMode(tvModes, 0f))
        assertNull(AutoFrameRate.selectMode(emptyList(), 50f))
        assertNull(
            AutoFrameRate.selectMode(listOf(DisplayModeOption(9, 0f)), 50f),
        )
    }

    @Test
    fun exactMatchWins() {
        assertEquals(1, AutoFrameRate.selectMode(tvModes, 50f)?.id)
    }
}
