package dev.anon.novatv.feature.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import dev.anon.novatv.core.model.Playlist
import dev.anon.novatv.core.model.PlaylistType
import dev.anon.novatv.core.model.SyncState
import dev.anon.novatv.ui.component.NovaBackground
import dev.anon.novatv.ui.component.NovaBadge
import dev.anon.novatv.ui.component.NovaLabel
import dev.anon.novatv.ui.component.NovaPanel
import dev.anon.novatv.ui.component.NovaPill
import dev.anon.novatv.ui.component.NovaRule
import dev.anon.novatv.ui.theme.NovaColors
import dev.anon.novatv.ui.theme.NovaType

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    state: PlaylistsUiState,
    onAdd: () -> Unit,
    onEdit: (Playlist) -> Unit,
    onRefresh: (Playlist) -> Unit,
    onToggleActive: (Playlist) -> Unit,
    onMoveUp: (Playlist) -> Unit,
    onMoveDown: (Playlist) -> Unit,
    onDelete: (Playlist) -> Unit,
    onBackToLive: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var pendingDelete by remember { mutableStateOf<Playlist?>(null) }
    val emptyFocus = remember { FocusRequester() }

    LaunchedEffect(state.loading, state.items.size) {
        if (!state.loading && state.items.isEmpty()) {
            // Boş ekranda ilk odak doğrudan "+ Kaynak ekle" üzerinde olsun.
            withFrameNanos { }
            withFrameNanos { }
            runCatching { emptyFocus.requestFocus() }
        }
    }

    NovaBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 44.dp, vertical = 26.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    NovaLabel("Kaynaklar")
                    Text(text = "Yayın kaynakları", style = NovaType.Display)
                    Text(
                        text = "Xtream Codes, M3U bağlantısı veya Stalker portal.",
                        style = NovaType.BodyMuted,
                    )
                }
                Spacer(Modifier.weight(1f))
                NovaPill(
                    label = "Canlı TV",
                    onClick = onBackToLive,
                    testTag = "back-to-live",
                )
                Spacer(Modifier.width(10.dp))
                NovaPill(
                    label = "+ Kaynak ekle",
                    onClick = onAdd,
                    accent = true,
                    testTag = "add-source",
                )
            }

            Spacer(Modifier.height(24.dp))
            NovaRule(Modifier.fillMaxWidth())
            Spacer(Modifier.height(18.dp))

            when {
                state.loading -> Row(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = NovaColors.Accent,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                    )
                    Text("Kaynaklar yükleniyor", style = NovaType.BodyMuted)
                }

                state.items.isEmpty() -> Column(
                    modifier = Modifier.fillMaxSize().padding(bottom = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("Henüz kaynak yok", style = NovaType.Display.copy(fontSize = 30.sp))
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "İlk kaynağınızı ekleyin; kanallar, gruplar ve favoriler otomatik gelsin.",
                        style = NovaType.BodyMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(520.dp),
                    )
                    Spacer(Modifier.height(22.dp))
                    NovaPill(
                        label = "Kaynak ekle",
                        onClick = onAdd,
                        accent = true,
                        modifier = Modifier.focusRequester(emptyFocus),
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.items, key = { it.id }) { playlist ->
                        PlaylistRow(
                            playlist = playlist,
                            onEdit = { onEdit(playlist) },
                            onRefresh = { onRefresh(playlist) },
                            onToggleActive = { onToggleActive(playlist) },
                            onMoveUp = { onMoveUp(playlist) },
                            onMoveDown = { onMoveDown(playlist) },
                            onDelete = { pendingDelete = playlist },
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { playlist ->
        DeleteConfirmDialog(
            name = playlist.name,
            onConfirm = {
                pendingDelete = null
                onDelete(playlist)
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlaylistRow(
    playlist: Playlist,
    onEdit: () -> Unit,
    onRefresh: () -> Unit,
    onToggleActive: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NovaColors.Surface, shape)
            .border(1.dp, NovaColors.Border.copy(alpha = 0.8f), shape)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SourceMark(type = playlist.type, active = playlist.isActive)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = playlist.name,
                        fontSize = 22.sp,
                        style = NovaType.Title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false).testTag("playlist-name"),
                    )
                    Spacer(Modifier.width(12.dp))
                    NovaBadge(typeLabel(playlist.type), color = NovaColors.TextMuted)
                    Spacer(Modifier.width(8.dp))
                    StatusBadge(playlist.syncState)
                    if (!playlist.isActive) {
                        Spacer(Modifier.width(8.dp))
                        NovaBadge(text = "devre dışı", color = NovaColors.TextDim)
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${playlist.syncIntervalHours} saatte bir yenilenir",
                    style = NovaType.Small.copy(color = NovaColors.TextDim),
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NovaPill(label = "Düzenle", onClick = onEdit)
                    NovaPill(label = "Yenile", onClick = onRefresh)
                    NovaPill(
                        label = if (playlist.isActive) "Pasif yap" else "Aktif yap",
                        onClick = onToggleActive,
                    )
                    NovaPill(label = "Yukarı", onClick = onMoveUp)
                    NovaPill(label = "Aşağı", onClick = onMoveDown)
                    NovaPill(label = "Sil", onClick = onDelete, danger = true)
                }
            }
        }
    }
}

/** Kaynak türü işareti: X / M / S harfi, etkin kaynakta kehribar. */
@Composable
private fun SourceMark(type: PlaylistType, active: Boolean) {
    val color = if (active) NovaColors.Accent else NovaColors.TextDim
    Box(
        modifier = Modifier
            .size(54.dp)
            .background(color.copy(alpha = 0.11f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = type.name.first().toString(),
            style = NovaType.Title.copy(color = color),
        )
    }
}

@Composable
private fun StatusBadge(state: SyncState) {
    val (label, color) = when (state) {
        SyncState.OK -> "senkron" to NovaColors.Live
        SyncState.RUNNING -> "aktarılıyor" to NovaColors.Accent
        SyncState.PARTIAL -> "kısmi" to NovaColors.Warning
        SyncState.FAILED -> "hata" to NovaColors.Danger
        SyncState.NEVER -> "bekliyor" to NovaColors.TextDim
    }
    NovaBadge(text = label, color = color)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DeleteConfirmDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        NovaPanel {
            NovaLabel("Kaynağı sil")
            Text(text = "“$name” silinsin mi?", style = NovaType.Title)
            Text(
                text = "Kanallar, EPG ve kayıtlı kimlik bilgileri de silinir. Bu işlem geri alınamaz.",
                style = NovaType.BodyMuted,
            )
            NovaRule(Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NovaPill(label = "Sil", onClick = onConfirm, danger = true)
                NovaPill(label = "Vazgeç", onClick = onDismiss)
            }
        }
    }
}

private fun typeLabel(type: PlaylistType): String = when (type) {
    PlaylistType.XTREAM -> "Xtream"
    PlaylistType.M3U -> "M3U"
    PlaylistType.STALKER -> "Stalker"
}

