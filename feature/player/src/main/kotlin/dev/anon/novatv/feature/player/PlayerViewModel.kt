package dev.anon.novatv.feature.player

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.core.player.PlaybackController
import dev.anon.novatv.core.player.PlaybackState
import dev.anon.novatv.core.player.SubtitleSpec
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant

/** Kanalın yayın adresini üretir; Xtream kimliği Room'a yazılmadığı için her açılışta çözülür. */
fun interface ChannelStreamResolver {
    suspend fun resolve(channel: Channel): ResolvedStream

    /** Arşiv akışı; kaynak catch-up sunmuyorsa null. */
    suspend fun resolveCatchup(channel: Channel, at: Instant, minutes: Int): ResolvedStream? = null
}

/** VOD/dizi oynatması: başlık, çözücü ve ilerleme kaydı oynatıcıya taşınır. */
data class VodRequest(
    val title: String,
    val startPositionMs: Long = 0,
    val resolve: suspend () -> ResolvedStream,
    val onProgress: suspend (positionMs: Long, durationMs: Long) -> Unit = { _, _ -> },
)

data class PlayerUiState(
    val channel: Channel? = null,
    val title: String? = null,
    val vod: Boolean = false,
    val playback: PlaybackState = PlaybackState.Idle,
    val osdVisible: Boolean = false,
    val audioTracks: List<TrackChoice> = emptyList(),
    val subtitleTracks: List<TrackChoice> = emptyList(),
    val aspect: VideoAspect = VideoAspect.FIT,
    val bufferSeconds: Int = 30,
    val canSeekBack: Boolean = false,
    val error: String? = null,
    /** Donanım→yazılım geçişi gibi kısa ömürlü oynatıcı bildirimi. */
    val notice: String? = null,
    /** Kanal değişiminde kısa süre görünen kanal bandı. */
    val zap: Channel? = null,
)

class PlayerViewModel(
    private val playbackController: PlaybackController,
    private val repository: ChannelRepository,
    private val resolver: ChannelStreamResolver,
    private val preferenceStore: PreferenceStore,
) : ViewModel() {

    private val channel = MutableStateFlow<Channel?>(null)
    private val title = MutableStateFlow<String?>(null)
    private val osdVisible = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val zap = MutableStateFlow<Channel?>(null)
    private val order = MutableStateFlow<List<Channel>>(emptyList())
    private var zapJob: Job? = null
    private var hideJob: Job? = null
    private var lastRecordedChannelId: Long? = null
    private var externalSubtitle: SubtitleSpec? = null

    /** Açık arşiv oynatması; yeniden deneme aynı aralığı ister. */
    private var catchup: Pair<Long, Int>? = null
    private var vod: VodRequest? = null
    private var progressJob: Job? = null

    private val bufferSeconds: StateFlow<Int> = preferenceStore.playerBufferSeconds
        .catch { emit(DEFAULT_BUFFER_SECONDS) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, DEFAULT_BUFFER_SECONDS)

    /**
     * Zapping sırası: Rehber'in gördüğü liste ([ChannelRepository.zappingOrder],
     * gizli kanal/grup yok, numara/ad sırası). [stepChannel] ve [selectChannel]
     * tek bu sıradan seçim yapar; dokunmatik kanal listesi de bu akışı okur.
     *
     * Açılışta bir kez yüklenir. İlk yükleme başarısızsa boş kalır, fırlatmaz;
     * listeden seçim yapılırken bir kez daha denenir.
     */
    val channelOrder: StateFlow<List<Channel>> = order.asStateFlow()

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

    private val messages = combine(error, playbackController.notice, zap) { failure, notice, zap ->
        Triple(failure, notice, zap)
    }

    private val identity = combine(channel, title) { channel, title -> channel to title }

    val state: StateFlow<PlayerUiState> = combine(
        identity,
        playbackInfo,
        osdInfo,
        bufferSeconds,
        messages,
    ) { identity, playback, osd, buffer, message ->
        PlayerUiState(
            channel = identity.first,
            title = identity.second,
            vod = vod != null,
            playback = playback.first,
            osdVisible = osd.visible,
            audioTracks = playback.second,
            subtitleTracks = playback.third,
            aspect = osd.aspect,
            bufferSeconds = buffer,
            canSeekBack = osd.canSeekBack,
            error = message.first,
            notice = message.second,
            zap = message.third,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PlayerUiState(),
    )

    init {
        // Bildirim kalıcı olmasın: birkaç saniye görünür, sonra kendiliğinden kapanır.
        viewModelScope.launch {
            playbackController.notice.filterNotNull().collectLatest {
                delay(NOTICE_TIMEOUT_MS)
                playbackController.clearNotice()
            }
        }
        viewModelScope.launch {
            playbackController.state.collect { playback ->
                val current = channel.value ?: return@collect
                if (playback !is PlaybackState.Playing) return@collect
                if (current.id == lastRecordedChannelId) return@collect
                lastRecordedChannelId = current.id
                runCatching { repository.recordWatch(current) }.onFailure(::report)
            }
        }
        loadChannelOrder()
    }

    fun open(channelId: Long, catchupAt: Long? = null, catchupMinutes: Int = 0) {
        clearZap()
        lastRecordedChannelId = null
        externalSubtitle = null
        vod = null
        title.value = null
        cancelProgress()
        catchup = catchupAt?.takeIf { it > 0 && catchupMinutes > 0 }?.let { it to catchupMinutes }
        viewModelScope.launch {
            val found = runCatching { repository.byId(channelId) }
                .onFailure(::report)
                .getOrNull()
            if (found == null) {
                error.value = "Kanal bulunamadı"
                return@launch
            }
            channel.value = found
            playCurrent(found)
        }
    }

    /** Film/bölüm oynatması; konum ve ilerleme geri bildirimi çağıran tarafta. */
    fun openVod(request: VodRequest) {
        clearZap()
        lastRecordedChannelId = null
        externalSubtitle = null
        catchup = null
        vod = request
        channel.value = null
        title.value = request.title
        viewModelScope.launch { playVod(request) }
        startProgress(request)
    }

    /**
     * Kanalı [delta] kadar ilerletir (CH+/CH- ya da D-pad yukarı/ağı).
     *
     * Sıra [channelOrder]: gizli kanal/gruplar yok, numara/ad sırası. Uçlarda
     * başa döner; arşiv (catch-up) izlerken canlıya geçer, film/dizide çalışmaz.
     */
    fun stepChannel(delta: Int) {
        if (vod != null) return
        val current = channel.value ?: return
        viewModelScope.launch {
            val order = currentOrder() ?: return@launch
            val target = channelStep(order, current.id, delta) ?: return@launch
            stageChannel(target)
        }
    }

    /**
     * Listeden doğrudan kanal seçimi (dokunmatik kanal listesi).
     *
     * Aynı kanala dokunmak yeniden yükleme yapmaz. Farklı kanalda arşiv aralığı
     * temizlenir, OSD kapanır, kanal bandı görünür ve canlı yayın açılır — TV'de
     * CH+/CH- ile geçişin yaptığı işin aynısı. Film/dizi oynarken yok sayılır.
     */
    fun selectChannel(selected: Channel) {
        if (vod != null) return
        val currentId = channel.value?.id
        viewModelScope.launch {
            val order = currentOrder() ?: return@launch
            val target = channelSwitchTarget(order, currentId, selected.id) ?: return@launch
            stageChannel(target)
        }
    }

    fun retry() {
        vod?.let { request ->
            viewModelScope.launch { playVod(request) }
            return
        }
        val current = channel.value ?: return
        viewModelScope.launch { playCurrent(current) }
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

    fun seekBackMinute() = seekBack(SEEK_BACK_MINUTE_MS)

    /**
     * [ms] kadar ileri sarar (film/dizi; dokunmatik OSD'nin +10 sn düğmesi).
     * Hedef, süre biliniyorsa süreyi aşmaz. Canlıda sarma yok.
     */
    fun seekForward(ms: Long = SEEK_FORWARD_STEP_MS) {
        if (vod == null) return
        runCatching {
            val duration = playbackController.durationMs
            val target = playbackController.positionMs + ms
            playbackController.seekTo(if (duration > 0) target.coerceAtMost(duration) else target)
        }
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

    /** Harici altyazı dosyasını akışa ekler; yayın yeniden hazırlanır. */
    fun setExternalSubtitle(uri: Uri, mimeType: String) {
        externalSubtitle = SubtitleSpec(uri = uri, mimeType = mimeType)
        vod?.let { request ->
            viewModelScope.launch { playVod(request) }
            return
        }
        val current = channel.value ?: return
        viewModelScope.launch { playCurrent(current) }
    }

    /** Cihazda belge seçici yoksa kullanıcıya Türkçe bilgi ver. */
    fun reportMissingSubtitlePicker() {
        playbackController.showNotice("Bu cihazda dosya seçici yok; altyazıyı yayına gömülü kullanın.")
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

    /**
     * Zapping sırasını yükler (bir kez, açılışta). Hata fırlatmaz; başarısız
     * yüklemede liste boş kalır ve kanal değişimi çalışmaz.
     */
    private fun loadChannelOrder() {
        viewModelScope.launch {
            runCatching { repository.zappingOrder() }
                .onFailure(::report)
                .onSuccess { loaded -> order.value = loaded }
        }
    }

    /**
     * Seçim için sırayı verir. Yükleme henüz bitmediyse bekler; başarısız
     * yüklemeden sonra (liste boş kaldıysa) bir kez tazelemeyi dener.
     */
    private suspend fun currentOrder(): List<Channel>? {
        if (order.value.isNotEmpty()) return order.value
        // Açılıştaki yükleme henüz bitmemiş olabilir: birkaç tick bekle,
        // kanal açılışının peşinden gelen zapping bunu geciktirmesin.
        val settled = withTimeoutOrNull(ORDER_WAIT_MS) {
            order.first { it.isNotEmpty() }
        }
        if (settled != null) return settled
        if (order.value.isEmpty()) {
            runCatching { repository.zappingOrder() }
                .onFailure(::report)
                .onSuccess { loaded -> order.value = loaded }
        }
        return order.value.takeIf { it.isNotEmpty() }
    }

    /**
     * Kanala geçişin ortak etkisi: arşiv aralığı temizlenir, OSD kapanır,
     * kanal bandı görünür ve yayın açılır. [stepChannel] ve [selectChannel]
     * hedef kanalı kendi kararıyla bulur, etkiyi burada paylaşır.
     */
    private suspend fun stageChannel(target: Channel) {
        catchup = null
        hideOsd()
        channel.value = target
        showZapBadge(target)
        play(target)
    }

    private suspend fun playVod(request: VodRequest) {
        runCatching { request.resolve() }
            .onSuccess { stream ->
                error.value = null
                playbackController.prepare(
                    url = stream.url,
                    headers = stream.headers,
                    startPositionMs = request.startPositionMs,
                    subtitle = externalSubtitle,
                )
            }
            .onFailure { throwable ->
                report(throwable)
                error.value = throwable.message ?: "İçerik adresi çözülemedi"
            }
    }

    private fun startProgress(request: VodRequest) {
        cancelProgress()
        progressJob = viewModelScope.launch {
            while (true) {
                delay(PROGRESS_MS)
                val duration = playbackController.durationMs
                if (duration <= 0) continue
                runCatching {
                    request.onProgress(playbackController.positionMs, duration)
                }
            }
        }
    }

    private fun cancelProgress() {
        progressJob?.cancel()
        progressJob = null
    }

    override fun onCleared() {
        cancelProgress()
        super.onCleared()
    }

    private suspend fun playCurrent(channel: Channel) {
        val range = catchup
        if (range == null) {
            play(channel)
            return
        }
        val (startAt, minutes) = range
        runCatching { resolver.resolveCatchup(channel, Instant.ofEpochMilli(startAt), minutes) }
            .onSuccess { stream ->
                if (stream == null) {
                    error.value = "Bu kanalda arşiv yok"
                    play(channel)
                    return@onSuccess
                }
                error.value = null
                playbackController.prepare(stream.url, stream.headers, subtitle = externalSubtitle)
            }
            .onFailure { throwable ->
                report(throwable)
                error.value = throwable.message ?: "Arşiv akışı çözülemedi"
            }
    }

    private suspend fun play(channel: Channel) {
        runCatching { resolver.resolve(channel) }
            .onSuccess { stream ->
                error.value = null
                playbackController.prepare(stream.url, stream.headers, subtitle = externalSubtitle)
            }
            .onFailure { throwable ->
                report(throwable)
                error.value = throwable.message ?: "Yayın adresi çözülemedi"
            }
    }

    private fun showZapBadge(target: Channel) {
        zapJob?.cancel()
        zap.value = target
        zapJob = viewModelScope.launch {
            delay(ZAP_BADGE_MS)
            zap.value = null
        }
    }

    private fun clearZap() {
        zapJob?.cancel()
        zapJob = null
        zap.value = null
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
        const val NOTICE_TIMEOUT_MS = 6_000L
        const val ZAP_BADGE_MS = 2_500L
        const val SEEK_BACK_STEP_MS = 10_000L
        const val SEEK_BACK_MINUTE_MS = 60_000L
        const val SEEK_FORWARD_STEP_MS = 10_000L
        const val DEFAULT_BUFFER_SECONDS = 30
        private const val PROGRESS_MS = 3_000L
        private const val ORDER_WAIT_MS = 1_000L

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
