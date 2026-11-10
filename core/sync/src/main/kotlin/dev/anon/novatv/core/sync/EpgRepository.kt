package dev.anon.novatv.core.sync

import android.util.Log
import dev.anon.novatv.core.database.NovaDatabase
import dev.anon.novatv.core.database.entity.EpgChannelEntity
import dev.anon.novatv.core.database.entity.EpgProgramEntity
import dev.anon.novatv.core.database.mapper.toModel
import dev.anon.novatv.core.epg.XmltvParser
import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.core.model.EpgProgram
import dev.anon.novatv.core.network.NovaHttp
import dev.anon.novatv.core.network.SourceHttp
import dev.anon.novatv.core.network.SourceNetworkException
import dev.anon.novatv.core.settings.PreferenceStore
import dev.anon.novatv.core.source.SourceAdapterRegistry
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class EpgSyncResult(
    val channels: Int = 0,
    val programs: Int = 0,
    val skippedReason: String? = null,
) {
    val ok: Boolean get() = skippedReason == null
}

/** Rehber ızgarasında bir kanal ve o kanalın penceredeki programları. */
data class GuideRow(
    val channel: Channel,
    val programs: List<EpgProgram>,
)

data class NowNext(
    val now: EpgProgram? = null,
    val next: EpgProgram? = null,
)

/**
 * XMLTV senkronu ve rehber sorguları.
 *
 * `tvg-id` eşleşmeyen programlar veritabanına yazılmaz: rehberde
 * gösterilemeyecekleri için yüz binlerce satır boşuna tutulmaz.
 */
class EpgRepository(
    private val db: NovaDatabase,
    private val playlists: PlaylistRepository,
    private val registry: SourceAdapterRegistry,
    private val preferences: PreferenceStore? = null,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {

    /** Son rehber senkronu [maxAgeMs]'den eskiyse yeniler. */
    suspend fun syncIfStale(playlistId: Long, maxAgeMs: Long = STALE_AFTER_MS): EpgSyncResult {
        val last = preferences?.epgLastSync(playlistId)?.first()
        val now = clock()
        if (last != null && now - last in 0 until maxAgeMs) {
            return EpgSyncResult(skippedReason = "Rehber güncel")
        }
        return sync(playlistId)
    }

    /**
     * Kaynağın XMLTV adresini indirir, `tvg-id` ile kanallara bağlar.
     * Aynı kaynağın eski rehber satırları önce silinir; yarım kalan indirme
     * eski rehberi bırakmaz.
     */
    suspend fun sync(playlistId: Long, now: Long = clock()): EpgSyncResult {
        val (playlist, credentials) = playlists.load(playlistId)
        if (!playlist.isActive) return EpgSyncResult(skippedReason = "Kaynak pasif")

        val adapter = registry.adapterFor(playlist, credentials)
        val source = adapter.epg() ?: return EpgSyncResult(skippedReason = "Kaynak EPG sunmuyor")

        val channels = db.channelDao().byPlaylist(playlistId)
        val channelByTvg = HashMap<String, Long>(channels.size * 2)
        channels.forEach { entity ->
            val tvg = entity.tvgId?.trim()?.takeIf { it.isNotEmpty() } ?: return@forEach
            channelByTvg[tvg] = entity.id
            channelByTvg[tvg.lowercase()] = entity.id
        }
        if (channelByTvg.isEmpty()) {
            return EpgSyncResult(skippedReason = "Kanallarda tvg-id yok")
        }
        Log.i(TAG, "epg sync start playlist=$playlistId channels=${channels.size} tvg=${channelByTvg.size / 2}")

        val windowDays = runCatching { preferences?.appearance?.first()?.windowDays }
            .getOrNull() ?: DEFAULT_WINDOW_DAYS
        val from = now - DAY_MS
        val to = now + windowDays * DAY_MS

        db.epgDao().clearPlaylist(playlistId)

        // Programlardan önce kanal satırlarını hazırla: XMLTV bazı panellerde
        // <channel> bildirimlerini programlardan sonra gönderiyor.
        val epgChannelIdByTvg = HashMap<String, Long>(channels.size)
        var epgChannelCount = 0
        channels.forEach { entity ->
            val tvg = entity.tvgId?.trim()?.takeIf { it.isNotEmpty() } ?: return@forEach
            val epgChannelId = db.epgDao().upsertChannel(
                EpgChannelEntity(
                    playlistId = playlistId,
                    tvgId = tvg,
                    displayName = entity.name,
                    iconUrl = entity.logoUrl,
                ),
            )
            epgChannelIdByTvg[tvg] = epgChannelId
            epgChannelIdByTvg[tvg.lowercase()] = epgChannelId
            epgChannelCount++
        }

        fun epgChannelId(tvg: String): Long? =
            epgChannelIdByTvg[tvg] ?: epgChannelIdByTvg[tvg.lowercase()]

        fun channelId(tvg: String): Long? =
            channelByTvg[tvg] ?: channelByTvg[tvg.lowercase()]

        val http = SourceHttp(
            client = NovaHttp.client(),
            userAgent = playlist.userAgent ?: NovaHttp.DEFAULT_USER_AGENT,
            referer = playlist.referer,
        )

        var stored = 0
        var seenPrograms = 0
        var seenChannels = 0
        var windowSkipped = 0
        var unmatched = 0
        val buffer = ArrayList<EpgProgramEntity>(BATCH_SIZE)

        // SAX geri çağrıları askıya alınamaz; yazma IO iş parçacığında
        // engellenerek yapılır (çağıran zaten withContext(IO) içinde).
        fun writeBuffer() {
            if (buffer.isEmpty()) return
            val chunk = buffer.toList()
            buffer.clear()
            runBlocking { db.epgDao().upsertPrograms(chunk) }
            stored += chunk.size
        }

        // Ağ ve ayrıştırma ana iş parçacığında çalışmamalı: OkHttp yanıtı
        // kapatırken kalan baytları okuyor ve StrictMode bunu engelliyor.
        withContext(Dispatchers.IO) {
            http.openStream(source.url, source.headers).use { body ->
                val stream = body.body?.byteStream()
                    ?: throw SourceNetworkException("EPG yanıtı boş")
                stream.use { input ->
                    XmltvParser(zone).parse(
                        input = input,
                        onChannel = { xmlChannel ->
                            seenChannels++
                            val channelRowId = channelId(xmlChannel.id) ?: return@parse
                            val epgId = epgChannelId(xmlChannel.id) ?: return@parse
                            runBlocking {
                                db.epgDao().upsertChannel(
                                    EpgChannelEntity(
                                        id = epgId,
                                        playlistId = playlistId,
                                        tvgId = xmlChannel.id,
                                        displayName = xmlChannel.displayName,
                                        iconUrl = xmlChannel.iconUrl
                                            ?: db.channelDao().byId(channelRowId)?.logoUrl,
                                    ),
                                )
                            }
                        },
                        onProgram = { program ->
                            seenPrograms++
                            val epgId = epgChannelId(program.channelId) ?: run {
                                unmatched++
                                return@parse
                            }
                            val channelRowId = channelId(program.channelId) ?: run {
                                unmatched++
                                return@parse
                            }
                            val endAt = program.endAt ?: (program.startAt + DEFAULT_PROGRAM_MS)
                            if (endAt <= from || program.startAt >= to) {
                                windowSkipped++
                                return@parse
                            }
                            buffer += EpgProgramEntity(
                                epgChannelId = epgId,
                                channelId = channelRowId,
                                startAt = program.startAt,
                                endAt = endAt,
                                title = program.title,
                                subtitle = program.subTitle,
                                description = program.description,
                                category = program.category,
                                rating = program.rating,
                                episodeNum = program.episodeNum,
                                isNew = program.isNew,
                            )
                            if (buffer.size >= BATCH_SIZE) writeBuffer()
                        },
                    )
                }
            }
            writeBuffer()
        }
        Log.i(
            TAG,
            "epg sync done playlist=$playlistId xmlChannels=$seenChannels programs=$seenPrograms " +
                "stored=$stored unmatched=$unmatched windowSkipped=$windowSkipped " +
                "from=$from to=$to now=$now",
        )

        db.epgDao().deleteEndedBefore(now - 2 * DAY_MS)
        db.epgDao().deleteStartingAfter(to + DAY_MS)
        preferences?.setEpgLastSync(playlistId, now)

        return EpgSyncResult(channels = epgChannelCount, programs = stored)
    }

    /** Bir grup/kanal kümesinin pencere içindeki rehberi. */
    fun guide(
        group: String? = null,
        from: Long = clock(),
        to: Long = from + DAY_MS,
    ): Flow<List<GuideRow>> =
        combine(
            db.playlistDao().observeActive(),
            preferences?.activeProfileId ?: flowOf(1L),
        ) { active, profileId -> active.map { it.id } to profileId }
            .flatMapLatest { (playlistIds, profileId) ->
                if (playlistIds.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    db.channelDao().observeForGuide(playlistIds, group, profileId)
                        .flatMapLatest { channels ->
                            val ids = channels.map { it.id }
                            if (ids.isEmpty()) {
                                flowOf(emptyList())
                            } else {
                                db.epgDao().observeWindow(ids, from, to).map { programs ->
                                    val byChannel = programs.groupBy { it.channelId }
                                    channels.map { entity ->
                                        GuideRow(
                                            channel = entity.toModel(),
                                            programs = byChannel[entity.id].orEmpty().map { it.toModel() },
                                        )
                                    }
                                }
                            }
                        }
                }
            }

    /**
     * Şimdi/sıradaki. Program bitişinde sorgunun kendiliğinden ilerlemesi için
     * 30 saniyelik nabız akışı kullanılır.
     */
    fun nowNext(channelId: Long): Flow<NowNext> =
        ticker().flatMapLatest { now ->
            combine(
                db.epgDao().observeCurrent(channelId, now),
                db.epgDao().observeNext(channelId, now),
            ) { current, next ->
                NowNext(now = current?.toModel(), next = next?.toModel())
            }
        }

    suspend fun programs(channelId: Long, from: Long, to: Long): List<EpgProgram> =
        db.epgDao().programsFor(channelId, from, to).map { it.toModel() }

    /** Tüm aktif kaynakların rehberini yeniler; biri hata verse diğerleri sürer. */
    suspend fun syncAll(force: Boolean = true): List<EpgSyncResult> {
        val active = db.playlistDao().observeActive().first()
        return active.map { playlist ->
            runCatching { if (force) sync(playlist.id) else syncIfStale(playlist.id) }
                .onFailure { error ->
                    Log.w(TAG, "epg sync failed playlist=${playlist.id}", error)
                }
                .getOrElse { error ->
                    EpgSyncResult(skippedReason = error.message ?: "Rehber yenilenemedi")
                }
        }
    }

    private fun ticker(): Flow<Long> = flow {
        while (true) {
            emit(clock())
            delay(TICK_MS)
        }
    }

    companion object {
        private const val TAG = "EpgSync"
        const val DAY_MS = 24 * 60 * 60 * 1_000L
        const val STALE_AFTER_MS = 6 * 60 * 60 * 1_000L
        const val DEFAULT_WINDOW_DAYS = 3
        const val DEFAULT_PROGRAM_MS = 30 * 60 * 1_000L
        private const val BATCH_SIZE = 500
        private const val TICK_MS = 30_000L
    }
}
