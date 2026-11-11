package dev.anon.novatv.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import dev.anon.novatv.core.sync.SearchHit
import dev.anon.novatv.core.sync.SearchKind
import dev.anon.novatv.ui.component.NovaBackground
import dev.anon.novatv.ui.component.NovaBadge
import dev.anon.novatv.ui.component.NovaLabel
import dev.anon.novatv.ui.component.NovaPill
import dev.anon.novatv.ui.component.NovaRule
import dev.anon.novatv.ui.focus.tvFieldNavigation
import dev.anon.novatv.ui.layout.NovaWindow
import dev.anon.novatv.ui.theme.NovaColors
import dev.anon.novatv.ui.theme.NovaTextFieldTheme
import dev.anon.novatv.ui.theme.NovaType
import dev.anon.novatv.ui.theme.novaTextFieldColors

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchScreen(
    state: SearchUiState,
    onQuery: (String) -> Unit,
    onFilter: (SearchKind?) -> Unit,
    onSelect: (SearchHit) -> Unit,
    onBack: () -> Unit,
    window: NovaWindow = NovaWindow.Expanded,
    modifier: Modifier = Modifier,
) {
    when (window) {
        // Telefonda ilk odak istenmez: klavye dokunuşla açılır.
        NovaWindow.Compact, NovaWindow.Medium -> CompactSearch(
            state = state,
            onQuery = onQuery,
            onFilter = onFilter,
            onSelect = onSelect,
            onBack = onBack,
            modifier = modifier,
        )

        NovaWindow.Expanded -> TvSearch(
            state = state,
            onQuery = onQuery,
            onFilter = onFilter,
            onSelect = onSelect,
            onBack = onBack,
            modifier = modifier,
        )
    }
}

/**
 * TV gövdesi: alan açılışta odaklanır (kumandayla yazma), satırlar tek
 * çizgide. Davranış `window == Expanded` iken bugünkü gibidir.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvSearch(
    state: SearchUiState,
    onQuery: (String) -> Unit,
    onFilter: (SearchKind?) -> Unit,
    onSelect: (SearchHit) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fieldFocus = androidx.compose.runtime.remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { fieldFocus.requestFocus() } }

    NovaBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 26.dp, end = 26.dp, top = 20.dp, bottom = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    NovaLabel("Ara")
                    Text(
                        text = when {
                            state.query.isBlank() -> "Kanal, film, dizi, program"
                            else -> "${state.visible.size} sonuç"
                        },
                        style = NovaType.Title,
                    )
                }
                NovaPill(label = "Canlı TV", onClick = onBack, testTag = "search-back")
            }

            Spacer(Modifier.height(12.dp))

            NovaTextFieldTheme {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQuery,
                    placeholder = {
                        Text("Yazmaya başlayın", style = NovaType.Body.copy(color = NovaColors.TextDim))
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    colors = novaTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(fieldFocus)
                        .tvFieldNavigation(horizontal = true)
                        .testTag("search-field"),
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterPill("Tümü", null, state, onFilter)
                FilterPill("Kanallar", SearchKind.CHANNEL, state, onFilter)
                FilterPill("Filmler", SearchKind.MOVIE, state, onFilter)
                FilterPill("Diziler", SearchKind.SERIES, state, onFilter)
                FilterPill("Rehber", SearchKind.PROGRAM, state, onFilter)
            }

            Spacer(Modifier.height(8.dp))
            NovaRule(Modifier.fillMaxWidth())

            when {
                state.query.isBlank() -> CenterMessage(
                    title = "Aramaya hazır",
                    body = "Kanal adı, film, dizi veya program başlığı yazın.",
                    tag = "search-idle",
                )

                state.visible.isEmpty() -> CenterMessage(
                    title = "Sonuç yok",
                    body = "\"${state.query}\" için eşleşme bulunamadı.",
                    tag = "search-empty",
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("search-results"),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(items = state.visible, key = { "${it.kind}-${it.id}-${it.startsAt ?: 0}" }) { hit ->
                        SearchRow(hit = hit, onClick = { onSelect(hit) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FilterPill(
    label: String,
    kind: SearchKind?,
    state: SearchUiState,
    onFilter: (SearchKind?) -> Unit,
) {
    NovaPill(
        label = label,
        onClick = { onFilter(kind) },
        selected = state.filter == kind,
        testTag = "search-filter-${kind?.name ?: "ALL"}",
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SearchRow(hit: SearchHit, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        NovaPill(
            label = hit.title,
            onClick = onClick,
            modifier = Modifier.weight(1f),
            testTag = "search-hit-${hit.kind.name}-${hit.id}",
        )
        val subtitle = hit.subtitle
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = NovaType.Small.copy(color = NovaColors.TextMuted),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(190.dp),
            )
        }
        NovaBadge(
            text = when (hit.kind) {
                SearchKind.CHANNEL -> "KANAL"
                SearchKind.MOVIE -> "FİLM"
                SearchKind.SERIES -> "DİZİ"
                SearchKind.PROGRAM -> "REHBER"
            },
            color = when (hit.kind) {
                SearchKind.CHANNEL -> NovaColors.Live
                SearchKind.MOVIE -> NovaColors.Accent
                SearchKind.SERIES -> NovaColors.Accent
                SearchKind.PROGRAM -> NovaColors.TextMuted
            },
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CenterMessage(title: String, body: String, tag: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = NovaType.Display.copy(fontSize = 24.sp),
            modifier = Modifier.testTag(tag),
        )
        Spacer(Modifier.height(6.dp))
        Text(text = body, style = NovaType.BodyMuted)
    }
}
