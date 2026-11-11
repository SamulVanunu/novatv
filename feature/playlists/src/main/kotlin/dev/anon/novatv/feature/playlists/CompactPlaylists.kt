package dev.anon.novatv.feature.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import dev.anon.novatv.core.model.Playlist
import dev.anon.novatv.core.model.PlaylistType
import dev.anon.novatv.core.model.SyncState
import dev.anon.novatv.core.model.redactUrl
import dev.anon.novatv.ui.component.NovaBackground
import dev.anon.novatv.ui.component.NovaBadge
import dev.anon.novatv.ui.component.NovaLabel
import dev.anon.novatv.ui.component.NovaPill
import dev.anon.novatv.ui.component.NovaRule
import dev.anon.novatv.ui.layout.rememberNovaSpacing
import dev.anon.novatv.ui.theme.NovaColors
import dev.anon.novatv.ui.theme.NovaType

/**
 * Kaynaklar ekranının telefon/tablet düzeni.
 *
 * TV gövdesinden ayrıdır; `window == Expanded` bu dosyadaki hiçbir bileşeni
 * çizmez. Tek sütun, `NovaSpacing` boşlukları, içerik `widthIn(max = 720.dp)`
 * ile ortalanır.
 *
 * Kart üstte ad/işaret/rozetler ve arındırılmış adresi taşır; altta düzenleme
 * eylemleri her zaman görünür, en az 48 dp dokunma hedefleridir — odakta
 * beliren küçük düğmeler yok. Her kart en az 64 dp yüksek. İlk odak
 * istenmez: krem odak dolgusu yalnız kumanda odağında görünür.
 *
 * `open-playlists` etiketi TV'de kalır; kompakt yol onu tekrar çizmez.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun CompactPlaylists(
    state: PlaylistsUiState,
    onAdd: () -> Unit,
    onEdit: (Playlist) -> Unit,
    onRefresh: (Playlist) -> Unit,
    onToggleActive: (Playlist) -> Unit,
    onMoveUp: (Playlist) -> Unit,
    onMoveDown: (Playlist) -> Unit,
    onDelete: (Playlist) -> Unit,
    onBackToLive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = rememberNovaSpacing()

    NovaBackground(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    // Sıra önemli: önce sınır, sonra doldurma. `fillMaxSize`
                    // dışta kalırsa genişlik sabitlenir, 720 dp sınırı işlemez.
                    .widthIn(max = 720.dp)
                    .fillMaxSize()
                    .testTag("playlists-compact"),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = spacing.screen,
                    end = spacing.screen,
                    top = spacing.screen,
                    bottom = spacing.screen,
                ),
                verticalArrangement = Arrangement.spacedBy(spacing.gap),
            ) {
                item(key = "header") {
                    CompactPlaylistsHeader(
                        onAdd = onAdd,
                        onBackToLive = onBackToLive,
                    )
                }

                item(key = "rule") {
                    NovaRule(Modifier.fillMaxWidth().padding(top = spacing.gap))
                }

                when {
                    state.loading -> item(key = "loading") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing.gap),
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                color = NovaColors.Accent,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp),
                            )
                            Text("Kaynaklar yükleniyor", style = NovaType.BodyMuted)
                        }
                    }

                    state.items.isEmpty() -> item(key = "empty") {
                        CompactPlaylistsEmpty(onAdd = onAdd)
                    }

                    else -> items(state.items, key = { it.id }) { playlist ->
                        CompactPlaylistCard(
                            playlist = playlist,
                            onEdit = { onEdit(playlist) },
                            onRefresh = { onRefresh(playlist) },
                            onToggleActive = { onToggleActive(playlist) },
                            onMoveUp = { onMoveUp(playlist) },
                            onMoveDown = { onMoveDown(playlist) },
                            onDelete = { onDelete(playlist) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Başlık ve gezinme: TV'de tek satır sığar, 411 dp'de sığmaz. Düğmeler
 * başlığın altında yatay kaydırılan şeritte durur, hiçbiri kırpılmaz.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CompactPlaylistsHeader(
    onAdd: () -> Unit,
    onBackToLive: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        NovaLabel("Kaynaklar")
        Text(text = "Yayın kaynakları", style = NovaType.Title)
        Text(
            text = "Xtream Codes, M3U bağlantısı veya Stalker portal.",
            style = NovaType.Small.copy(color = NovaColors.TextMuted),
        )
        Spacer(Modifier.height(6.dp))
        CompactPillRow {
            CompactPill(
                label = "+ Kaynak ekle",
                onClick = onAdd,
                accent = true,
                testTag = "add-source",
            )
            CompactPill(label = "Canlı TV", onClick = onBackToLive, testTag = "back-to-live")
        }
    }
}

/**
 * Kaynak kartı. Adres [redactUrl] ile temizlenir: sorgu, parça ve kullanıcı
 * bilgisi hiçbir koşulda çizilmez.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CompactPlaylistCard(
    playlist: Playlist,
    onEdit: () -> Unit,
    onRefresh: () -> Unit,
    onToggleActive: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val safeUrl = redactUrl(playlist.baseUrl)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = CompactPlaylistRowHeight)
            .background(NovaColors.Surface, shape)
            .border(1.dp, NovaColors.Border.copy(alpha = 0.8f), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompactSourceMark(type = playlist.type, active = playlist.isActive)
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = NovaType.Body.copy(fontSize = 17.sp, lineHeight = 22.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("playlist-name"),
                )
                Text(
                    text = "${playlist.syncIntervalHours} saatte bir yenilenir",
                    style = NovaType.Small.copy(color = NovaColors.TextDim),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Tür ve durum rozetleri: TV kartındaki künyenin dokunulabilir satırı.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            NovaBadge(listTypeLabel(playlist.type), color = NovaColors.TextMuted)
            CompactStatusBadge(playlist.syncState)
            if (!playlist.isActive) {
                NovaBadge(text = "devre dışı", color = NovaColors.TextDim)
            }
        }

        Text(
            text = safeUrl.ifBlank { "adres yok" },
            style = NovaType.Small.copy(fontSize = 13.sp, color = NovaColors.TextMuted),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("playlist-url"),
        )

        playlist.lastError?.let { message ->
            Text(
                text = message,
                style = NovaType.Small.copy(color = NovaColors.Danger),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("playlist-error"),
            )
        }

        Spacer(Modifier.height(2.dp))

        // Eylem şeridi kayar, kırpılmaz: 411 dp'de altı düğme tek satıra
        // sığmaz. Sil en sonda, kendi şeridinde yalıtılır.
        CompactPillRow(modifier = Modifier.testTag("playlist-actions-${playlist.id}")) {
            CompactPill(label = "Düzenle", onClick = onEdit, testTag = "playlist-edit-${playlist.id}")
            CompactPill(label = "Yenile", onClick = onRefresh, testTag = "playlist-refresh-${playlist.id}")
            CompactPill(
                label = if (playlist.isActive) "Pasif yap" else "Aktif yap",
                onClick = onToggleActive,
                testTag = "playlist-toggle-${playlist.id}",
            )
            CompactPill(label = "Yukarı", onClick = onMoveUp, testTag = "playlist-up-${playlist.id}")
            CompactPill(label = "Aşağı", onClick = onMoveDown, testTag = "playlist-down-${playlist.id}")
        }

        NovaPill(
            label = "Sil",
            onClick = onDelete,
            danger = true,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = CompactControlHeight),
            testTag = "playlist-delete-${playlist.id}",
        )
    }
}

/** Kaynak yoksa: TV'deki metnin aynısı, tam genişlik 56 dp düğme. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CompactPlaylistsEmpty(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = "Henüz kaynak yok", style = NovaType.Title)
        Text(
            text = "İlk kaynağınızı ekleyin; kanallar, gruplar ve favoriler otomatik gelsin.",
            style = NovaType.BodyMuted,
        )
        Spacer(Modifier.height(6.dp))
        NovaPill(
            label = "Kaynak ekle",
            onClick = onAdd,
            accent = true,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = CompactPrimaryHeight),
            testTag = "empty-add-source",
        )
    }
}

/** Kaynak türü işareti: 40 dp kutu, etkin kaynakta kehribar harf. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CompactSourceMark(type: PlaylistType, active: Boolean) {
    val color = if (active) NovaColors.Accent else NovaColors.TextDim
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(color.copy(alpha = 0.11f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = type.name.first().toString(), style = NovaType.Title.copy(color = color))
    }
}

/** Senkron durumu: TV rozet diliyle aynı etiketler. */
@Composable
private fun CompactStatusBadge(state: SyncState) {
    val (label, color) = when (state) {
        SyncState.OK -> "senkron" to NovaColors.Live
        SyncState.RUNNING -> "aktarılıyor" to NovaColors.Accent
        SyncState.PARTIAL -> "kısmi" to NovaColors.Warning
        SyncState.FAILED -> "hata" to NovaColors.Danger
        SyncState.NEVER -> "bekliyor" to NovaColors.TextDim
    }
    NovaBadge(text = label, color = color)
}

/** 48 dp dokunma hedefi: kart eylemleri. */
@Composable
private fun CompactPill(
    label: String,
    onClick: () -> Unit,
    accent: Boolean = false,
    testTag: String? = null,
) {
    NovaPill(
        label = label,
        onClick = onClick,
        accent = accent,
        modifier = Modifier.heightIn(min = CompactControlHeight),
        testTag = testTag,
    )
}

/**
 * Tek satır, yatay kaydırılan düğme şeridi; sığmayan kuyruk kırpılmaz,
 * parmakla kaydırılır. Dolgu çağırandan gelir, ilk düğme satır hizasında
 * başlar.
 */
@Composable
private fun CompactPillRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scrollState = androidx.compose.foundation.rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = CompactControlHeight)
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
        if (scrollState.maxValue > 0) Spacer(Modifier.size(8.dp))
    }
}

/**
 * Silme onayı. TV diyaloğu ile aynı metin; düğmeler tam genişlik ve 56 dp,
 * çünkü telefonda `NovaPanel`in sabit 440 dp genişliği ekranı taşırır.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun CompactDeleteConfirmDialog(
    name: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(NovaColors.SurfaceHigh, RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NovaLabel("Kaynağı sil")
            Text(text = "“$name” silinsin mi?", style = NovaType.Title)
            Text(
                text = "Kanallar, EPG ve kayıtlı kimlik bilgileri de silinir. Bu işlem geri alınamaz.",
                style = NovaType.BodyMuted,
            )
            NovaRule(Modifier.fillMaxWidth())
            NovaPill(
                label = "Sil",
                onClick = onConfirm,
                danger = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = CompactPrimaryHeight),
                testTag = "delete-confirm",
            )
            NovaPill(
                label = "Vazgeç",
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = CompactPrimaryHeight),
                testTag = "delete-cancel",
            )
        }
    }
}

/** Kaynak kartı alt sınırı; içerik her zaman daha yüksek olsa da taban. */
internal val CompactPlaylistRowHeight = 64.dp

/** Kart eylemi dokunma hedefi (erişilebilirlik alt sınırı 48 dp). */
internal val CompactControlHeight = 48.dp

/** Birincil düğme (Kaynak ekle, silme onayı) yüksekliği. */
internal val CompactPrimaryHeight = 56.dp
