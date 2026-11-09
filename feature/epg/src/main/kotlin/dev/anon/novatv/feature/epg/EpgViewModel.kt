package dev.anon.novatv.feature.epg

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.core.model.EpgProgram
import dev.anon.novatv.core.recording.RecordingPlan
import dev.anon.novatv.core.recording.RecordingScheduler
import dev.anon.novatv.core.recording.RecordingStore
import dev.anon.novatv.core.recording.ScheduleResult
import dev.anon.novatv.core.settings.PreferenceStore
import dev.anon.novatv.core.sync.EpgRepository
import dev.anon.novatv.core.sync.GuideRow
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EpgUiState(
    val dayOffset: Int = 0,
    val windowDays: Int = 3,
    val rowHeightDp: Int = 64,
    val clock24h: Boolean = true,
    val rows: List<GuideRow> = emptyList(),
    val now: Long = System.currentTimeMillis(),
    val loading: Boolean = true,
    val syncing: Boolean = false,
    val message: String? = null,
    val reminderIds: Set<String> = emptySet(),
    val recordedIds: Set<String> = emptySet(),
) {
    val dayLabel: String get() = epgDayLabel(dayOffset)
}

private val DAY_MONTH: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM", Locale("tr"))
private val DAY_NAME: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE", Locale("tr"))

/** Gün başlangıcı; rehber ızgarası gece yarısından gece yarısına uzanır. */
internal fun dayStart(offset: Int, zone: ZoneId = ZoneId.systemDefault()): Long =
    LocalDate.now(zone).plusDays(offset.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()

internal fun epgDayLabel(offset: Int, zone: ZoneId = ZoneId.systemDefault()): String {
    val date = LocalDate.now(zone).plusDays(offset.toLong())
    return when (offset) {
        0 -> "Bugün · " + DAY_MONTH.format(date)
        1 -> "Yarın · " + DAY_MONTH.format(date)
        else -> DAY_NAME.format(date).replaceFirstChar { it.uppercase(Locale("tr")) } +
            " · " + DAY_MONTH.format(date)
    }
}

/** Kayıt kimliği; kanal + program başlangıcı tekil anahtardır. */
internal fun recordingId(channelId: Long, startAt: Long): String = "rec-$channelId@$startAt"

/** Hatırlatıcı kimliği; kanal + başlangıç tekil anahtardır. */
internal fun reminderId(channelId: Long, startAt: Long): String = "$channelId@$startAt"

class EpgViewModel(
    private val repository: EpgRepository,
    private val preferences: PreferenceStore,
    private val reminders: ReminderScheduler? = null,
    private val recordingStore: RecordingStore? = null,
    private val recordingScheduler: RecordingScheduler? = null,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _state = MutableStateFlow(EpgUiState(now = clock()))
    val state: StateFlow<EpgUiState> = _state.asStateFlow()

    private val dayOffset = MutableStateFlow(0)
    private var guideJob: Job? = null
    private var syncedOnce = false

    init {
        _state.update {
            it.copy(
                reminderIds = reminders?.ids() ?: emptySet(),
                recordedIds = activeRecordingIds(),
            )
        }

        viewModelScope.launch {
            preferences.appearance.collect { appearance ->
                _state.update {
                    it.copy(
                        windowDays = appearance.windowDays,
                        rowHeightDp = appearance.epgRowHeight,
                        clock24h = appearance.clock24h,
                    )
                }
            }
        }

        viewModelScope.launch {
            dayOffset.collectLatest { offset -> loadGuide(offset) }
        }

        // Program bitişlerinde "şimdi" vurgusu ve detaydaki canlı durumu tazelensin.
        viewModelScope.launch {
            while (true) {
                _state.update { it.copy(now = clock()) }
                delay(TICK_MS)
            }
        }

        refresh(force = false)
    }

    fun selectDay(offset: Int) {
        val clamped = offset.coerceIn(0, _state.value.windowDays - 1)
        if (clamped == dayOffset.value) return
        _state.update { it.copy(dayOffset = clamped, loading = true, rows = emptyList()) }
        dayOffset.value = clamped
    }

    /** Elle yenileme; rehber kaynağı yoksa bunu kullanıcıya söyler. */
    fun refresh(force: Boolean = true) {
        if (_state.value.syncing) return
        viewModelScope.launch {
            _state.update { it.copy(syncing = true, message = null) }
            val results = runCatching { repository.syncAll(force = force || !syncedOnce) }
                .onFailure { throwable ->
                    _state.update { it.copy(message = throwable.message ?: "Rehber yenilenemedi") }
                }
                .getOrElse { emptyList() }
            syncedOnce = true
            val stored = results.sumOf { it.programs }
            val skipped = results.firstOrNull { it.skippedReason != null }?.skippedReason
            _state.update {
                it.copy(
                    syncing = false,
                    message = when {
                        results.isEmpty() -> "Aktif kaynak yok"
                        stored > 0 -> null
                        skipped != null -> skipped
                        else -> "Rehber verisi bulunamadı"
                    },
                )
            }
        }
    }

    fun toggleReminder(channel: Channel, program: EpgProgram) {
        val scheduler = reminders ?: return
        val id = reminderId(channel.id, program.startAt)
        val scheduled = id in _state.value.reminderIds
        if (scheduled) {
            scheduler.cancel(id)
        } else {
            scheduler.schedule(
                EpgReminder(
                    id = id,
                    channelId = channel.id,
                    channelName = channel.name,
                    title = program.title,
                    startAt = program.startAt,
                    endAt = program.endAt,
                ),
            )
        }
        _state.update {
            it.copy(reminderIds = if (scheduled) it.reminderIds - id else it.reminderIds + id)
        }
    }

    fun isReminded(channel: Channel, program: EpgProgram): Boolean =
        reminderId(channel.id, program.startAt) in _state.value.reminderIds

    /** Programı kaydet / planı iptal et. Aynı anda tek kayıt kuralı burada. */
    fun toggleRecording(channel: Channel, program: EpgProgram) {
        val scheduler = recordingScheduler ?: return
        if (recordingStore == null) return
        val id = recordingId(channel.id, program.startAt)
        if (id in _state.value.recordedIds) {
            scheduler.cancel(id)
            _state.update { it.copy(recordedIds = activeRecordingIds(), message = "Kayıt iptal edildi") }
            return
        }
        val now = clock()
        if (program.endAt <= now) {
            _state.update { it.copy(message = "Geçmiş program kaydedilemez") }
            return
        }
        val plan = RecordingPlan(
            id = id,
            channelId = channel.id,
            channelName = channel.name,
            title = program.title,
            startAt = program.startAt.coerceAtLeast(now),
            endAt = program.endAt,
            createdAt = now,
        )
        when (val result = scheduler.schedule(plan)) {
            is ScheduleResult.Scheduled -> _state.update {
                it.copy(
                    recordedIds = activeRecordingIds(),
                    message = "Kayıt planlandı · ${channel.name}",
                )
            }

            is ScheduleResult.Rejected -> _state.update { it.copy(message = result.reason) }
        }
    }

    fun isRecording(channel: Channel, program: EpgProgram): Boolean =
        recordingId(channel.id, program.startAt) in _state.value.recordedIds

    private fun activeRecordingIds(): Set<String> =
        recordingStore?.entries()
            ?.filter { it.isActive }
            ?.map { it.id }
            ?.toSet()
            ?: emptySet()

    private fun loadGuide(offset: Int) {
        guideJob?.cancel()
        guideJob = viewModelScope.launch {
            val from = dayStart(offset)
            repository.guide(from = from, to = from + EpgRepository.DAY_MS)
                .collectLatest { rows ->
                    _state.update { it.copy(rows = rows, loading = false) }
                }
        }
    }

    companion object {
        private const val TICK_MS = 30_000L

        fun factory(
            repository: EpgRepository,
            preferences: PreferenceStore,
            reminders: ReminderScheduler?,
            recordingStore: RecordingStore? = null,
            recordingScheduler: RecordingScheduler? = null,
        ) = viewModelFactory {
            initializer {
                EpgViewModel(
                    repository = repository,
                    preferences = preferences,
                    reminders = reminders,
                    recordingStore = recordingStore,
                    recordingScheduler = recordingScheduler,
                )
            }
        }
    }
}
