package dev.anon.novatv.feature.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.anon.novatv.core.player.PlaybackController
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
    onCycleAspect: () -> Unit,
    onCycleBuffer: () -> Unit,
    onSelectAudio: (String) -> Unit,
    onSelectSubtitle: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rootFocus = remember { FocusRequester() }
    val osdFocus = remember { FocusRequester() }
    val retryFocus = remember { FocusRequester() }

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
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
                when {
                    event.key == Key.Back -> {
                        if (state.osdVisible) onHideOsd() else onBackToChannels()
                        true
                    }

                    (event.key == Key.Info || event.key == Key.DirectionCenter) && !state.osdVisible -> {
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

        if (state.osdVisible) {
            PlayerOsd(
                state = state,
                onBackToChannels = onBackToChannels,
                onSeekBack = onSeekBack,
                onCycleAspect = onCycleAspect,
                onCycleBuffer = onCycleBuffer,
                onSelectAudio = onSelectAudio,
                onSelectSubtitle = onSelectSubtitle,
                firstFocusRequester = osdFocus,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlaybackError(
    reason: String,
    retryable: Boolean,
    retryFocusRequester: FocusRequester,
    onRetry: () -> Unit,
    onBackToChannels: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Color(0xE6101418))
            .padding(48.dp)
            .testTag("playback-error"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Yayın açılamadı",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = reason,
            fontSize = 16.sp,
            color = Color(0xFFB9C3CE),
            modifier = Modifier.width(640.dp).testTag("error-detail"),
        )
        Spacer(Modifier.height(28.dp))
        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (retryable) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .focusRequester(retryFocusRequester)
                        .testTag("playback-retry"),
                ) {
                    Text("Yeniden dene")
                }
            }
            Button(onClick = onBackToChannels, modifier = Modifier.testTag("error-channels")) {
                Text("Kanallar")
            }
        }
    }
}

private fun buildPlayerView(context: Context, controller: PlaybackController): PlayerView =
    PlayerView(context).apply {
        useController = false
        isFocusable = false
        isFocusableInTouchMode = false
        player = controller.player
    }

private fun VideoAspect.resizeMode(): Int = when (this) {
    VideoAspect.FIT, VideoAspect.RATIO_16_9, VideoAspect.RATIO_4_3, VideoAspect.ORIGINAL ->
        AspectRatioFrameLayout.RESIZE_MODE_FIT

    VideoAspect.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    VideoAspect.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
}

/** API 26+ ve PiP destekli cihazlarda oynatıcıyı küçük pencereye alır. */
fun Activity.enterPlayerPictureInPicture(aspect: Rational = Rational(16, 9)) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    ) {
        enterPip(aspect)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun Activity.enterPip(aspect: Rational) {
    enterPictureInPictureMode(
        PictureInPictureParams.Builder()
            .setAspectRatio(aspect)
            .build(),
    )
}
