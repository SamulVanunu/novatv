package dev.anon.novatv.feature.library

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import dev.anon.novatv.core.model.Episode
import dev.anon.novatv.ui.component.NovaBackground
import dev.anon.novatv.ui.component.NovaBadge
import dev.anon.novatv.ui.component.NovaLabel
import dev.anon.novatv.ui.component.NovaPill
import dev.anon.novatv.ui.component.NovaRule
import dev.anon.novatv.ui.layout.NovaWindow
import dev.anon.novatv.ui.theme.NovaColors
import dev.anon.novatv.ui.theme.NovaType

/**
 * Dizi detayı: künye, sezon sekmeleri ve bölümler.
 *
 * [window] `Expanded` iken — TV — bugünkü geniş düzen çizilir. Diğer
 * sınıflarda kompakt düzen (`CompactSeriesDetail`) kullanılır: sezonlar
 * yatay kaydırılan şerit, bölümler tek sütun ve her satır dokununca oynatır.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SeriesDetailScreen(
    state: SeriesDetailUiState,
    onSelectSeason: (Long) -> Unit,
    onPlayEpisode: (Episode) -> Unit,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    window: NovaWindow = NovaWindow.Expanded,
) {
    if (window != NovaWindow.Expanded) {
        NovaBackground(modifier = modifier) {
            CompactSeriesDetail(
                state = state,
                onSelectSeason = onSelectSeason,
                onPlayEpisode = onPlayEpisode,
                onToggleFavorite = onToggleFavorite,
                onBack = onBack,
            )
        }
        return
    }

    NovaBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    NovaLabel("Dizi")
                    Text(
                        text = state.series?.name ?: "Dizi",
                        style = NovaType.Title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                NovaPill(
                    label = if (state.favorite) "★ Favori" else "☆ Favori",
                    onClick = onToggleFavorite,
                    selected = state.favorite,
                    testTag = "series-favorite",
                )
                NovaPill(label = "Kütüphane", onClick = onBack, testTag = "series-back")
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Box(modifier = Modifier.width(120.dp).height(180.dp)) {
                    PosterImage(imageUrl = state.series?.posterUrl, title = state.series?.name.orEmpty())
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.series?.year?.let {
                            NovaBadge(text = it.toString(), color = NovaColors.TextMuted)
                        }
                        state.series?.rating?.let {
                            NovaBadge(text = "★ %.1f".format(it), color = NovaColors.Accent)
                        }
                        if (state.episodes.isNotEmpty()) {
                            NovaBadge(
                                text = "${state.episodes.size} bölüm",
                                color = NovaColors.TextMuted,
                            )
                        }
                    }
                    if (state.series?.genres?.isNotEmpty() == true) {
                        Text(
                            text = state.series.genres.joinToString(" · "),
                            style = NovaType.Small.copy(color = NovaColors.TextMuted),
                        )
                    }
                    state.series?.plot?.let { plot ->
                        Text(
                            text = plot,
                            style = NovaType.BodyMuted.copy(fontSize = 15.sp, lineHeight = 20.sp),
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.seasonTabs().forEach { (seasonId, label) ->
                            NovaPill(
                                label = label,
                                onClick = { onSelectSeason(seasonId) },
                                selected = state.selectedSeason == seasonId,
                                testTag = "season-$seasonId",
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            NovaRule(Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))

            when {
                state.loading && state.episodes.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = NovaColors.Accent)
                }

                state.episodes.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.message ?: "Bu dizi için bölüm bilgisi yok",
                        style = NovaType.BodyMuted,
                        modifier = Modifier.testTag("series-empty"),
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("series-episodes"),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(items = state.visibleEpisodes, key = { it.id }) { episode ->
                        EpisodeRow(episode = episode, onClick = { onPlayEpisode(episode) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EpisodeRow(episode: Episode, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = episode.number.toString().padStart(2, '0'),
            style = NovaType.Number.copy(color = NovaColors.Accent),
            modifier = Modifier.width(30.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episode.title,
                style = NovaType.Body.copy(fontSize = 15.sp, lineHeight = 20.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = buildList {
                episode.durationMin?.let { add("$it dk") }
                episode.plot?.takeIf { it.isNotBlank() }?.let { add(it) }
            }.joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    style = NovaType.Small.copy(fontSize = 12.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        NovaPill(label = "Oynat", onClick = onClick, testTag = "episode-play-${episode.id}")
    }
}
