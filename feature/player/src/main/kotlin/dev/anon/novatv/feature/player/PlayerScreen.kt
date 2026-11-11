package dev.anon.novatv.feature.player

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import androidx.tv.material3.ExperimentalTvMaterial3Api
import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.core.player.PlaybackController
import dev.anon.novatv.ui.component.NovaLabel
import dev.anon.novatv.ui.component.NovaPill
import dev.anon.novatv.ui.theme.NovaColors
import dev.anon.novatv.ui.theme.NovaType
import dev.anon.novatv.core.player.PlaybackState
import dev.anon.novatv.core.player.VideoAspect

/**
 * Tam ekran oynatıcı. Media3 `PlayerView` yalnızca görüntüyü çizer
 * (`useController = false`); tüm denetim kendi OSD'mizde ve D-pad ile.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerScreen(
    state: PlayerUiState,
    controller: PlaybackController,
    onToggleOsd: () -> Unit,
    onHideOsd: () -> Unit,
    onRetry: () -> Unit,
    onBackToChannels: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekBackMinute: () -> Unit = onSeekBack,
    onCycleAspect: () -> Unit,
    onCycleBuffer: () -> Unit,
    onSelectAudio: (String) -> Unit,
    onSelectSubtitle: (String?) -> Unit,
    onPickSubtitleFile: () -> Unit = {},
    onChannelStep: (Int) -> Unit = {},
    onMenuOpenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rootFocus = remember { FocusRequester() }
    val osdFocus = remember { FocusRequester() }
    val retryFocus = remember { FocusRequester() }
    val activity = LocalContext.current.findActivity()
    val inPip by PlayerPip.inPip.collectAsState()

    // PiP: API 31+ otomatik giriş, alt sürümlerde onUserLeaveHint.
    val playing = state.playback is PlaybackState.Playing || state.playback is PlaybackState.Buffering
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
    LaunchedEffect(inPip) {
        if (inPip) onHideOsd()
    }

    LaunchedEffect(state.osdVisible) {
        // Hedef düğme kompoze olduktan sonra odak iste.
        withFrameNanos { }
        runCatching {
            if (state.osdVisible) osdFocus.requestFocus() else rootFocus.requestFocus()
        }
    }

    LaunchedEffect(state.playback is PlaybackState.Failed) {
        if (state.playback is PlaybackState.Failed) {
            withFrameNanos { }
            runCatching { retryFocus.requestFocus() }
        } else if (!state.osdVisible) {
            // Hata ekranı kapandığında odaklı düğme de kaybolur; odağı
            // oynatıcı köküne geri ver, yoksa D-pad tamamen sağır kalır.
            withFrameNanos { }
            runCatching { rootFocus.requestFocus() }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocus)
            .focusable()
            .onPreviewKeyEvent { event ->
                // Kanallar arasında gezinme. Bazı kutuların kumandasında
                // CH+/CH- yok; D-pad yukarı/ağı onların yerini tutar. OSD
                // açıkken D-pad OSD'de dolaşır, tuşu yutmayız.
                val zapEnabled = !state.vod && state.playback !is PlaybackState.Failed
                val zapKey = when {
                    event.key == Key.ChannelUp -> 1
                    event.key == Key.ChannelDown -> -1
                    !state.osdVisible && event.key == Key.DirectionUp -> 1
                    !state.osdVisible && event.key == Key.DirectionDown -> -1
                    else -> 0
                }
                when {
                    // Geri tuşu KeyDown'da işlenir: parça menüsü gibi bir diyalog
                    // kapandığında artan KeyUp olayı oynatıcıdan çıkarmasın.
                    event.key == Key.Back && event.type == KeyEventType.KeyDown -> {
                        if (state.osdVisible) onHideOsd() else onBackToChannels()
                        true
                    }

                    zapKey != 0 && event.type == KeyEventType.KeyDown && zapEnabled -> {
                        onChannelStep(zapKey)
                        true
                    }

                    // Hata ekranı açıkken OK tuşu "Yeniden dene" düğmesine
                    // gitmeli; OSD açma bu olayı yutmamalı.
                    (event.key == Key.Info || event.key == Key.DirectionCenter) &&
                        event.type == KeyEventType.KeyUp && !state.osdVisible &&
                        state.playback !is PlaybackState.Failed -> {
                        onToggleOsd()
                        true
                    }

                    else -> false
                }
            }
            .testTag("player-root"),
    ) {
        AndroidView(
            factory = { context -> buildPlayerView(context, controller) },
            update = { view ->
                view.player = controller.player
                view.resizeMode = state.aspect.resizeMode()
            },
            modifier = Modifier.fillMaxSize(),
        )

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

        state.notice?.let { message ->
            NoticeBanner(
                message = message,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp),
            )
        }

        state.zap?.let { channel ->
            ChannelBanner(
                channel = channel,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 28.dp, top = 26.dp),
            )
        }

        if (state.osdVisible && !inPip) {
            PlayerOsd(
                state = state,
                onBackToChannels = onBackToChannels,
                onSeekBack = onSeekBack,
                onSeekBackMinute = onSeekBackMinute,
                onCycleAspect = onCycleAspect,
                onCycleBuffer = onCycleBuffer,
                onSelectAudio = onSelectAudio,
                onSelectSubtitle = onSelectSubtitle,
                onPickSubtitleFile = onPickSubtitleFile,
                onMenuOpenChange = onMenuOpenChange,
                firstFocusRequester = osdFocus,
            )
        }
    }
}

/**
 * Kanal değişimi bandı: numara, ad ve canlı işareti. OSD'den bağımsız,
 * birkaç saniye sonra kendiliğinden kaybolur.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun ChannelBanner(channel: Channel, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(NovaColors.Background.copy(alpha = 0.92f), RoundedCornerShape(10.dp))
            .border(1.dp, NovaColors.Border, RoundedCornerShape(10.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .testTag("player-zap"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(38.dp)
                .background(NovaColors.Accent, RoundedCornerShape(50)),
        )
        channel.number?.let { number ->
            Text(
                text = number.toString(),
                style = NovaType.Number.copy(fontSize = 20.sp, color = NovaColors.Text),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = channel.name,
                style = NovaType.Title.copy(fontSize = 20.sp, lineHeight = 26.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "CANLI",
                style = NovaType.Label.copy(color = NovaColors.Accent),
                maxLines = 1,
            )
        }
    }
}

/** Donanım→yazılım geçişi gibi sessiz kalmaması gereken oynatıcı bildirimleri. */@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun NoticeBanner(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(NovaColors.Surface.copy(alpha = 0.94f), RoundedCornerShape(999.dp))
            .border(1.dp, NovaColors.Accent.copy(alpha = 0.7f), RoundedCornerShape(999.dp))
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .testTag("player-notice"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("i", style = NovaType.Title.copy(color = NovaColors.Accent))
        Text(message, style = NovaType.Small.copy(color = NovaColors.TextMuted))
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun PlaybackError(
    reason: String,
    retryable: Boolean,
    retryFocusRequester: FocusRequester,
    onRetry: () -> Unit,
    onBackToChannels: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    0f to Color(0xFA070A0E),
                    0.6f to Color(0xF5070A0E),
                    1f to Color(0xFF05080B),
                ),
            )
            .testTag("playback-error"),
        contentAlignment = Alignment.Center,
    ) {
        Column(modifier = Modifier.width(680.dp)) {
            NovaLabel(if (retryable) "Bağlantı sorunu" else "Yayın hatası")
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .width(38.dp)
                    .height(3.dp)
                    .background(if (retryable) NovaColors.Accent else NovaColors.Danger),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Yayın açılamadı",
                style = NovaType.Display.copy(fontSize = 36.sp, lineHeight = 42.sp),
                color = Color.White,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (retryable) {
                    "Sunucuya ulaşılamadı. Ağ bağlantısını ve kaynak adresini kontrol edin, sonra yeniden deneyin."
                } else {
                    "Bu yayın oynatılamıyor. Kaynağı yenileyip yeniden deneyin."
                },
                style = NovaType.BodyMuted,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = reason,
                style = NovaType.Small.copy(color = NovaColors.TextDim, fontSize = 12.sp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("error-detail"),
            )
            Spacer(Modifier.height(26.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (retryable) {
                    NovaPill(
                        label = "Yeniden dene",
                        onClick = onRetry,
                        accent = true,
                        modifier = Modifier.focusRequester(retryFocusRequester),
                        testTag = "playback-retry",
                    )
                }
                NovaPill(
                    label = "Kanallar",
                    onClick = onBackToChannels,
                    testTag = "error-channels",
                )
            }
        }
    }
}

internal fun buildPlayerView(context: Context, controller: PlaybackController): PlayerView =
    PlayerView(context).apply {
        useController = false
        isFocusable = false
        isFocusableInTouchMode = false
        player = controller.player
    }

internal fun VideoAspect.resizeMode(): Int = when (this) {
    VideoAspect.FIT, VideoAspect.RATIO_16_9, VideoAspect.RATIO_4_3, VideoAspect.ORIGINAL ->
        AspectRatioFrameLayout.RESIZE_MODE_FIT

    VideoAspect.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    VideoAspect.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
}

