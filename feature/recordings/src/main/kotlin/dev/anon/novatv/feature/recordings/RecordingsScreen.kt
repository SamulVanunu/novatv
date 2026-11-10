package dev.anon.novatv.feature.recordings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import dev.anon.novatv.core.recording.RecordingEntry
import dev.anon.novatv.core.recording.RecordingStatus
import dev.anon.novatv.ui.component.NovaBackground
import dev.anon.novatv.ui.component.NovaBadge
import dev.anon.novatv.ui.component.NovaLabel
import dev.anon.novatv.ui.component.NovaPill
import dev.anon.novatv.ui.component.NovaRule
import dev.anon.novatv.ui.theme.NovaColors
import dev.anon.novatv.ui.theme.NovaType

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun RecordingsScreen(
    state: RecordingsUiState,
    now: Long = System.currentTimeMillis(),
    onPlay: (RecordingEntry) -> Unit,
    onCancel: (RecordingEntry) -> Unit,
    onDelete: (RecordingEntry) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NovaBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    NovaLabel("Kayıtlar")
                    Text(
                        text = if (state.active.isEmpty()) {
                            "${state.finished.size} kayıt"
                        } else {
                            "${state.active.size} sürüyor · ${state.finished.size} tamamlandı"
                        },
                        style = NovaType.Title,
                    )
                }
                NovaPill(label = "Yenile", onClick = onRefresh, testTag = "recordings-refresh")
                NovaPill(label = "Canlı TV", onClick = onBack, testTag = "recordings-back")
            }

            Spacer(Modifier.height(12.dp))
            NovaRule(Modifier.fillMaxWidth())

            if (state.entries.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Kayıt yok",
                        style = NovaType.Display.copy(fontSize = 26.sp),
                        modifier = Modifier.testTag("recordings-empty"),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Rehberde bir program seçip \"Kaydet\" ile plan oluşturun.",
                        style = NovaType.BodyMuted,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("recordings-list"),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(items = state.entries, key = { it.id }) { entry ->
                        RecordingRow(
                            entry = entry,
                            now = now,
                            onPlay = { onPlay(entry) },
                            onCancel = { onCancel(entry) },
                            onDelete = { onDelete(entry) },
                        )
                    }
                }
            }

            state.message?.let { message ->
                Text(
                    text = message,
                    style = NovaType.Small.copy(color = NovaColors.Accent),
                    modifier = Modifier.padding(top = 6.dp).testTag("recordings-message"),
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RecordingRow(
    entry: RecordingEntry,
    now: Long,
    onPlay: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    val status = entry.statusValue
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NovaColors.Surface.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(46.dp)
                .background(statusColor(status), RoundedCornerShape(50)),
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = entry.title,
                style = NovaType.Body.copy(fontSize = 16.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.channelName + " · " + recordingEntryTimeLabel(entry),
                style = NovaType.Small.copy(color = NovaColors.TextMuted),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val failure = entry.message
            if (failure != null) {
                Text(
                    text = failure,
                    style = NovaType.Small.copy(color = NovaColors.Warning, fontSize = 12.sp),
                    maxLines = 1,
                )
            }
            if (status == RecordingStatus.RECORDING) {
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(3.dp)
                        .background(NovaColors.Border, RoundedCornerShape(50)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(recordingProgress(entry, now))
                            .fillMaxHeight()
                            .background(NovaColors.Accent, RoundedCornerShape(50)),
                    )
                }
            }
        }

        NovaBadge(
            text = recordingStatusLabel(status),
            color = statusColor(status),
        )
        if (entry.bytes > 0) {
            Text(
                text = entry.sizeLabel,
                style = NovaType.Number.copy(color = NovaColors.TextDim),
                modifier = Modifier.width(88.dp),
            )
        }

        if (entry.isActive) {
            NovaPill(
                label = if (status == RecordingStatus.RECORDING) "Durdur" else "İptal",
                onClick = onCancel,
                testTag = "recording-cancel-${entry.id}",
            )
        } else {
            NovaPill(
                label = "Oynat",
                onClick = onPlay,
                accent = true,
                enabled = status == RecordingStatus.DONE && entry.filePath != null,
                testTag = "recording-play-${entry.id}",
            )
            NovaPill(
                label = "Sil",
                onClick = onDelete,
                testTag = "recording-delete-${entry.id}",
            )
        }
    }
}

private fun statusColor(status: RecordingStatus) = when (status) {
    RecordingStatus.RECORDING -> NovaColors.Live
    RecordingStatus.SCHEDULED -> NovaColors.Accent
    RecordingStatus.DONE -> NovaColors.Live
    RecordingStatus.FAILED -> NovaColors.Warning
    RecordingStatus.CANCELLED -> NovaColors.TextDim
}
