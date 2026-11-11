package dev.anon.novatv.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import dev.anon.novatv.core.player.PlaybackState
import dev.anon.novatv.core.player.TrackChoice
import dev.anon.novatv.ui.component.NovaBadge
import dev.anon.novatv.ui.component.NovaLabel
import dev.anon.novatv.ui.component.NovaPanel
import dev.anon.novatv.ui.component.NovaPill
import dev.anon.novatv.ui.component.NovaRule
import dev.anon.novatv.ui.theme.NovaColors
import dev.anon.novatv.ui.theme.NovaType

/**
 * Yarı saydam OSD: kanal bilgisi, parça/oran/tampon seçimleri ve kanal listesine dönüş.
 * Üst ve alt kenar gradyanla karartılır; görüntünün ortası görünür kalır.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerOsd(
    state: PlayerUiState,
    onBackToChannels: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekBackMinute: () -> Unit = onSeekBack,
    onCycleAspect: () -> Unit,
    onCycleBuffer: () -> Unit,
    onSelectAudio: (String) -> Unit,
    onSelectSubtitle: (String?) -> Unit,
    onPickSubtitleFile: () -> Unit = {},
    onMenuOpenChange: (Boolean) -> Unit,
    firstFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    var audioMenu by remember { mutableStateOf(false) }
    var subtitleMenu by remember { mutableStateOf(false) }

    // Menü açıkken OSD zamanlayıcısı durur; aksi hâlde menü kapanınca
    // kullanıcı boş ekrana döner.
    val menuOpen = audioMenu || subtitleMenu
    LaunchedEffect(menuOpen) { onMenuOpenChange(menuOpen) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("osd"),
    ) {
        // Üst karartma: kanal adı okunsun, görüntü boğulmasın.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        0f to Color(0xFB070A0E),
                        0.38f to Color(0xCC070A0E),
                        0.72f to Color(0x77070A0E),
                        1f to Color.Transparent,
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 44.dp, end = 44.dp, top = 36.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(26.dp)
                        .height(2.dp)
                        .background(NovaColors.Accent),
                )
                NovaLabel("Şimdi izleniyor")
                if (state.playback is PlaybackState.Playing) {
                    NovaBadge(text = "canlı", color = NovaColors.Live)
                } else if (state.playback is PlaybackState.Buffering) {
                    NovaBadge(text = "tampon", color = NovaColors.Warning)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = state.title ?: state.channel?.name ?: "Kanal",
                style = NovaType.Display.copy(
                    fontSize = 38.sp,
                    lineHeight = 44.sp,
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.55f),
                        offset = androidx.compose.ui.geometry.Offset(0f, 2f),
                        blurRadius = 14f,
                    ),
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("osd-channel-name"),
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                state.channel?.groupName?.takeIf { it.isNotBlank() }?.let { group ->
                    Text(group, style = NovaType.Small.copy(color = NovaColors.TextMuted))
                }
                state.channel?.number?.let { number ->
                    Text(
                        text = "#$number",
                        style = NovaType.Number.copy(color = NovaColors.Accent.copy(alpha = 0.85f)),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                EpgSlot(label = "Şimdi", value = "Yayın bilgisi yok")
                EpgSlot(label = "Sıradaki", value = "—")
            }
        }

        // Alt karartma ve denetim çubuğu.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0x00070A0E),
                        0.30f to Color(0x8C070A0E),
                        0.62f to Color(0xD9070A0E),
                        1f to Color(0xF5070A0E),
                    ),
                ),
        ) {
            Spacer(Modifier.height(72.dp))
            NovaRule(Modifier.fillMaxWidth().padding(horizontal = 44.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NovaPill(
                    label = "‹ Kanallar",
                    onClick = onBackToChannels,
                    accent = true,
                    modifier = Modifier.focusRequester(firstFocusRequester),
                    testTag = "osd-channels",
                )

                if (state.canSeekBack) {
                    NovaPill(
                        label = "−10 sn",
                        onClick = onSeekBack,
                        testTag = "osd-seek-back",
                    )
                    NovaPill(
                        label = "−60 sn",
                        onClick = onSeekBackMinute,
                        testTag = "osd-seek-minute",
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .width(1.dp)
                        .height(28.dp)
                        .background(NovaColors.BorderStrong),
                )

                NovaPill(
                    label = audioLabel(state.audioTracks),
                    onClick = { audioMenu = true },
                    testTag = "osd-audio",
                )
                NovaPill(
                    label = subtitleLabel(state.subtitleTracks),
                    onClick = { subtitleMenu = true },
                    testTag = "osd-subtitle",
                )
                NovaPill(
                    label = aspectLabel(state),
                    onClick = onCycleAspect,
                    testTag = "osd-aspect",
                )
                NovaPill(
                    label = "Tampon ${state.bufferSeconds} sn",
                    onClick = onCycleBuffer,
                    testTag = "osd-buffer",
                )

            }
        }
    }

    if (audioMenu) {
        TrackMenuDialog(
            title = "Ses parçası",
            choices = state.audioTracks,
            onSelect = { id ->
                id?.let(onSelectAudio)
                audioMenu = false
            },
            onDismiss = { audioMenu = false },
        )
    }

    if (subtitleMenu) {
        TrackMenuDialog(
            title = "Altyazı",
            choices = state.subtitleTracks,
            allowOff = true,
            extraAction = "Dosyadan seç…" to {
                subtitleMenu = false
                onPickSubtitleFile()
            },
            onSelect = {
                onSelectSubtitle(it)
                subtitleMenu = false
            },
            onDismiss = { subtitleMenu = false },
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EpgSlot(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NovaLabel(label, Modifier.padding(top = 1.dp))
        Text(value, style = NovaType.Small.copy(color = NovaColors.TextMuted))
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TrackMenuDialog(
    title: String,
    choices: List<TrackChoice>,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
    allowOff: Boolean = false,
    extraAction: Pair<String, () -> Unit>? = null,
) {
    Dialog(onDismissRequest = onDismiss) {
        NovaPanel {
            NovaLabel("Parça")
            Text(text = title, style = NovaType.Title)
            NovaRule(Modifier.fillMaxWidth())
            if (choices.isEmpty()) {
                Text(
                    text = "Bu yayında parça yok",
                    style = NovaType.BodyMuted,
                )
            }
            choices.forEach { choice ->
                NovaPill(
                    label = choice.label,
                    onClick = { onSelect(choice.id) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (allowOff) {
                NovaPill(
                    label = "Kapalı",
                    onClick = { onSelect(null) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            extraAction?.let { (label, action) ->
                NovaPill(
                    label = label,
                    onClick = action,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "osd-subtitle-file",
                )
            }
            NovaPill(
                label = "Vazgeç",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

internal fun audioLabel(tracks: List<TrackChoice>): String =
    tracks.firstOrNull()?.label?.let { "Ses · $it" } ?: "Ses"

internal fun subtitleLabel(tracks: List<TrackChoice>): String =
    tracks.firstOrNull()?.label?.let { "Altyazı · $it" } ?: "Altyazı"

internal fun aspectLabel(state: PlayerUiState): String = when (state.aspect) {
    dev.anon.novatv.core.player.VideoAspect.FIT -> "Oran · Sığdır"
    dev.anon.novatv.core.player.VideoAspect.FILL -> "Oran · Doldur"
    dev.anon.novatv.core.player.VideoAspect.ZOOM -> "Oran · Kırp"
    dev.anon.novatv.core.player.VideoAspect.RATIO_16_9 -> "Oran · 16:9"
    dev.anon.novatv.core.player.VideoAspect.RATIO_4_3 -> "Oran · 4:3"
    dev.anon.novatv.core.player.VideoAspect.ORIGINAL -> "Oran · Özgün"
}
