package dev.anon.novatv.app.common

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anon.novatv.core.player.PlaybackController
import dev.anon.novatv.feature.player.PlayerScreen
import dev.anon.novatv.feature.player.PlayerViewModel
import dev.anon.novatv.feature.player.TouchPlayerScreen
import dev.anon.novatv.feature.player.findActivity
import kotlinx.coroutines.delay

/**
 * İki uygulamanın paylaştığı rota sabitleri.
 *
 * TV uygulaması `:app` içindeki `NovaNavHost`'a kendi TV rotalarını ekler;
 * telefon uygulaması aynı sabitleri kendi kabuğunda kullanır.
 */
object Routes {
    const val LIVE = "live"
}

/** Seçilen dosyanın uzantısına göre Media3 altyazı MIME türü. */
internal fun subtitleMimeType(context: Context, uri: Uri): String {
    val name = (uri.lastPathSegment ?: uri.path.orEmpty()).lowercase()
    return when {
        name.endsWith(".srt") -> "application/x-subrip"
        name.endsWith(".vtt") -> "text/vtt"
        name.endsWith(".ssa") -> "text/x-ssa"
        name.endsWith(".ass") -> "text/x-ass"
        else -> context.contentResolver.getType(uri) ?: "application/x-subrip"
    }
}

/**
 * Android TV görüntülerinde belge seçici yerine yalnızca "uygulama yok" diyen
 * bir saplama (stub) olabilir; onu gerçek seçici sayma.
 */
fun hasDocumentPicker(context: Context): Boolean {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        .addCategory(Intent.CATEGORY_OPENABLE)
        .setType("*/*")
    val resolved = context.packageManager.resolveActivity(intent, 0) ?: return false
    val packageName = resolved.activityInfo?.packageName ?: return false
    return !packageName.startsWith("com.android.tv.frameworkpackagestubs") &&
        !packageName.startsWith("com.google.android.tv.frameworkpackagestubs")
}

/**
 * Oynatıcı ekranının ortak gövdesi: altyazı seçici, ekran modu, uyku zamanlayıcısı.
 *
 * [touch] `false` iken (TV uygulaması) D-pad OSD'li [PlayerScreen] çizilir ve
 * çağrı listesi bugünküyle aynı kalır. `true` iken (telefon) dokunmatik
 * [TouchPlayerScreen] çizilir ve rota açıkken etkinlik yatay kilitlenir;
 * rotadan çıkışta yön `UNSPECIFIED`a döner. Yön kilidi burada yaşar çünkü iki
 * uygulamanın tek ortak yeri burasıdır.
 */
@Composable
fun PlayerRouteContent(
    controller: PlaybackController,
    viewModel: PlayerViewModel,
    sleepMinutes: Int = 0,
    autoFrameRate: Boolean = false,
    onBack: () -> Unit,
    touch: Boolean = false,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val frameRate by controller.videoFrameRate.collectAsStateWithLifecycle()

    LaunchedEffect(autoFrameRate, frameRate) {
        if (autoFrameRate) dev.anon.novatv.feature.player.AutoFrameRate.apply(activity, frameRate)
    }
    // Oynatıcıdan çıkınca ekran modu varsayılana döner; yoksa menü 24 Hz'de kalır.
    DisposableEffect(autoFrameRate) {
        onDispose {
            if (autoFrameRate) dev.anon.novatv.feature.player.AutoFrameRate.reset(activity)
        }
    }
    LaunchedEffect(sleepMinutes) {
        if (sleepMinutes > 0) {
            delay(sleepMinutes * 60_000L)
            runCatching { controller.pause() }
        }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val subtitlePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) viewModel.setExternalSubtitle(uri, subtitleMimeType(context, uri))
    }
    if (touch) {
        // Oynatıcı yataydır; çıkışta cihaz kendi yönüne döner. Etkinlik yoksa
        // (önizleme, test) kilit sessizce atlanır.
        DisposableEffect(activity) {
            val locked = activity
            if (locked != null) {
                runCatching {
                    locked.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }
            }
            onDispose {
                if (locked != null) {
                    runCatching {
                        locked.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
                }
            }
        }
        TouchPlayerScreen(
            state = state,
            controller = controller,
            channelOrder = viewModel.channelOrder,
            onSelectChannel = viewModel::selectChannel,
            onToggleOsd = viewModel::toggleOsd,
            onHideOsd = viewModel::hideOsd,
            onRetry = viewModel::retry,
            onBackToChannels = onBack,
            onSeekBack = { viewModel.seekBack() },
            onSeekForward = { viewModel.seekForward() },
            onCycleAspect = viewModel::cycleAspect,
            onCycleBuffer = viewModel::cycleBuffer,
            onSelectAudio = viewModel::selectAudio,
            onSelectSubtitle = viewModel::selectSubtitle,
            onChannelStep = viewModel::stepChannel,
            onMenuOpenChange = viewModel::keepOsdVisible,
        )
        return
    }
    PlayerScreen(
        state = state,
        controller = controller,
        onToggleOsd = viewModel::toggleOsd,
        onHideOsd = viewModel::hideOsd,
        onRetry = viewModel::retry,
        onBackToChannels = onBack,
        onSeekBack = { viewModel.seekBack() },
        onSeekBackMinute = viewModel::seekBackMinute,
        onCycleAspect = viewModel::cycleAspect,
        onCycleBuffer = viewModel::cycleBuffer,
        onSelectAudio = viewModel::selectAudio,
        onSelectSubtitle = viewModel::selectSubtitle,
        onPickSubtitleFile = {
            // Belge seçici olmayan cihazlarda (bazı TV kutuları) İngilizce
            // sistem toast'u yerine Türkçe bilgi göster.
            if (hasDocumentPicker(context)) {
                subtitlePicker.launch(arrayOf("*/*"))
            } else {
                viewModel.reportMissingSubtitlePicker()
            }
        },
        onChannelStep = viewModel::stepChannel,
        onMenuOpenChange = viewModel::keepOsdVisible,
    )
}
