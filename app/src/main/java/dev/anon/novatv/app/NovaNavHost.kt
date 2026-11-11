package dev.anon.novatv.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.paging.compose.collectAsLazyPagingItems
import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.core.model.ItemType
import dev.anon.novatv.core.settings.Appearance
import dev.anon.novatv.core.sync.SearchKind
import dev.anon.novatv.core.player.PlaybackController
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
import dev.anon.novatv.feature.player.PlayerScreen
import dev.anon.novatv.feature.player.PlayerViewModel
import dev.anon.novatv.feature.player.VodPlayerRoutes
import dev.anon.novatv.feature.player.VodRequest
import dev.anon.novatv.feature.player.findActivity
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
import java.io.File
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object Routes {
    const val LIVE = "live"
}

/** Seçilen dosyanın uzantısına göre Media3 altyazı MIME türü. */
private fun subtitleMimeType(context: Context, uri: Uri): String {
    val name = (uri.lastPathSegment ?: uri.path.orEmpty()).lowercase()
    return when {
        name.endsWith(".srt") -> "application/x-subrip"
        name.endsWith(".vtt") -> "text/vtt"
        name.endsWith(".ssa") -> "text/x-ssa"
        name.endsWith(".ass") -> "text/x-ass"
        else -> context.contentResolver.getType(uri) ?: "application/x-subrip"
    }
}

/**
 * Android TV görüntülerinde belge seçici yerine yalnızca "uygulama yok" diyen
 * bir saplama (stub) olabilir; onu gerçek seçici sayma.
 */
private fun hasDocumentPicker(context: Context): Boolean {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        .addCategory(Intent.CATEGORY_OPENABLE)
        .setType("*/*")
    val resolved = context.packageManager.resolveActivity(intent, 0) ?: return false
    val packageName = resolved.activityInfo?.packageName ?: return false
    return !packageName.startsWith("com.android.tv.frameworkpackagestubs") &&
        !packageName.startsWith("com.google.android.tv.frameworkpackagestubs")
}

@Composable
fun NovaNavHost(graph: NovaGraph) {
    val navController = rememberNavController()
    val appearance by graph.preferences.appearance.collectAsStateWithLifecycle(
        initialValue = Appearance(),
    )

    // Açılış hedefi: son izlenen kanal varsa oynatıcı, yoksa canlı TV.
    // Navigasyon yığınına sonradan eklemek yerine başlangıç hedefi seçilir;
    // böylece açılışta yaşam döngüsü yarışı oluşmaz ve Back temiz çalışır.
    var startDestination by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        startDestination = runCatching { PlayerRoutes.lastWatched(graph.channels) }
            .getOrNull()
            ?: Routes.LIVE
    }

    val channelResolver = remember(graph) {
        object : ChannelStreamResolver {
            override suspend fun resolve(channel: Channel) = graph.streamResolver.resolve(channel)

            override suspend fun resolveCatchup(
                channel: Channel,
                at: Instant,
                minutes: Int,
            ) = graph.streamResolver.resolveCatchupStream(channel, at, minutes)
        }
    }

    val start = startDestination ?: return
    val parentalGuard = rememberParentalGuard(graph.profiles)

    NavHost(navController = navController, startDestination = start) {
        composable(Routes.LIVE) {
            val viewModel: LiveViewModel = viewModel(
                factory = LiveViewModel.factory(graph.channels, graph.preferences),
            )
            val state by viewModel.state.collectAsStateWithLifecycle()
            val channels = viewModel.channels.collectAsLazyPagingItems()
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
                onOpenRecordings = { navController.navigate(RecordingsRoutes.ROUTE) },
                onOpenMultiwall = { navController.navigate(MultiWallRoutes.of(4)) },
                onOpenSearch = { navController.navigate(SettingsRoutes.SEARCH) },
                onOpenSettings = { navController.navigate(SettingsRoutes.SETTINGS) },
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
                onBack = {
                    if (!navController.popBackStack()) navController.navigate(Routes.LIVE)
                },
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
                onBack = {
                    if (!navController.popBackStack()) navController.navigate(Routes.LIVE)
                },
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
                onBack = {
                    if (!navController.popBackStack()) navController.navigate(Routes.LIVE)
                },
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
                onBack = {
                    if (!navController.popBackStack()) navController.navigate(Routes.LIVE)
                },
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
                onBack = {
                    if (!navController.popBackStack()) navController.navigate(Routes.LIVE)
                },
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
                onBack = {
                    if (!navController.popBackStack()) navController.navigate(Routes.LIVE)
                },
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
                onOpenRecordings = { navController.navigate(RecordingsRoutes.ROUTE) },
                onBack = {
                    if (!navController.popBackStack()) navController.navigate(Routes.LIVE)
                },
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

    ParentalGuardDialog(parentalGuard)
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

@Composable
private fun PlayerRouteContent(
    controller: PlaybackController,
    viewModel: PlayerViewModel,
    sleepMinutes: Int = 0,
    autoFrameRate: Boolean = false,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val frameRate by controller.videoFrameRate.collectAsStateWithLifecycle()

    LaunchedEffect(autoFrameRate, frameRate) {
        if (autoFrameRate) dev.anon.novatv.feature.player.AutoFrameRate.apply(activity, frameRate)
    }
    // Oynatıcıdan çıkınca ekran modu varsayılana döner; yoksa menü 24 Hz'de kalır.
    DisposableEffect(autoFrameRate) {
        onDispose {
            if (autoFrameRate) dev.anon.novatv.feature.player.AutoFrameRate.reset(activity)
        }
    }
    LaunchedEffect(sleepMinutes) {
        if (sleepMinutes > 0) {
            delay(sleepMinutes * 60_000L)
            runCatching { controller.pause() }
        }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val subtitlePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) viewModel.setExternalSubtitle(uri, subtitleMimeType(context, uri))
    }
    PlayerScreen(
        state = state,
        controller = controller,
        onToggleOsd = viewModel::toggleOsd,
        onHideOsd = viewModel::hideOsd,
        onRetry = viewModel::retry,
        onBackToChannels = onBack,
        onSeekBack = { viewModel.seekBack() },
        onSeekBackMinute = viewModel::seekBackMinute,
        onCycleAspect = viewModel::cycleAspect,
        onCycleBuffer = viewModel::cycleBuffer,
        onSelectAudio = viewModel::selectAudio,
        onSelectSubtitle = viewModel::selectSubtitle,
        onPickSubtitleFile = {
            // Belge seçici olmayan cihazlarda (bazı TV kutuları) İngilizce
            // sistem toast'u yerine Türkçe bilgi göster.
            if (hasDocumentPicker(context)) {
                subtitlePicker.launch(arrayOf("*/*"))
            } else {
                viewModel.reportMissingSubtitlePicker()
            }
        },
        onChannelStep = viewModel::stepChannel,
        onMenuOpenChange = viewModel::keepOsdVisible,
    )
}
