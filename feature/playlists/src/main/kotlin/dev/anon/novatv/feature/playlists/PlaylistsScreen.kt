package dev.anon.novatv.feature.playlists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.anon.novatv.core.model.Playlist

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

    Column(modifier = modifier.fillMaxSize().padding(48.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Kaynaklar",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.width(32.dp))
            Button(onClick = onAdd, modifier = Modifier.testTag("add-source")) {
                Text("Kaynak ekle")
            }
            Spacer(Modifier.width(12.dp))
            Button(onClick = onBackToLive, modifier = Modifier.testTag("back-to-live")) {
                Text("Canlı TV")
            }
        }

        when {
            state.loading -> Text(
                text = "Yükleniyor…",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 32.dp),
            )

            state.items.isEmpty() -> Text(
                text = "Henüz kaynak yok. Başlamak için “Kaynak ekle”.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 32.dp),
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.testTag("playlist-name"),
            )
            Text(
                text = "${playlist.type.name} • ${if (playlist.isActive) "Aktif" else "Pasif"} • " +
                    "${playlist.syncIntervalHours} sa",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(onClick = onEdit) { Text("Düzenle") }
        Spacer(Modifier.width(8.dp))
        Button(onClick = onRefresh) { Text("Yenile") }
        Spacer(Modifier.width(8.dp))
        Button(onClick = onToggleActive) { Text(if (playlist.isActive) "Pasif yap" else "Aktif yap") }
        Spacer(Modifier.width(8.dp))
        Button(onClick = onMoveUp) { Text("Yukarı") }
        Spacer(Modifier.width(8.dp))
        Button(onClick = onMoveDown) { Text("Aşağı") }
        Spacer(Modifier.width(8.dp))
        Button(onClick = onDelete) { Text("Sil") }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DeleteConfirmDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("“$name” silinsin mi?", fontSize = 20.sp)
            Text(
                "Kanallar, EPG ve kayıtlı kimlik bilgileri de silinir.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onConfirm) { Text("Sil") }
                Button(onClick = onDismiss) { Text("Vazgeç") }
            }
        }
    }
}
