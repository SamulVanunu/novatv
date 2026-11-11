package dev.anon.novatv.feature.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.core.player.PlaybackController
import dev.anon.novatv.core.player.PlaybackState
import dev.anon.novatv.ui.component.NovaBadge
import dev.anon.novatv.ui.component.NovaLabel
import dev.anon.novatv.ui.component.NovaRule
import dev.anon.novatv.ui.component.NovaTapSurface
import dev.anon.novatv.ui.theme.NovaColors
import dev.anon.novatv.ui.theme.NovaType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

/** OSD görünürken ilerleme çubuğunun yenilenme aralığı. */
private const val PROGRESS_POLL_MS = 500L

/** Dokunmatik OSD düğmelerinin en küçük dokunma yüksekliği. */
private val TouchTargetHeight = 48.dp

/**
 * Tam ekran dokunmatik oynatıcı.
 *
 * Görüntü tam ekran çizilir ([buildPlayerView], `useController = false`); tüm
 * denetim alt panelde dokunma ile. D-pad yolu TV ekranında ([PlayerScreen]) kalır;
 * burada tuş işleyicisi ve bu ekrana ait `BackHandler` yok — sistem Geri tuşu
 * rotayı kapatır (oynatıcıdan çıkar), OSD'yi yalnız perdeye dokunma kapatır.
 *
 * Katman sırası: video → OSD'nin altındaki tam ekran dokunma perdesi → hata /
 * bildirim / kanal bandı → OSD ve kanal listesi. Perde OSD'nin altında durduğu
 * için OSD düğmelerine dokunma perdeye düşmez; perde video görüntüsüne dokunmayı
 * yakalar ve OSD'yi açar/kapar.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TouchPlayerScreen(
    state: PlayerUiState,
    controller: PlaybackController,
    channelOrder: StateFlow<List<Channel>>,
    onSelectChannel: (Channel) -> Unit,
    onToggleOsd: () -> Unit,
    onHideOsd: () -> Unit,
    onRetry: () -> Unit,
    onBackToChannels: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onCycleAspect: () -> Unit,
    onCycleBuffer: () -> Unit,
    onSelectAudio: (String) -> Unit,
    onSelectSubtitle: (String?) -> Unit,
    onChannelStep: (Int) -> Unit = {},
    onMenuOpenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val channelFocus = remember { FocusRequester() }
    val retryFocus = remember { FocusRequester() }
    var pickerOpen by remember { mutableStateOf(false) }
    // Parça penceresi: hangisi açık; hiçbiri açık değilken NONE.
    var trackMenu by remember { mutableStateOf(TrackMenu.NONE) }

    // Oynatma durumu (canlı rozet, oynat/duraklat etiketi) aynı kaynaktan.
    val playing = state.playback is PlaybackState.Playing ||
        state.playback is PlaybackState.Buffering

    // PiP: TV ekranıyla aynı kural. Yatay kilit rota düzeyinde yaşar; burada
    // yalnız sistemin küçük pencereye geçiş izni yönetilir.
    LaunchedEffect(playing, state.playback is PlaybackState.Failed) {
        PlayerPip.active = state.playback !is PlaybackState.Failed
        PlayerPip.playing = playing
        activity?.updatePictureInPictureParams(playing)
    }
    DisposableEffect(activity) {
        onDispose {
            PlayerPip.active = false
            PlayerPip.playing = false
            activity?.updatePictureInPictureParams(false)
        }
    }

    // Menü açıkken OSD'nin zamanlayıcısı durur; TV ekranıyla aynı sözleşme.
    // Parça penceresi açıkken OSD görünür kalır, kanal listesinde gizlenir.
    val menuOpen = pickerOpen || trackMenu != TrackMenu.NONE
    LaunchedEffect(menuOpen) { onMenuOpenChange(menuOpen) }
    DisposableEffect(Unit) { onDispose { onMenuOpenChange(false) } }

    LaunchedEffect(pickerOpen) {
        if (pickerOpen) {
            trackMenu = TrackMenu.NONE
            onHideOsd()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("touch-player-root"),
    ) {
        AndroidView(
            factory = { context -> buildPlayerView(context, controller) },
            update = { view ->
                view.player = controller.player
                view.resizeMode = state.aspect.resizeMode()
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Tam ekran dokunma perdesi: video üstüne dokunma OSD'yi açar/kapar.
        // OSD'den önce çizilir; OSD düğmeleri üstte kalır, dokunma onlara düşmez.
        if (!pickerOpen) {
            NovaTapSurface(
                onClick = onToggleOsd,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("touch-video-tap"),
            ) { }
        }

        (state.playback as? PlaybackState.Failed)?.let { failed ->
            PlaybackError(
                reason = failed.reason,
                retryable = failed.retryable,
                retryFocusRequester = retryFocus,
                onRetry = onRetry,
                onBackToChannels = onBackToChannels,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Üst bölge: kanal bandı ve bildirim, TV'deki okuma düzenini izler.
        // Video kenara ulaşır ama yazı sistem çubuğunun altına girmez.
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                    ),
                )
                .padding(start = 16.dp, end = 16.dp, top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.zap?.let { channel ->
                ChannelBanner(channel = channel)
            }
            state.notice?.let { message ->
                NoticeBanner(message = message)
            }
        }

        if (state.osdVisible && !pickerOpen) {
            TouchOsd(
                state = state,
                playing = playing,
                controller = controller,
                firstFocusRequester = channelFocus,
                onBackToChannels = onBackToChannels,
                onTogglePlayPause = {
                    runCatching { if (playing) controller.pause() else controller.play() }
                },
                onChannelStep = onChannelStep,
                onSeekBack = onSeekBack,
                onSeekForward = onSeekForward,
                onCycleAspect = onCycleAspect,
                onCycleBuffer = onCycleBuffer,
                onOpenAudio = { trackMenu = TrackMenu.AUDIO },
                onOpenSubtitle = { trackMenu = TrackMenu.SUBTITLE },
                onOpenPicker = { pickerOpen = true },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        when (trackMenu) {
            TrackMenu.AUDIO -> TrackMenuDialog(
                title = "Ses",
                choices = state.audioTracks,
                onSelect = { id ->
                    id?.let(onSelectAudio)
                    trackMenu = TrackMenu.NONE
                },
                onDismiss = { trackMenu = TrackMenu.NONE },
            )

            TrackMenu.SUBTITLE -> TrackMenuDialog(
                title = "Altyazı",
                choices = state.subtitleTracks,
                allowOff = true,
                onSelect = {
                    onSelectSubtitle(it)
                    trackMenu = TrackMenu.NONE
                },
                onDismiss = { trackMenu = TrackMenu.NONE },
            )

            TrackMenu.NONE -> Unit
        }

        if (pickerOpen) {
            ChannelPickerSheet(
                order = channelOrder.collectAsStateWithLifecycle().value,
                current = state.channel,
                onSelect = { channel ->
                    pickerOpen = false
                    onSelectChannel(channel)
                },
                onDismiss = { pickerOpen = false },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Açık parça penceresi; hiçbiri açık değilken `NONE`. */
private enum class TrackMenu { NONE, AUDIO, SUBTITLE }

/**
 * Alt panel OSD: birinci satır çıkış / oynat-duraklat / kanal geçişi, ikinci
 * satır ses, altyazı, oran, tampon ve kanal listesi. Film/dizide ilerleme
 * çubuğu ile ±10 sn düğmeleri eklenir.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TouchOsd(
    state: PlayerUiState,
    playing: Boolean,
    controller: PlaybackController,
    firstFocusRequester: FocusRequester,
    onBackToChannels: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onChannelStep: (Int) -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onCycleAspect: () -> Unit,
    onCycleBuffer: () -> Unit,
    onOpenAudio: () -> Unit,
    onOpenSubtitle: () -> Unit,
    onOpenPicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Degrade tam genişlikte kalır (video kenara ulaşır); düğmeler sistem
    // çubuklarının altına girmez. Yatayda da pay verilir: telefonda navigasyon
    // çubuğu yatayda sağ kenara biner ve "Liste" düğmesini yiyordu.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to Color(0x00070A0E),
                    0.35f to Color(0xB3070A0E),
                    1f to Color(0xF7070A0E),
                ),
            )
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal,
                ),
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("osd"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NovaLabel(state.title ?: state.channel?.name ?: "Kanal")
            // "canlı" damgası yalnız canlı yayında anlamlı; film/dizide yanlış.
            if (!state.vod && state.playback is PlaybackState.Playing) {
                NovaBadge(text = "canlı", color = NovaColors.Live)
            } else if (state.playback is PlaybackState.Buffering) {
                NovaBadge(text = "tampon", color = NovaColors.Warning)
            }
        }

        if (state.vod) {
            VodProgress(
                controller = controller,
                onSeekBack = onSeekBack,
                onSeekForward = onSeekForward,
            )
        }

        // Birinci satır: ‹ Kanal · oynat/duraklat · Kanal ›
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TouchButton(
                label = if (state.vod) "‹ Geri" else "‹ Kanal",
                onClick = onBackToChannels,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(firstFocusRequester),
                testTag = "osd-channels",
                accent = true,
            )
            TouchButton(
                label = if (playing) "Duraklat" else "Oynat",
                onClick = onTogglePlayPause,
                modifier = Modifier.weight(1.6f),
                testTag = "osd-play-pause",
            )
            if (!state.vod) {
                TouchButton(
                    label = "Kanal ›",
                    onClick = { onChannelStep(1) },
                    modifier = Modifier.weight(1f),
                    testTag = "osd-channel-next",
                )
            }
        }

        // İkinci satır: Ses · Altyazı · Oran · Tampon · Liste
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // TV'deki parça penceresi (TrackMenuDialog) aynen açılır; etiket
            // geçerli parçayı gösterir.
            TouchButton(
                label = audioLabel(state.audioTracks),
                onClick = onOpenAudio,
                modifier = Modifier.weight(1f),
                testTag = "osd-audio",
            )
            TouchButton(
                label = subtitleLabel(state.subtitleTracks),
                onClick = onOpenSubtitle,
                modifier = Modifier.weight(1f),
                testTag = "osd-subtitle",
            )
            TouchButton(
                label = aspectLabel(state),
                onClick = onCycleAspect,
                modifier = Modifier.weight(1f),
                testTag = "osd-aspect",
            )
            TouchButton(
                label = "Tampon ${state.bufferSeconds} sn",
                onClick = onCycleBuffer,
                modifier = Modifier.weight(1f),
                testTag = "osd-buffer",
            )
            if (!state.vod) {
                TouchButton(
                    label = "Liste",
                    onClick = onOpenPicker,
                    modifier = Modifier.weight(1f),
                    testTag = "osd-list",
                    accent = true,
                )
            }
        }
    }
}

/**
 * Film/dizi ilerlemesi: çubuk ve süre yazısı. OSD görünürken 500 ms'de bir
 * denetleyiciden okunur; denetleyici yoksa değerler sıfır kalır.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun VodProgress(
    controller: PlaybackController,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
) {
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    LaunchedEffect(controller) {
        while (true) {
            runCatching {
                position = controller.positionMs
                duration = controller.durationMs
            }
            delay(PROGRESS_POLL_MS)
        }
    }
    val fraction = if (duration > 0) {
        (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(NovaColors.Border, RoundedCornerShape(50)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(3.dp)
                    .background(NovaColors.Accent, RoundedCornerShape(50)),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = formatClock(position),
                style = NovaType.Number.copy(color = NovaColors.TextMuted),
                modifier = Modifier.testTag("osd-position"),
            )
            Box(modifier = Modifier.weight(1f))
            // Sarma: hedef konum doğrudan verilir; alt sınır denetleyicide.
            TouchButton(
                label = "−10 sn",
                onClick = onSeekBack,
                testTag = "osd-seek-back",
            )
            TouchButton(
                label = "+10 sn",
                onClick = onSeekForward,
                testTag = "osd-seek-forward",
            )
            Text(
                text = formatClock(duration),
                style = NovaType.Number.copy(color = NovaColors.TextDim),
                modifier = Modifier.testTag("osd-duration"),
            )
        }
    }
}

/** Dokunmatik düğme: en az 48 dp yükseklik, çerçeve, odakta krem dolgu. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TouchButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    accent: Boolean = false,
) {
    val shape = RoundedCornerShape(10.dp)
    val idleContainer = if (accent) NovaColors.Accent.copy(alpha = 0.10f) else NovaColors.Surface.copy(alpha = 0.72f)
    val idleContent = if (accent) NovaColors.Accent else NovaColors.Text
    val idleBorder = if (accent) NovaColors.Accent.copy(alpha = 0.45f) else NovaColors.Border
    NovaTapSurface(
        onClick = onClick,
        shape = shape,
        containerColor = idleContainer,
        contentColor = idleContent,
        focusedContainerColor = NovaColors.Focus,
        focusedContentColor = NovaColors.FocusInk,
        pressedContainerColor = NovaColors.Focus,
        pressedContentColor = NovaColors.FocusInk,
        border = BorderStroke(1.dp, idleBorder),
        focusedBorder = BorderStroke(2.dp, NovaColors.Focus),
        pressedBorder = BorderStroke(2.dp, NovaColors.Focus),
        pressedScale = 0.98f,
        modifier = modifier
            .heightIn(min = TouchTargetHeight)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
    ) { state ->
        Text(
            text = label,
            style = NovaType.Button.copy(color = state.contentColor, fontSize = 15.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

/** Saat biçimi: `m:ss` ya da `h:mm:ss`. */
internal fun formatClock(millis: Long): String {
    val total = (millis.coerceAtLeast(0) / 1_000)
    val seconds = total % 60
    val minutes = (total / 60) % 60
    val hours = total / 3_600
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
