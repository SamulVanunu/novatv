package dev.anon.novatv.feature.multiwall

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import dev.anon.novatv.core.player.PlaybackController
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
 * Çoklu ekranın telefon/tablet düzeni.
 *
 * Her zaman iki kutu, alt alta ve 16:9; kullanıcı 4'lü düzeni seçerse
 * kutuların üstünde "Telefonda küçük" rozeti çıkar, yine iki kutu çizilir.
 * Kutu dokunuşı, kanal seçici ve temizleme TV'dekiyle aynı davranır;
 * oynatıcı denetleyicileri [MultiWallScreen]'teki ortak listeden gelir.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun CompactMultiWall(
    state: MultiWallUiState,
    controllers: List<PlaybackController>,
    onLayout: (Int) -> Unit,
    onFocus: (Int) -> Unit,
    onOpenPicker: (Int) -> Unit,
    onClear: (Int) -> Unit,
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
                // Başlık ve düzen düğmeleri ayrı satırda: dar ekranda üste binmesin.
                Column {
                    NovaLabel("Çoklu ekran")
                    Text(
                        text = "Ses: ${state.panes.getOrNull(state.focused)?.name ?: "kutu ${state.focused + 1}"}",
                        style = NovaType.Title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(spacing.gap))
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.gap)) {
                    NovaPill(
                        label = "2'li",
                        onClick = { onLayout(2) },
                        selected = state.layout == 2,
                        modifier = Modifier.heightIn(min = 48.dp),
                        testTag = "multiwall-2",
                    )
                    NovaPill(
                        label = "4'lü",
                        onClick = { onLayout(4) },
                        selected = state.layout == 4,
                        modifier = Modifier.heightIn(min = 48.dp),
                        testTag = "multiwall-4",
                    )
                    NovaPill(
                        label = "Canlı TV",
                        onClick = onBack,
                        modifier = Modifier.heightIn(min = 48.dp),
                        testTag = "multiwall-back",
                    )
                }

                Spacer(Modifier.height(spacing.gap))
                NovaRule(Modifier.fillMaxWidth())

                if (state.layout == 4) {
                    Spacer(Modifier.height(spacing.gap))
                    NovaBadge(text = "Telefonda küçük", color = NovaColors.Warning)
                }

                Spacer(Modifier.height(spacing.gap))

                // İki kutu 16:9 alt alta; dar dikey ekranda liste kaydırılır.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(spacing.gap),
                ) {
                    CompactPane(
                        index = 0,
                        state = state,
                        controller = controllers[0],
                        onFocus = onFocus,
                        onOpenPicker = onOpenPicker,
                        onClear = onClear,
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                    )
                    CompactPane(
                        index = 1,
                        state = state,
                        controller = controllers[1],
                        onFocus = onFocus,
                        onOpenPicker = onOpenPicker,
                        onClear = onClear,
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                    )
                }

                state.message?.let { message ->
                    Text(
                        text = message,
                        style = NovaType.Small.copy(color = NovaColors.Warning),
                        modifier = Modifier.padding(top = spacing.gap).testTag("multiwall-message"),
                    )
                }
            }
        }
    }
}

/**
 * Kompakt kutu: 16:9 video alanı, üstte kanal şeridi, sağ altta temizleme.
 *
 * TV'deki `Surface` yerine [NovaTapSurface] kullanılır: tv yüzeyi yalnız
 * D-pad Enter'ı işler, parmak olayı işlemez. İlk dokunuş kutuya odak/ses
 * verir, odaktayken ikinci dokunuş kanal seçiciyi açar — TV ile aynı akış.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CompactPane(
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
    val spacing = rememberNovaSpacing()
    val shape = RoundedCornerShape(10.dp)

    NovaTapSurface(
        onClick = {
            if (focused) onOpenPicker(index) else onFocus(index)
        },
        modifier = modifier.testTag("multiwall-pane-$index"),
        shape = shape,
        containerColor = Color.Black,
        focusedContainerColor = Color.Black,
        pressedContainerColor = Color.Black,
        border = BorderStroke(1.dp, NovaColors.Border),
        focusedBorder = BorderStroke(2.dp, NovaColors.Focus),
        pressedBorder = BorderStroke(2.dp, NovaColors.Focus),
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
                    .padding(horizontal = spacing.gap, vertical = 6.dp),
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
                    modifier = Modifier.align(Alignment.BottomEnd).padding(spacing.gap),
                    testTag = "multiwall-clear-$index",
                )
            }
        }
    }
}
