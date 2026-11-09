package dev.anon.novatv.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.core.player.PlaybackController
import dev.anon.novatv.core.player.PlaybackState
import dev.anon.novatv.core.player.TrackChoice
import dev.anon.novatv.core.player.VideoAspect
import dev.anon.novatv.core.settings.PreferenceStore
import dev.anon.novatv.core.sync.ChannelRepository
import dev.anon.novatv.core.sync.ResolvedStream
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Kanalın yayın adresini üretir; Xtream kimliği Room'a yazılmadığı için her açılışta çözülür. */
fun interface ChannelStreamResolver {
    suspend fun resolve(channel: Channel): ResolvedStream
}

data class PlayerUiState(
    val channel: Channel? = null,
    val playback: PlaybackState = PlaybackState.Idle,
    val osdVisible: Boolean = false,
    val audioTracks: List<TrackChoice> = emptyList(),
    val subtitleTracks: List<TrackChoice> = emptyList(),
    val aspect: VideoAspect = VideoAspect.FIT,
    val bufferSeconds: Int = 30,
    val canSeekBack: Boolean = false,
    val error: String? = null,
)

class PlayerViewModel(
    private val playbackController: PlaybackController,
    private val repository: ChannelRepository,
    private val resolver: ChannelStreamResolver,
    private val preferenceStore: PreferenceStore,
) : ViewModel() {

    private val channel = MutableStateFlow<Channel?>(null)
    private val osdVisible = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private var hideJob: Job? = null
    private var lastRecordedChannelId: Long? = null

    private val bufferSeconds: StateFlow<Int> = preferenceStore.playerBufferSeconds
        .catch { emit(DEFAULT_BUFFER_SECONDS) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, DEFAULT_BUFFER_SECONDS)

    private val playbackInfo = combine(
        playbackController.state,
        playbackController.audioTracks,
        playbackController.subtitleTracks,
    ) { playback, audio, subtitles -> Triple(playback, audio, subtitles) }

    private val osdInfo = combine(
        osdVisible,
        playbackController.aspect,
        playbackController.canSeekBack,
    ) { visible, aspect, seekBack -> OsdInfo(visible, aspect, seekBack) }

    val state: StateFlow<PlayerUiState> = combine(
        channel,
        playbackInfo,
        osdInfo,
        bufferSeconds,
        error,
    ) { channel, playback, osd, buffer, message ->
        PlayerUiState(
            channel = channel,
            playback = playback.first,
            osdVisible = osd.visible,
            audioTracks = playback.second,
            subtitleTracks = playback.third,
            aspect = osd.aspect,
            bufferSeconds = buffer,
            canSeekBack = osd.canSeekBack,
            error = message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PlayerUiState(),
    )

    init {
        viewModelScope.launch {
            playbackController.state.collect { playback ->
                val current = channel.value ?: return@collect
                if (playback !is PlaybackState.Playing) return@collect
                if (current.id == lastRecordedChannelId) return@collect
                lastRecordedChannelId = current.id
                runCatching { repository.recordWatch(current) }.onFailure(::report)
            }
        }
    }

    fun open(channelId: Long) {
        lastRecordedChannelId = null
        viewModelScope.launch {
            val found = runCatching { repository.byId(channelId) }
                .onFailure(::report)
                .getOrNull()
            if (found == null) {
                error.value = "Kanal bulunamadı"
                return@launch
            }
            channel.value = found
            play(found)
        }
    }

    fun retry() {
        val current = channel.value ?: return
        viewModelScope.launch { play(current) }
    }

    fun toggleOsd() {
        if (osdVisible.value) hideOsd() else showOsd()
    }

    fun showOsd() {
        osdVisible.value = true
        hideJob?.cancel()
        hideJob = viewModelScope.launch {
            delay(OSD_TIMEOUT_MS)
            osdVisible.value = false
        }
    }

    fun hideOsd() {
        hideJob?.cancel()
        osdVisible.value = false
    }

    /** Açık bir menü varken OSD'nin kapanmasını engeller. */
    fun keepOsdVisible(pinned: Boolean) {
        if (pinned) {
            hideJob?.cancel()
            osdVisible.value = true
        } else {
            showOsd()
        }
    }

    fun seekBack(ms: Long = SEEK_BACK_STEP_MS) {
        playbackController.seekBack(ms)
        showOsd()
    }

    fun selectAudio(id: String) {
        playbackController.selectAudio(id)
        showOsd()
    }

    fun selectSubtitle(id: String?) {
        playbackController.selectSubtitle(id)
        showOsd()
    }

    fun cycleAspect() {
        val values = VideoAspect.entries
        val next = values[(values.indexOf(playbackController.aspect.value) + 1) % values.size]
        playbackController.setAspect(next)
        showOsd()
    }

    fun cycleBuffer() {
        val next = when (state.value.bufferSeconds) {
            10 -> 30
            30 -> 60
            else -> 10
        }
        viewModelScope.launch {
            runCatching {
                preferenceStore.setPlayerBufferSeconds(next)
                playbackController.setBufferMs(next * 1_000)
            }.onFailure(::report)
        }
        showOsd()
    }

    fun playback(): PlaybackController = playbackController

    private suspend fun play(channel: Channel) {
        runCatching { resolver.resolve(channel) }
            .onSuccess { stream ->
                error.value = null
                playbackController.prepare(stream.url, stream.headers)
            }
            .onFailure { throwable ->
                report(throwable)
                error.value = throwable.message ?: "Yayın adresi çözülemedi"
            }
    }

    private fun report(throwable: Throwable) {
        error.value = throwable.message ?: throwable::class.simpleName
    }

    private data class OsdInfo(
        val visible: Boolean,
        val aspect: VideoAspect,
        val canSeekBack: Boolean,
    )

    companion object {
        const val OSD_TIMEOUT_MS = 4_000L
        const val SEEK_BACK_STEP_MS = 10_000L
        const val DEFAULT_BUFFER_SECONDS = 30

        fun factory(
            playbackController: PlaybackController,
            repository: ChannelRepository,
            resolver: ChannelStreamResolver,
            preferenceStore: PreferenceStore,
        ) = viewModelFactory {
            initializer { PlayerViewModel(playbackController, repository, resolver, preferenceStore) }
        }
    }
}
