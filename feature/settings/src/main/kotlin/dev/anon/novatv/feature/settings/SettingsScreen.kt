package dev.anon.novatv.feature.settings

import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.tv.material3.Text
import dev.anon.novatv.core.settings.Accent
import dev.anon.novatv.ui.component.NovaBackground
import dev.anon.novatv.ui.component.NovaLabel
import dev.anon.novatv.ui.component.NovaPill
import dev.anon.novatv.ui.component.NovaRule
import dev.anon.novatv.ui.theme.NovaColors
import dev.anon.novatv.ui.theme.NovaType

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onDark: (Boolean) -> Unit,
    onAccent: (Accent) -> Unit,
    onTypeScale: (Float) -> Unit,
    onClock24h: (Boolean) -> Unit,
    onEpgRowHeight: (Int) -> Unit,
    onEpgWindowDays: (Int) -> Unit,
    onAutoFrameRate: (Boolean) -> Unit,
    onAutoStart: (Boolean) -> Unit,
    onSleepMinutes: (Int) -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onProfiles: () -> Unit,
    onSearch: () -> Unit,
    onOpenRecordings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appearance = state.appearance
    val context = LocalContext.current
    // İzin sistem ayarlarından verilip dönüldüğünde yeniden okunsun.
    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        overlayGranted = Settings.canDrawOverlays(context)
    }

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
                    NovaLabel("Ayarlar")
                    Text(text = "Görünüm ve sistem", style = NovaType.Title)
                }
                NovaPill(label = "Profiller", onClick = onProfiles, testTag = "settings-profiles")
                NovaPill(label = "Ara", onClick = onSearch, testTag = "settings-search")
                NovaPill(label = "Canlı TV", onClick = onBack, testTag = "settings-back")
            }

            Spacer(Modifier.height(10.dp))
            NovaRule(Modifier.fillMaxWidth())

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Section("Tema") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NovaPill(
                            label = "Koyu",
                            onClick = { onDark(true) },
                            selected = appearance.dark,
                            testTag = "settings-dark",
                        )
                        NovaPill(
                            label = "Açık",
                            onClick = { onDark(false) },
                            selected = !appearance.dark,
                            testTag = "settings-light",
                        )
                    }
                }

                Section("Vurgu rengi") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Accent.entries.forEachIndexed { index, accent ->
                            NovaPill(
                                label = NovaColors.accents.getOrNull(index)?.first
                                    ?: accent.name,
                                onClick = { onAccent(accent) },
                                selected = appearance.accent == accent,
                                testTag = "settings-accent-${accent.name}",
                            )
                        }
                    }
                }

                Section("Yazı boyutu") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        NovaPill(
                            label = "A−",
                            onClick = { onTypeScale(appearance.typeScale - 0.05f) },
                            testTag = "settings-smaller",
                        )
                        Text(
                            text = "%${(appearance.typeScale * 100).toInt()}",
                            style = NovaType.Number,
                            modifier = Modifier.width(56.dp),
                        )
                        NovaPill(
                            label = "A+",
                            onClick = { onTypeScale(appearance.typeScale + 0.05f) },
                            testTag = "settings-larger",
                        )
                        NovaPill(
                            label = if (appearance.clock24h) "Saat 24s" else "Saat 12s",
                            onClick = { onClock24h(!appearance.clock24h) },
                            testTag = "settings-clock",
                        )
                    }
                }

                Section("Rehber") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Satır yüksekliği", style = NovaType.Small)
                        NovaPill(
                            label = "−",
                            onClick = { onEpgRowHeight(appearance.epgRowHeightDp - 8) },
                        )
                        Text(
                            text = "${appearance.epgRowHeightDp} dp",
                            style = NovaType.Number,
                            modifier = Modifier.width(58.dp),
                        )
                        NovaPill(
                            label = "+",
                            onClick = { onEpgRowHeight(appearance.epgRowHeightDp + 8) },
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Gün penceresi", style = NovaType.Small)
                        NovaPill(
                            label = "−",
                            onClick = { onEpgWindowDays(appearance.epgWindowDays - 1) },
                        )
                        Text(
                            text = "${appearance.epgWindowDays} gün",
                            style = NovaType.Number,
                            modifier = Modifier.width(58.dp),
                        )
                        NovaPill(
                            label = "+",
                            onClick = { onEpgWindowDays(appearance.epgWindowDays + 1) },
                        )
                    }
                }

                Section("Oynatma") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NovaPill(
                            label = if (appearance.autoFrameRate) "AFR açık" else "AFR kapalı",
                            onClick = { onAutoFrameRate(!appearance.autoFrameRate) },
                            selected = appearance.autoFrameRate,
                            testTag = "settings-afr",
                        )
                        NovaPill(
                            label = if (appearance.autoStart) "Açılışta başlat" else "Açılışta başlatma",
                            onClick = { onAutoStart(!appearance.autoStart) },
                            selected = appearance.autoStart,
                            testTag = "settings-autostart",
                        )
                    }
                    if (appearance.autoStart && !overlayGranted) {
                        Text(
                            text = "Açılışta başlatma için \"Diğer uygulamaların üzerinde göster\" iznini verin.",
                            style = NovaType.Small.copy(color = NovaColors.Warning),
                            modifier = Modifier.testTag("settings-autostart-hint"),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Uyku zamanlayıcısı",
                            style = NovaType.Small,
                            modifier = Modifier.align(Alignment.CenterVertically),
                        )
                        SLEEP_OPTIONS.forEach { minutes ->
                            NovaPill(
                                label = if (minutes == 0) "Kapalı" else "$minutes dk",
                                onClick = { onSleepMinutes(minutes) },
                                selected = appearance.sleepMinutes == minutes,
                                testTag = "settings-sleep-$minutes",
                            )
                        }
                    }
                }

                Section("Kayıtlar") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = state.recordingPath.ifBlank { "Uygulama kayıt klasörü" },
                            style = NovaType.Small,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        NovaPill(
                            label = "Kayıtları aç",
                            onClick = onOpenRecordings,
                            testTag = "settings-recordings",
                        )
                    }
                }

                Section("Yedekleme") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NovaPill(
                            label = if (state.busy) "Çalışıyor…" else "Yedek al",
                            onClick = onBackup,
                            enabled = !state.busy,
                            accent = true,
                            testTag = "settings-backup",
                        )
                        NovaPill(
                            label = "Geri yükle",
                            onClick = onRestore,
                            enabled = !state.busy,
                            testTag = "settings-restore",
                        )
                    }
                    Text(
                        text = "Yedek yalnızca görünüm ve profil adlarını taşır; " +
                            "kaynak adresleri ve kimlik bilgileri yedeğe girmez.",
                        style = NovaType.Small.copy(fontSize = 12.sp, color = NovaColors.TextDim),
                    )
                }

                state.message?.let { message ->
                    Text(
                        text = message,
                        style = NovaType.Body.copy(fontSize = 15.sp, color = NovaColors.Accent),
                        modifier = Modifier.testTag("settings-message"),
                    )
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        NovaLabel(title)
        content()
    }
}

private val SLEEP_OPTIONS = listOf(0, 15, 30, 60, 90)
