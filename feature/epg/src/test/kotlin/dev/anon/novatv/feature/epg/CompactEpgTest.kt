package dev.anon.novatv.feature.epg

import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.core.model.EpgProgram
import dev.anon.novatv.core.sync.GuideRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * `nowNextOf` seçimi: şimdiki program `startAt <= now < endAt`, sıradaki
 * en erken `startAt > now` programı. Geçmiş program asla sıradaki olmaz.
 */
class CompactEpgTest {

    @Test
    fun midProgramIsCurrentAndNextIsTheFollowingProgram() {
        val row = guide(
            program("A", start = 0, end = HOUR),
            program("B", start = HOUR, end = 2 * HOUR),
        )

        val (current, next) = nowNextOf(row, now = HOUR / 2)

        assertEquals("A", current?.title)
        assertEquals("B", next?.title)
    }

    @Test
    fun programStartingExactlyNowIsCurrent() {
        // Sınır programı ayrı tutulur: A tam bu anda biter, B tam bu anda başlar.
        val row = guide(
            program("A", start = 0, end = HOUR),
            program("B", start = HOUR, end = 2 * HOUR),
            program("C", start = 2 * HOUR, end = 3 * HOUR),
        )

        val (current, next) = nowNextOf(row, now = HOUR)

        assertEquals("B", current?.title)
        assertEquals("C", next?.title)
    }

    @Test
    fun programEndingExactlyNowIsNotCurrent() {
        // Bitiş sınırdadır: `now < endAt` açık uçludur, A kalmış sayılır.
        val row = guide(
            program("A", start = 0, end = HOUR),
            program("B", start = 2 * HOUR, end = 3 * HOUR),
        )

        val (current, next) = nowNextOf(row, now = HOUR)

        assertNull(current)
        assertEquals("B", next?.title)
    }

    @Test
    fun gapBetweenProgramsHasNoCurrent() {
        val row = guide(
            program("A", start = 0, end = HOUR),
            program("B", start = 2 * HOUR, end = 3 * HOUR),
        )

        val (current, next) = nowNextOf(row, now = HOUR + 1)

        assertNull(current)
        assertEquals("B", next?.title)
    }

    @Test
    fun pastProgramsAreNeverNext() {
        val row = guide(
            program("geçen", start = -2 * HOUR, end = -HOUR),
            program("A", start = 0, end = HOUR),
        )

        val (_, next) = nowNextOf(row, now = HOUR + 1)

        assertNull(next)
    }

    @Test
    fun allProgramsPastYieldsNoCurrentAndNoNext() {
        val row = guide(
            program("A", start = 0, end = HOUR),
            program("B", start = HOUR, end = 2 * HOUR),
        )

        val (current, next) = nowNextOf(row, now = 2 * HOUR)

        assertNull(current)
        assertNull(next)
    }

    @Test
    fun emptyRowIsNullOrNull() {
        val (current, next) = nowNextOf(guide(), now = HOUR)

        assertNull(current)
        assertNull(next)
    }

    @Test
    fun unsortedProgramsArePickedByTimeNotByOrder() {
        val row = guide(
            program("C", start = 2 * HOUR, end = 3 * HOUR),
            program("A", start = 0, end = HOUR),
            program("B", start = HOUR, end = 2 * HOUR),
        )

        val (current, next) = nowNextOf(row, now = 3 * HOUR / 2)

        assertEquals("B", current?.title)
        assertEquals("C", next?.title)
    }

    private fun guide(vararg programs: EpgProgram) =
        GuideRow(channel = Channel(playlistId = 1, remoteId = "1", name = "Kanal", groupName = ""), programs = programs.toList())

    private fun program(title: String, start: Long, end: Long) = EpgProgram(
        epgChannelId = 1,
        channelId = 1,
        startAt = start,
        endAt = end,
        title = title,
    )

    private companion object {
        const val HOUR = 3_600_000L
    }
}
