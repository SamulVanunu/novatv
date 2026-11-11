package dev.anon.novatv.feature.live

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.core.sync.GroupItem
import dev.anon.novatv.core.sync.RECENT_GROUP_KEY
import dev.anon.novatv.ui.component.NovaBackground
import dev.anon.novatv.ui.component.NovaPill
import dev.anon.novatv.ui.component.NovaRule
import dev.anon.novatv.ui.component.NovaTapSurface
import dev.anon.novatv.ui.layout.NovaWindow
import dev.anon.novatv.ui.theme.NovaColors
import dev.anon.novatv.ui.theme.NovaTextFieldTheme
import dev.anon.novatv.ui.theme.NovaType
import dev.anon.novatv.ui.theme.novaTextFieldColors

/**
 * Canlı ekranının telefon/tablet düzeni.
 *
 * Yukarıdan aşağıya: başlık ve liste/ızgara düğmesi, tam genişlik arama,
 * yatay grup çipleri, sonra kanal listesi ya da ızgara. Dokunma odaklıdır:
 * ilk odak istenmez, `autoFocus` yoktur; krem odak dolgusu yalnız kumanda
 * odağında görünür.
 *
 * Eylem yüzeyi TV'nin uzun basışla açtığı diyaloğun aynısıdır (aynı model):
 * seçili kanal [LiveScreen]'de tutulur, buradan yalnız tetiklenir.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun CompactLive(
    state: LiveUiState,
    channels: LazyPagingItems<Channel>,
    window: NovaWindow,
    onSelectGroup: (String?) -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleFavorites: () -> Unit,
    onToggleGrid: () -> Unit,
    onChannelClick: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onChannelFocused: (Channel?) -> Unit,
    onOpenPlaylists: () -> Unit,
    onOpenActions: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    NovaBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("live-compact"),
        ) {
            CompactHeader(
                state = state,
                onQueryChange = onQueryChange,
                onToggleGrid = onToggleGrid,
            )

            GroupChips(
                groups = state.groups,
                selectedGroup = state.selectedGroup,
                favoritesOnly = state.favoritesOnly,
                onSelectGroup = onSelectGroup,
                onToggleFavorites = onToggleFavorites,
            )

            Spacer(Modifier.height(8.dp))

            // Hiç aktif kaynak yoksa paging akışı yüklenmez; spinner'da takılı
            // kalmamak için boş durumu gruplardan anlarız.
            val noGroups = state.groups.isEmpty()
            val noSource = noGroups && channels.itemCount == 0
            val loading = channels.loadState.refresh is LoadState.Loading

            when {
                !state.ready || (loading && channels.itemCount == 0 && !noGroups) -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        CircularProgressIndicator(color = NovaColors.Accent)
                        Text(
                            text = "Kanallar yükleniyor",
                            style = NovaType.Small.copy(color = NovaColors.TextMuted),
                        )
                    }
                }

                // TV boş durumu; odak isteği yok, yalnız yazı genişliği daralır.
                channels.itemCount == 0 -> EmptyState(
                    noSource = noSource,
                    favoritesOnly = state.favoritesOnly,
                    onOpenPlaylists = onOpenPlaylists,
                    modifier = Modifier.weight(1f),
                )

                state.grid -> LazyVerticalGrid(
                    columns = GridCells.Fixed(liveGridColumns(window)),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("channel-grid"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        count = channels.itemCount,
                        key = channels.itemKey { it.id },
                    ) { index ->
                        val channel = channels[index] ?: return@items
                        CompactChannelCard(
                            channel = channel,
                            favorite = channel.id in state.favoriteIds,
                            index = index,
                            onClick = { onChannelClick(channel) },
                            onFocused = { onChannelFocused(channel) },
                        )
                    }
                }

                else -> {
                    // Tek gruba süzülen listede grup adı tekrarlanmaz.
                    val singleGroup = state.selectedGroup.let {
                        it != null && it != RECENT_GROUP_KEY && !state.favoritesOnly
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("channel-list"),
                    ) {
                        items(
                            count = channels.itemCount,
                            key = channels.itemKey { it.id },
                        ) { index ->
                            val channel = channels[index] ?: return@items
                            CompactChannelRow(
                                channel = channel,
                                favorite = channel.id in state.favoriteIds,
                                index = index,
                                showGroup = !singleGroup,
                                onClick = { onChannelClick(channel) },
                                onToggleFavorite = { onToggleFavorite(channel) },
                                onMore = { onOpenActions(channel) },
                                onFocused = { onChannelFocused(channel) },
                            )
                        }
                    }
                }
            }

            state.error?.let { message ->
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = NovaColors.Danger,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("live-error"),
                )
            }
        }
    }
}

/** Başlık, liste/ızgara düğmesi ve tam genişlik arama alanı. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CompactHeader(
    state: LiveUiState,
    onQueryChange: (String) -> Unit,
    onToggleGrid: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (state.favoritesOnly) "Favoriler" else "Canlı TV",
                    style = NovaType.Title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                state.groups.firstOrNull {
                    it.key == (state.selectedGroup ?: LiveUiState.ALL_GROUPS_KEY)
                }?.let { group ->
                    Text(
                        text = "${group.count} kanal",
                        style = NovaType.Number.copy(color = NovaColors.TextDim),
                    )
                }
            }
            CompactGlyphButton(
                glyph = if (state.grid) "☰" else "▦",
                contentDescription = if (state.grid) "Liste" else "Izgara",
                onClick = onToggleGrid,
                tint = NovaColors.Text,
                testTag = "toggle-grid",
            )
        }

        Spacer(Modifier.height(10.dp))

        // Dokununca klavye normal açılır: D-pad'e özel gezinme eklenmez.
        NovaTextFieldTheme {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = { Text("Ara", style = NovaType.Body.copy(color = NovaColors.TextDim)) },
                singleLine = true,
                shape = RoundedCornerShape(50),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search,
                ),
                colors = novaTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live-search"),
            )
        }

        Spacer(Modifier.height(10.dp))
    }
}

/**
 * Yatay kaydırılan grup çipleri: "Tümü", son izlenenler, gruplar, en sonda
 * favoriler. Seçili çip krem dolgu (odak diliyle aynı) taşır.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun GroupChips(
    groups: List<GroupItem>,
    selectedGroup: String?,
    favoritesOnly: Boolean,
    onSelectGroup: (String?) -> Unit,
    onToggleFavorites: () -> Unit,
) {
    val selectedKey = selectedGroup ?: LiveUiState.ALL_GROUPS_KEY
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("group-chips"),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(groups, key = { it.key }) { item ->
            NovaPill(
                label = item.label,
                onClick = { onSelectGroup(item.key.ifBlank { null }) },
                selected = !favoritesOnly && item.key == selectedKey,
                testTag = if (item.key.isEmpty()) "group-all" else "group-${item.key}",
            )
        }
        item(key = "chip-favorites") {
            NovaPill(
                label = if (favoritesOnly) "★ Favoriler" else "☆ Favoriler",
                onClick = onToggleFavorites,
                selected = favoritesOnly,
                testTag = "toggle-favorites",
            )
        }
    }
}

/**
 * Dokunmatik kanal satırı: logo 44 dp, numara, ad, grup; sağda yıldız ve
 * taşma. Gövdeye dokunmak oynatır; yıldız (48 dp) favoriyi açar/kapatır,
 * taşma (48 dp) TV'nin uzun basış eylem yüzeyini açar. En az 56 dp yüksek.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CompactChannelRow(
    channel: Channel,
    favorite: Boolean,
    index: Int,
    showGroup: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMore: () -> Unit,
    onFocused: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NovaTapSurface(
                onClick = onClick,
                shape = shape,
                containerColor = Color.Transparent,
                contentColor = NovaColors.Text,
                focusedContainerColor = NovaColors.SurfaceHigh,
                focusedContentColor = NovaColors.Text,
                pressedContainerColor = NovaColors.SurfacePressed,
                pressedContentColor = NovaColors.Text,
                focusedBorder = BorderStroke(2.dp, NovaColors.Focus),
                focusedScale = 1f,
                pressedScale = 0.995f,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .onFocusChanged { if (it.isFocused) onFocused() }
                    .testTag("channel-$index"),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(start = 6.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ChannelLogo(
                        url = channel.logoUrl,
                        name = channel.name,
                        modifier = Modifier.size(44.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            channel.number?.let { number ->
                                Text(
                                    text = "#$number",
                                    style = NovaType.Number.copy(
                                        color = NovaColors.Accent.copy(alpha = 0.8f),
                                    ),
                                    maxLines = 1,
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                text = channel.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = NovaColors.Text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (showGroup && channel.groupName.isNotBlank()) {
                            Text(
                                text = channel.groupName,
                                fontSize = 13.sp,
                                color = NovaColors.TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            CompactGlyphButton(
                glyph = if (favorite) "★" else "☆",
                contentDescription = if (favorite) "Favoriden çıkar" else "Favorilere ekle",
                onClick = onToggleFavorite,
                tint = if (favorite) NovaColors.Accent else NovaColors.TextMuted,
                testTag = "favorite-$index",
            )
            CompactGlyphButton(
                glyph = "⋯",
                contentDescription = "Kanal eylemleri",
                onClick = onMore,
                testTag = "actions-$index",
            )
        }
        NovaRule(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 72.dp, end = 10.dp),
            color = NovaColors.Border.copy(alpha = 0.45f),
        )
    }
}

/** Izgara kartı: logo, ad ve numara. Dokununca oynar. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CompactChannelCard(
    channel: Channel,
    favorite: Boolean,
    index: Int,
    onClick: () -> Unit,
    onFocused: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    NovaTapSurface(
        onClick = onClick,
        shape = shape,
        containerColor = NovaColors.Surface,
        contentColor = NovaColors.Text,
        focusedContainerColor = NovaColors.SurfaceHigh,
        focusedContentColor = NovaColors.Text,
        pressedContainerColor = NovaColors.SurfacePressed,
        pressedContentColor = NovaColors.Text,
        border = BorderStroke(1.dp, NovaColors.Border.copy(alpha = 0.8f)),
        focusedBorder = BorderStroke(2.dp, NovaColors.Focus),
        focusedScale = 1.02f,
        pressedScale = 0.99f,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .onFocusChanged { if (it.isFocused) onFocused() }
            .testTag("channel-$index"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChannelLogo(
                    url = channel.logoUrl,
                    name = channel.name,
                    modifier = Modifier.size(40.dp),
                )
                Spacer(Modifier.weight(1f))
                if (favorite) {
                    Text(text = "★", fontSize = 15.sp, color = NovaColors.Accent)
                    Spacer(Modifier.width(8.dp))
                }
                channel.number?.let { number ->
                    Text(
                        text = "#$number",
                        style = NovaType.Number.copy(color = NovaColors.TextDim),
                        maxLines = 1,
                    )
                }
            }
            Text(
                text = channel.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 21.sp,
                color = NovaColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Metin glifiyle 48 dp dokunma hedefi; ikon kütüphanesi kullanılmaz. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CompactGlyphButton(
    glyph: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = NovaColors.TextMuted,
    testTag: String? = null,
) {
    NovaTapSurface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        containerColor = Color.Transparent,
        contentColor = tint,
        focusedContainerColor = NovaColors.Focus,
        focusedContentColor = NovaColors.FocusInk,
        focusedScale = 1f,
        pressedScale = 0.94f,
        modifier = modifier
            .size(48.dp)
            .semantics { this.contentDescription = contentDescription }
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
    ) { state ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = glyph,
                fontSize = 20.sp,
                color = state.contentColor,
                maxLines = 1,
            )
        }
    }
}
