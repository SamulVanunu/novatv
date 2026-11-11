package dev.anon.novatv.feature.live

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.core.sync.RECENT_GROUP_KEY
import dev.anon.novatv.ui.component.NovaBackground
import dev.anon.novatv.ui.component.NovaLabel
import dev.anon.novatv.ui.component.NovaPanel
import dev.anon.novatv.ui.component.NovaPill
import dev.anon.novatv.ui.component.NovaRule
import dev.anon.novatv.ui.focus.tvFieldNavigation
import dev.anon.novatv.ui.layout.NovaWindow
import dev.anon.novatv.ui.theme.NovaColors
import dev.anon.novatv.ui.theme.NovaTextFieldTheme
import dev.anon.novatv.ui.theme.novaTextFieldColors
import dev.anon.novatv.ui.theme.NovaType

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LiveScreen(
    state: LiveUiState,
    channels: LazyPagingItems<Channel>,
    onSelectGroup: (String?) -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleFavorites: () -> Unit,
    onToggleGrid: () -> Unit,
    onChannelClick: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onHideChannel: (Channel) -> Unit,
    onHideGroup: (String) -> Unit,
    onSetNumber: (Channel, Int?) -> Unit,
    onChannelFocused: (Channel?) -> Unit,
    onOpenPlaylists: () -> Unit = {},
    onOpenGuide: () -> Unit = {},
    onOpenLibrary: () -> Unit = {},
    onOpenRecordings: () -> Unit = {},
    onOpenMultiwall: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    window: NovaWindow = NovaWindow.Expanded,
    modifier: Modifier = Modifier,
) {
    // Eylem yüzeyi TV ile ortak: kompakt taşma düğmesi de TV uzun basışı da
    // aynı kanalı seçer, diyaloglar iki yolda da aynı.
    var actionChannel by remember { mutableStateOf<Channel?>(null) }
    var numberChannel by remember { mutableStateOf<Channel?>(null) }

    when (window) {
        NovaWindow.Compact, NovaWindow.Medium -> CompactLive(
            state = state,
            channels = channels,
            window = window,
            onSelectGroup = onSelectGroup,
            onQueryChange = onQueryChange,
            onToggleFavorites = onToggleFavorites,
            onToggleGrid = onToggleGrid,
            onChannelClick = onChannelClick,
            onToggleFavorite = onToggleFavorite,
            onChannelFocused = onChannelFocused,
            onOpenPlaylists = onOpenPlaylists,
            onOpenActions = { actionChannel = it },
            modifier = modifier,
        )

        NovaWindow.Expanded -> TvLive(
            state = state,
            channels = channels,
            onSelectGroup = onSelectGroup,
            onQueryChange = onQueryChange,
            onToggleFavorites = onToggleFavorites,
            onToggleGrid = onToggleGrid,
            onChannelClick = onChannelClick,
            onToggleFavorite = onToggleFavorite,
            onChannelFocused = onChannelFocused,
            onOpenPlaylists = onOpenPlaylists,
            onOpenGuide = onOpenGuide,
            onOpenLibrary = onOpenLibrary,
            onOpenRecordings = onOpenRecordings,
            onOpenMultiwall = onOpenMultiwall,
            onOpenSearch = onOpenSearch,
            onOpenSettings = onOpenSettings,
            onActionChannel = { actionChannel = it },
            modifier = modifier,
        )
    }

    actionChannel?.let { channel ->
        ChannelActionsDialog(
            channel = channel,
            favorite = channel.id in state.favoriteIds,
            onFavorite = {
                onToggleFavorite(channel)
                actionChannel = null
            },
            onHideChannel = {
                onHideChannel(channel)
                actionChannel = null
            },
            onHideGroup = {
                onHideGroup(channel.groupName)
                actionChannel = null
            },
            onSetNumber = {
                actionChannel = null
                numberChannel = channel
            },
            onDismiss = { actionChannel = null },
        )
    }

    numberChannel?.let { channel ->
        ChannelNumberDialog(
            channel = channel,
            onConfirm = { number ->
                onSetNumber(channel, number)
                numberChannel = null
            },
            onDismiss = { numberChannel = null },
        )
    }
}

/** TV gövdesi: grup rayı solda, başlık ve kanal listesi/ızgara sağda. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvLive(
    state: LiveUiState,
    channels: LazyPagingItems<Channel>,
    onSelectGroup: (String?) -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleFavorites: () -> Unit,
    onToggleGrid: () -> Unit,
    onChannelClick: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onChannelFocused: (Channel?) -> Unit,
    onOpenPlaylists: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenRecordings: () -> Unit,
    onOpenMultiwall: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onActionChannel: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val railFocus = remember { FocusRequester() }
    val emptyFocus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    var pendingFirstFocus by remember { mutableStateOf(true) }
    var menuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(state.selectedGroup) {
        pendingFirstFocus = true
    }

    // Seçili grup kaybolduysa (ör. son izlenenler boşaldı) Tümü'ye dön.
    LaunchedEffect(state.groups, state.selectedGroup) {
        val selected = state.selectedGroup
        if (selected != null && state.groups.none { it.key == selected }) {
            onSelectGroup(null)
        }
    }

    // Liste boşsa odak kaybolmaz, grup rayına döner.
    LaunchedEffect(channels.itemCount, channels.loadState.refresh) {
        if (channels.itemCount == 0 && channels.loadState.refresh !is LoadState.Loading) {
            runCatching { railFocus.requestFocus() }
            pendingFirstFocus = false
        }
    }

    val currentGroup = state.selectedGroup
    val onRowFocused: (Channel) -> Unit = { channel ->
        onChannelFocused(channel)
        if (currentGroup == null || currentGroup == RECENT_GROUP_KEY || channel.groupName == currentGroup) {
            pendingFirstFocus = false
        }
    }

    NovaBackground(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp && event.key == Key.Back) {
                        runCatching { railFocus.requestFocus() }
                        true
                    } else {
                        false
                    }
                },
        ) {
            GroupRail(
                groups = state.groups,
                selectedGroup = state.selectedGroup,
                onSelect = onSelectGroup,
                firstItemFocusRequester = railFocus,
            )
            NovaRule(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight(),
                color = NovaColors.Border.copy(alpha = 0.5f),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 28.dp, end = 28.dp, top = 22.dp, bottom = 16.dp),
            ) {
                LiveHeader(
                    state = state,
                    onQueryChange = onQueryChange,
                    onToggleFavorites = onToggleFavorites,
                    onToggleGrid = onToggleGrid,
                    onOpenPlaylists = onOpenPlaylists,
                    onOpenGuide = onOpenGuide,
                    onOpenMenu = { menuOpen = true },
                )

                Spacer(Modifier.height(18.dp))

                // Hiç aktif kaynak yoksa paging akışı hiç yüklenmez; spinner'da
                // takılı kalmamak için boş durumu gruplardan anlarız.
                val noGroups = state.groups.isEmpty()
                val noSource = noGroups && channels.itemCount == 0
                val loading = channels.loadState.refresh is LoadState.Loading

                LaunchedEffect(noSource, state.ready) {
                    if (state.ready && noSource) {
                        // Kaynak yok: ilk D-pad odağı doğrudan "Kaynak ekle"ye gitsin.
                        runCatching { emptyFocus.requestFocus() }
                    }
                }

                when {
                    !state.ready || (loading && channels.itemCount == 0 && !noGroups) -> Box(
                        modifier = Modifier.fillMaxSize(),
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

                    channels.itemCount == 0 -> EmptyState(
                        noSource = noSource,
                        favoritesOnly = state.favoritesOnly,
                        focusRequester = emptyFocus,
                        onOpenPlaylists = onOpenPlaylists,
                    )

                    state.grid -> LazyVerticalGrid(
                        columns = GridCells.Fixed(TV_GRID_COLUMNS),
                        state = gridState,
                        modifier = Modifier.fillMaxSize().testTag("channel-grid"),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            count = channels.itemCount,
                            key = channels.itemKey { it.id },
                        ) { index ->
                            val channel = channels[index] ?: return@items
                            ChannelCard(
                                channel = channel,
                                favorite = channel.id in state.favoriteIds,
                                index = index,
                                onClick = { onChannelClick(channel) },
                                onLongClick = { onActionChannel(channel) },
                                onFocused = { onRowFocused(channel) },
                                autoFocus = pendingFirstFocus && index == 0 &&
                                    (currentGroup == null || currentGroup == RECENT_GROUP_KEY ||
                                        channel.groupName == currentGroup),
                            )
                        }
                    }

                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().testTag("channel-list"),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(
                            count = channels.itemCount,
                            key = channels.itemKey { it.id },
                        ) { index ->
                            val channel = channels[index] ?: return@items
                            ChannelRow(
                                channel = channel,
                                favorite = channel.id in state.favoriteIds,
                                index = index,
                                onClick = { onChannelClick(channel) },
                                onLongClick = { onActionChannel(channel) },
                                onFocused = { onRowFocused(channel) },
                                autoFocus = pendingFirstFocus && index == 0 &&
                                    (currentGroup == null || currentGroup == RECENT_GROUP_KEY ||
                                        channel.groupName == currentGroup),
                            )
                        }
                    }
                }

                state.error?.let { message ->
                    Text(
                        text = message,
                        fontSize = 14.sp,
                        color = NovaColors.Danger,
                        modifier = Modifier.padding(top = 8.dp).testTag("live-error"),
                    )
                }
            }
        }
    }

    if (menuOpen) {
        LiveMenuDialog(
            onGuide = {
                menuOpen = false
                onOpenGuide()
            },
            onLibrary = {
                menuOpen = false
                onOpenLibrary()
            },
            onPlaylists = {
                menuOpen = false
                onOpenPlaylists()
            },
            onRecordings = {
                menuOpen = false
                onOpenRecordings()
            },
            onMultiwall = {
                menuOpen = false
                onOpenMultiwall()
            },
            onSearch = {
                menuOpen = false
                onOpenSearch()
            },
            onSettings = {
                menuOpen = false
                onOpenSettings()
            },
            onDismiss = { menuOpen = false },
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LiveHeader(
    state: LiveUiState,
    onQueryChange: (String) -> Unit,
    onToggleFavorites: () -> Unit,
    onToggleGrid: () -> Unit,
    onOpenPlaylists: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenMenu: () -> Unit,
) {
    val groupCount = state.groups.firstOrNull {
        it.key == (state.selectedGroup ?: LiveUiState.ALL_GROUPS_KEY)
    }?.count
    Column(modifier = Modifier.fillMaxWidth()) {
        NovaLabel(if (state.favoritesOnly) "Favoriler" else "Canlı TV")
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = if (state.favoritesOnly) "Favoriler" else state.selectedGroupLabel,
                style = NovaType.Title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            groupCount?.let { count ->
                Text(
                    text = "$count kanal",
                    style = NovaType.Number.copy(color = NovaColors.TextDim),
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
        NovaPill(
            label = if (state.favoritesOnly) "★ Favoriler" else "☆ Favoriler",
            onClick = onToggleFavorites,
            selected = state.favoritesOnly,
            testTag = "toggle-favorites",
        )
        NovaPill(
            label = if (state.grid) "Liste" else "Izgara",
            onClick = onToggleGrid,
            testTag = "toggle-grid",
        )
        NovaPill(
            label = "Kaynaklar",
            onClick = onOpenPlaylists,
            testTag = "open-playlists",
        )
        NovaPill(
            label = "Menü",
            onClick = onOpenMenu,
            testTag = "open-menu",
        )

        // Arama en sağda: D-pad ile gezinirken metin alanına istemeden
        // girilmesin (odaklanınca klavye açılır).
        NovaTextFieldTheme {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = { Text("Ara", style = NovaType.Body.copy(color = NovaColors.TextDim)) },
                singleLine = true,
                shape = RoundedCornerShape(50),
                colors = novaTextFieldColors(),
                modifier = Modifier
                    .width(196.dp)
                    .tvFieldNavigation(horizontal = true)
                    .testTag("live-search"),
            )
        }
        }
    }
}

/**
 * Boş durumun hangi metni göstereceği: saf karar, test edilebilir.
 *
 * [query] boş değilse arama süzgeci kaynak yokluğundan önce gelir — telefonda
 * arama alanı her zaman görünür, boş sonuç "kaynak ekle" dememeli.
 * [noSource] yalnız `null`ken kaynak pili çizilmez; `false` bugünkü TV
 * davranışını (Kaynaklar pili) korur.
 */
internal fun emptyStateContent(
    noSource: Boolean,
    favoritesOnly: Boolean,
    query: String,
): EmptyStateContent = when {
    query.isNotBlank() -> EmptyStateContent(
        title = "«$query» için kanal yok",
        body = "Farklı bir kelimeyle arayın veya aramayı temizleyin.",
        action = EmptyStateAction.ClearQuery,
    )
    noSource -> EmptyStateContent(
        title = "Henüz kanal yok",
        body = "İzlemeye başlamak için bir Xtream, M3U veya Stalker kaynağı ekleyin.",
        action = EmptyStateAction.OpenPlaylists,
    )
    favoritesOnly -> EmptyStateContent(
        title = "Favori kanal yok",
        body = "Kanalların üzerinde uzun basıp favorilere ekleyin.",
        action = EmptyStateAction.None,
    )
    else -> EmptyStateContent(
        title = "Bu grupta kanal yok",
        body = "Başka bir grup seçin veya kaynaklarınızı yenileyin.",
        action = EmptyStateAction.OpenPlaylists,
    )
}

/** Boş durum metni ve altındaki tek eylem. */
internal data class EmptyStateContent(
    val title: String,
    val body: String,
    val action: EmptyStateAction,
)

/** Boş durum eylemi: kaynaklara git, aramayı temizle ya da eylem yok. */
internal enum class EmptyStateAction { OpenPlaylists, ClearQuery, None }

/**
 * Boş durum metni.
 *
 * [noSource] `false` bugünkü TV metnini aynen çizer; `null` ise kaynak pilini
 * hiç çizmez (kompakt yol kararını [emptyStateContent] verir ve pilini kendi
 * çizer). [bodyModifier] yolu kendi yazı genişliğini seçer: TV sabit 520 dp,
 * kompakt ekran genişliği. [focusRequester] yalnız TV'de dolu.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun EmptyState(
    noSource: Boolean?,
    favoritesOnly: Boolean,
    focusRequester: FocusRequester? = null,
    onOpenPlaylists: () -> Unit = {},
    modifier: Modifier = Modifier,
    bodyModifier: Modifier = Modifier.width(520.dp),
) {
    val content = emptyStateContent(
        noSource = noSource != false,
        favoritesOnly = favoritesOnly,
        query = "",
    )
    Column(
        modifier = modifier.fillMaxSize().padding(bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = content.title,
            style = NovaType.Display.copy(fontSize = 30.sp),
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("empty-state"),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = content.body,
            style = NovaType.BodyMuted,
            textAlign = TextAlign.Center,
            modifier = bodyModifier,
        )
        if (noSource == null) {
            // Kompakt yol: metin kararı ve pili burada değil çağıranda.
            return@Column
        }
        Spacer(Modifier.height(24.dp))
        NovaPill(
            label = if (noSource) "Kaynak ekle" else "Kaynaklar",
            onClick = onOpenPlaylists,
            accent = true,
            modifier = if (focusRequester != null) {
                Modifier.focusRequester(focusRequester)
            } else {
                Modifier
            },
            testTag = "empty-open-playlists",
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ChannelActionsDialog(
    channel: Channel,
    favorite: Boolean,
    onFavorite: () -> Unit,
    onHideChannel: () -> Unit,
    onHideGroup: () -> Unit,
    onSetNumber: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        NovaPanel {
            NovaLabel("Kanal")
            Column {
                Text(
                    text = channel.name,
                    style = NovaType.Title,
                    maxLines = 1,
                )
                Text(
                    text = channel.groupName.ifBlank { "Grup yok" },
                    style = NovaType.Small,
                )
            }
            NovaRule(Modifier.fillMaxWidth())
            NovaPill(
                label = if (favorite) "Favoriden çıkar" else "Favorilere ekle",
                onClick = onFavorite,
                modifier = Modifier.fillMaxWidth(),
                testTag = "action-favorite",
            )
            NovaPill(
                label = "Kanal numarası ata",
                onClick = onSetNumber,
                modifier = Modifier.fillMaxWidth(),
                testTag = "action-number",
            )
            NovaPill(
                label = "Kanalı gizle",
                onClick = onHideChannel,
                modifier = Modifier.fillMaxWidth(),
                testTag = "action-hide-channel",
            )
            NovaPill(
                label = "Grubu gizle",
                onClick = onHideGroup,
                modifier = Modifier.fillMaxWidth(),
                testTag = "action-hide-group",
            )
            NovaRule(Modifier.fillMaxWidth())
            NovaPill(
                label = "Vazgeç",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                testTag = "action-cancel",
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ChannelNumberDialog(
    channel: Channel,
    onConfirm: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember(channel.id) { mutableStateOf(channel.number?.toString().orEmpty()) }

    Dialog(onDismissRequest = onDismiss) {
        NovaPanel {
            NovaLabel("Kanal numarası")
            Text(
                text = channel.name,
                style = NovaType.Title,
                maxLines = 1,
            )
            NovaTextFieldTheme {
                OutlinedTextField(
                    value = value,
                    onValueChange = { input -> value = input.filter { it.isDigit() }.take(5) },
                    label = { Text("Numara") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = novaTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().tvFieldNavigation().testTag("number-input"),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NovaPill(
                    label = "Tamam",
                    onClick = { onConfirm(value.toIntOrNull()) },
                    accent = true,
                    testTag = "number-confirm",
                )
                NovaPill(label = "Vazgeç", onClick = onDismiss)
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LiveMenuDialog(
    onGuide: () -> Unit,
    onLibrary: () -> Unit,
    onPlaylists: () -> Unit,
    onRecordings: () -> Unit,
    onMultiwall: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        NovaPanel(width = 380.dp) {
            NovaLabel("Menü")
            NovaPill(
                label = "Kütüphane",
                onClick = onLibrary,
                modifier = Modifier.fillMaxWidth(),
                testTag = "open-library",
            )
            NovaPill(
                label = "Rehber",
                onClick = onGuide,
                modifier = Modifier.fillMaxWidth(),
                testTag = "open-guide",
            )
            NovaPill(
                label = "Ara",
                onClick = onSearch,
                modifier = Modifier.fillMaxWidth(),
                testTag = "open-search",
            )
            NovaPill(
                label = "Ayarlar",
                onClick = onSettings,
                modifier = Modifier.fillMaxWidth(),
                testTag = "open-settings",
            )
            NovaPill(
                label = "Çoklu ekran",
                onClick = onMultiwall,
                modifier = Modifier.fillMaxWidth(),
                testTag = "open-multiwall",
            )
            NovaPill(
                label = "Kayıtlar",
                onClick = onRecordings,
                modifier = Modifier.fillMaxWidth(),
                testTag = "open-recordings",
            )
            NovaPill(
                label = "Kaynaklar",
                onClick = onPlaylists,
                modifier = Modifier.fillMaxWidth(),
                testTag = "open-playlists",
            )
            NovaRule(Modifier.fillMaxWidth())
            NovaPill(
                label = "Kapat",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                testTag = "menu-dismiss",
            )
        }
    }
}
