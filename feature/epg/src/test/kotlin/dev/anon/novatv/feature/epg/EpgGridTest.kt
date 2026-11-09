package dev.anon.novatv.feature.epg

import dev.anon.novatv.core.model.EpgProgram
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EpgGridTest {

    private val utc = ZoneId.of("UTC")

    @Test
    fun blocksFillGapsBeforeBetweenAndAfter() {
        val from = 0L
        val to = 4 * HOUR
        val blocks = buildBlocks(
            listOf(
                program(start = HOUR, end = 2 * HOUR, title = "A"),
                program(start = 3 * HOUR, end = 4 * HOUR, title = "B"),
            ),
            from = from,
            to = to,
        )

        assertEquals(4, blocks.size)
        assertNull(blocks[0].program)
        assertEquals(0L, blocks[0].start)
        assertEquals(HOUR, blocks[0].end)
        assertEquals("A", blocks[1].program?.title)
        assertNull(blocks[2].program)
        assertEquals(2 * HOUR, blocks[2].start)
        assertEquals(3 * HOUR, blocks[2].end)
        assertEquals("B", blocks[3].program?.title)
    }

    @Test
    fun overlappingAndPartiallyOutsideProgramsAreClipped() {
        val blocks = buildBlocks(
            listOf(
                program(start = -HOUR, end = HOUR, title = "taşan"),
                program(start = HOUR / 2, end = 2 * HOUR, title = "örtüşen"),
                program(start = 5 * HOUR, end = 6 * HOUR, title = "pencere dışı"),
            ),
            from = 0L,
            to = 4 * HOUR,
        )

        // Örtüşen program kırpılır, sıradaki program ondan sonra başlar.
        assertEquals(3, blocks.size)
        assertEquals(0L, blocks[0].start)
        assertEquals(HOUR, blocks[0].end)
        assertEquals("taşan", blocks[0].program?.title)
        assertEquals(HOUR, blocks[1].start)
        assertEquals(2 * HOUR, blocks[1].end)
        assertEquals("örtüşen", blocks[1].program?.title)
        assertNull(blocks[2].program)
        assertEquals(4 * HOUR, blocks[2].end)
    }

    @Test
    fun emptyGuideIsOneGap() {
        val blocks = buildBlocks(emptyList(), from = 0, to = 2 * HOUR)
        assertEquals(1, blocks.size)
        assertNull(blocks[0].program)
        assertEquals(2 * HOUR, blocks[0].end)
    }

    @Test
    fun dayLabelsNameTodayTomorrowAndWeekday() {
        // Gün adı biçimi JVM yereline göre kısaltılır; önek ve tarih parçası yeter.
        assertTrue(epgDayLabel(0, utc).startsWith("Bugün · "))
        assertTrue(epgDayLabel(1, utc).startsWith("Yarın · "))
        assertTrue(epgDayLabel(2, utc).contains(" · "))
    }

    @Test
    fun dayTabLabelsAreShort() {
        assertEquals("Bugün", dayTabLabel(0, utc))
        assertEquals("Yarın", dayTabLabel(1, utc))
        assertTrue(dayTabLabel(2, utc).length <= 3)
    }

    @Test
    fun reminderIdIsUniquePerChannelAndStart() {
        assertEquals("7@1000", reminderId(7, 1000))
        assertTrue(reminderId(7, 1000) != reminderId(7, 1001))
    }

    private fun program(start: Long, end: Long, title: String) = EpgProgram(
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
