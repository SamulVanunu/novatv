package dev.anon.novatv.feature.epg

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.core.model.EpgProgram
import dev.anon.novatv.core.sync.EpgRepository
import dev.anon.novatv.core.sync.GuideRow
import dev.anon.novatv.ui.component.NovaBackground
import dev.anon.novatv.ui.component.NovaBadge
import dev.anon.novatv.ui.component.NovaLabel
import dev.anon.novatv.ui.component.NovaPanel
import dev.anon.novatv.ui.component.NovaPill
import dev.anon.novatv.ui.component.NovaRule
import dev.anon.novatv.ui.theme.NovaColors
import dev.anon.novatv.ui.theme.NovaType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private val CHANNEL_COLUMN = 172.dp
private val HOUR_WIDTH = 188.dp
private const val MINUTES_PER_DAY = 24 * 60
private val PX_PER_MINUTE = HOUR_WIDTH.value / 60f

/**
 * Kanal × saat rehberi.
 *
 * Tek bir yatay kaydırma durumu tüm satırlarda paylaşılır; saat şeridi ve
 * program blokları birlikte kayar. Dikeyde `LazyColumn` sanallaştırır, böylece
 * binlerce kanallı listelerde bile kumanda gecikmez.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EpgScreen(
    state: EpgUiState,
    onSelectDay: (Int) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onWatchChannel: (Channel) -> Unit,
    onWatchCatchup: (Channel, EpgProgram) -> Unit,
    onToggleReminder: (Channel, EpgProgram) -> Unit,
    onToggleRecording: (Channel, EpgProgram) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val firstProgramFocus = remember { FocusRequester() }
    var selected by remember { mutableStateOf<Pair<Channel, EpgProgram>?>(null) }
    val from = remember(state.dayOffset) { dayStart(state.dayOffset) }
    val to = from + EpgRepository.DAY_MS
    val firstRowId = remember(state.rows) {
        state.rows.firstOrNull { it.programs.isNotEmpty() }?.channel?.id
    }

    LaunchedEffect(state.rows.size, state.dayOffset) {
        if (state.rows.any { it.programs.isNotEmpty() }) {
            runCatching { firstProgramFocus.requestFocus() }
        }
    }

    NovaBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 12.dp),
        ) {
            EpgHeader(
                state = state,
                onSelectDay = onSelectDay,
                onRefresh = onRefresh,
                onBack = onBack,
                onNow = {
                    val target = ((state.now - from) / 60_000L).coerceIn(0, MINUTES_PER_DAY.toLong())
                    val px = (target * PX_PER_MINUTE).toInt()
                    scope.launch { scroll.animateScrollTo(px) }
                },
            )

            Spacer(Modifier.height(12.dp))
            TimeStrip(scroll = scroll, from = from, clock24h = state.clock24h)
            NovaRule(Modifier.fillMaxWidth().padding(top = 4.dp))

            when {
                state.loading && state.rows.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Rehber yükleniyor", style = NovaType.BodyMuted)
                }

                state.rows.isEmpty() -> EmptyGuide(
                    message = state.message,
                    syncing = state.syncing,
                    onRefresh = onRefresh,
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("epg-grid"),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    items(items = state.rows, key = { it.channel.id }) { row ->
                        GuideRowView(
                            row = row,
                            from = from,
                            to = to,
                            now = state.now,
                            rowHeightDp = state.rowHeightDp,
                            clock24h = state.clock24h,
                            scroll = scroll,
                            onClick = { program -> selected = row.channel to program },
                            focusRequester = firstProgramFocus.takeIf { row.channel.id == firstRowId },
                        )
                    }
                }
            }

            state.message?.takeIf { state.rows.isNotEmpty() }?.let { message ->
                Text(
                    text = message,
                    style = NovaType.Small.copy(color = NovaColors.Warning),
                    modifier = Modifier.padding(top = 6.dp).testTag("epg-message"),
                )
            }
        }
    }

    selected?.let { (channel, program) ->
        ProgramDialog(
            channel = channel,
            program = program,
            now = state.now,
            reminded = reminderId(channel.id, program.startAt) in state.reminderIds,
            recorded = recordingId(channel.id, program.startAt) in state.recordedIds,
            onWatch = {
                selected = null
                onWatchChannel(channel)
            },
            onCatchup = {
                selected = null
                onWatchCatchup(channel, program)
            },
            onReminder = { onToggleReminder(channel, program) },
            onRecord = { onToggleRecording(channel, program) },
            onDismiss = { selected = null },
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EpgHeader(
    state: EpgUiState,
    onSelectDay: (Int) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onNow: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
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

        for (offset in 0 until state.windowDays) {
            NovaPill(
                label = dayTabLabel(offset),
                onClick = { onSelectDay(offset) },
                selected = state.dayOffset == offset,
                testTag = "epg-day-$offset",
            )
        }

        NovaPill(label = "Şimdi", onClick = onNow, testTag = "epg-now")
        NovaPill(
            label = if (state.syncing) "Yenileniyor" else "Yenile",
            onClick = onRefresh,
            enabled = !state.syncing,
            testTag = "epg-refresh",
        )
        NovaPill(label = "Canlı TV", onClick = onBack, testTag = "epg-back")
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TimeStrip(scroll: androidx.compose.foundation.ScrollState, from: Long, clock24h: Boolean) {
    val zone = remember { ZoneId.systemDefault() }
    Row(modifier = Modifier.fillMaxWidth().height(22.dp)) {
        Spacer(Modifier.width(CHANNEL_COLUMN))
        Box(modifier = Modifier.weight(1f).clipToBounds()) {
            Row(modifier = Modifier.horizontalScroll(scroll).height(22.dp)) {
                for (hour in 0 until 24) {
                    val at = from + hour * 3_600_000L
                    Box(
                        modifier = Modifier.width(HOUR_WIDTH).fillMaxHeight(),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            text = timeText(at, clock24h, zone),
                            style = NovaType.Number.copy(color = NovaColors.TextDim),
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideRowView(
    row: GuideRow,
    from: Long,
    to: Long,
    now: Long,
    rowHeightDp: Int,
    clock24h: Boolean,
    scroll: androidx.compose.foundation.ScrollState,
    onClick: (EpgProgram) -> Unit,
    focusRequester: FocusRequester?,
) {
    val blocks = remember(row.programs, from, to) { buildBlocks(row.programs, from, to) }
    val height = rowHeightDp.dp

    Row(modifier = Modifier.fillMaxWidth().height(height)) {
        ChannelCell(
            channel = row.channel,
            hasPrograms = row.programs.isNotEmpty(),
            modifier = Modifier.width(CHANNEL_COLUMN).fillMaxHeight(),
        )
        Box(modifier = Modifier.weight(1f).fillMaxHeight().clipToBounds()) {
            Row(modifier = Modifier.horizontalScroll(scroll).height(height)) {
                var focusAssigned = false
                blocks.forEach { block ->
                    val width = (((block.end - block.start) / 60_000L) * PX_PER_MINUTE)
                        .coerceAtLeast(2f)
                        .dp
                    val program = block.program
                    if (program == null) {
                        Box(
                            modifier = Modifier
                                .width(width)
                                .fillMaxHeight()
                                .padding(vertical = 4.dp)
                                .background(NovaColors.Surface.copy(alpha = 0.35f)),
                        )
                    } else {
                        val requestFocus = focusRequester != null && !focusAssigned
                        if (requestFocus) focusAssigned = true
                        ProgramBlock(
                            program = program,
                            current = program.startAt <= now && program.endAt > now,
                            past = program.endAt <= now,
                            clock24h = clock24h,
                            modifier = Modifier
                                .width(width)
                                .fillMaxHeight()
                                .padding(vertical = 3.dp)
                                .then(
                                    if (requestFocus && focusRequester != null) {
                                        Modifier.focusRequester(focusRequester)
                                    } else {
                                        Modifier
                                    },
                                ),
                            onClick = { onClick(program) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ChannelCell(channel: Channel, hasPrograms: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = channel.name,
            style = NovaType.Small.copy(
                color = if (hasPrograms) NovaColors.Text else NovaColors.TextDim,
                fontSize = 14.sp,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            channel.number?.let { number ->
                Text(
                    text = number.toString(),
                    style = NovaType.Number.copy(color = NovaColors.TextDim),
                )
            }
            if (channel.hasArchive) {
                NovaBadge(
                    text = if (channel.archiveDays > 0) "ARŞİV ${channel.archiveDays}G" else "ARŞİV",
                    color = NovaColors.Accent,
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProgramBlock(
    program: EpgProgram,
    current: Boolean,
    past: Boolean,
    clock24h: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    val container = when {
        current -> NovaColors.Accent.copy(alpha = 0.16f)
        past -> NovaColors.Surface.copy(alpha = 0.55f)
        else -> NovaColors.SurfaceHigh
    }
    val content = when {
        current -> NovaColors.Accent
        past -> NovaColors.TextDim
        else -> NovaColors.Text
    }
    val borderColor = if (current) NovaColors.Accent.copy(alpha = 0.7f) else NovaColors.Border

    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = container,
            contentColor = content,
            focusedContainerColor = NovaColors.Accent,
            focusedContentColor = NovaColors.AccentInk,
            pressedContainerColor = NovaColors.Accent,
            pressedContentColor = NovaColors.AccentInk,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(border = BorderStroke(1.dp, borderColor), shape = shape),
            focusedBorder = Border(border = BorderStroke(1.dp, NovaColors.Accent), shape = shape),
            pressedBorder = Border(border = BorderStroke(1.dp, NovaColors.Accent), shape = shape),
        ),
        scale = ClickableSurfaceDefaults.scale(1f, 1f, 0.98f),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = program.title,
                style = NovaType.Body.copy(fontSize = 13.sp, lineHeight = 16.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = timeRange(program.startAt, program.endAt, clock24h),
                style = NovaType.Number.copy(
                    fontSize = 11.sp,
                    color = if (current) NovaColors.Accent else NovaColors.TextDim,
                ),
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EmptyGuide(message: String?, syncing: Boolean, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Bu gün için rehber yok",
            style = NovaType.Display.copy(fontSize = 26.sp),
            modifier = Modifier.testTag("epg-empty"),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message
                ?: "Kaynağın XMLTV adresi yoksa veya kanallarda tvg-id tanımlı değilse rehber dolmaz.",
            style = NovaType.BodyMuted,
            modifier = Modifier.width(520.dp).padding(horizontal = 10.dp),
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProgramDialog(
    channel: Channel,
    program: EpgProgram,
    now: Long,
    reminded: Boolean,
    recorded: Boolean,
    onWatch: () -> Unit,
    onCatchup: () -> Unit,
    onReminder: () -> Unit,
    onRecord: () -> Unit,
    onDismiss: () -> Unit,
) {
    val live = program.startAt <= now && program.endAt > now
    val future = program.startAt > now
    val catchup = channel.hasArchive && program.endAt <= now &&
        (channel.archiveDays <= 0 ||
            now - program.startAt <= channel.archiveDays * 86_400_000L)

    Dialog(onDismissRequest = onDismiss) {
        NovaPanel(width = 520.dp) {
            NovaLabel(channel.name)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = program.title, style = NovaType.Title, maxLines = 2)
                program.subtitle?.let { subtitle ->
                    Text(text = subtitle, style = NovaType.BodyMuted, maxLines = 2)
                }
                Text(
                    text = timeRange(program.startAt, program.endAt, clock24h = true) +
                        (if (live) " · ŞİMDİ" else ""),
                    style = NovaType.Small.copy(
                        color = if (live) NovaColors.Accent else NovaColors.TextDim,
                    ),
                )
                program.episodeNum?.let { episode ->
                    Text(text = episode, style = NovaType.Small)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                program.category?.let { NovaBadge(text = it, color = NovaColors.TextMuted) }
                program.rating?.let { NovaBadge(text = "Reyting $it", color = NovaColors.Warning) }
                if (program.isNew) NovaBadge(text = "Yeni", color = NovaColors.Live)
            }
            program.description?.let { description ->
                Text(
                    text = description,
                    style = NovaType.BodyMuted.copy(fontSize = 16.sp),
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            NovaRule(Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NovaPill(
                    label = if (live) "Kanala git" else "Kanalı aç",
                    onClick = onWatch,
                    accent = true,
                    testTag = "epg-watch",
                )
                if (catchup) {
                    NovaPill(
                        label = "Baştan izle",
                        onClick = onCatchup,
                        testTag = "epg-catchup",
                    )
                }
                if (future) {
                    NovaPill(
                        label = if (reminded) "Hatırlatıcıyı kaldır" else "Hatırlat",
                        onClick = onReminder,
                        selected = reminded,
                        testTag = "epg-reminder",
                    )
                }
                if (!catchup) {
                    NovaPill(
                        label = if (recorded) "Kaydı iptal" else "Kaydet",
                        onClick = onRecord,
                        selected = recorded,
                        testTag = "epg-record",
                    )
                }
            }
            NovaRule(Modifier.fillMaxWidth())
            NovaPill(
                label = "Kapat",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                testTag = "epg-dismiss",
            )
        }
    }
}

internal data class GuideBlock(val start: Long, val end: Long, val program: EpgProgram?)

/** Programları gün penceresine kırpar, aralara boşluk blokları ekler. */
internal fun buildBlocks(programs: List<EpgProgram>, from: Long, to: Long): List<GuideBlock> {
    val blocks = ArrayList<GuideBlock>(programs.size * 2 + 2)
    var cursor = from
    programs.sortedBy { it.startAt }.forEach { program ->
        val start = program.startAt.coerceAtLeast(from)
        val end = program.endAt.coerceAtMost(to)
        if (end <= start) return@forEach
        if (start > cursor) blocks += GuideBlock(cursor, start, null)
        val blockStart = start.coerceAtLeast(cursor)
        if (end > blockStart) blocks += GuideBlock(blockStart, end, program)
        cursor = end.coerceAtLeast(cursor)
    }
    if (cursor < to) blocks += GuideBlock(cursor, to, null)
    return blocks
}

internal fun timeText(at: Long, clock24h: Boolean, zone: ZoneId = ZoneId.systemDefault()): String {
    val time = Instant.ofEpochMilli(at).atZone(zone)
    val pattern = if (clock24h) "HH:mm" else "h:mm a"
    return DateTimeFormatter.ofPattern(pattern, Locale("tr")).format(time)
}

internal fun timeRange(startAt: Long, endAt: Long, clock24h: Boolean): String =
    timeText(startAt, clock24h) + " – " + timeText(endAt, clock24h)

/** Gün sekmesi kısa etiketi: Bugün / Yarın / Cmt. */
internal fun dayTabLabel(offset: Int, zone: ZoneId = ZoneId.systemDefault()): String = when (offset) {
    0 -> "Bugün"
    1 -> "Yarın"
    else -> {
        val date = LocalDate.now(zone).plusDays(offset.toLong())
        DateTimeFormatter.ofPattern("EEE", Locale("tr")).format(date)
            .replaceFirstChar { it.uppercase(Locale("tr")) }
    }
}
