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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.anon.novatv.core.player.TrackChoice

/** Yarı saydam OSD: kanal bilgisi, parça/oran/tampon seçimleri ve kanal listesine dönüş. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerOsd(
    state: PlayerUiState,
    onBackToChannels: () -> Unit,
    onSeekBack: () -> Unit,
    onCycleAspect: () -> Unit,
    onCycleBuffer: () -> Unit,
    onSelectAudio: (String) -> Unit,
    onSelectSubtitle: (String?) -> Unit,
    firstFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    var audioMenu by remember { mutableStateOf(false) }
    var subtitleMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .testTag("osd"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .padding(horizontal = 40.dp, vertical = 28.dp),
        ) {
            Text(
                text = state.channel?.name ?: "Kanal",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.testTag("osd-channel-name"),
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                state.channel?.groupName?.let { group ->
                    Text(group, fontSize = 15.sp, color = Color(0xFFB9C3CE))
                }
                state.channel?.number?.let { number ->
                    Text("#$number", fontSize = 15.sp, color = Color(0xFFB9C3CE))
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                Text("Şimdi: —", fontSize = 15.sp, color = Color(0xFFB9C3CE))
                Text("Sıradaki: —", fontSize = 15.sp, color = Color(0xFFB9C3CE))
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xCC101418))
                .padding(horizontal = 32.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onBackToChannels,
                modifier = Modifier
                    .focusRequester(firstFocusRequester)
                    .testTag("osd-channels"),
            ) {
                Text("Kanallar")
            }

            if (state.canSeekBack) {
                Button(onClick = onSeekBack, modifier = Modifier.testTag("osd-seek-back")) {
                    Text("−10 sn")
                }
            }

            Button(onClick = { audioMenu = true }, modifier = Modifier.testTag("osd-audio")) {
                Text(audioLabel(state.audioTracks))
            }

            Button(onClick = { subtitleMenu = true }, modifier = Modifier.testTag("osd-subtitle")) {
                Text(subtitleLabel(state.subtitleTracks))
            }

            Button(onClick = onCycleAspect, modifier = Modifier.testTag("osd-aspect")) {
                Text(aspectLabel(state))
            }

            Button(onClick = onCycleBuffer, modifier = Modifier.testTag("osd-buffer")) {
                Text("Tampon ${state.bufferSeconds} sn")
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "Geri: OSD kapat / kanallar",
                fontSize = 13.sp,
                color = Color(0xFF8E9AA6),
                modifier = Modifier.width(220.dp),
            )
        }

        if (state.canSeekBack) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .padding(horizontal = 32.dp)
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
                    .background(Color(0x663A4754), RoundedCornerShape(2.dp)),
            )
        }
    }

    if (audioMenu) {
        TrackMenuDialog(
            title = "Ses",
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
private fun TrackMenuDialog(
    title: String,
    choices: List<TrackChoice>,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
    allowOff: Boolean = false,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(420.dp)
                .padding(24.dp)
                .background(Color(0xFF141A20), RoundedCornerShape(12.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (choices.isEmpty()) {
                Text(
                    text = "Bu yayında parça yok",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            choices.forEach { choice ->
                Button(onClick = { onSelect(choice.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text(choice.label)
                }
            }
            if (allowOff) {
                Button(onClick = { onSelect(null) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Kapalı")
                }
            }
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Vazgeç")
            }
        }
    }
}

private fun audioLabel(tracks: List<TrackChoice>): String =
    tracks.firstOrNull()?.label?.let { "Ses: $it" } ?: "Ses"

private fun subtitleLabel(tracks: List<TrackChoice>): String =
    tracks.firstOrNull()?.label?.let { "Altyazı: $it" } ?: "Altyazı"

private fun aspectLabel(state: PlayerUiState): String = when (state.aspect) {
    dev.anon.novatv.core.player.VideoAspect.FIT -> "Oran: Sığdır"
    dev.anon.novatv.core.player.VideoAspect.FILL -> "Oran: Doldur"
    dev.anon.novatv.core.player.VideoAspect.ZOOM -> "Oran: Kırp"
    dev.anon.novatv.core.player.VideoAspect.RATIO_16_9 -> "Oran: 16:9"
    dev.anon.novatv.core.player.VideoAspect.RATIO_4_3 -> "Oran: 4:3"
    dev.anon.novatv.core.player.VideoAspect.ORIGINAL -> "Oran: Özgün"
}
