package dev.anon.novatv.ui.layout

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.anon.novatv.ui.theme.NovaColors
import dev.anon.novatv.ui.theme.NovaType

/** Gezinme sekmeleri: kimlik, etiket ve test etiketi çağırandan gelir. */
data class NovaTab(val key: String, val label: String, val testTag: String)

/** İskeletin gezinme yeri: geniş pencerede yan ray, dar pencerede alt çubuk. */
enum class NovaNavMode { Rail, Bottom }

/** Yalnız `Expanded` (≥840 dp) raya geçer; tablet alt çubuğu korur. */
fun navigationMode(window: NovaWindow): NovaNavMode = when (window) {
    NovaWindow.Expanded -> NovaNavMode.Rail
    NovaWindow.Compact, NovaWindow.Medium -> NovaNavMode.Bottom
}

/**
 * Ortak kabuk: içerik + gezinme.
 *
 * `Bottom`: içerik üstte, eşit genişlikte beş dokunma hedefi altta.
 * `Rail`: içerik sağda, solda ince ray — `GroupRail`'in dikey akışı.
 *
 * Seçili sekme krem etiket + ray tarafındaki 3 dp kırmızı damga ile anlatılır;
 * odak dolgusu yalnız D-pad/klavye odaklanmasında görünür.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NovaScaffold(
    window: NovaWindow,
    tabs: List<NovaTab>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NovaColors.Background),
    ) {
        when (navigationMode(window)) {
            NovaNavMode.Bottom -> {
                val barHeight = BottomBarHeight
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f)) {
                        content(
                            PaddingValues(
                                start = 0.dp,
                                top = 0.dp,
                                end = 0.dp,
                                bottom = barHeight,
                            ),
                        )
                    }
                    BottomTabBar(
                        tabs = tabs,
                        selected = selected,
                        onSelect = onSelect,
                        barHeight = barHeight,
                    )
                }
            }

            NovaNavMode.Rail -> {
                val railWidth = RailWidth
                Row(Modifier.fillMaxSize()) {
                    NovaRail(tabs = tabs, selected = selected, onSelect = onSelect)
                    Box(Modifier.weight(1f)) {
                        content(
                            PaddingValues(
                                start = railWidth,
                                top = 0.dp,
                                end = 0.dp,
                                bottom = 0.dp,
                            ),
                        )
                    }
                }
            }
        }
    }
}

/** Ray genişliği; dokunma hedefi genişliğinden (≥48 dp) geniş. */
private val RailWidth = 112.dp

/** Alt çubuk yüksekliği; sekme öğesi ≥56 dp kuralına uyar. */
private val BottomBarHeight = 64.dp

/** Alt çubuktaki dokunma hedefi yüksekliği. */
private val BottomItemHeight = 56.dp

/** Seçim damgası: 3 dp kırmızı gösterge. */
private val IndicatorThickness = 3.dp

/** Ray öğesi yüksekliği: dokunma hedefi ≥48 dp, kural ≥56 dp'yi aşar. */
private val RailItemHeight = 72.dp

/** Sekme glifi: kutu, yuvarlatılmış kare. */
private val GlyphSize = 18.dp

/**
 * Alt çubuk: `tabs` eşit pay alır, her biri en az 56 dp yüksek.
 * İnce üst ayırıcı çubuğu içerikten koparır.
 */
@Composable
private fun BottomTabBar(
    tabs: List<NovaTab>,
    selected: String,
    onSelect: (String) -> Unit,
    barHeight: Dp,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeight)
            .background(NovaColors.BackgroundRaised)
            .testTag("nova-bottom-bar"),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(NovaColors.Border),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                BottomTabItem(
                    tab = tab,
                    selected = tab.key == selected,
                    onClick = { onSelect(tab.key) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomTabItem(
    tab: NovaTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TabSurface(
        tab = tab,
        selected = selected,
        onClick = onClick,
        minHeight = BottomItemHeight,
        horizontal = false,
        modifier = modifier,
    )
}

/**
 * Yan ray: `GroupRail` gibi dikey liste; her sekme en az 72 dp yüksek,
 * seçim sol tarafa yerleşen 3 dp kırmızı damga ile gösterilir.
 */
@Composable
private fun NovaRail(
    tabs: List<NovaTab>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(RailWidth)
            .fillMaxHeight()
            .background(NovaColors.BackgroundRaised)
            .padding(vertical = 16.dp, horizontal = 8.dp)
            .testTag("nova-rail"),
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(tabs, key = { it.key }) { tab ->
                RailTabItem(
                    tab = tab,
                    selected = tab.key == selected,
                    onClick = { onSelect(tab.key) },
                )
            }
        }
    }
}

@Composable
private fun RailTabItem(
    tab: NovaTab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    TabSurface(
        tab = tab,
        selected = selected,
        onClick = onClick,
        minHeight = RailItemHeight,
        horizontal = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Soyut glif: yuvarlatılmış kare; ikon kütüphanesi yok, yazının tonunu izler. */
@Composable
private fun TabGlyph(color: Color, alpha: Float = 0.75f) {
    Box(
        modifier = Modifier
            .size(GlyphSize)
            .background(color.copy(alpha = alpha), RoundedCornerShape(5.dp)),
    )
}

@Composable
private fun TabLabel(text: String, color: Color, maxLines: Int) {
    Text(
        text = text.uppercase(),
        style = NovaType.Label.copy(color = color),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
    )
}

/**
 * Tek sekme yüzeyi: alt çubuk ve ray aynı görsel dili paylaşır, yalnız
 * düzen farklıdır (`horizontal`: glif yanında yatay, değilse üstte).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TabSurface(
    tab: NovaTab,
    selected: Boolean,
    onClick: () -> Unit,
    minHeight: Dp,
    horizontal: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    // Odak dolgusu yalnız D-pad/klavye odağında görünür; dokunmada yok.
    var focused by remember { mutableStateOf(false) }
    val contentColor by animateColorAsState(
        targetValue = when {
            focused -> NovaColors.FocusInk
            selected -> NovaColors.Text
            else -> NovaColors.TextMuted
        },
        label = "nova-tab-content",
    )
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = contentColor,
            focusedContainerColor = NovaColors.Focus,
            focusedContentColor = NovaColors.FocusInk,
            pressedContainerColor = NovaColors.SurfacePressed,
            pressedContentColor = NovaColors.Text,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, Color.Transparent), shape = shape),
            focusedBorder = Border(BorderStroke(2.dp, NovaColors.Focus), shape = shape),
            pressedBorder = Border(BorderStroke(1.dp, NovaColors.Border), shape = shape),
        ),
        scale = ClickableSurfaceDefaults.scale(scale = 1f, focusedScale = 1f, pressedScale = 0.98f),
        modifier = modifier
            .heightIn(min = minHeight)
            .widthIn(min = 48.dp)
            .onFocusChanged { focused = it.isFocused }
            .semantics {
                this.selected = selected
                contentDescription = tab.label
            }
            .testTag(tab.testTag),
    ) {
        if (horizontal) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Seçim damgası: yalnız seçilide, 3 dp kırmızı.
                Indicator(
                    modifier = Modifier
                        .width(IndicatorThickness)
                        .height(28.dp),
                    selected = selected,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TabGlyph(color = contentColor, alpha = 1f)
                    Spacer(Modifier.height(6.dp))
                    TabLabel(text = tab.label, color = contentColor, maxLines = 2)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                TabGlyph(color = contentColor, alpha = 1f)
                Spacer(Modifier.height(4.dp))
                TabLabel(text = tab.label, color = contentColor, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Indicator(
                    modifier = Modifier
                        .width(16.dp)
                        .height(IndicatorThickness),
                    selected = selected,
                )
            }
        }
    }
}

/** Seçili sekmenin kırmızı damgası; seçili değilken yer tutar. */
@Composable
private fun Indicator(modifier: Modifier, selected: Boolean) {
    Box(
        modifier = modifier.background(
            color = if (selected) NovaColors.Accent else Color.Transparent,
            shape = CircleShape,
        ),
    )
}
