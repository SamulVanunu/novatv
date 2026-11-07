@file:OptIn(UnstableApi::class)

package dev.anon.novatv.core.player

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.util.EventLogger
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** Kaynak türü — uzantıdan çıkarılır. */
enum class SourceKind { HLS, DASH, PROGRESSIVE }

/**
 * Saf yardımcı: desteklenen üç kaynak fabrikasından URL'e uygun olanı seçer.
 * Testler bu fonksiyonu sahte fabrikalarla doğrular.
 */
fun <T> mediaSourceFor(
    url: String,
    hls: (String) -> T,
    dash: (String) -> T,
    progressive: (String) -> T,
): T = when (sourceKindFor(url)) {
    SourceKind.HLS -> hls(url)
    SourceKind.DASH -> dash(url)
    SourceKind.PROGRESSIVE -> progressive(url)
}

fun sourceKindFor(url: String): SourceKind {
    val path = url.substringBefore('?').substringBefore('#').lowercase(Locale.ROOT)
    return when {
        path.endsWith(".m3u8") -> SourceKind.HLS
        path.endsWith(".mpd") -> SourceKind.DASH
        else -> SourceKind.PROGRESSIVE
    }
}

/**
 * Media3 hatalarını kullanıcıya gösterilebilir duruma çeviren saf kural.
 * Ağ/HTTP kaynaklı hatalar yeniden denenebilir; kapsayıcı/kodek/DRM hataları değil.
 */
internal object PlaybackErrorPolicy {

    fun isDecoderFailure(error: PlaybackException): Boolean = when (error.errorCode) {
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
        -> true

        else -> false
    }

    fun map(error: PlaybackException): PlaybackState.Failed = PlaybackState.Failed(
        reason = reason(error),
        retryable = isRetryable(error),
    )

    private fun reason(error: PlaybackException): String {
        val cause = error.cause
        if (cause is HttpDataSource.InvalidResponseCodeException) {
            return "HTTP ${cause.responseCode} ${cause.responseMessage.orEmpty()}".trim()
        }
        return cause?.message?.takeIf { it.isNotBlank() }
            ?: error.message?.takeIf { it.isNotBlank() }
            ?: "Oynatma hatası"
    }

    private fun isRetryable(error: PlaybackException): Boolean = when (error.errorCode) {
        PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
        PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED,
        -> true

        else -> false
    }
}

/**
 * ExoPlayer sarmalayıcısı: durum, parça seçimi, oran, tampon ve hata davranışı.
 * Tüm metotlar ana iş parçacığından çağrılmalıdır.
 */
class PlaybackController(private val context: Context) {

    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<TrackChoice>>(emptyList())
    val audioTracks: StateFlow<List<TrackChoice>> = _audioTracks.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<TrackChoice>>(emptyList())
    val subtitleTracks: StateFlow<List<TrackChoice>> = _subtitleTracks.asStateFlow()

    private val _canSeekBack = MutableStateFlow(false)
    val canSeekBack: StateFlow<Boolean> = _canSeekBack.asStateFlow()

    private val _aspect = MutableStateFlow(VideoAspect.FIT)
    val aspect: StateFlow<VideoAspect> = _aspect.asStateFlow()

    /** Donanım çözmeden yazılıma düşüldüğünde bir kez doldurulur; UI gösterip temizler. */
    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    private var lastTracks: Tracks? = null
    private var currentUrl: String? = null
    private var currentHeaders: Map<String, String> = emptyMap()

    // buildPlayer() bu alanları okur; player en son kurulmalı.
    private var hardwareDecoding = true
    private var tunneling = Build.VERSION.SDK_INT >= 29 && isTelevision()
    private var bufferMs = DEFAULT_BUFFER_MS

    private var player: ExoPlayer = buildPlayer()

    /** Yalnızca testlerin hazırlama sayısını görmesi için. */
    internal var prepareCount = 0
        private set

    internal val hardwareDecodingEnabled: Boolean
        get() = hardwareDecoding

    private val listener = object : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) = updateState()

        override fun onIsPlayingChanged(isPlaying: Boolean) = updateState()

        override fun onTracksChanged(tracks: Tracks) = refreshTracks(tracks)

        override fun onTimelineChanged(timeline: Timeline, reason: Int) = updateCanSeekBack()

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) = updateCanSeekBack()

        override fun onPlayerError(error: PlaybackException) = handleError(error)
    }

    init {
        player.addListener(listener)
        player.addAnalyticsListener(EventLogger())
    }

    fun prepare(url: String, headers: Map<String, String> = emptyMap(), startPositionMs: Long = 0) {
        currentUrl = url
        currentHeaders = headers
        prepareCount += 1
        _state.value = PlaybackState.Buffering
        player.setMediaSource(createMediaSource(url, headers), startPositionMs.coerceAtLeast(0))
        player.prepare()
        player.playWhenReady = true
    }

    fun play() {
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0))
        updateCanSeekBack()
    }

    fun seekBack(ms: Long) {
        if (!_canSeekBack.value) return
        val target = (player.currentPosition - ms).coerceAtLeast(0)
        player.seekTo(target)
        updateCanSeekBack()
    }

    fun selectAudio(id: String) = selectTrack(C.TRACK_TYPE_AUDIO, id)

    fun selectSubtitle(id: String?) {
        val builder = player.trackSelectionParameters.buildUpon()
        builder.clearOverridesOfType(C.TRACK_TYPE_TEXT)
        if (id == null) {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        } else {
            val resolved = resolveTrack(C.TRACK_TYPE_TEXT, id) ?: return
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            builder.setOverrideForType(
                TrackSelectionOverride(resolved.group.mediaTrackGroup, listOf(resolved.trackIndex)),
            )
        }
        player.trackSelectionParameters = builder.build()
    }

    fun setAspect(aspect: VideoAspect) {
        _aspect.value = aspect
    }

    fun setBufferMs(ms: Int) {
        val clamped = ms.coerceAtLeast(MIN_BUFFER_MS)
        if (clamped == bufferMs) return
        bufferMs = clamped
        rebuildFromCurrentPosition()
    }

    fun setHardwareDecoding(enabled: Boolean) {
        if (enabled == hardwareDecoding) return
        hardwareDecoding = enabled
        rebuildFromCurrentPosition()
    }

    fun setTunneling(enabled: Boolean) {
        if (enabled == tunneling) return
        tunneling = enabled
        rebuildFromCurrentPosition()
    }

    fun clearNotice() {
        _notice.value = null
    }

    fun release() {
        player.removeListener(listener)
        player.release()
        _state.value = PlaybackState.Idle
    }

    internal fun handleErrorForTest(error: PlaybackException) = handleError(error)

    private fun handleError(error: PlaybackException) {
        if (hardwareDecoding && PlaybackErrorPolicy.isDecoderFailure(error)) {
            hardwareDecoding = false
            _notice.value = "Donanım çözme başarısız; yazılım çözmeye geçildi."
            rebuildFromCurrentPosition()
            return
        }
        _state.value = PlaybackErrorPolicy.map(error)
    }

    private fun rebuildFromCurrentPosition() {
        val url = currentUrl
        if (url == null) {
            // Henüz kaynak yokken ayar değişti: yalnızca bir sonraki prepare için sakla.
            player.removeListener(listener)
            player.release()
            player = buildPlayer()
            return
        }
        val position = player.currentPosition
        val playWhenReady = player.playWhenReady
        player.removeListener(listener)
        player.release()
        player = buildPlayer()
        prepareCount += 1
        _state.value = PlaybackState.Buffering
        player.setMediaSource(createMediaSource(url, currentHeaders), position)
        player.prepare()
        player.playWhenReady = playWhenReady
    }

    private fun updateState() {
        val next = when {
            player.playerError != null -> _state.value
            player.playbackState == Player.STATE_BUFFERING -> PlaybackState.Buffering
            player.playbackState == Player.STATE_READY -> PlaybackState.Playing
            else -> PlaybackState.Idle
        }
        _state.value = next
        updateCanSeekBack()
    }

    private fun updateCanSeekBack() {
        if (!player.isCurrentMediaItemSeekable || player.currentTimeline.isEmpty) {
            _canSeekBack.value = false
            return
        }
        val window = Timeline.Window()
        player.currentTimeline.getWindow(player.currentMediaItemIndex, window)
        // Canlı yayında geri sarma, pencere en az 60 s ise anlamlı; VOD'da her zaman.
        _canSeekBack.value = !window.isDynamic || player.currentPosition >= SEEK_BACK_WINDOW_MS
    }

    private fun refreshTracks(tracks: Tracks) {
        lastTracks = tracks
        _audioTracks.value = choices(tracks, C.TRACK_TYPE_AUDIO, "Ses")
        _subtitleTracks.value = choices(tracks, C.TRACK_TYPE_TEXT, "Altyazı")
    }

    private fun choices(tracks: Tracks, type: Int, fallback: String): List<TrackChoice> = buildList {
        tracks.groups.forEachIndexed { groupIndex, group ->
            if (group.type != type) return@forEachIndexed
            for (trackIndex in 0 until group.length) {
                val format = group.getTrackFormat(trackIndex)
                val language = format.language
                add(
                    TrackChoice(
                        id = trackId(groupIndex, trackIndex),
                        label = format.label
                            ?: language?.uppercase(Locale.ROOT)
                            ?: "$fallback ${trackIndex + 1}",
                        language = language,
                    ),
                )
            }
        }
    }

    private fun selectTrack(type: Int, id: String) {
        val resolved = resolveTrack(type, id) ?: return
        val builder = player.trackSelectionParameters.buildUpon()
        builder.clearOverridesOfType(type)
        builder.setTrackTypeDisabled(type, false)
        builder.setOverrideForType(
            TrackSelectionOverride(resolved.group.mediaTrackGroup, listOf(resolved.trackIndex)),
        )
        player.trackSelectionParameters = builder.build()
    }

    private fun resolveTrack(type: Int, id: String): ResolvedTrack? {
        val tracks = lastTracks ?: return null
        val (groupIndex, trackIndex) = id.split(':').mapNotNull(String::toIntOrNull).takeIf { it.size == 2 }
            ?: return null
        val group = tracks.groups.getOrNull(groupIndex) ?: return null
        if (group.type != type || trackIndex !in 0 until group.length) return null
        return ResolvedTrack(group, trackIndex)
    }

    private fun createMediaSource(url: String, headers: Map<String, String>): MediaSource =
        mediaSourceFor(
            url = url,
            hls = {
                HlsMediaSource.Factory(dataSourceFactory(headers))
                    .setLoadErrorHandlingPolicy(loadErrorPolicy())
                    .createMediaSource(MediaItem.fromUri(it))
            },
            dash = {
                DashMediaSource.Factory(dataSourceFactory(headers))
                    .setLoadErrorHandlingPolicy(loadErrorPolicy())
                    .createMediaSource(MediaItem.fromUri(it))
            },
            progressive = {
                ProgressiveMediaSource.Factory(dataSourceFactory(headers))
                    .setLoadErrorHandlingPolicy(loadErrorPolicy())
                    .createMediaSource(MediaItem.fromUri(it))
            },
        )

    private fun loadErrorPolicy() = DefaultLoadErrorHandlingPolicy(RETRY_COUNT)

    private fun dataSourceFactory(headers: Map<String, String>): DataSource.Factory {
        val factory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(CONNECT_TIMEOUT_MS)
            .setReadTimeoutMs(READ_TIMEOUT_MS)
        if (headers.isNotEmpty()) factory.setDefaultRequestProperties(headers)
        return factory
    }

    private fun buildPlayer(): ExoPlayer {
        // Ses passthrough'u Media3'te varsayılan: cihaz AC3/EAC3/DTS destekliyorsa
        // DefaultAudioSink doğrudan iletir; ayrı bir anahtar 1.5.1'de yok.
        val renderers = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .setMediaCodecSelector(codecSelector())
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(MIN_BUFFER_MS, bufferMs, MIN_BUFFER_MS, MIN_BUFFER_MS)
            .setBackBuffer(SEEK_BACK_WINDOW_MS.toInt(), true)
            .build()
        return ExoPlayer.Builder(context, renderers)
            .setTrackSelector(DefaultTrackSelector(context))
            .setLoadControl(loadControl)
            .build()
    }

    private fun isTelevision(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION)

    private data class ResolvedTrack(val group: Tracks.Group, val trackIndex: Int)

    companion object {
        const val DEFAULT_SEEK_BACK_WINDOW_MS = 60_000L
        const val SEEK_BACK_WINDOW_MS = DEFAULT_SEEK_BACK_WINDOW_MS
        const val DEFAULT_BUFFER_MS = 30_000
        const val MIN_BUFFER_MS = 1_000
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 15_000
        private const val RETRY_COUNT = 3

        private fun trackId(groupIndex: Int, trackIndex: Int) = "$groupIndex:$trackIndex"
    }

    /**
     * Donanım çözme kapalıysa yalnızca yazılım kodeklerini, tunneling açıksa
     * tunneling destekli kodekleri tercih eder; hiçbiri yoksa tüm listeye döner.
     */
    private fun codecSelector(): MediaCodecSelector = object : MediaCodecSelector {
        override fun getDecoderInfos(
            mimeType: String,
            requiresSecureDecoder: Boolean,
            requiresTunnelingDecoder: Boolean,
        ): List<MediaCodecInfo> {
            val all = MediaCodecUtil.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)
            var filtered = all
            if (!hardwareDecoding) filtered = filtered.filter { it.softwareOnly }
            if (tunneling) filtered = filtered.filter { it.tunneling }
            return filtered.ifEmpty { all }
        }
    }
}
