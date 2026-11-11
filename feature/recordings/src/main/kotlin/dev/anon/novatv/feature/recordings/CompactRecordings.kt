package dev.anon.novatv.feature.recordings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
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
import dev.anon.novatv.ui.component.NovaTapSurface
import dev.anon.novatv.ui.layout.rememberNovaSpacing
import dev.anon.novatv.ui.theme.NovaColors
import dev.anon.novatv.ui.theme.NovaType

/** Kompakt içerik sütununun üst sınırı: tablette satır ortalanır, telefon taşmaz. */
private val CompactContentMaxWidth = 720.dp

/**
 * Kayıtlar ekranının telefon/tablet düzeni.
 *
 * Tek sütunlu kart listesi: karta dokunmak kaydı oynatır, kartın sağındaki
 * düğme aktif kaydı durdurur/iptal eder, biten kaydı siler. Tarih, süre ve
 * durum kartta görünür; boş durumda dokunulabilir bir "Yenile" vardır.
 * Boşluklar `NovaSpacing` tokenlarından gelir, TV ölçüsü değişmez.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun CompactRecordings(
    state: RecordingsUiState,
    now: Long,
    onPlay: (RecordingEntry) -> Unit,
    onCancel: (RecordingEntry) -> Unit,
    onDelete: (RecordingEntry) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = rememberNovaSpacing()
    NovaBackground(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = CompactContentMaxWidth)
                    .fillMaxWidth()
                    .padding(spacing.screen),
            ) {
                // Başlık ve eylemler ayrı satırda: dar ekranda üste binmesin.
                Column {
                    NovaLabel("Kayıtlar")
                    Text(
                        text = if (state.active.isEmpty()) {
                            "${state.finished.size} kayıt"
                        } else {
                            "${state.active.size} sürüyor · ${state.finished.size} tamamlandı"
                        },
                        style = NovaType.Title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(spacing.gap))
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.gap)) {
                    NovaPill(
                        label = "Yenile",
                        onClick = onRefresh,
                        modifier = Modifier.heightIn(min = 48.dp),
                        testTag = "recordings-refresh",
                    )
                    NovaPill(
                        label = "Canlı TV",
                        onClick = onBack,
                        modifier = Modifier.heightIn(min = 48.dp),
                        testTag = "recordings-back",
                    )
                }

                Spacer(Modifier.height(spacing.gap))
                NovaRule(Modifier.fillMaxWidth())
                Spacer(Modifier.height(spacing.gap))

                if (state.entries.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("recordings-empty"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "Kayıt yok",
                            style = NovaType.Display.copy(fontSize = 26.sp),
                        )
                        Spacer(Modifier.height(spacing.gap))
                        Text(
                            text = "Rehberde bir program seçip \"Kaydet\" ile plan oluşturun.",
                            style = NovaType.BodyMuted,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(spacing.screen))
                        NovaPill(
                            label = "Yenile",
                            onClick = onRefresh,
                            accent = true,
                            modifier = Modifier.heightIn(min = 48.dp),
                            testTag = "recordings-empty-refresh",
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("recordings-list"),
                        verticalArrangement = Arrangement.spacedBy(spacing.gap),
                    ) {
                        items(items = state.entries, key = { it.id }) { entry ->
                            CompactRecordingCard(
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
                        modifier = Modifier.padding(top = spacing.gap).testTag("recordings-message"),
                    )
                }
            }
        }
    }
}

/**
 * Kompakt kayıt kartı: en az 72 dp, tek dokunuş oynatır.
 *
 * Oynatılamayan kayıt (planlı, sürüyor, başarısız) kartı dokunulmaz yüzey
 * olarak kalır; silme/iptal her zaman görünür bir düğmededir.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CompactRecordingCard(
    entry: RecordingEntry,
    now: Long,
    onPlay: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    val spacing = rememberNovaSpacing()
    val status = entry.statusValue
    val playable = status == RecordingStatus.DONE && entry.filePath != null
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .background(NovaColors.Surface.copy(alpha = 0.65f), shape)
            .border(1.dp, NovaColors.Border.copy(alpha = 0.7f), shape)
            .padding(spacing.gap),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.gap),
    ) {
        if (playable) {
            NovaTapSurface(
                onClick = onPlay,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .testTag("recording-play-${entry.id}"),
                shape = RoundedCornerShape(8.dp),
                pressedContainerColor = NovaColors.SurfacePressed,
                pressedScale = 0.99f,
            ) {
                RecordingCardBody(
                    entry = entry,
                    now = now,
                    modifier = Modifier.padding(spacing.gap),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp),
            ) {
                RecordingCardBody(
                    entry = entry,
                    now = now,
                    modifier = Modifier.padding(spacing.gap),
                )
            }
        }

        if (entry.isActive) {
            NovaPill(
                label = if (status == RecordingStatus.RECORDING) "Durdur" else "İptal",
                onClick = onCancel,
                modifier = Modifier.heightIn(min = 48.dp),
                testTag = "recording-cancel-${entry.id}",
            )
        } else {
            NovaPill(
                label = "Sil",
                onClick = onDelete,
                danger = true,
                modifier = Modifier.heightIn(min = 48.dp),
                testTag = "recording-delete-${entry.id}",
            )
        }
    }
}

/** Kartın metin gövdesi: başlık + rozet, tarih/saat, kanal + süre, hata, ilerleme. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RecordingCardBody(
    entry: RecordingEntry,
    now: Long,
    modifier: Modifier = Modifier,
) {
    val status = entry.statusValue
    val spacing = rememberNovaSpacing()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.gap),
        ) {
            Text(
                text = entry.title,
                style = NovaType.Body.copy(fontSize = 16.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            NovaBadge(
                text = recordingStatusLabel(status),
                color = statusColor(status),
            )
        }

        Text(
            text = recordingEntryTimeLabel(entry),
            style = NovaType.Small.copy(color = NovaColors.TextMuted),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        val size = if (entry.bytes > 0) " · ${entry.sizeLabel}" else ""
        Text(
            text = entry.channelName + " · " + recordingDurationLabel(entry) + size,
            style = NovaType.Small.copy(color = NovaColors.TextDim),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        entry.message?.let { failure ->
            Text(
                text = failure,
                style = NovaType.Small.copy(color = NovaColors.Warning, fontSize = 12.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (status == RecordingStatus.RECORDING) {
            Spacer(Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
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
}
