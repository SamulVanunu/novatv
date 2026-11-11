package dev.anon.novatv.feature.library

import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.SubcomposeAsyncImage
import dev.anon.novatv.core.model.ItemType
import dev.anon.novatv.core.model.Movie
import dev.anon.novatv.core.model.Series
import dev.anon.novatv.core.sync.ContinueItem
import dev.anon.novatv.ui.component.NovaBackground
import dev.anon.novatv.ui.component.NovaBadge
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
 * Kütüphane: film ve dizi ızgarası, filtreler, arama ve film detayı.
 *
 * [window] `Expanded` iken — TV — bugünkü geniş düzen çizilir, bir piksel
 * değişmez. Diğer sınıflarda kompakt düzen kullanılır: `CompactLibraryBody`
 * ve film detayında `Dialog` yerine tam sayfa `CompactMovieDetail`.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    movies: LazyPagingItems<Movie>,
    series: LazyPagingItems<Series>,
    onSelectTab: (LibraryTab) -> Unit,
    onQueryChange: (String) -> Unit,
    onGenreChange: (String?) -> Unit,
    onYearChange: (Int?) -> Unit,
    onClearFilters: () -> Unit,
    onRefresh: () -> Unit,
    onPlayMovie: (Movie) -> Unit,
    onOpenSeries: (Series) -> Unit,
    onContinue: (ContinueItem) -> Unit,
    onToggleFavorite: (Movie) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    window: NovaWindow = NovaWindow.Expanded,
) {
    var selectedMovie by remember { mutableStateOf<Movie?>(null) }
    var genreDialog by remember { mutableStateOf(false) }
    var yearDialog by remember { mutableStateOf(false) }

    // Film detayı bir navigasyon rotası değil, bu ekranın kendi durumudur; nav
    // host onu bilmez. Geri tuşunu kütüphanenin geri çağrısından (Live'a dönüş)
    // önce yakalamak için burada `enabled` durumuyla bir geri yakalayıcı kurulur.
    MovieDetailBackGuard(enabled = selectedMovie != null) { selectedMovie = null }

    if (window == NovaWindow.Expanded) {
        ExpandedLibrary(
            state = state,
            movies = movies,
            series = series,
            onSelectTab = onSelectTab,
            onQueryChange = onQueryChange,
            onGenreChange = onGenreChange,
            onYearChange = onYearChange,
            onClearFilters = onClearFilters,
            onRefresh = onRefresh,
            onPlayMovie = onPlayMovie,
            onOpenSeries = onOpenSeries,
            onContinue = onContinue,
            onToggleFavorite = onToggleFavorite,
            onBack = onBack,
            onOpenGenre = { genreDialog = true },
            onOpenYear = { yearDialog = true },
            onSelectMovie = { selectedMovie = it },
            modifier = modifier,
        )
    } else {
        NovaBackground(modifier = modifier) {
            CompactLibraryBody(
                window = window,
                state = state,
                movies = movies,
                series = series,
                onSelectTab = onSelectTab,
                onQueryChange = onQueryChange,
                onGenreChange = onGenreChange,
                onYearChange = onYearChange,
                onClearFilters = onClearFilters,
                onRefresh = onRefresh,
                onOpenGenre = { genreDialog = true },
                onOpenYear = { yearDialog = true },
                onPlayMovie = onPlayMovie,
                onOpenSeries = onOpenSeries,
                onContinue = onContinue,
                onBack = onBack,
            )
        }
    }

    selectedMovie?.let { movie ->
        if (window == NovaWindow.Expanded) {
            MovieDialog(
                movie = movie,
                favorite = movie.id in state.favoriteIds,
                onPlay = {
                    selectedMovie = null
                    onPlayMovie(movie)
                },
                onFavorite = { onToggleFavorite(movie) },
                onDismiss = { selectedMovie = null },
            )
        } else {
            NovaBackground {
                CompactMovieDetail(
                    movie = movie,
                    favorite = movie.id in state.favoriteIds,
                    onPlay = {
                        selectedMovie = null
                        onPlayMovie(movie)
                    },
                    onFavorite = { onToggleFavorite(movie) },
                    onDismiss = { selectedMovie = null },
                )
            }
        }
    }

    if (genreDialog) {
        ValueDialog(
            title = "Tür",
            values = listOf<String?>(null) + state.genres,
            label = { it ?: "Tümü" },
            selected = state.genre,
            onSelect = {
                onGenreChange(it)
                genreDialog = false
            },
            onDismiss = { genreDialog = false },
        )
    }

    if (yearDialog) {
        ValueDialog(
            title = "Yıl",
            values = listOf<Int?>(null) + state.years,
            label = { it?.toString() ?: "Tümü" },
            selected = state.year,
            onSelect = {
                onYearChange(it)
                yearDialog = false
            },
            onDismiss = { yearDialog = false },
        )
    }
}

/**
 * Film detayı açıkken sistem Geri'sini tüketen yakalayıcı.
 *
 * Yakalayıcı, [OnBackPressedDispatcher]'a kompozisyona giriş anında eklenir,
 * [DisposableEffect] ile ayrılırken düşer. `androidx.activity.compose.BackHandler`
 * kullanılmadı: o yardımcı yakalayıcıyı yaşam döngüsünün RESUME durumuna bağlar
 * ve yeniden etkinleşirken sırayı değiştirebilir; burada sıra yalnız
 * giriş/çıkışla kurulmalı (aşağıda).
 *
 * Öncelik: gönderici **en son eklenen etkin** yakalayıcıyı önce çalıştırır.
 * Nav host'un sekme geri çağrısı ekrandan önce kurulduğu için derinlik
 * sırasında altta kalır; bu yakalayıcı detay açılırken üste eklenir, kapanınca
 * düşer. Yaşam döngüsüne bağlanmıyoruz: o durumda ekran RESUME'a dönerken
 * yeniden etkin olup sırada öne geçebilir ve detay kapalıyken bile nav host'un
 * sekme/geri davranışını gölgeleyebilirdi. Yalnız giriş/çıkışla bağlamak
 * sırayı kesin tutar.
 */
@Composable
private fun MovieDetailBackGuard(enabled: Boolean, onClose: () -> Unit) {
    val owner = LocalView.current.context.findBackOwner()
    val close by rememberUpdatedState(onClose)
    val dispatcher = owner?.onBackPressedDispatcher
    DisposableEffect(dispatcher, enabled) {
        if (dispatcher == null || !enabled) return@DisposableEffect onDispose {}
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                close()
            }
        }
        dispatcher.addCallback(callback)
        onDispose { callback.remove() }
    }
}

/** Compose bağlamında geri göndericisi sahibi; Etkinlik ya da konak. */
private fun android.content.Context.findBackOwner(): OnBackPressedDispatcherOwner? = when (this) {
    is OnBackPressedDispatcherOwner -> this
    is android.content.ContextWrapper -> baseContext.findBackOwner()
    else -> null
}

/** Geniş düzen — TV: bugünkü kütüphane, değişmeden. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ExpandedLibrary(
    state: LibraryUiState,
    movies: LazyPagingItems<Movie>,
    series: LazyPagingItems<Series>,
    onSelectTab: (LibraryTab) -> Unit,
    onQueryChange: (String) -> Unit,
    onGenreChange: (String?) -> Unit,
    onYearChange: (Int?) -> Unit,
    onClearFilters: () -> Unit,
    onRefresh: () -> Unit,
    onPlayMovie: (Movie) -> Unit,
    onOpenSeries: (Series) -> Unit,
    onContinue: (ContinueItem) -> Unit,
    onToggleFavorite: (Movie) -> Unit,
    onBack: () -> Unit,
    onOpenGenre: () -> Unit,
    onOpenYear: () -> Unit,
    onSelectMovie: (Movie) -> Unit,
    modifier: Modifier = Modifier,
) {
    NovaBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 12.dp),
        ) {
            LibraryHeader(
                state = state,
                onSelectTab = onSelectTab,
                onQueryChange = onQueryChange,
                onOpenGenre = onOpenGenre,
                onOpenYear = onOpenYear,
                onClearFilters = onClearFilters,
                onRefresh = onRefresh,
                onBack = onBack,
            )

            if (state.continueItems.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                NovaLabel("Devam et")
                LazyRow(
                    modifier = Modifier.fillMaxWidth().testTag("library-continue"),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp),
                ) {
                    items(items = state.continueItems, key = { "${it.itemType}-${it.itemId}" }) { item ->
                        ContinueCard(item = item, onClick = { onContinue(item) })
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            val current = if (state.tab == LibraryTab.MOVIES) movies else series
            val loading = current.loadState.refresh is LoadState.Loading
            when {
                loading && current.itemCount == 0 -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = NovaColors.Accent)
                }

                current.itemCount == 0 -> EmptyLibrary(
                    message = state.message,
                    filtered = state.filtered,
                    onRefresh = onRefresh,
                )

                state.tab == LibraryTab.MOVIES -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    modifier = Modifier.fillMaxSize().testTag("library-movies"),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(count = movies.itemCount, key = movies.itemKey { it.id }) { index ->
                        val movie = movies[index] ?: return@items
                        PosterCard(
                            title = movie.name,
                            subtitle = movie.year?.toString(),
                            imageUrl = movie.posterUrl,
                            favorite = movie.id in state.favoriteIds,
                            onClick = { onSelectMovie(movie) },
                        )
                    }
                }

                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    modifier = Modifier.fillMaxSize().testTag("library-series"),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(count = series.itemCount, key = series.itemKey { it.id }) { index ->
                        val item = series[index] ?: return@items
                        PosterCard(
                            title = item.name,
                            subtitle = item.year?.toString(),
                            imageUrl = item.posterUrl,
                            favorite = false,
                            onClick = { onOpenSeries(item) },
                        )
                    }
                }
            }

            state.message?.takeIf { current.itemCount > 0 }?.let { message ->
                Text(
                    text = message,
                    style = NovaType.Small.copy(color = NovaColors.Accent),
                    modifier = Modifier.padding(top = 6.dp).testTag("library-message"),
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LibraryHeader(
    state: LibraryUiState,
    onSelectTab: (LibraryTab) -> Unit,
    onQueryChange: (String) -> Unit,
    onOpenGenre: () -> Unit,
    onOpenYear: () -> Unit,
    onClearFilters: () -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            NovaLabel("Kütüphane")
            Text(
                text = if (state.tab == LibraryTab.MOVIES) "Filmler" else "Diziler",
                style = NovaType.Title,
            )
        }

        NovaPill(
            label = "Filmler",
            onClick = { onSelectTab(LibraryTab.MOVIES) },
            selected = state.tab == LibraryTab.MOVIES,
            testTag = "library-tab-movies",
        )
        NovaPill(
            label = "Diziler",
            onClick = { onSelectTab(LibraryTab.SERIES) },
            selected = state.tab == LibraryTab.SERIES,
            testTag = "library-tab-series",
        )
        NovaPill(
            label = state.genre ?: "Tür",
            onClick = onOpenGenre,
            selected = state.genre != null,
            testTag = "library-genre",
        )
        NovaPill(
            label = state.year?.toString() ?: "Yıl",
            onClick = onOpenYear,
            selected = state.year != null,
            testTag = "library-year",
        )
        if (state.filtered) {
            NovaPill(label = "Temizle", onClick = onClearFilters, testTag = "library-clear")
        }
        NovaPill(
            label = if (state.syncing) "Yenileniyor" else "Yenile",
            onClick = onRefresh,
            enabled = !state.syncing,
            testTag = "library-refresh",
        )
        NovaPill(label = "Canlı TV", onClick = onBack, testTag = "library-back")

        NovaTextFieldTheme {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = { Text("Ara", style = NovaType.Body.copy(color = NovaColors.TextDim)) },
                singleLine = true,
                shape = RoundedCornerShape(50),
                colors = novaTextFieldColors(),
                modifier = Modifier
                    .width(170.dp)
                    .tvFieldNavigation(horizontal = true)
                    .testTag("library-search"),
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PosterCard(
    title: String,
    subtitle: String?,
    imageUrl: String?,
    favorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    aspect: Float = 2f / 3f,
) {
    val shape = RoundedCornerShape(10.dp)
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = NovaColors.Text,
            focusedContainerColor = NovaColors.Focus.copy(alpha = 0.08f),
            focusedContentColor = NovaColors.Text,
            pressedContainerColor = NovaColors.AccentSoft,
            pressedContentColor = NovaColors.Text,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = androidx.compose.foundation.BorderStroke(1.dp, NovaColors.Border),
                shape = shape,
            ),
            focusedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(2.dp, NovaColors.Focus),
                shape = shape,
            ),
            pressedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(2.dp, NovaColors.Focus),
                shape = shape,
            ),
        ),
        scale = ClickableSurfaceDefaults.scale(1f, 1.04f, 0.98f),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(7.dp)) {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(aspect).clip(RoundedCornerShape(7.dp))) {
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
                )
            }
        }
    }
}

@Composable
fun PosterImage(imageUrl: String?, title: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(7.dp)
    val base = modifier
        .fillMaxSize()
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(NovaColors.SurfaceHigh, NovaColors.Surface),
            ),
        )
    if (imageUrl.isNullOrBlank()) {
        Box(modifier = base, contentAlignment = Alignment.Center) {
            Text(
                text = title.firstOrNull()?.uppercase() ?: "?",
                style = NovaType.Display.copy(fontSize = 34.sp, color = NovaColors.TextDim),
            )
        }
    } else {
        SubcomposeAsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = base,
            contentScale = ContentScale.Crop,
            loading = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("…", style = NovaType.Title.copy(color = NovaColors.TextDim))
                }
            },
            error = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = title.firstOrNull()?.uppercase() ?: "?",
                        style = NovaType.Display.copy(fontSize = 34.sp, color = NovaColors.TextDim),
                    )
                }
            },
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ContinueCard(item: ContinueItem, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = NovaColors.Surface,
            contentColor = NovaColors.Text,
            focusedContainerColor = NovaColors.Focus.copy(alpha = 0.08f),
            focusedContentColor = NovaColors.Text,
            pressedContainerColor = NovaColors.AccentSoft,
            pressedContentColor = NovaColors.Text,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = androidx.compose.foundation.BorderStroke(1.dp, NovaColors.Border),
                shape = shape,
            ),
            focusedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(2.dp, NovaColors.Focus),
                shape = shape,
            ),
            pressedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(2.dp, NovaColors.Focus),
                shape = shape,
            ),
        ),
        scale = ClickableSurfaceDefaults.scale(1f, 1.03f, 0.98f),
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(96.dp).height(58.dp).clip(RoundedCornerShape(6.dp))) {
                PosterImage(imageUrl = item.imageUrl, title = item.title)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.width(150.dp)) {
                Text(
                    text = item.title,
                    style = NovaType.Body.copy(fontSize = 13.sp, lineHeight = 17.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.subtitle ?: if (item.itemType == ItemType.MOVIE) "Film" else "Bölüm",
                    style = NovaType.Small.copy(fontSize = 12.sp),
                    maxLines = 1,
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(NovaColors.Border, RoundedCornerShape(50)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(item.progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(NovaColors.Accent, RoundedCornerShape(50)),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EmptyLibrary(message: String?, filtered: Boolean, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if (filtered) "Eşleşen içerik yok" else "Kitaplık boş",
            style = NovaType.Display.copy(fontSize = 26.sp),
            modifier = Modifier.testTag("library-empty"),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message ?: if (filtered) {
                "Aramayı veya filtreleri değiştirin."
            } else {
                "Xtream kaynağında film ve dizi listesini yenileyin."
            },
            style = NovaType.BodyMuted,
        )
        Spacer(Modifier.height(18.dp))
        if (!filtered) {
            NovaPill(label = "Kitaplığı yenile", onClick = onRefresh, accent = true)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MovieDialog(
    movie: Movie,
    favorite: Boolean,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        NovaPanel(width = 620.dp) {
            NovaLabel("Film")
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.width(132.dp).height(198.dp)) {
                    PosterImage(imageUrl = movie.posterUrl, title = movie.name)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(text = movie.name, style = NovaType.Title, maxLines = 2)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        movie.year?.let { NovaBadge(text = it.toString(), color = NovaColors.TextMuted) }
                        movie.rating?.let {
                            NovaBadge(text = "★ %.1f".format(it), color = NovaColors.Accent)
                        }
                        movie.durationMin?.let {
                            NovaBadge(text = "$it dk", color = NovaColors.TextMuted)
                        }
                    }
                    if (movie.genres.isNotEmpty()) {
                        Text(
                            text = movie.genres.joinToString(" · "),
                            style = NovaType.Small.copy(color = NovaColors.TextMuted),
                        )
                    }
                    movie.plot?.let { plot ->
                        Text(
                            text = plot,
                            style = NovaType.BodyMuted.copy(fontSize = 15.sp, lineHeight = 20.sp),
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            NovaRule(Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NovaPill(label = "Oynat", onClick = onPlay, accent = true, testTag = "movie-play")
                NovaPill(
                    label = if (favorite) "Favoriden çıkar" else "Favorilere ekle",
                    onClick = onFavorite,
                    selected = favorite,
                    testTag = "movie-favorite",
                )
            }
            NovaPill(
                label = "Kapat",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                testTag = "movie-dismiss",
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun <T> ValueDialog(
    title: String,
    values: List<T>,
    label: (T) -> String,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        NovaPanel(width = 360.dp) {
            NovaLabel(title)
            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(320.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(items = values, key = { it?.toString() ?: "all" }) { value ->
                    NovaPill(
                        label = label(value),
                        onClick = { onSelect(value) },
                        selected = value == selected,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            NovaPill(
                label = "Kapat",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
