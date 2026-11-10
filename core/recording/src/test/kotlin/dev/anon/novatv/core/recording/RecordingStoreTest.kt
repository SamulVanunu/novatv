package dev.anon.novatv.core.recording

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RecordingStoreTest {

    private val context: android.content.Context = ApplicationProvider.getApplicationContext()
    private val store = RecordingStore(context)

    @Test
    fun manualStopKeepsActualFinishTime() {
        val plan = plan()
        assertTrue(store.addPlan(plan))

        store.markRecording(plan.id)
        store.markDone(plan.id, "/tmp/kayit.ts", 1234L, finishedAt = plan.startAt + 25 * 60_000L)

        val entry = store.entry(plan.id)
        assertNotNull(entry)
        assertEquals(RecordingStatus.DONE.name, entry?.status)
        assertEquals(plan.startAt + 25 * 60_000L, entry?.finishedAt)
        assertEquals(1234L, entry?.bytes)
        assertTrue(store.plans().isEmpty())
    }

    @Test
    fun failedRecordingAlsoRecordsFinishTime() {
        val plan = plan()
        store.addPlan(plan)

        store.markFailed(plan.id, "Kayıt yarıda kaldı", bytes = 10L, finishedAt = plan.startAt + 60_000L)

        assertEquals(plan.startAt + 60_000L, store.entry(plan.id)?.finishedAt)
    }

    private fun plan() = RecordingPlan(
        id = "rec-test",
        channelId = 7,
        channelName = "TRT 1",
        title = "Test",
        startAt = 1_790_000_000_000L,
        endAt = 1_790_003_600_000L,
    )
}
