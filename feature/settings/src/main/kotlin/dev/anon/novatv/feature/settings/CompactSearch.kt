package dev.anon.novatv.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import dev.anon.novatv.core.sync.SearchHit
import dev.anon.novatv.core.sync.SearchKind
import dev.anon.novatv.ui.component.NovaBadge
import dev.anon.novatv.ui.component.NovaLabel
import dev.anon.novatv.ui.component.NovaPill
import dev.anon.novatv.ui.component.NovaRule
import dev.anon.novatv.ui.component.NovaTapSurface
import dev.anon.novatv.ui.layout.rememberNovaSpacing
import dev.anon.novatv.ui.theme.NovaColors
import dev.anon.novatv.ui.theme.NovaTextFieldTheme
import dev.anon.novatv.ui.theme.NovaType
import dev.anon.novatv.ui.theme.novaTextFieldColors

/**
 * Arama ekranının telefon/tablet düzeni.
 *
 * TV'de alan açılışta odak ister (kumanda yazar); telefonda odak istenmez,
 * ilk dokunuşta sistem klavyesi açılır. Sayfa [imePadding] ile klavyenin
 * üstüne kalkar, sonuç listesi kaydırılabilir kalır.
 *
 * Tek sütun: her sonuç tam genişlikte, en az 56 dp bir yüzey; başlık üstte,
 * alt bilgi altta, tür rozeti sağda. Filtre çipleri yatay kaydırılır.
 *
 * Boş durum metni TV ile aynıdır: yazılmamış sorgu "Aramaya hazır",
 * eşleşmeyen sorgu "Sonuç yok". "Kaynak yok" bu ekranda ayrı bir durum
 * değildir; boş sonuç her zaman sorgunun eşleşmediğini söyler.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun CompactSearch(
    state: SearchUiState,
    onQuery: (String) -> Unit,
    onFilter: (SearchKind?) -> Unit,
    onSelect: (SearchHit) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = rememberNovaSpacing()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .imePadding()
                .padding(
                    start = spacing.screen,
                    end = spacing.screen,
                    top = spacing.screen,
                    bottom = spacing.screen,
                )
                .testTag("search-compact"),
            verticalArrangement = Arrangement.spacedBy(spacing.gap),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.gap),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    NovaLabel("Ara")
                    Text(
                        text = when {
                            state.query.isBlank() -> "Kanal, film, dizi, program"
                            else -> "${state.visible.size} sonuç"
                        },
                        style = NovaType.Title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                NovaPill(
                    label = "Canlı TV",
                    onClick = onBack,
                    modifier = Modifier.heightIn(min = CompactSearchControlHeight),
                    testTag = "search-back",
                )
            }

            NovaTextFieldTheme {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQuery,
                    placeholder = {
                        Text("Yazmaya başlayın", style = NovaType.Body.copy(color = NovaColors.TextDim))
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Search,
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboard?.hide()
                            focusManager.clearFocus()
                        },
                    ),
                    colors = novaTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = CompactSearchControlHeight)
                        .testTag("search-field"),
                )
            }

            // Filtreler beş çip: 411 dp'de sığmaz, yatay kaydırılır.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CompactFilterPill("Tümü", null, state, onFilter)
                CompactFilterPill("Kanallar", SearchKind.CHANNEL, state, onFilter)
                CompactFilterPill("Filmler", SearchKind.MOVIE, state, onFilter)
                CompactFilterPill("Diziler", SearchKind.SERIES, state, onFilter)
                CompactFilterPill("Rehber", SearchKind.PROGRAM, state, onFilter)
            }

            NovaRule(Modifier.fillMaxWidth())

            when {
                state.query.isBlank() -> CompactSearchMessage(
                    copy = compactSearchIdle(),
                    tag = "search-idle",
                )

                state.visible.isEmpty() -> CompactSearchMessage(
                    copy = compactSearchEmpty(state.query),
                    tag = "search-empty",
                )

                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("search-results"),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    items(
                        items = state.visible,
                        key = { "${it.kind}-${it.id}-${it.startsAt ?: 0}" },
                    ) { hit ->
                        CompactSearchRow(hit = hit, onClick = { onSelect(hit) })
                    }
                }
            }
        }
    }
}

/** Filtre çipi: 48 dp hedef, TV etiketleri ve tag'leri aynı. */
@Composable
private fun CompactFilterPill(
    label: String,
    kind: SearchKind?,
    state: SearchUiState,
    onFilter: (SearchKind?) -> Unit,
) {
    NovaPill(
        label = label,
        onClick = { onFilter(kind) },
        selected = state.filter == kind,
        modifier = Modifier.heightIn(min = CompactSearchControlHeight),
        testTag = "search-filter-${kind?.name ?: "ALL"}",
    )
}

/**
 * Sonuç satırı: tek dokunma yüzeyi partiyi seçer. TV'de başlık bir pill,
 * alt bilgi ve tür ayrı sütunlardır; 411 dp'de üçü yan yana sığmaz. Burada
 * başlık üstte (iki satıra kadar), alt bilgi altta, tür rozeti sağda.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CompactSearchRow(hit: SearchHit, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    NovaTapSurface(
        onClick = onClick,
        shape = shape,
        containerColor = Color.Transparent,
        contentColor = NovaColors.Text,
        focusedContainerColor = NovaColors.SurfacePressed,
        focusedContentColor = NovaColors.Text,
        border = null,
        focusedBorder = BorderStroke(2.dp, NovaColors.Focus),
        focusedScale = 1f,
        pressedScale = 0.995f,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = CompactSearchRowHeight)
            .testTag("search-hit-${hit.kind.name}-${hit.id}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = hit.title,
                    style = NovaType.Body.copy(fontSize = 16.sp, lineHeight = 21.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                hit.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = NovaType.Small.copy(color = NovaColors.TextMuted),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            CompactKindBadge(hit.kind)
        }
    }
}

/** Tür rozeti: TV ile aynı etiketler ve renkler. */
@Composable
private fun CompactKindBadge(kind: SearchKind) {
    NovaBadge(
        text = when (kind) {
            SearchKind.CHANNEL -> "KANAL"
            SearchKind.MOVIE -> "FİLM"
            SearchKind.SERIES -> "DİZİ"
            SearchKind.PROGRAM -> "REHBER"
        },
        color = when (kind) {
            SearchKind.CHANNEL -> NovaColors.Live
            SearchKind.MOVIE -> NovaColors.Accent
            SearchKind.SERIES -> NovaColors.Accent
            SearchKind.PROGRAM -> NovaColors.TextMuted
        },
    )
}

/** Boş durum: başlık ve açıklama, dikeyde ortada. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CompactSearchMessage(copy: SearchCopy, tag: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = copy.title,
            style = NovaType.Title,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(tag),
        )
        Text(text = copy.body, style = NovaType.BodyMuted, textAlign = TextAlign.Center)
    }
}

/** Boş durum metni: başlık + açıklama; tek kaynak, test edilebilir. */
internal data class SearchCopy(val title: String, val body: String)

/** Sorgu yokken: TV metniyle aynı. */
internal fun compactSearchIdle(): SearchCopy = SearchCopy(
    title = "Aramaya hazır",
    body = "Kanal adı, film, dizi veya program başlığı yazın.",
)

/**
 * Sorgu eşleşmediğinde. "Kaynak yok" demez — kaynağın boşluğu ayrı bir
 * durumdur ve bu ekranın kapsamı değildir; açıklama sorguya dönüktür.
 */
internal fun compactSearchEmpty(query: String): SearchCopy = SearchCopy(
    title = "Sonuç yok",
    body = "\"$query\" için eşleşme bulunamadı. Yazımı kısaltın veya farklı bir tür seçin.",
)

/** Durumdan boş durum metni: boş sorgu önce gelir. */
internal fun compactEmptyCopy(state: SearchUiState): SearchCopy =
    if (state.query.isBlank()) compactSearchIdle() else compactSearchEmpty(state.query)

/** Sonuç satırının alt sınırı; iki satır başlıkla 56 dp'yi aşar. */
internal val CompactSearchRowHeight = 56.dp

/** Arama ekranındaki düğme ve alan hedefi. */
internal val CompactSearchControlHeight = 48.dp
