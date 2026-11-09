package dev.anon.novatv.feature.live

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.ui.focus.tvFieldNavigation
import dev.anon.novatv.ui.theme.NovaTextFieldTheme

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
    modifier: Modifier = Modifier,
) {
    val railFocus = remember { FocusRequester() }
    val emptyFocus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    var pendingFirstFocus by remember { mutableStateOf(true) }
    var actionChannel by remember { mutableStateOf<Channel?>(null) }
    var numberChannel by remember { mutableStateOf<Channel?>(null) }

    LaunchedEffect(state.selectedGroup) {
        pendingFirstFocus = true
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
        if (currentGroup == null || channel.groupName == currentGroup) {
            pendingFirstFocus = false
        }
    }

    Row(
        modifier = modifier
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

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
            LiveHeader(
                state = state,
                onQueryChange = onQueryChange,
                onToggleFavorites = onToggleFavorites,
                onToggleGrid = onToggleGrid,
                onOpenPlaylists = onOpenPlaylists,
            )

            Spacer(Modifier.height(16.dp))

            val noSource = state.groups.isEmpty() && channels.itemCount == 0
            val loading = channels.loadState.refresh is LoadState.Loading

            LaunchedEffect(noSource) {
                if (noSource) {
                    // Kaynak yok: ilk D-pad odağı doğrudan "Kaynak ekle"ye gitsin.
                    runCatching { emptyFocus.requestFocus() }
                }
            }

            when {
                channels.itemCount == 0 && (noSource || !loading) -> Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Bu grupta kanal yok",
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("empty-state"),
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onOpenPlaylists,
                        modifier = Modifier
                            .focusRequester(emptyFocus)
                            .testTag("empty-open-playlists"),
                    ) {
                        Text("Kaynak ekle veya yenile")
                    }
                }

                loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                state.grid -> LazyVerticalGrid(
                    columns = GridCells.Fixed(GRID_COLUMNS),
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
                            onLongClick = { actionChannel = channel },
                            onFocused = { onRowFocused(channel) },
                            autoFocus = pendingFirstFocus && index == 0 &&
                                (currentGroup == null || channel.groupName == currentGroup),
                        )
                    }
                }

                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().testTag("channel-list"),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
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
                            onLongClick = { actionChannel = channel },
                            onFocused = { onRowFocused(channel) },
                            autoFocus = pendingFirstFocus && index == 0 &&
                                (currentGroup == null || channel.groupName == currentGroup),
                        )
                    }
                }
            }

            state.error?.let { message ->
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp).testTag("live-error"),
                )
            }
        }
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LiveHeader(
    state: LiveUiState,
    onQueryChange: (String) -> Unit,
    onToggleFavorites: () -> Unit,
    onToggleGrid: () -> Unit,
    onOpenPlaylists: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = onToggleFavorites,
            modifier = Modifier.testTag("toggle-favorites"),
        ) {
            Text(if (state.favoritesOnly) "★ Favoriler" else "☆ Favoriler")
        }

        Button(
            onClick = onToggleGrid,
            modifier = Modifier.testTag("toggle-grid"),
        ) {
            Text(if (state.grid) "Liste" else "Izgara")
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onOpenPlaylists,
            modifier = Modifier.testTag("open-playlists"),
        ) {
            Text("Kaynaklar")
        }

        // Arama en sağda: D-pad ile gezinirken metin alanına istemeden
        // girilmesin (odaklanınca klavye açılır).
        NovaTextFieldTheme {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = { Text("Ara") },
                singleLine = true,
                modifier = Modifier.width(220.dp).tvFieldNavigation(horizontal = true).testTag("live-search"),
            )
        }
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
        Column(
            modifier = Modifier
                .width(420.dp)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = channel.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = channel.groupName,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onFavorite, modifier = Modifier.fillMaxWidth().testTag("action-favorite")) {
                Text(if (favorite) "Favoriden çıkar" else "Favorilere ekle")
            }
            Button(onClick = onSetNumber, modifier = Modifier.fillMaxWidth().testTag("action-number")) {
                Text("Kanal numarası ata")
            }
            Button(onClick = onHideChannel, modifier = Modifier.fillMaxWidth().testTag("action-hide-channel")) {
                Text("Kanalı gizle")
            }
            Button(onClick = onHideGroup, modifier = Modifier.fillMaxWidth().testTag("action-hide-group")) {
                Text("Grubu gizle")
            }
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().testTag("action-cancel")) {
                Text("Vazgeç")
            }
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
        Column(
            modifier = Modifier
                .width(420.dp)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Kanal numarası: ${channel.name}",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            NovaTextFieldTheme {
                OutlinedTextField(
                    value = value,
                    onValueChange = { input -> value = input.filter { it.isDigit() }.take(5) },
                    label = { Text("Numara") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().tvFieldNavigation().testTag("number-input"),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onConfirm(value.toIntOrNull()) },
                    modifier = Modifier.testTag("number-confirm"),
                ) { Text("Tamam") }
                Button(onClick = onDismiss) { Text("Vazgeç") }
            }
        }
    }
}

private const val GRID_COLUMNS = 4
