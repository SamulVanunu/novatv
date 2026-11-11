package dev.anon.novatv.feature.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
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
import dev.anon.novatv.core.model.Episode
import dev.anon.novatv.core.model.Movie
import dev.anon.novatv.core.model.Series
import dev.anon.novatv.core.sync.ContinueItem
import dev.anon.novatv.ui.component.NovaBadge
import dev.anon.novatv.ui.component.NovaLabel
import dev.anon.novatv.ui.component.NovaPill
import dev.anon.novatv.ui.component.NovaRule
import dev.anon.novatv.ui.component.NovaTapSurface
import dev.anon.novatv.ui.layout.NovaWindow
import dev.anon.novatv.ui.theme.NovaColors
import dev.anon.novatv.ui.theme.NovaTextFieldTheme
import dev.anon.novatv.ui.theme.NovaType
import dev.anon.novatv.ui.theme.novaTextFieldColors

/**
 * Kompakt (telefon) kütüphane düzeni. Geniş ekran — TV — bu dosyadaki hiçbir
 * bileşeni çizmez; `window == Expanded` bugünkü bileşenleri kullanır.
 *
 * Ölçü: 411 dp genişlikte 16 dp yan boşlukla içerik 379 dp kalır. Filtreler ve
 * düğmeler tek satırda yatay kaydırılır; başlıklar tek satırda üç nokta ile
 * kesilir. Dokunma hedefleri en az 48 dp, bölüm satırları en az 56 dp.
 * Sekmeli ızgarada filtre şeridi ızgaranın başlık satırıdır: içerikle birlikte
 * kayar, klavye açıldığında yer kaplamaz.
 */

/** Kompakt ekranın yan boşluğu. */
internal val CompactScreenPadding = 16.dp

/** Dokunma hedefi alt sınırı (erişilebilirlik). */
internal val NovaMinTouchTarget = 48.dp

/** Bölüm satırı alt sınırı. */
internal val CompactEpisodeRowHeight = 56.dp

/** Kompakt kütüphane: tam genişlik arama, sekmeler, filtre şeridi ve ızgara. */
@Composable
internal fun CompactLibraryBody(
    window: NovaWindow,
    state: LibraryUiState,
    movies: LazyPagingItems<Movie>,
    series: LazyPagingItems<Series>,
    onSelectTab: (LibraryTab) -> Unit,
    onQueryChange: (String) -> Unit,
    onGenreChange: (String?) -> Unit,
    onYearChange: (Int?) -> Unit,
    onClearFilters: () -> Unit,
    onRefresh: () -> Unit,
    onOpenGenre: () -> Unit,
    onOpenYear: () -> Unit,
    onPlayMovie: (Movie) -> Unit,
    onOpenSeries: (Series) -> Unit,
    onContinue: (ContinueItem) -> Unit,
    onBack: () -> Unit,
) {
    val current = if (state.tab == LibraryTab.MOVIES) movies else series
    val loading = current.loadState.refresh is LoadState.Loading
    val gridState = rememberLazyGridState()
    // Sütun kuralının tek kaynağı çağrı yerinin penceresi; gövde sınıfı
    // yeniden hesaplamaz.
    val columns = libraryGridColumns(window)

    LazyVerticalGrid(
        columns = columns,
        state = gridState,
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = CompactScreenPadding)
            .testTag(if (state.tab == LibraryTab.MOVIES) "library-movies" else "library-series"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
    ) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                NovaLabel("Kütüphane")
                Text(
                    text = if (state.tab == LibraryTab.MOVIES) "Filmler" else "Diziler",
                    style = NovaType.Title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                CompactSearchField(query = state.query, onQueryChange = onQueryChange)
                CompactPillRow {
                    CompactPill(
                        label = "Filmler",
                        onClick = { onSelectTab(LibraryTab.MOVIES) },
                        selected = state.tab == LibraryTab.MOVIES,
                        testTag = "library-tab-movies",
                    )
                    CompactPill(
                        label = "Diziler",
                        onClick = { onSelectTab(LibraryTab.SERIES) },
                        selected = state.tab == LibraryTab.SERIES,
                        testTag = "library-tab-series",
                    )
                }
            }
        }

        state.continueItems.takeIf { it.isNotEmpty() }?.let { items ->
            item(key = "continue", span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    NovaLabel("Devam et")
                    Spacer(Modifier.height(6.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().testTag("library-continue"),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(items = items, key = { "${it.itemType}-${it.itemId}" }) { item ->
                            CompactContinueCard(item = item, onClick = { onContinue(item) })
                        }
                    }
                }
            }
        }

        item(key = "filters", span = { GridItemSpan(maxLineSpan) }) {
            CompactPillRow(modifier = Modifier.testTag("library-filters")) {
                CompactPill(
                    label = state.genre ?: "Tür",
                    onClick = onOpenGenre,
                    selected = state.genre != null,
                    testTag = "library-genre",
                )
                CompactPill(
                    label = state.year?.toString() ?: "Yıl",
                    onClick = onOpenYear,
                    selected = state.year != null,
                    testTag = "library-year",
                )
                if (state.filtered) {
                    CompactPill(
                        label = "Temizle",
                        onClick = {
                            // Tür/yıl `clearFilters`'ın işi; arama ayrı bir kanal
                            // olduğu için burada da boşaltılır.
                            if (state.query.isNotEmpty()) onQueryChange("")
                            onClearFilters()
                        },
                        testTag = "library-clear",
                    )
                }
                CompactPill(
                    label = if (state.syncing) "Yenileniyor" else "Yenile",
                    onClick = onRefresh,
                    enabled = !state.syncing,
                    testTag = "library-refresh",
                )
                CompactPill(label = "Canlı'ya dön", onClick = onBack, testTag = "library-back")
            }
        }

        state.message?.takeIf { current.itemCount > 0 }?.let { message ->
            item(key = "message", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = message,
                    style = NovaType.Small.copy(color = NovaColors.Accent),
                    modifier = Modifier.testTag("library-message"),
                )
            }
        }

        when {
            loading && current.itemCount == 0 -> item(span = { GridItemSpan(maxLineSpan) }) {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    Text("Yükleniyor", style = NovaType.BodyMuted)
                }
            }

            current.itemCount == 0 -> item(span = { GridItemSpan(maxLineSpan) }) {
                CompactEmptyLibrary(
                    message = state.message,
                    filtered = state.filtered,
                    onRefresh = onRefresh,
                )
            }

            state.tab == LibraryTab.MOVIES -> items(
                count = movies.itemCount,
                key = movies.itemKey { it.id },
            ) { index ->
                val movie = movies[index] ?: return@items
                CompactPosterCard(
                    title = movie.name,
                    subtitle = movie.year?.toString(),
                    imageUrl = movie.posterUrl,
                    favorite = movie.id in state.favoriteIds,
                    onClick = { onPlayMovie(movie) },
                )
            }

            else -> items(
                count = series.itemCount,
                key = series.itemKey { it.id },
            ) { index ->
                val item = series[index] ?: return@items
                CompactPosterCard(
                    title = item.name,
                    subtitle = item.year?.toString(),
                    imageUrl = item.posterUrl,
                    favorite = false,
                    onClick = { onOpenSeries(item) },
                )
            }
        }
    }
}

/** Dokunmaya uygun pill: aynı dil, en az 48 dp yükseklik. */
@Composable
internal fun CompactPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    accent: Boolean = false,
    enabled: Boolean = true,
    testTag: String? = null,
) {
    NovaPill(
        label = label,
        onClick = onClick,
        selected = selected,
        accent = accent,
        enabled = enabled,
        modifier = modifier.heightIn(min = NovaMinTouchTarget),
        testTag = testTag,
    )
}

/**
 * Tek satır, yatay kaydırılan filtre/düğme şeridi.
 *
 * 411 dp'de sığmayan kuyruk (Temizle, Yenile, Canlı'ya dön) kırpılmaz:
 * [horizontalScroll] ile parmakla kaydırılarak erişilir. Dolgu şeridin içinde
 * değil çağırandan gelir; ilk düğme ekran kenarıyla aynı hizada başlar. Boşluk
 * 8 dp. Şerit taşıyorsa sona parmak payı eklenir; taşmıyorsa düzen değişmez.
 */
@Composable
internal fun CompactPillRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = NovaMinTouchTarget)
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
        if (scrollState.maxValue > 0) {
            Spacer(Modifier.width(CompactScreenPadding))
        }
    }
}

/** Tam genişlik, tek satır arama alanı; dokununca klavye açılır. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun CompactSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    NovaTextFieldTheme {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Ara", style = NovaType.Body.copy(color = NovaColors.TextDim)) },
            singleLine = true,
            shape = RoundedCornerShape(50),
            colors = novaTextFieldColors(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search,
            ),
            keyboardActions = KeyboardActions(
                onSearch = { focusManager.clearFocus() },
            ),
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = NovaMinTouchTarget)
                .testTag("library-search"),
        )
    }
}

/** Film / dizi kapağı: ızgaranın dokunulabilir kartı. */
@Composable
internal fun CompactPosterCard(
    title: String,
    subtitle: String?,
    imageUrl: String?,
    favorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NovaTapSurface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, NovaColors.Border),
        focusedBorder = BorderStroke(2.dp, NovaColors.Focus),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(7.dp)),
            ) {
                PosterImage(imageUrl = imageUrl, title = title)
                if (favorite) {
                    NovaBadge(
                        text = "★",
                        color = NovaColors.Accent,
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                style = NovaType.Body.copy(fontSize = 14.sp, lineHeight = 18.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = NovaType.Number.copy(color = NovaColors.TextDim),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** "Devam et" kartı: sabit genişlikte, başlık üç noktayla kesilir. */
@Composable
private fun CompactContinueCard(item: ContinueItem, onClick: () -> Unit) {
    NovaTapSurface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        containerColor = NovaColors.Surface,
        border = BorderStroke(1.dp, NovaColors.Border),
        focusedBorder = BorderStroke(2.dp, NovaColors.Focus),
        modifier = Modifier.width(200.dp),
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(88.dp).height(54.dp).clip(RoundedCornerShape(6.dp))) {
                PosterImage(imageUrl = item.imageUrl, title = item.title)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = NovaType.Body.copy(fontSize = 13.sp, lineHeight = 17.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // TV ile aynı: altyazı yoksa uydurulmaz; satır boş kalır ama
                // yüksekliğini korur, kart boyu içeriğe göre zıplamaz.
                Text(
                    text = item.subtitle.orEmpty(),
                    style = NovaType.Small.copy(fontSize = 12.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Boş kitaplık / eşleşmeyen filtre durumu; kompakt boşluklarla. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CompactEmptyLibrary(message: String?, filtered: Boolean, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp)
            .testTag("library-empty"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = if (filtered) "Eşleşen içerik yok" else "Kitaplık boş",
            style = NovaType.Display.copy(fontSize = 26.sp),
        )
        Text(
            text = message ?: if (filtered) {
                "Aramayı veya filtreleri değiştirin."
            } else {
                "Xtream kaynağında film ve dizi listesini yenileyin."
            },
            style = NovaType.BodyMuted,
        )
        if (!filtered) {
            Spacer(Modifier.height(4.dp))
            CompactPill(label = "Kitaplığı yenile", onClick = onRefresh, accent = true)
        }
    }
}

/** Film detayı: kompakt ekranda diyalog değil, tam sayfa. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun CompactMovieDetail(
    movie: Movie,
    favorite: Boolean,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(CompactScreenPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NovaLabel("Film")
        Text(
            text = movie.name,
            style = NovaType.Title,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.width(120.dp).height(180.dp)) {
                PosterImage(imageUrl = movie.posterUrl, title = movie.name)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    movie.year?.let { NovaBadge(text = it.toString(), color = NovaColors.TextMuted) }
                    movie.rating?.let {
                        NovaBadge(text = "★ %.1f".format(it), color = NovaColors.Accent)
                    }
                }
                movie.durationMin?.let {
                    NovaBadge(text = "$it dk", color = NovaColors.TextMuted)
                }
                if (movie.genres.isNotEmpty()) {
                    Text(
                        text = movie.genres.joinToString(" · "),
                        style = NovaType.Small.copy(color = NovaColors.TextMuted),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        movie.plot?.let { plot ->
            Text(
                text = plot,
                style = NovaType.BodyMuted.copy(fontSize = 15.sp, lineHeight = 20.sp),
            )
        }
        NovaRule(Modifier.fillMaxWidth())
        CompactPill(
            label = "Oynat",
            onClick = onPlay,
            accent = true,
            modifier = Modifier.fillMaxWidth(),
            testTag = "movie-play",
        )
        CompactPill(
            label = if (favorite) "Favoriden çıkar" else "Favorilere ekle",
            onClick = onFavorite,
            selected = favorite,
            modifier = Modifier.fillMaxWidth(),
            testTag = "movie-favorite",
        )
        CompactPill(
            label = "Kapat",
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            testTag = "movie-dismiss",
        )
    }
}

/**
 * Kompakt dizi detayı: künye, sezonlar yatay şerit, bölümler tek sütun.
 *
 * Sayfanın tamamı tek bir [LazyColumn]'dur: künye, sezon şeridi ve ayırıcı
 * listenin ilk öğeleri, bölümler geri kalanıdır. Böylece uzun konu ya da çok
 * sezon bölüm listesini sıfır yüksekliğe sıkıştıramaz; bölümler her zaman
 * kaydırılarak erişilir. Sezon şeridi bu listenin içinde kendi yatay
 * kaydırmasını korur.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun CompactSeriesDetail(
    state: SeriesDetailUiState,
    onSelectSeason: (Long) -> Unit,
    onPlayEpisode: (Episode) -> Unit,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = CompactScreenPadding)
            .testTag("series-episodes"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = CompactScreenPadding),
    ) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        NovaLabel("Dizi")
                        Text(
                            text = state.series?.name ?: "Dizi",
                            style = NovaType.Title,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    CompactPill(label = "Kütüphane", onClick = onBack, testTag = "series-back")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.width(110.dp).height(165.dp)) {
                        PosterImage(
                            imageUrl = state.series?.posterUrl,
                            title = state.series?.name.orEmpty(),
                        )
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
                        }
                        if (state.episodes.isNotEmpty()) {
                            NovaBadge(
                                text = "${state.episodes.size} bölüm",
                                color = NovaColors.TextMuted,
                            )
                        }
                        if (state.series?.genres?.isNotEmpty() == true) {
                            Text(
                                text = state.series.genres.joinToString(" · "),
                                style = NovaType.Small.copy(color = NovaColors.TextMuted),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        CompactPill(
                            label = if (state.favorite) "★ Favori" else "☆ Favori",
                            onClick = onToggleFavorite,
                            selected = state.favorite,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "series-favorite",
                        )
                    }
                }

                state.series?.plot?.let { plot ->
                    Text(
                        text = plot,
                        style = NovaType.BodyMuted.copy(fontSize = 15.sp, lineHeight = 20.sp),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        item(key = "seasons") {
            LazyRow(
                modifier = Modifier.fillMaxWidth().testTag("season-row"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items = state.seasonTabs(), key = { it.first }) { (seasonId, label) ->
                    CompactPill(
                        label = label,
                        onClick = { onSelectSeason(seasonId) },
                        selected = state.selectedSeason == seasonId,
                        testTag = "season-$seasonId",
                    )
                }
            }
        }

        item(key = "rule") {
            NovaRule(Modifier.fillMaxWidth().padding(top = 4.dp))
        }

        when {
            state.loading && state.episodes.isEmpty() -> item(key = "loading") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "Bölümler yükleniyor", style = NovaType.BodyMuted)
                }
            }

            state.visibleEpisodes.isEmpty() -> item(key = "empty") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.message ?: "Bu dizi için bölüm bilgisi yok",
                        style = NovaType.BodyMuted,
                        modifier = Modifier.testTag("series-empty"),
                    )
                }
            }

            else -> items(items = state.visibleEpisodes, key = { it.id }) { episode ->
                CompactEpisodeRow(episode = episode, onClick = { onPlayEpisode(episode) })
            }
        }
    }
}

/** Tek sütun bölüm satırı: numara, başlık, süre; dokununca oynatır. */
@Composable
internal fun CompactEpisodeRow(episode: Episode, onClick: () -> Unit) {
    NovaTapSurface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        containerColor = NovaColors.Surface,
        border = BorderStroke(1.dp, NovaColors.Border),
        focusedBorder = BorderStroke(2.dp, NovaColors.Focus),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = CompactEpisodeRowHeight)
            .testTag("episode-play-${episode.id}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = episode.number.toString().padStart(2, '0'),
                style = NovaType.Number.copy(color = NovaColors.Accent),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title,
                    style = NovaType.Body.copy(fontSize = 15.sp, lineHeight = 20.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                episode.durationMin?.let { minutes ->
                    Text(
                        text = "$minutes dk",
                        style = NovaType.Small.copy(fontSize = 12.sp, color = NovaColors.TextMuted),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
