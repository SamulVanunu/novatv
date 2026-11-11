package dev.anon.novatv.mobile

import android.net.Uri
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import dev.anon.novatv.app.common.NovaGraph
import dev.anon.novatv.app.common.ParentalGuardDialog
import dev.anon.novatv.app.common.ParentalGuardState
import dev.anon.novatv.app.common.PlayerRouteContent
import dev.anon.novatv.app.common.Routes
import dev.anon.novatv.app.common.hasDocumentPicker
import dev.anon.novatv.app.common.rememberParentalGuard
import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.core.model.ItemType
import dev.anon.novatv.core.model.Playlist
import dev.anon.novatv.core.player.PlaybackController
import dev.anon.novatv.core.settings.Appearance
import dev.anon.novatv.core.sync.SearchKind
import dev.anon.novatv.feature.epg.EpgRoutes
import dev.anon.novatv.feature.epg.EpgScreen
import dev.anon.novatv.feature.epg.EpgViewModel
import dev.anon.novatv.feature.epg.ReminderScheduler
import dev.anon.novatv.feature.library.LibraryRoutes
import dev.anon.novatv.feature.library.LibraryScreen
import dev.anon.novatv.feature.library.LibraryViewModel
import dev.anon.novatv.feature.library.SeriesDetailScreen
import dev.anon.novatv.feature.library.SeriesDetailViewModel
import dev.anon.novatv.feature.live.LiveScreen
import dev.anon.novatv.feature.live.LiveViewModel
import dev.anon.novatv.feature.multiwall.MultiWallRoutes
import dev.anon.novatv.feature.multiwall.MultiWallScreen
import dev.anon.novatv.feature.multiwall.MultiWallViewModel
import dev.anon.novatv.feature.player.ChannelStreamResolver
import dev.anon.novatv.feature.player.PlayerRoutes
import dev.anon.novatv.feature.player.PlayerViewModel
import dev.anon.novatv.feature.player.VodPlayerRoutes
import dev.anon.novatv.feature.player.VodRequest
import dev.anon.novatv.feature.playlists.PlaylistFormScreen
import dev.anon.novatv.feature.playlists.PlaylistFormViewModel
import dev.anon.novatv.feature.playlists.PlaylistsRoutes
import dev.anon.novatv.feature.playlists.PlaylistsScreen
import dev.anon.novatv.feature.playlists.PlaylistsViewModel
import dev.anon.novatv.feature.recordings.RecordingPlayerRoutes
import dev.anon.novatv.feature.recordings.RecordingsRoutes
import dev.anon.novatv.feature.recordings.RecordingsScreen
import dev.anon.novatv.feature.recordings.RecordingsViewModel
import dev.anon.novatv.feature.settings.ProfileViewModel
import dev.anon.novatv.feature.settings.ProfilesScreen
import dev.anon.novatv.feature.settings.SearchScreen
import dev.anon.novatv.feature.settings.SearchViewModel
import dev.anon.novatv.feature.settings.SettingsRoutes
import dev.anon.novatv.feature.settings.SettingsScreen
import dev.anon.novatv.feature.settings.SettingsViewModel
import dev.anon.novatv.ui.component.NovaLabel
import dev.anon.novatv.ui.component.NovaPanel
import dev.anon.novatv.ui.component.NovaPill
import dev.anon.novatv.ui.component.NovaTapSurface
import dev.anon.novatv.ui.layout.NovaScaffold
import dev.anon.novatv.ui.layout.NovaTab
import dev.anon.novatv.ui.layout.rememberNovaWindow
import dev.anon.novatv.ui.theme.NovaColors
import dev.anon.novatv.ui.theme.NovaType
import java.io.File
import java.time.Instant
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Telefon kabuğunun sekme kimlikleri. Bunlar rota değil gezinme durumudur:
 * her sekme bir kök rota açar, o rotadan açılan alt ekranlar sekmeden
 * bağımsız olarak geri yığınında yaşar.
 */
private object MobileTabs {
    const val LIVE = "live"
    const val GUIDE = "guide"
    const val LIBRARY = "library"
    const val RECORDINGS = "recordings"
    const val MORE = "more"
}

/**
 * "Daha" menüsünün rotası. Sekme anahtarı [MobileTabs.MORE] ile karışmasın
 * diye ayrı bir dize; rota yalnız telefondadır, `Routes` (iki uygulamanın
 * ortak sabitleri) bu ekranı bilmez.
 */
private const val MORE_ROUTE = "more-menu"

/** Beş sekme: Canlı, Rehber, Kütüphane, Kayıtlar, Daha. */
private val Tabs = listOf(
    NovaTab(key = MobileTabs.LIVE, label = "Canlı", testTag = "tab-live"),
    NovaTab(key = MobileTabs.GUIDE, label = "Rehber", testTag = "tab-guide"),
    NovaTab(key = MobileTabs.LIBRARY, label = "Kütüphane", testTag = "tab-library"),
    NovaTab(key = MobileTabs.RECORDINGS, label = "Kayıtlar", testTag = "tab-recordings"),
    NovaTab(key = MobileTabs.MORE, label = "Daha", testTag = "tab-more"),
)

/**
 * Telefon uygulamasının gezinme gövdesi.
 *
 * Rotalar TV'deki `NovaNavHost` ile aynıdır: aynı sabitler, aynı ViewModel
 * fabrikaları, aynı ekranlar. Fark yalnız kabukta — kök ekranlar
 * `NovaScaffold`'un verdiği boşluğa yerleşir; alt ekranlarda gezinme çubuğu
 * çizilmez ve içerik tam ekran kalır.
 */
@Composable
fun MobileNavHost(graph: NovaGraph) {
    val navController = rememberNavController()
    val appearance by graph.preferences.appearance.collectAsStateWithLifecycle(
        initialValue = Appearance(),
    )
    val window = rememberNovaWindow()
    val parentalGuard = rememberParentalGuard(graph.profiles)
    val channelResolver = remember(graph) { GraphChannelResolver(graph) }

    // Açılış hedefi depodan bir kez çözülür; `NavHost` kurulmadan önce hazır
    // olmalı. Okuma düşerse `resolveStartInputs` "kaynak yok"a indirger, yani
    // ekran boş kalmaz: Canlı açılır ve kaynak çağrısı görünür. TV'deki
    // `NovaNavHost` da aynı yarışı aynı şekilde bekleyerek çözer.
    val startInputs by produceState<StartInputs?>(initialValue = null) {
        value = resolveStartInputs(graph)
    }
    val resolved = startInputs ?: return
    val startRoute = startDestination(resolved.hasSources, resolved.lastChannelRoute)
    // Kaynak sayısı akıştan okunur: kaynak eklenince boş açılış yüzeyi
    // kendiliğinden kalkar, tek seferlik bir bayrağa bağlı değildir. Akış
    // düşerse yukarıda çözülen değere dönülür — sorgu hatası ekranı çökertmez.
    val sourceCount by graph.playlists.observeAll()
        .map { playlists -> activeSourceCount(playlists) }
        .catch { emit(if (resolved.hasSources) 1 else 0) }
        .collectAsStateWithLifecycle(initialValue = if (resolved.hasSources) 1 else 0)

    // Kök hedeflerde kabuk görünür; alt ekranlar tam ekran.
    val entry by navController.currentBackStackEntryAsState()
    val selectedTab = tabForRoute(entry?.destination?.route)

    // targetSdk 35 ile Android 15 kenardan kenara çizer: sistem çubukları
    // içeriğin üstüne biner. Oynatıcılar dışında içerik ve kabuk güvenli
    // alana yerleşir; oynatıcılar tam ekran kalır (video kenara ulaşmalı).
    // Alt çubuğu `NovaScaffold` alt kenara hizalar; navigasyon çubuğu payı
    // bir kez burada (kabuğun dışında) verilir, içerikte tekrarlanmaz.
    val fullBleed = isFullBleedRoute(entry?.destination?.route)
    val insets = WindowInsets.safeDrawing
    val chromeModifier = if (fullBleed) {
        Modifier.fillMaxSize()
    } else {
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(insets)
    }

    // Sistem Geri: sekme kökünden ve alt ekrandan çıkmak yığını boşaltmaz.
    // Canlı yığında kalan tek giriş olduğunda Geri uygulamadan çıkar; bunun
    // için geri çağrısını etkin bırakmayız, etkinlik varsayılanı işler.
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    DisposableEffect(backDispatcher, selectedTab) {
        val dispatcher = backDispatcher ?: return@DisposableEffect onDispose {}
        val callback: OnBackPressedCallback? = if (selectedTab == null) {
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    navController.popBackStack()
                }
            }
        } else if (selectedTab != MobileTabs.LIVE) {
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    navController.backToTabRoot(MobileTabs.LIVE)
                }
            }
        } else {
            null
        }
        callback?.let(dispatcher::addCallback)
        onDispose { callback?.remove() }
    }

    if (selectedTab != null) {
        // Alt çubuğun kendi payı `NovaScaffold`'tan gelir; navigasyon çubuğu
        // inset'i yalnız kabuğa eklenir, içeriğe ikinci kez eklenmez.
        Box(modifier = chromeModifier) {
            NovaScaffold(
                window = window,
                tabs = Tabs,
                selected = selectedTab,
                onSelect = navController::openTab,
            ) { padding ->
                MobileGraph(
                    graph = graph,
                    navController = navController,
                    appearance = appearance,
                    channelResolver = channelResolver,
                    parentalGuard = parentalGuard,
                    startRoute = startRoute,
                    needsSourceOnboarding = needsSourceOnboarding(sourceCount == 0),
                    // Sekme kökü: boşluk yalnız kabuktan gelir.
                    contentModifier = Modifier.fillMaxSize().padding(padding),
                )
            }
        }
    } else {
        Box(modifier = chromeModifier) {
            MobileGraph(
                graph = graph,
                navController = navController,
                appearance = appearance,
                channelResolver = channelResolver,
                parentalGuard = parentalGuard,
                startRoute = startRoute,
                needsSourceOnboarding = needsSourceOnboarding(sourceCount == 0),
                contentModifier = Modifier.fillMaxSize(),
            )
        }
    }

    ParentalGuardDialog(parentalGuard)
}

@Composable
private fun MobileGraph(
    graph: NovaGraph,
    navController: NavHostController,
    appearance: Appearance,
    channelResolver: ChannelStreamResolver,
    parentalGuard: ParentalGuardState,
    startRoute: String,
    needsSourceOnboarding: Boolean,
    contentModifier: Modifier,
) {
    NavHost(navController = navController, startDestination = startRoute) {
        composable(Routes.LIVE) {
            val viewModel: LiveViewModel = viewModel(
                factory = LiveViewModel.factory(graph.channels, graph.preferences),
            )
            val state by viewModel.state.collectAsStateWithLifecycle()
            val channels = viewModel.channels.collectAsLazyPagingItems()
            // Boş açılış yüzeyi mobil rotaya sarılır: `LiveScreen`'in
            // imzası ve TV yolu değişmez. Yüzey görünürken `LiveScreen`
            // hiç kurulmaz; arkada etkin bir liste bırakılmaz.
            SourceOnboardingGate(
                visible = needsSourceOnboarding,
                graph = graph,
                navController = navController,
                modifier = contentModifier,
            ) {
                LiveScreen(
                    state = state,
                    channels = channels,
                    onSelectGroup = viewModel::selectGroup,
                    onQueryChange = viewModel::setQuery,
                    onToggleFavorites = viewModel::toggleFavoritesOnly,
                    onToggleGrid = viewModel::toggleGrid,
                    onChannelClick = { channel -> navController.navigate(PlayerRoutes.of(channel.id)) },
                    onToggleFavorite = viewModel::toggleFavorite,
                    onHideChannel = viewModel::hideChannel,
                    onHideGroup = viewModel::hideGroup,
                    onSetNumber = viewModel::setNumber,
                    onChannelFocused = viewModel::onChannelFocused,
                    onOpenPlaylists = { navController.navigate(PlaylistsRoutes.LIST) },
                    onOpenGuide = { navController.navigate(EpgRoutes.of()) },
                    onOpenLibrary = { navController.navigate(LibraryRoutes.ROUTE) },
                    onOpenRecordings = { navController.openTab(MobileTabs.RECORDINGS) },
                    onOpenMultiwall = { navController.navigate(MultiWallRoutes.of(4)) },
                    onOpenSearch = { navController.navigate(SettingsRoutes.SEARCH) },
                    onOpenSettings = { navController.navigate(SettingsRoutes.SETTINGS) },
                    window = rememberNovaWindow(),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        composable(MORE_ROUTE) {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            // Son yedek/geri yükleme sonucu; her yolda doldurulur, yoksa
            // hatalı dosya seçimi ekranda hiçbir şey değiştirmiyordu.
            var message by remember { mutableStateOf<String?>(null) }
            val backupLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/json"),
            ) { uri ->
                if (uri != null) {
                    // Ayarlar'daki akışın aynısı: belge seçici varsa dosyaya,
                    // yoksa uygulamanın kendi yedek dosyasına yazar.
                    scope.launch {
                        message = runCatching { graph.backup.backup(uri) }
                            .getOrElse { "Yedek alınamadı" }
                    }
                }
            }
            val restoreLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument(),
            ) { uri ->
                if (uri != null) {
                    scope.launch {
                        message = runCatching { graph.backup.restore(uri) }
                            .getOrElse { "Yedek geri yüklenemedi" }
                    }
                }
            }
            MobileMoreScreen(
                onSearch = { navController.navigate(SettingsRoutes.SEARCH) },
                onMultiwall = { navController.navigate(MultiWallRoutes.of(4)) },
                onSources = { navController.navigate(PlaylistsRoutes.LIST) },
                onSettings = { navController.navigate(SettingsRoutes.SETTINGS) },
                onBackup = {
                    if (hasDocumentPicker(context)) {
                        backupLauncher.launch("novatv-backup.json")
                    } else {
                        scope.launch {
                            message = runCatching { graph.backup.backup(null) }
                                .getOrElse { "Yedek alınamadı" }
                        }
                    }
                },
                onRestore = {
                    if (hasDocumentPicker(context)) {
                        restoreLauncher.launch(
                            arrayOf("application/json", "text/plain", "*/*"),
                        )
                    } else {
                        scope.launch {
                            message = runCatching { graph.backup.restore(null) }
                                .getOrElse { "Yedek geri yüklenemedi" }
                        }
                    }
                },
                message = message,
                modifier = contentModifier,
            )
        }

        composable(
            route = EpgRoutes.PATTERN,
            arguments = listOf(
                navArgument("day") {
                    type = NavType.IntType
                    defaultValue = 0
                },
            ),
        ) {
            val context = LocalContext.current
            val reminders = remember(context) { ReminderScheduler(context) }
            val viewModel: EpgViewModel = viewModel(
                factory = EpgViewModel.factory(
                    repository = graph.epg,
                    preferences = graph.preferences,
                    reminders = reminders,
                    recordingStore = graph.recordings,
                    recordingScheduler = graph.recordingScheduler,
                ),
            )
            val state by viewModel.state.collectAsStateWithLifecycle()
            EpgScreen(
                state = state,
                onSelectDay = viewModel::selectDay,
                onRefresh = { viewModel.refresh(force = true) },
                onBack = { navController.backToTabRoot(MobileTabs.LIVE) },
                onWatchChannel = { channel ->
                    parentalGuard.requestRating(null, channel.name) {
                        navController.navigate(PlayerRoutes.of(channel.id))
                    }
                },
                onWatchCatchup = { channel, program ->
                    val minutes = ((program.endAt - program.startAt) / 60_000L)
                        .toInt()
                        .coerceIn(1, 6 * 60)
                    parentalGuard.requestRating(program.rating, program.title) {
                        navController.navigate(
                            PlayerRoutes.catchup(channel.id, program.startAt, minutes),
                        )
                    }
                },
                onToggleReminder = viewModel::toggleReminder,
                onToggleRecording = viewModel::toggleRecording,
                window = rememberNovaWindow(),
            )
        }

        composable(LibraryRoutes.ROUTE) {
            val viewModel: LibraryViewModel = viewModel(
                factory = LibraryViewModel.factory(graph.library, graph.preferences),
            )
            val state by viewModel.state.collectAsStateWithLifecycle()
            val movies = viewModel.movies.collectAsLazyPagingItems()
            val series = viewModel.series.collectAsLazyPagingItems()
            LibraryScreen(
                state = state,
                movies = movies,
                series = series,
                onSelectTab = viewModel::selectTab,
                onQueryChange = viewModel::setQuery,
                onGenreChange = viewModel::setGenre,
                onYearChange = viewModel::setYear,
                onClearFilters = viewModel::clearFilters,
                onRefresh = { viewModel.refresh(showMessage = true) },
                onPlayMovie = { movie ->
                    parentalGuard.requestScore(movie.rating, movie.name) {
                        navController.navigate(VodPlayerRoutes.movie(movie.id))
                    }
                },
                onOpenSeries = { seriesItem ->
                    navController.navigate(LibraryRoutes.series(seriesItem.id))
                },
                onContinue = { item ->
                    val seriesId = item.seriesId
                    when {
                        item.itemType == ItemType.MOVIE ->
                            navController.navigate(VodPlayerRoutes.movie(item.itemId))
                        seriesId != null ->
                            navController.navigate(
                                VodPlayerRoutes.episode(seriesId, item.itemId),
                            )
                    }
                },
                onToggleFavorite = viewModel::toggleFavorite,
                onBack = { navController.backToTabRoot(MobileTabs.LIVE) },
            )
        }

        composable(
            route = LibraryRoutes.SERIES_PATTERN,
            arguments = listOf(
                navArgument(LibraryRoutes.ARG_SERIES_ID) { type = NavType.LongType },
            ),
        ) { entry ->
            val seriesId = entry.arguments?.getLong(LibraryRoutes.ARG_SERIES_ID) ?: -1L
            val viewModel: SeriesDetailViewModel = viewModel(
                factory = SeriesDetailViewModel.factory(graph.library, seriesId),
            )
            val state by viewModel.state.collectAsStateWithLifecycle()
            SeriesDetailScreen(
                state = state,
                onSelectSeason = viewModel::selectSeason,
                onPlayEpisode = { episode ->
                    navController.navigate(VodPlayerRoutes.episode(seriesId, episode.id))
                },
                onToggleFavorite = viewModel::toggleFavorite,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = MultiWallRoutes.PATTERN,
            arguments = listOf(
                navArgument(MultiWallRoutes.ARG_COUNT) {
                    type = NavType.IntType
                    defaultValue = 4
                },
            ),
        ) { entry ->
            val count = entry.arguments?.getInt(MultiWallRoutes.ARG_COUNT) ?: 4
            val viewModel: MultiWallViewModel = viewModel(
                factory = MultiWallViewModel.factory(graph.channels, graph.streamResolver, count),
            )
            val state by viewModel.state.collectAsStateWithLifecycle()
            MultiWallScreen(
                state = state,
                onLayout = viewModel::setLayout,
                onFocus = viewModel::focus,
                onOpenPicker = viewModel::openPicker,
                onClosePicker = viewModel::closePicker,
                onQuery = viewModel::setQuery,
                onAssign = viewModel::assign,
                onClear = viewModel::clearPane,
                onBack = { navController.backToTabRoot(MobileTabs.LIVE) },
            )
        }

        composable(RecordingsRoutes.ROUTE) {
            val viewModel: RecordingsViewModel = viewModel(
                factory = RecordingsViewModel.factory(
                    store = graph.recordings,
                    scheduler = graph.recordingScheduler,
                    storage = graph.recordingStorage,
                ),
            )
            val state by viewModel.state.collectAsStateWithLifecycle()
            RecordingsScreen(
                state = state,
                onPlay = { entry ->
                    if (entry.filePath != null) {
                        navController.navigate(RecordingPlayerRoutes.of(entry.id))
                    }
                },
                onCancel = viewModel::cancel,
                onDelete = viewModel::delete,
                onRefresh = viewModel::refresh,
                onBack = { navController.backToTabRoot(MobileTabs.LIVE) },
            )
        }

        composable(
            route = RecordingPlayerRoutes.PATTERN,
            arguments = listOf(
                navArgument(RecordingPlayerRoutes.ARG_ID) { type = NavType.StringType },
            ),
        ) { entry ->
            val recordingId = entry.arguments?.getString(RecordingPlayerRoutes.ARG_ID).orEmpty()
            val recording = remember(recordingId) { graph.recordings.entry(recordingId) }
            val controller = rememberPlayerController()
            val viewModel: PlayerViewModel = viewModel(
                factory = PlayerViewModel.factory(
                    playbackController = controller,
                    repository = graph.channels,
                    resolver = channelResolver,
                    preferenceStore = graph.preferences,
                ),
            )
            LaunchedEffect(recordingId) {
                val path = recording?.filePath
                if (path == null) {
                    navController.popBackStack()
                    return@LaunchedEffect
                }
                viewModel.openVod(
                    VodRequest(
                        title = recording.title,
                        resolve = {
                            dev.anon.novatv.core.sync.ResolvedStream(
                                url = Uri.fromFile(File(path)).toString(),
                            )
                        },
                    ),
                )
            }
            PlayerRouteContent(
                controller = controller,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }

        composable(PlaylistsRoutes.LIST) {
            val viewModel: PlaylistsViewModel = viewModel(
                factory = PlaylistsViewModel.factory(
                    graph.playlists,
                    graph.syncEngine,
                    graph.syncScheduler,
                ),
            )
            val state by viewModel.state.collectAsStateWithLifecycle()
            PlaylistsScreen(
                state = state,
                onAdd = { navController.navigate(PlaylistsRoutes.form()) },
                onEdit = { playlist -> navController.navigate(PlaylistsRoutes.form(playlist.id)) },
                onRefresh = viewModel::refresh,
                onToggleActive = { playlist -> viewModel.setActive(playlist, !playlist.isActive) },
                onMoveUp = { playlist -> viewModel.move(playlist, -1) },
                onMoveDown = { playlist -> viewModel.move(playlist, 1) },
                onDelete = viewModel::delete,
                onBackToLive = { navController.popBackStack(Routes.LIVE, false) },
            )
        }

        composable(
            route = PlaylistsRoutes.FORM,
            arguments = listOf(
                navArgument("playlistId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { entry ->
            val playlistId = entry.arguments?.getLong("playlistId")?.takeIf { it > 0 }
            val viewModel: PlaylistFormViewModel = viewModel(
                factory = PlaylistFormViewModel.factory(graph.playlists, playlistId, graph.registry),
            )
            val state by viewModel.state.collectAsStateWithLifecycle()
            PlaylistFormScreen(
                state = state,
                onName = viewModel::setName,
                onType = viewModel::setType,
                onUrl = viewModel::setUrl,
                onUsername = viewModel::setUsername,
                onPassword = viewModel::setPassword,
                onEpgUrl = viewModel::setEpgUrl,
                onUserAgent = viewModel::setUserAgent,
                onReferer = viewModel::setReferer,
                onSyncInterval = viewModel::setSyncInterval,
                onTest = viewModel::test,
                onSave = viewModel::save,
                onBack = { navController.popBackStack() },
                onSaved = { id ->
                    // Kaynak eklenince kanallar hemen gelsin; senkron ekran
                    // kapansa da uygulama kapsamında sürer.
                    graph.syncEngine.requestSync(id)
                    navController.popBackStack()
                },
            )
        }

        composable(
            route = PlayerRoutes.PATTERN,
            arguments = listOf(
                navArgument(PlayerRoutes.ARG_CHANNEL_ID) { type = NavType.LongType },
                navArgument(PlayerRoutes.ARG_CATCHUP_START) {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument(PlayerRoutes.ARG_CATCHUP_MINUTES) {
                    type = NavType.IntType
                    defaultValue = 0
                },
            ),
        ) { entry ->
            val channelId = entry.arguments?.getLong(PlayerRoutes.ARG_CHANNEL_ID) ?: -1L
            val catchupStart = entry.arguments?.getLong(PlayerRoutes.ARG_CATCHUP_START) ?: -1L
            val catchupMinutes = entry.arguments?.getInt(PlayerRoutes.ARG_CATCHUP_MINUTES) ?: 0
            val controller = rememberPlayerController()
            val viewModel: PlayerViewModel = viewModel(
                factory = PlayerViewModel.factory(
                    playbackController = controller,
                    repository = graph.channels,
                    resolver = channelResolver,
                    preferenceStore = graph.preferences,
                ),
            )
            LaunchedEffect(channelId, catchupStart, catchupMinutes) {
                viewModel.open(channelId, catchupStart, catchupMinutes)
            }
            PlayerRouteContent(
                controller = controller,
                viewModel = viewModel,
                sleepMinutes = appearance.sleepMinutes,
                autoFrameRate = appearance.autoFrameRate,
                // Oynatıcıyı açan sekmeye dön; ekran geri çağrısı zaten oradaki sekme köküne iner.
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = VodPlayerRoutes.PATTERN,
            arguments = listOf(
                navArgument(VodPlayerRoutes.ARG_MOVIE_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument(VodPlayerRoutes.ARG_SERIES_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument(VodPlayerRoutes.ARG_EPISODE_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { entry ->
            val movieId = entry.arguments?.getLong(VodPlayerRoutes.ARG_MOVIE_ID) ?: -1L
            val seriesId = entry.arguments?.getLong(VodPlayerRoutes.ARG_SERIES_ID) ?: -1L
            val episodeId = entry.arguments?.getLong(VodPlayerRoutes.ARG_EPISODE_ID) ?: -1L
            val controller = rememberPlayerController()
            val viewModel: PlayerViewModel = viewModel(
                factory = PlayerViewModel.factory(
                    playbackController = controller,
                    repository = graph.channels,
                    resolver = channelResolver,
                    preferenceStore = graph.preferences,
                ),
            )
            LaunchedEffect(movieId, seriesId, episodeId) {
                if (movieId > 0) {
                    val movie = graph.library.movie(movieId) ?: return@LaunchedEffect
                    val startAt = graph.library.resumePosition(ItemType.MOVIE, movie.id)
                    viewModel.openVod(
                        VodRequest(
                            title = movie.name,
                            startPositionMs = startAt,
                            resolve = { graph.library.movieStream(movie) },
                            onProgress = { position, duration ->
                                graph.library.recordWatch(
                                    ItemType.MOVIE,
                                    movie.id,
                                    position,
                                    duration,
                                )
                            },
                        ),
                    )
                } else if (seriesId > 0 && episodeId > 0) {
                    val detail = graph.library.seriesDetail(seriesId).first() ?: return@LaunchedEffect
                    val episode = detail.episodes.firstOrNull { it.id == episodeId }
                        ?: return@LaunchedEffect
                    val startAt = graph.library.resumePosition(ItemType.EPISODE, episode.id)
                    viewModel.openVod(
                        VodRequest(
                            title = detail.series.name + " · " + episode.title,
                            startPositionMs = startAt,
                            resolve = { graph.library.episodeStream(seriesId, episode) },
                            onProgress = { position, duration ->
                                graph.library.recordWatch(
                                    ItemType.EPISODE,
                                    episode.id,
                                    position,
                                    duration,
                                )
                            },
                        ),
                    )
                }
            }
            PlayerRouteContent(
                controller = controller,
                viewModel = viewModel,
                sleepMinutes = appearance.sleepMinutes,
                autoFrameRate = appearance.autoFrameRate,
                // Oynatıcıyı açan sekmeye dön.
                onBack = { navController.popBackStack() },
            )
        }

        composable(SettingsRoutes.SETTINGS) {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val viewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.factory(
                    preferences = graph.preferences,
                    recordingPath = graph.recordingStorage.directory().absolutePath,
                ),
            )
            val state by viewModel.state.collectAsStateWithLifecycle()
            val backupLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/json"),
            ) { uri ->
                if (uri != null) {
                    viewModel.setBusy(true)
                    scope.launch { viewModel.backupFinished(graph.backup.backup(uri)) }
                }
            }
            val restoreLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument(),
            ) { uri ->
                if (uri != null) {
                    viewModel.setBusy(true)
                    scope.launch { viewModel.backupFinished(graph.backup.restore(uri)) }
                }
            }
            SettingsScreen(
                state = state,
                onDark = viewModel::setDark,
                onTypeScale = viewModel::setTypeScale,
                onClock24h = viewModel::setClock24h,
                onEpgRowHeight = viewModel::setEpgRowHeight,
                onEpgWindowDays = viewModel::setEpgWindowDays,
                onAutoFrameRate = viewModel::setAutoFrameRate,
                onAutoStart = { enabled ->
                    viewModel.setAutoStart(enabled)
                    // Android 10+ arka plandan etkinlik açmayı kısıtlar; tek
                    // kullanıcı-onaylı istisna üstte gösterme izni.
                    if (enabled && !android.provider.Settings.canDrawOverlays(context)) {
                        runCatching {
                            context.startActivity(
                                android.content.Intent(
                                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    android.net.Uri.parse("package:${context.packageName}"),
                                ),
                            )
                        }
                    }
                },
                onSleepMinutes = viewModel::setSleepMinutes,
                onBackup = {
                    if (hasDocumentPicker(context)) {
                        backupLauncher.launch("novatv-backup.json")
                    } else {
                        viewModel.setBusy(true)
                        scope.launch { viewModel.backupFinished(graph.backup.backup(null)) }
                    }
                },
                onRestore = {
                    if (hasDocumentPicker(context)) {
                        restoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    } else {
                        viewModel.setBusy(true)
                        scope.launch { viewModel.backupFinished(graph.backup.restore(null)) }
                    }
                },
                onProfiles = { navController.navigate(SettingsRoutes.PROFILES) },
                onSearch = { navController.navigate(SettingsRoutes.SEARCH) },
                onOpenRecordings = { navController.openTab(MobileTabs.RECORDINGS) },
                onBack = { navController.backToTabRoot(MobileTabs.LIVE) },
            )
        }

        composable(SettingsRoutes.PROFILES) {
            val viewModel: ProfileViewModel = viewModel(
                factory = ProfileViewModel.factory(graph.profiles),
            )
            val state by viewModel.state.collectAsStateWithLifecycle()
            ProfilesScreen(
                state = state,
                onSelect = viewModel::select,
                onSubmitPin = viewModel::submitPin,
                onCancelPin = viewModel::cancelPin,
                onAdd = viewModel::add,
                onUpdate = viewModel::update,
                onRemovePin = viewModel::removePin,
                onDelete = viewModel::delete,
                onBack = { navController.popBackStack() },
            )
        }

        composable(SettingsRoutes.SEARCH) {
            val viewModel: SearchViewModel = viewModel(
                factory = SearchViewModel.factory(graph.search),
            )
            val state by viewModel.state.collectAsStateWithLifecycle()
            SearchScreen(
                state = state,
                onQuery = viewModel::setQuery,
                onFilter = viewModel::setFilter,
                onSelect = { hit ->
                    when (hit.kind) {
                        SearchKind.CHANNEL -> navController.navigate(PlayerRoutes.of(hit.id))
                        SearchKind.MOVIE -> navController.navigate(VodPlayerRoutes.movie(hit.id))
                        SearchKind.SERIES -> navController.navigate(LibraryRoutes.series(hit.id))
                        SearchKind.PROGRAM -> if (hit.id > 0) {
                            navController.navigate(PlayerRoutes.of(hit.id))
                        }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
    }
}

/**
 * Canlı sekmesindeki boş açılış yüzeyi.
 *
 * Kaynak yokken `LiveScreen` hiç kurulmaz: altında etkin ama görünmez bir
 * kanal listesi bırakmak, panelin kapatmadığı yerlerden satır ve arama
 * alanının dokunulması demekti. Bunun yerine ekranın tamamı bu yüzey olur;
 * kaynak eklenince yüzey kalkar ve `content()` normal `LiveScreen` olarak
 * açılır. Eylemler mevcut akışlar: `PlaylistsRoutes.form()` ve Ayarlar'daki
 * yedek geri yükleme (`OpenDocument` + `BackupManager`).
 *
 * Geri yükleme metni kısa tutulmaz: `BackupManager` yalnız görünüm ve profil
 * adlarını taşır, kanal listesi yedeğe girmez. Metin bunu açıkça söylemezse
 * kullanıcı kaynak bekleyip boş ekranda kalır.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SourceOnboardingGate(
    visible: Boolean,
    graph: NovaGraph,
    navController: NavHostController,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (!visible) {
        content()
        return
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                message = runCatching { graph.backup.restore(uri) }
                    .getOrElse { "Yedek geri yüklenemedi" }
            }
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NovaPanel(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("source-onboarding"),
            width = Dp.Unspecified,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                NovaLabel("Kaynak")
                Text(text = "Henüz kaynak yok", style = NovaType.Title)
                Text(
                    text = "Kanal listesi eklemek için bir kaynak gerekir. " +
                        "TV yedeği yalnız görünüm ayarlarını ve profil adlarını " +
                        "taşır; kanal listeniz yedekte yer almaz.",
                    style = NovaType.BodyMuted,
                )
            }
            NovaPill(
                label = "Kaynak ekle",
                onClick = { navController.navigate(PlaylistsRoutes.form()) },
                accent = true,
                testTag = "onboarding-add-source",
            )
            // Satır 56 dp: pill'in dokunma hedefi tek başına 48 dp'yi geçer.
            NovaTapSurface(
                onClick = {
                    if (hasDocumentPicker(context)) {
                        restoreLauncher.launch(
                            arrayOf("application/json", "text/plain", "*/*"),
                        )
                    } else {
                        scope.launch {
                            message = runCatching { graph.backup.restore(null) }
                                .getOrElse { "Yedek geri yüklenemedi" }
                        }
                    }
                },
                contentColor = NovaColors.Text,
                focusedContentColor = NovaColors.FocusInk,
                focusedContainerColor = NovaColors.Focus,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag("onboarding-restore-backup"),
            ) { state ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "TV yedeğini geri yükle (görünüm ve profiller)",
                        style = NovaType.Body.copy(color = state.contentColor),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            message?.let { text ->
                Text(
                    text = text,
                    style = NovaType.Small.copy(color = NovaColors.Accent),
                    modifier = Modifier.testTag("onboarding-message"),
                )
            }
        }
    }
}

/** Akış çözümleyicisini grafiğe bağlar; TV'dekiyle aynı sözleşme. */
private class GraphChannelResolver(private val graph: NovaGraph) : ChannelStreamResolver {
    override suspend fun resolve(channel: Channel) = graph.streamResolver.resolve(channel)

    override suspend fun resolveCatchup(channel: Channel, at: Instant, minutes: Int) =
        graph.streamResolver.resolveCatchupStream(channel, at, minutes)
}

/**
 * Oynatıcı rotaları güvenli alanı aşar: video sistem çubuklarının altına
 * kadar uzanır ve bu ekranlar zaten gezinme çubuğu çizmez.
 */
private fun isFullBleedRoute(route: String?): Boolean = when (route) {
    PlayerRoutes.PATTERN,
    VodPlayerRoutes.PATTERN,
    RecordingPlayerRoutes.PATTERN,
    -> true
    else -> false
}

/**
 * Açılış hedefi: kaynak yoksa her zaman Canlı. Kaynak varken son izlenen
 * kanalın rotası varsa oradan devam edilir.
 *
 * Kaynak yokken son kanal rotası yok sayılır: geçmiş silinmiş bir kaynağın
 * kanalını işaret edebilir ve oynatıcı boş açılır.
 */
internal fun startDestination(hasSources: Boolean, lastChannelRoute: String?): String =
    if (hasSources && lastChannelRoute != null) lastChannelRoute else Routes.LIVE

/**
 * Boş açılış yönlendirmesi gerekiyor mu: hiç kaynak yokken Canlı ekranı
 * kanal yerine kaynak ekleme çağrısı gösterir.
 */
internal fun needsSourceOnboarding(hasSources: Boolean): Boolean = !hasSources

/**
 * Kullanılabilir kaynak sayısı: yalnız etkin listeler sayılır, çünkü kanallar
 * da yalnız etkin listelerden gelir. Açılış kararı ve boş açılış yüzeyi aynı
 * kuralı buradan okur; iki yerde ayrışmaz.
 */
internal fun activeSourceCount(playlists: List<Playlist>): Int = playlists.count { it.isActive }

/**
 * Açılış kararının girdileri. İkisi de depodan bir kez okunur; okuma
 * başarısız olursa navigasyon çökmesin diye "kaynak yok, Canlı"ya düşülür.
 */
private data class StartInputs(val hasSources: Boolean, val lastChannelRoute: String?)

private suspend fun resolveStartInputs(graph: NovaGraph): StartInputs = runCatching {
    val hasSources = activeSourceCount(graph.playlists.observeAll().first()) > 0
    val route = if (hasSources) PlayerRoutes.lastWatched(graph.channels) else null
    StartInputs(hasSources = hasSources, lastChannelRoute = route)
}.getOrElse { StartInputs(hasSources = false, lastChannelRoute = null) }

/** Rota → sekme eşlemesi; alt ekranlar (oynatıcı, detay, form) `null`. */
private fun tabForRoute(route: String?): String? = when (route) {
    Routes.LIVE -> MobileTabs.LIVE
    EpgRoutes.PATTERN -> MobileTabs.GUIDE
    LibraryRoutes.ROUTE -> MobileTabs.LIBRARY
    RecordingsRoutes.ROUTE -> MobileTabs.RECORDINGS
    MORE_ROUTE -> MobileTabs.MORE
    // Ayarlar menüden açılır; menüye bağlı olduğu için "Daha" sekmesi seçili kalır.
    SettingsRoutes.SETTINGS -> MobileTabs.MORE
    else -> null
}

/**
 * Sekme dokunuşu: standart alt gezinti deseni. Yığın her zaman
 * `[Canlı, geçerli sekme]`; sekme durumu `saveState` ile korunup
 * `restoreState` ile geri gelir. Hedef zaten üstteyse bir şey yapılmaz.
 */
private fun NavHostController.openTab(key: String) {
    val destination = when (key) {
        MobileTabs.LIVE -> Routes.LIVE
        MobileTabs.GUIDE -> EpgRoutes.of()
        MobileTabs.LIBRARY -> LibraryRoutes.ROUTE
        MobileTabs.RECORDINGS -> RecordingsRoutes.ROUTE
        MobileTabs.MORE -> MORE_ROUTE
        else -> return
    }
    if (currentDestination?.route == destination) return
    navigate(destination) {
        popUpTo(Routes.LIVE) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Kök ekranın "Canlı" düğmesi: Canlı'nın üstündeki her şeyi (sekme kökü
 * dahil) kapatır ve Canlı'ya döner. Kök ekran asla yığından düşmez.
 */
private fun NavHostController.backToTabRoot(key: String) {
    val root = when (key) {
        MobileTabs.LIVE -> Routes.LIVE
        else -> return
    }
    popBackStack(root, false)
}

@Composable
private fun rememberPlayerController(): PlaybackController {
    val context = LocalContext.current
    val controller = remember { PlaybackController(context) }
    DisposableEffect(controller) {
        onDispose { controller.release() }
    }
    return controller
}
