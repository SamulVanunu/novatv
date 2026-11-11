package dev.anon.novatv.feature.epg

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.core.model.EpgProgram
import dev.anon.novatv.core.sync.GuideRow
import dev.anon.novatv.ui.component.NovaBackground
import dev.anon.novatv.ui.component.NovaLabel
import dev.anon.novatv.ui.component.NovaPill
import dev.anon.novatv.ui.component.NovaRule
import dev.anon.novatv.ui.component.NovaTapSurface
import dev.anon.novatv.ui.theme.NovaColors
import dev.anon.novatv.ui.theme.NovaType

/**
 * Kanalın [now] anındaki şimdiki ve sıradaki programı.
 *
 * Şimdiki program açık aralıktır: `startAt <= now < endAt`, yani programın
 * bittiği an şimdiki sayılmaz. Sıradaki, en erken başlayan gelecek
 * programdır; geçmiş programlar asla sıradaki olmaz. Sıralama girişi değil
 * zamanı esas alır, program yoksa `(null, null)` döner.
 */
internal fun nowNextOf(row: GuideRow, now: Long): Pair<EpgProgram?, EpgProgram?> {
    val current = row.programs
        .filter { it.startAt <= now && now < it.endAt }
        .minByOrNull { it.startAt }
    val next = row.programs
        .filter { it.startAt > now }
        .minByOrNull { it.startAt }
    return current to next
}

/**
 * Telefon/tablet rehberi: kanal başına "şimdi / sıradaki" listesi.
 *
 * 94 dp'lik ızgara 411 dp'de okunmadığı için aynı `EpgUiState` üzerinde tek
 * sütunlu liste çizilir. Satıra dokunmak kanalın tam ekran program sayfasını
 * açar; sayfa durum içindedir, sistem Geri'si [BackHandler] ile listeye döner.
 * TV (`NovaWindow.Expanded`) bu yola hiç girmez.
 */
@Composable
internal fun CompactEpg(
    state: EpgUiState,
    onSelectDay: (Int) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onWatchChannel: (Channel) -> Unit,
    onWatchCatchup: (Channel, EpgProgram) -> Unit,
    onToggleReminder: (Channel, EpgProgram) -> Unit,
    onToggleRecording: (Channel, EpgProgram) -> Unit,
    modifier: Modifier = Modifier,
) {
    var openChannelId by remember { mutableStateOf<Long?>(null) }
    val openRow = state.rows.firstOrNull { it.channel.id == openChannelId }
    // Liste kaydırma konumu burada hatırlanır: detay açılınca liste dalı
    // kompozisyondan düşer, Geri'de aynı durumla geri gelir — kullanıcı
    // kaldığı satırda kalır. Dalın içinde `remember` edilseydi her dönüşte
    // yeni durum doğar ve liste başa sarardı.
    val listState = rememberLazyListState()

    // Rehber yenilenip kanal satırı düşerse açık kalan detay kapanır.
    LaunchedEffect(openChannelId, openRow) {
        if (openChannelId != null && openRow == null) openChannelId = null
    }

    // Detay listeden bağımsız bir durum; Geri tuşu listeye dönsün diye
    // sistem seviyesinde burada yakalanır.
    BackHandler(enabled = openChannelId != null) { openChannelId = null }

    NovaBackground(modifier = modifier) {
        val row = openRow
        if (row != null) {
            CompactProgramPage(
                row = row,
                state = state,
                onBack = { openChannelId = null },
                onWatchChannel = onWatchChannel,
                onWatchCatchup = onWatchCatchup,
                onToggleReminder = onToggleReminder,
                onToggleRecording = onToggleRecording,
            )
        } else {
            CompactGuideList(
                state = state,
                onSelectDay = onSelectDay,
                onRefresh = onRefresh,
                onBack = onBack,
                onOpenChannel = { openChannelId = it },
                listState = listState,
            )
        }
    }
}

/** Gün sekmeleri, başlık ve kanal satırlarından oluşan liste. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CompactGuideList(
    state: EpgUiState,
    onSelectDay: (Int) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onOpenChannel: (Long) -> Unit,
    listState: LazyListState,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                NovaLabel("Rehber")
                Text(
                    text = state.dayLabel,
                    style = NovaType.Title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            NovaPill(
                label = if (state.syncing) "Yenileniyor" else "Yenile",
                onClick = onRefresh,
                enabled = !state.syncing,
                testTag = "epg-refresh",
            )
            NovaPill(label = "Canlı TV", onClick = onBack, testTag = "epg-back")
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (offset in 0 until state.windowDays) {
                NovaPill(
                    label = dayTabLabel(offset),
                    onClick = { onSelectDay(offset) },
                    selected = state.dayOffset == offset,
                    testTag = "epg-day-$offset",
                )
            }
        }

        // Senkron hatası rehber boş olsa da görünür (TV ile aynı davranış).
        state.message?.let { message ->
            Text(
                text = message,
                style = NovaType.Small.copy(color = NovaColors.Warning),
                modifier = Modifier
                    .padding(top = 6.dp)
                    .testTag("epg-message"),
            )
        }

        Spacer(Modifier.height(10.dp))

        when {
            state.loading && state.rows.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("Rehber yükleniyor", style = NovaType.BodyMuted)
            }

            state.rows.isEmpty() -> CompactEmptyGuide(
                message = state.message,
                syncing = state.syncing,
                onRefresh = onRefresh,
            )

            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().testTag("epg-list"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                items(items = state.rows, key = { it.channel.id }) { row ->
                    CompactChannelRow(
                        row = row,
                        state = state,
                        onClick = { onOpenChannel(row.channel.id) },
                    )
                }
            }
        }
    }
}

/** Gün için rehber yoksa: TV'deki aynı metin, dokunulan yenileme hapı. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CompactEmptyGuide(message: String?, syncing: Boolean, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Bu gün için rehber yok",
            style = NovaType.Display.copy(fontSize = 24.sp),
            modifier = Modifier.testTag("epg-empty"),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message
                ?: "Kaynağın EPG (XMLTV) adresi tanımlı değil. Kaynaklar → Düzenle " +
                "bölümünden ekleyip yenileyin.",
            style = NovaType.BodyMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        )
        Spacer(Modifier.height(18.dp))
        NovaPill(
            label = if (syncing) "Yenileniyor" else "Rehberi yenile",
            onClick = onRefresh,
            accent = true,
            enabled = !syncing,
            testTag = "epg-empty-refresh",
        )
    }
}

/** Kanal satırı: logo + ad + numara, altında şimdi/sıradaki satırları. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CompactChannelRow(
    row: GuideRow,
    state: EpgUiState,
    onClick: () -> Unit,
) {
    val now = state.now
    val (current, next) = nowNextOf(row, now)

    NovaTapSurface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag("epg-row-${row.channel.id}"),
        shape = RoundedCornerShape(12.dp),
        containerColor = NovaColors.Surface.copy(alpha = ROW_SURFACE_ALPHA),
        contentColor = NovaColors.Text,
        focusedContainerColor = NovaColors.Focus,
        focusedContentColor = NovaColors.FocusInk,
        pressedContainerColor = NovaColors.Focus,
        pressedContentColor = NovaColors.FocusInk,
        border = BorderStroke(1.dp, NovaColors.Border),
        focusedBorder = BorderStroke(2.dp, NovaColors.Focus),
        pressedBorder = BorderStroke(2.dp, NovaColors.Focus),
    ) { tap ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CompactChannelLogo(name = row.channel.name, modifier = Modifier.size(44.dp))
                Text(
                    text = row.channel.name,
                    style = NovaType.Small.copy(fontSize = 15.sp, color = tap.contentColor),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                row.channel.number?.let { number ->
                    Text(
                        text = number.toString(),
                        style = NovaType.Number.copy(
                            fontSize = 12.sp,
                            color = tap.contentColor.copy(alpha = 0.65f),
                        ),
                        maxLines = 1,
                    )
                }
            }

            Text(
                text = nowNextLine(
                    label = "ŞİMDİ",
                    stamp = NovaColors.Accent,
                    time = current?.let { timeText(it.startAt, state.clock24h) },
                    title = current?.title ?: "yayın bilgisi yok",
                    body = tap.contentColor,
                ),
                style = NovaType.Small,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            ProgressStamp(
                share = current?.let { progressShare(it.startAt, it.endAt, now) } ?: 0f,
            )
            Text(
                text = nowNextLine(
                    label = "SIRADAKİ",
                    stamp = tap.contentColor.copy(alpha = 0.6f),
                    time = next?.let { timeText(it.startAt, state.clock24h) },
                    title = next?.title ?: "yayın bilgisi yok",
                    body = tap.contentColor.copy(alpha = 0.7f),
                ),
                style = NovaType.Small,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Kanalın tam ekran program sayfası: TV'nin aksine diyalog değil, listenin
 * üstüne açılan sayfa. Eylemler TV diyalogla aynı kurallarla **seçili**
 * programa uygulanır; satıra dokunmak seçimi değiştirir.
 */
@Composable
private fun CompactProgramPage(
    row: GuideRow,
    state: EpgUiState,
    onBack: () -> Unit,
    onWatchChannel: (Channel) -> Unit,
    onWatchCatchup: (Channel, EpgProgram) -> Unit,
    onToggleReminder: (Channel, EpgProgram) -> Unit,
    onToggleRecording: (Channel, EpgProgram) -> Unit,
) {
    val channel = row.channel
    val now = state.now
    val (current, _) = nowNextOf(row, now)
    val upcoming = row.programs
        .filter { it.startAt > now }
        .sortedBy { it.startAt }
        .take(DETAIL_UPCOMING)
    val previous = row.programs
        .filter { it.endAt <= now }
        .maxByOrNull { it.endAt }
    // Hedef program seçili başlar: önce şimdiki, o yoksa sıradaki.
    var targetStartAt by remember(channel.id) { mutableStateOf<Long?>(null) }
    val target = row.programs.firstOrNull { it.startAt == targetStartAt }
        ?: current
        ?: upcoming.firstOrNull()
        ?: row.programs.minByOrNull { it.startAt }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NovaPill(label = "Geri", onClick = onBack, testTag = "epg-detail-back")
            Spacer(Modifier.weight(1f))
            Text(
                text = state.dayLabel,
                style = NovaType.Label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        state.message?.let { message ->
            Text(
                text = message,
                style = NovaType.Small.copy(color = NovaColors.Warning),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .testTag("epg-message"),
            )
        }

        Spacer(Modifier.height(14.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CompactChannelLogo(name = channel.name, modifier = Modifier.size(44.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = NovaType.Title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                channel.number?.let { number ->
                    Text(
                        text = number.toString(),
                        style = NovaType.Number.copy(color = NovaColors.TextDim),
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        NovaRule(Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))

        previous?.let { past ->
            NovaLabel("ÖNCEKİ")
            Spacer(Modifier.height(6.dp))
            CompactProgramRow(
                time = timeRange(past.startAt, past.endAt, state.clock24h),
                title = past.title,
                dimmed = true,
                selected = target?.startAt == past.startAt,
                onClick = { targetStartAt = past.startAt },
            )
            Spacer(Modifier.height(12.dp))
        }

        NovaLabel("ŞİMDİ")
        Spacer(Modifier.height(6.dp))
        if (current != null) {
            CompactCurrentBlock(
                program = current,
                clock24h = state.clock24h,
                now = now,
                selected = target?.startAt == current.startAt,
                onClick = { targetStartAt = current.startAt },
            )
        } else {
            Text(text = "yayın bilgisi yok", style = NovaType.BodyMuted)
        }

        Spacer(Modifier.height(14.dp))
        NovaRule(Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))

        NovaLabel("SIRADAKİ")
        Spacer(Modifier.height(6.dp))
        if (upcoming.isEmpty()) {
            Text(text = "yayın bilgisi yok", style = NovaType.BodyMuted)
        } else {
            upcoming.forEachIndexed { index, program ->
                if (index > 0) Spacer(Modifier.height(6.dp))
                CompactProgramRow(
                    time = timeRange(program.startAt, program.endAt, state.clock24h),
                    title = program.title,
                    dimmed = false,
                    selected = target?.startAt == program.startAt,
                    onClick = { targetStartAt = program.startAt },
                )
            }
        }

        target?.let { selectedProgram ->
            Spacer(Modifier.height(16.dp))
            NovaRule(Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            CompactActions(
                channel = channel,
                program = selectedProgram,
                now = now,
                reminded = reminderId(channel.id, selectedProgram.startAt) in state.reminderIds,
                recorded = recordingId(channel.id, selectedProgram.startAt) in state.recordedIds,
                onWatchChannel = { onWatchChannel(channel) },
                onWatchCatchup = { onWatchCatchup(channel, selectedProgram) },
                onToggleReminder = { onToggleReminder(channel, selectedProgram) },
                onToggleRecording = { onToggleRecording(channel, selectedProgram) },
            )
        }

        // Eylem şeridine kaydırdıktan sonra üstteki "Geri" ekran dışında kalır;
        // TV diyalogunun `Kapat`'ı gibi yerinde bir çıkış bırakılır.
        Spacer(Modifier.height(12.dp))
        NovaPill(
            label = "Kapat",
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            testTag = "epg-dismiss",
        )
    }
}

/** Şimdiki program bloğu: ad, saat aralığı ve geçen süre çubuğu. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CompactCurrentBlock(
    program: EpgProgram,
    clock24h: Boolean,
    now: Long,
    selected: Boolean,
    onClick: () -> Unit,
) {
    NovaTapSurface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        containerColor = NovaColors.Surface.copy(alpha = ROW_SURFACE_ALPHA),
        contentColor = NovaColors.Text,
        focusedContainerColor = NovaColors.Focus,
        focusedContentColor = NovaColors.FocusInk,
        pressedContainerColor = NovaColors.Focus,
        pressedContentColor = NovaColors.FocusInk,
        border = BorderStroke(
            1.dp,
            if (selected) NovaColors.Accent.copy(alpha = 0.75f) else NovaColors.Border,
        ),
        focusedBorder = BorderStroke(2.dp, NovaColors.Focus),
        pressedBorder = BorderStroke(2.dp, NovaColors.Focus),
    ) { tap ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = program.title,
                style = NovaType.Title.copy(fontSize = 19.sp, color = tap.contentColor),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = timeRange(program.startAt, program.endAt, clock24h),
                style = NovaType.Number.copy(color = tap.contentColor.copy(alpha = 0.8f)),
                maxLines = 1,
            )
            ProgressStamp(share = progressShare(program.startAt, program.endAt, now))
        }
    }
}

/** Geçmiş ve sıradaki program satırı: saat aralığı + ad; seçili olan vurgulu. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CompactProgramRow(
    time: String,
    title: String,
    dimmed: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    NovaTapSurface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        shape = RoundedCornerShape(10.dp),
        containerColor = if (selected) {
            NovaColors.SurfaceHigh
        } else {
            NovaColors.Surface.copy(alpha = ROW_SURFACE_ALPHA)
        },
        contentColor = if (dimmed) NovaColors.TextMuted else NovaColors.Text,
        focusedContainerColor = NovaColors.Focus,
        focusedContentColor = NovaColors.FocusInk,
        pressedContainerColor = NovaColors.Focus,
        pressedContentColor = NovaColors.FocusInk,
        border = BorderStroke(
            1.dp,
            if (selected) NovaColors.Accent.copy(alpha = 0.75f) else NovaColors.Border,
        ),
        focusedBorder = BorderStroke(2.dp, NovaColors.Focus),
        pressedBorder = BorderStroke(2.dp, NovaColors.Focus),
    ) { tap ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = time,
                style = NovaType.Number.copy(
                    color = tap.contentColor.copy(alpha = if (dimmed) 0.7f else 0.85f),
                ),
                maxLines = 1,
            )
            Text(
                text = title,
                style = NovaType.Small.copy(fontSize = 14.sp, color = tap.contentColor),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Seçili programın eylemleri. Koşullar TV program diyalogla birebir aynı:
 * canlı izle, arşiv varsa baştan izle, gelecek programa hatırlatıcı,
 * arşiv dışındaki programa kaydet.
 */
@Composable
private fun CompactActions(
    channel: Channel,
    program: EpgProgram,
    now: Long,
    reminded: Boolean,
    recorded: Boolean,
    onWatchChannel: () -> Unit,
    onWatchCatchup: () -> Unit,
    onToggleReminder: () -> Unit,
    onToggleRecording: () -> Unit,
) {
    val live = program.startAt <= now && program.endAt > now
    val future = program.startAt > now
    val catchup = channel.hasArchive && program.endAt <= now &&
        (channel.archiveDays <= 0 ||
            now - program.startAt <= channel.archiveDays * 86_400_000L)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NovaPill(
            label = if (live) "Kanala git" else "Kanalı aç",
            onClick = onWatchChannel,
            accent = true,
            testTag = "epg-watch",
        )
        if (catchup) {
            NovaPill(
                label = "Baştan izle",
                onClick = onWatchCatchup,
                testTag = "epg-catchup",
            )
        }
        if (future) {
            NovaPill(
                label = if (reminded) "Hatırlatıcıyı kaldır" else "Hatırlat",
                onClick = onToggleReminder,
                selected = reminded,
                testTag = "epg-reminder",
            )
        }
        if (!catchup) {
            NovaPill(
                label = if (recorded) "Kaydı iptal" else "Kaydet",
                onClick = onToggleRecording,
                selected = recorded,
                testTag = "epg-record",
            )
        }
    }
}

/** 44 dp logo yuvası: bu modülde görsel yükleme yok, kanal adının ilk harfi durur. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CompactChannelLogo(name: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .background(NovaColors.SurfaceHigh, shape)
            .border(1.dp, NovaColors.Border.copy(alpha = 0.8f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.firstOrNull()?.uppercase() ?: "?",
            style = NovaType.Body.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = NovaColors.TextMuted,
            ),
        )
    }
}

/** 3 dp'lik geçen süre çubuğu: kırmızı yalnızca ince bir damga. */
@Composable
private fun ProgressStamp(share: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(NovaColors.Border.copy(alpha = 0.7f)),
    ) {
        if (share > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(share)
                    .fillMaxHeight()
                    .background(NovaColors.Accent),
            )
        }
    }
}

/** "ŞİMDİ 19:00 · program"; etiket damga renginde, program adı sönükleşir. */
private fun nowNextLine(
    label: String,
    stamp: Color,
    time: String?,
    title: String,
    body: Color,
): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(color = stamp)) { append(label) }
    withStyle(SpanStyle(color = body)) { append(if (time != null) " $time · " else " · ") }
    withStyle(SpanStyle(color = body.copy(alpha = 0.75f))) { append(title) }
}

/** Programın başlangıcından bu yana geçen pay (0..1); ters aralıkta 0. */
private fun progressShare(startAt: Long, endAt: Long, now: Long): Float {
    val total = endAt - startAt
    if (total <= 0L) return 0f
    return ((now - startAt).toFloat() / total).coerceIn(0f, 1f)
}

/** Detay sayfasında gösterilecek sıradaki program sayısı. */
private const val DETAIL_UPCOMING = 4

/**
 * Kanala ait yüzeylerin (satır, şimdiki blok, program satırı) ortak dolgu
 * saydamlığı: üst üste gelen bloklar tek bir yüzey ailesi gibi okunur.
 */
private const val ROW_SURFACE_ALPHA = 0.85f
