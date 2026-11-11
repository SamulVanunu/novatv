package dev.anon.novatv.feature.multiwall

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.core.player.PlaybackController
import dev.anon.novatv.ui.component.NovaBackground
import dev.anon.novatv.ui.component.NovaLabel
import dev.anon.novatv.ui.component.NovaPanel
import dev.anon.novatv.ui.component.NovaPill
import dev.anon.novatv.ui.component.NovaRule
import dev.anon.novatv.ui.focus.tvFieldNavigation
import dev.anon.novatv.ui.layout.NovaWindow
import dev.anon.novatv.ui.theme.NovaColors
import dev.anon.novatv.ui.theme.NovaTextFieldTheme
import dev.anon.novatv.ui.theme.NovaType
import dev.anon.novatv.ui.theme.novaTextFieldColors

/**
 * 2 veya 4 kanalı aynı anda gösterir; ses yalnızca odaktaki kutudadır.
 * D-pad: yön tuşları kutular arasında gezer, OK odaktaki kutunun kanal
 * seçicisini açar, Geri ekranı kapatır.
 *
 * Pencere sınıfı düzeni seçer: `Expanded` TV'deki yatay/dikey ızgarayı,
 * dar sınıflar [CompactMultiWall] ile iki üst üste kutuyu çizer.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MultiWallScreen(
    state: MultiWallUiState,
    onLayout: (Int) -> Unit,
    onFocus: (Int) -> Unit,
    onOpenPicker: (Int) -> Unit,
    onClosePicker: () -> Unit,
    onQuery: (String) -> Unit,
    onAssign: (Int, Channel) -> Unit,
    onClear: (Int) -> Unit,
    onBack: () -> Unit,
    window: NovaWindow = NovaWindow.Expanded,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val controllers = remember {
        List(MultiWallViewModel.MAX_PANES) {
            PlaybackController(context).apply { setBufferMs(LOW_BUFFER_MS) }
        }
    }
    val prepared = remember { HashMap<Int, String>() }

    DisposableEffect(Unit) {
        onDispose { controllers.forEach { runCatching { it.release() } } }
    }

    // Kompakta yalnız çizilen kutular hazırlanır: telefonda 4'lü düzenle bile
    // ekranda iki kutu var, üçüncüsü/dördüncüsü akış boşa açmasın.
    val paneLimit = paneLimitFor(window, state.visiblePanes)

    LaunchedEffect(state.streams, state.layout, paneLimit) {
        controllers.forEachIndexed { index, controller ->
            val stream = state.streams.firstOrNull { it.pane == index }
            if (stream == null || index >= paneLimit) {
                if (prepared.remove(index) != null) {
                    controller.pause()
                }
            } else if (prepared[index] != stream.url) {
                controller.prepare(stream.url, stream.headers)
                prepared[index] = stream.url
            }
        }
    }

    // Pencere daralınca çizilen kutu sayısı düşer; odak dışarıda kalırsa
    // bütün kutular sessizleşir ve başlık görünmeyen kutuyu adlandırır.
    LaunchedEffect(paneLimit) {
        if (state.focused >= paneLimit) onFocus(0)
    }

    LaunchedEffect(state.focused, state.streams, state.layout, paneLimit) {
        controllers.forEachIndexed { index, controller ->
            val audible = index == state.focused &&
                index < paneLimit &&
                state.streams.any { it.pane == index }
            runCatching { controller.player.volume = if (audible) 1f else 0f }
        }
    }

    when (window) {
        NovaWindow.Expanded -> TvMultiWall(
            state = state,
            controllers = controllers,
            onLayout = onLayout,
            onFocus = onFocus,
            onOpenPicker = onOpenPicker,
            onClear = onClear,
            onBack = onBack,
            modifier = modifier,
        )

        NovaWindow.Compact, NovaWindow.Medium -> CompactMultiWall(
            state = state,
            controllers = controllers,
            onLayout = onLayout,
            onFocus = onFocus,
            onOpenPicker = onOpenPicker,
            onClear = onClear,
            onBack = onBack,
            modifier = modifier,
        )
    }

    state.pickerPane?.let { pane ->
        ChannelPickerDialog(
            title = "Kutu ${pane + 1} · kanal",
            state = state,
            touch = window != NovaWindow.Expanded,
            onQuery = onQuery,
            onSelect = { channel -> onAssign(pane, channel) },
            onClear = { onClear(pane) },
            onDismiss = onClosePicker,
        )
    }
}

/** TV gövdesi: 2'li düzen yatay, 4'lü düzen iki satır. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvMultiWall(
    state: MultiWallUiState,
    controllers: List<PlaybackController>,
    onLayout: (Int) -> Unit,
    onFocus: (Int) -> Unit,
    onOpenPicker: (Int) -> Unit,
    onClear: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NovaBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    NovaLabel("Çoklu ekran")
                    Text(
                        text = "Ses: ${state.panes.getOrNull(state.focused)?.name ?: "kutu ${state.focused + 1}"}",
                        style = NovaType.Title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                NovaPill(
                    label = "2'li",
                    onClick = { onLayout(2) },
                    selected = state.layout == 2,
                    testTag = "multiwall-2",
                )
                NovaPill(
                    label = "4'lü",
                    onClick = { onLayout(4) },
                    selected = state.layout == 4,
                    testTag = "multiwall-4",
                )
                NovaPill(label = "Canlı TV", onClick = onBack, testTag = "multiwall-back")
            }

            Spacer(Modifier.height(10.dp))
            NovaRule(Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))

            if (state.layout == 2) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Pane(
                        index = 0,
                        state = state,
                        controller = controllers[0],
                        onFocus = onFocus,
                        onOpenPicker = onOpenPicker,
                        onClear = onClear,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    Pane(
                        index = 1,
                        state = state,
                        controller = controllers[1],
                        onFocus = onFocus,
                        onOpenPicker = onOpenPicker,
                        onClear = onClear,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Pane(0, state, controllers[0], onFocus, onOpenPicker, onClear, Modifier.weight(1f).fillMaxHeight())
                        Pane(1, state, controllers[1], onFocus, onOpenPicker, onClear, Modifier.weight(1f).fillMaxHeight())
                    }
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Pane(2, state, controllers[2], onFocus, onOpenPicker, onClear, Modifier.weight(1f).fillMaxHeight())
                        Pane(3, state, controllers[3], onFocus, onOpenPicker, onClear, Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }

            state.message?.let { message ->
                Text(
                    text = message,
                    style = NovaType.Small.copy(color = NovaColors.Warning),
                    modifier = Modifier.padding(top = 4.dp).testTag("multiwall-message"),
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun Pane(
    index: Int,
    state: MultiWallUiState,
    controller: PlaybackController,
    onFocus: (Int) -> Unit,
    onOpenPicker: (Int) -> Unit,
    onClear: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val channel = state.panes.getOrNull(index)
    val stream = state.streamOf(index)
    val focused = state.focused == index
    val shape = RoundedCornerShape(10.dp)

    Surface(
        onClick = {
            if (focused) onOpenPicker(index) else onFocus(index)
        },
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Black,
            contentColor = NovaColors.Text,
            focusedContainerColor = Color.Black,
            focusedContentColor = NovaColors.Text,
            pressedContainerColor = Color.Black,
            pressedContentColor = NovaColors.Text,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, NovaColors.Border), shape = shape),
            focusedBorder = Border(BorderStroke(2.dp, NovaColors.Focus), shape = shape),
            pressedBorder = Border(BorderStroke(2.dp, NovaColors.Focus), shape = shape),
        ),
        scale = ClickableSurfaceDefaults.scale(1f, 1f, 1f),
        modifier = modifier.testTag("multiwall-pane-$index"),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (stream != null) {
                AndroidView(
                    factory = { context ->
                        PlayerView(context).apply {
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                            player = controller.player
                        }
                    },
                    update = { view -> view.player = controller.player },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = channel?.name ?: "Kutu ${index + 1} · boş",
                    style = NovaType.Small.copy(fontSize = 13.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (focused) {
                    Text(
                        text = if (channel == null) "Kanal seç" else "Ses açık",
                        style = NovaType.Number.copy(fontSize = 11.sp, color = NovaColors.Accent),
                    )
                }
            }

            if (channel != null) {
                NovaPill(
                    label = "Kanalı kaldır",
                    onClick = { onClear(index) },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                    testTag = "multiwall-clear-$index",
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ChannelPickerDialog(
    title: String,
    state: MultiWallUiState,
    onQuery: (String) -> Unit,
    onSelect: (Channel) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
    touch: Boolean = false,
) {
    val firstChannelFocus = remember { FocusRequester() }
    LaunchedEffect(state.channels) {
        if (state.channels.isNotEmpty()) {
            runCatching { firstChannelFocus.requestFocus() }
        }
    }

    // Panel sabit 520 dp: 411 dp telefonda pencere taşıyor, "Kapat" kırpılıyordu.
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val panelWidth = minOf(520.dp, screenWidth - 32.dp)
    Dialog(onDismissRequest = onDismiss) {
        NovaPanel(width = panelWidth) {
            NovaLabel(title)
            NovaTextFieldTheme {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQuery,
                    placeholder = {
                        Text("Kanal ara", style = NovaType.Body.copy(color = NovaColors.TextDim))
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    colors = novaTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().tvFieldNavigation(horizontal = true),
                )
            }
            if (state.loading) {
                Text("Kanallar yükleniyor", style = NovaType.BodyMuted)
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(360.dp).testTag("multiwall-picker"),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(items = state.pickerChannels, key = { _, it -> it.id }) { index, channel ->
                    NovaPill(
                        label = channel.name,
                        onClick = { onSelect(channel) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (touch) Modifier.heightIn(min = 48.dp) else Modifier)
                            .then(
                                if (index == 0) Modifier.focusRequester(firstChannelFocus)
                                else Modifier,
                            ),
                        testTag = "multiwall-channel-${channel.id}",
                    )
                }
            }
            NovaRule(Modifier.fillMaxWidth())
            PickerActions(touch = touch, onClear = onClear, onDismiss = onDismiss)
        }
    }
}

/**
 * Seçici alt eylemleri. Dokunmatikte yan yana iki düğme 411 dp panelde
 * "Kutuyu bo…" diye kırpılıyordu; dikey yığılır ve 48 dp tabanına oturur.
 */
@Composable
private fun PickerActions(
    touch: Boolean,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (touch) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NovaPill(
                label = "Kutuyu boşalt",
                onClick = onClear,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            )
            NovaPill(
                label = "Kapat",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            )
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NovaPill(
                label = "Kutuyu boşalt",
                onClick = onClear,
                modifier = Modifier.weight(1f),
            )
            NovaPill(
                label = "Kapat",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Çizilen kutu sayısı. Telefonda dörtlü düzen seçilse bile iki kutu çizilir;
 * açılmayan akışlar boşuna hazırlanmasın.
 */
internal fun paneLimitFor(window: NovaWindow, visiblePanes: Int): Int =
    if (window == NovaWindow.Expanded) visiblePanes else minOf(visiblePanes, 2)

private const val LOW_BUFFER_MS = 8_000
