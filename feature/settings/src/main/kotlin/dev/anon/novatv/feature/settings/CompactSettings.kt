package dev.anon.novatv.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import dev.anon.novatv.ui.component.NovaBackground
import dev.anon.novatv.ui.component.NovaLabel
import dev.anon.novatv.ui.component.NovaPill
import dev.anon.novatv.ui.layout.CompactContentMaxWidth
import dev.anon.novatv.ui.layout.rememberNovaSpacing
import dev.anon.novatv.ui.theme.NovaColors
import dev.anon.novatv.ui.theme.NovaType

/**
 * Ayarlar ekranının telefon/tablet düzeni.
 *
 * TV gövdesi yerleşimini değiştirmez; bu dosya yalnız `window != Expanded`
 * iken çizilir. Tek sütun, `NovaSpacing` boşlukları, içerik
 * `widthIn(max = CompactContentMaxWidth)` ile ortalanır.
 *
 * Bölüm başlıkları TV ile aynı metinlerdir. Her bölüm bir kart yüzeyidir,
 * her dokunma hedefi en az 56 dp. 411 dp'de yan yana sığmayan düğme kümeleri
 * [FlowRow] ile alt satıra iner — kırpılmaz. Yedekleme düğmeleri iki düğme
 * 379 dp'ye sığmadığından tam genişlikte alt alta durur.
 *
 * `settings-afr`, `settings-autostart` ve `settings-autostart-hint` etiketleri
 * TV ile aynı, kelimesi kelimesine korunur.
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun CompactSettings(
    state: SettingsUiState,
    overlayGranted: Boolean,
    onDark: (Boolean) -> Unit,
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
    val spacing = rememberNovaSpacing()

    NovaBackground(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    // Sıra önemli: önce sınır, sonra doldurma. `fillMaxSize`
                    // dışta kalırsa genişlik sabitlenir, 720 dp sınırı işlemez.
                    .widthIn(max = CompactContentMaxWidth)
                    .fillMaxSize()
                    .testTag("settings-compact"),
                contentPadding = PaddingValues(
                    start = spacing.screen,
                    end = spacing.screen,
                    top = spacing.screen,
                    bottom = spacing.screen,
                ),
                verticalArrangement = Arrangement.spacedBy(spacing.gap),
            ) {
                item(key = "header") {
                    CompactSettingsHeader(
                        onProfiles = onProfiles,
                        onSearch = onSearch,
                        onBack = onBack,
                    )
                }

                item(key = "theme") {
                    CompactSettingsCard {
                        NovaLabel("Tema")
                        CompactWrapRow {
                            CompactSettingPill(
                                label = "Koyu",
                                onClick = { onDark(true) },
                                selected = appearance.dark,
                                testTag = "settings-dark",
                            )
                            CompactSettingPill(
                                label = "Açık",
                                onClick = { onDark(false) },
                                selected = !appearance.dark,
                                testTag = "settings-light",
                            )
                        }
                    }
                }

                item(key = "type-scale") {
                    CompactSettingsCard {
                        NovaLabel("Yazı boyutu")
                        CompactWrapRow {
                            CompactSettingPill(
                                label = "A−",
                                onClick = { onTypeScale(appearance.typeScale - 0.05f) },
                                testTag = "settings-smaller",
                            )
                            CompactValue("%${(appearance.typeScale * 100).toInt()}")
                            CompactSettingPill(
                                label = "A+",
                                onClick = { onTypeScale(appearance.typeScale + 0.05f) },
                                testTag = "settings-larger",
                            )
                            CompactSettingPill(
                                label = if (appearance.clock24h) "Saat 24s" else "Saat 12s",
                                onClick = { onClock24h(!appearance.clock24h) },
                                testTag = "settings-clock",
                            )
                        }
                    }
                }

                item(key = "guide") {
                    CompactSettingsCard {
                        NovaLabel("Rehber")
                        // TV'de iki sayaç aynı satırdadır; 411 dp'de dört düğme
                        // ve iki değer sığmaz. Her sayaç kendi satırında.
                        CompactCounterRow(
                            label = "Satır yüksekliği",
                            value = "${appearance.epgRowHeightDp} dp",
                            onMinus = { onEpgRowHeight(appearance.epgRowHeightDp - 8) },
                            onPlus = { onEpgRowHeight(appearance.epgRowHeightDp + 8) },
                        )
                        CompactCounterRow(
                            label = "Gün penceresi",
                            value = "${appearance.epgWindowDays} gün",
                            onMinus = { onEpgWindowDays(appearance.epgWindowDays - 1) },
                            onPlus = { onEpgWindowDays(appearance.epgWindowDays + 1) },
                        )
                    }
                }

                item(key = "playback") {
                    CompactSettingsCard {
                        NovaLabel("Oynatma")
                        CompactSettingPill(
                            label = if (appearance.autoFrameRate) "AFR açık" else "AFR kapalı",
                            onClick = { onAutoFrameRate(!appearance.autoFrameRate) },
                            selected = appearance.autoFrameRate,
                            fillWidth = true,
                            testTag = "settings-afr",
                        )
                        CompactSettingPill(
                            label = if (appearance.autoStart) "Açılışta başlat" else "Açılışta başlatma",
                            onClick = { onAutoStart(!appearance.autoStart) },
                            selected = appearance.autoStart,
                            fillWidth = true,
                            testTag = "settings-autostart",
                        )
                        if (appearance.autoStart && !overlayGranted) {
                            Text(
                                text = "Açılışta başlatma için \"Diğer uygulamaların üzerinde göster\" iznini verin.",
                                style = NovaType.Small.copy(color = NovaColors.Warning),
                                modifier = Modifier.testTag("settings-autostart-hint"),
                            )
                        }
                        Text(
                            text = "Uyku zamanlayıcısı",
                            style = NovaType.Small.copy(color = NovaColors.TextMuted),
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SLEEP_OPTIONS.forEach { minutes ->
                                CompactSettingPill(
                                    label = if (minutes == 0) "Kapalı" else "$minutes dk",
                                    onClick = { onSleepMinutes(minutes) },
                                    selected = appearance.sleepMinutes == minutes,
                                    testTag = "settings-sleep-$minutes",
                                )
                            }
                        }
                    }
                }

                item(key = "recordings") {
                    CompactSettingsCard {
                        NovaLabel("Kayıtlar")
                        Text(
                            text = state.recordingPath.ifBlank { "Uygulama kayıt klasörü" },
                            style = NovaType.Small.copy(color = NovaColors.TextMuted),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        CompactSettingPill(
                            label = "Kayıtları aç",
                            onClick = onOpenRecordings,
                            fillWidth = true,
                            testTag = "settings-recordings",
                        )
                    }
                }

                item(key = "backup") {
                    CompactSettingsCard {
                        NovaLabel("Yedekleme")
                        // İki düğme yan yana 379 dp'ye sığmaz: tam genişlik, alt alta.
                        CompactSettingPill(
                            label = if (state.busy) "Çalışıyor…" else "Yedek al",
                            onClick = onBackup,
                            enabled = !state.busy,
                            accent = true,
                            fillWidth = true,
                            testTag = "settings-backup",
                        )
                        CompactSettingPill(
                            label = "Geri yükle",
                            onClick = onRestore,
                            enabled = !state.busy,
                            fillWidth = true,
                            testTag = "settings-restore",
                        )
                        Text(
                            text = "Yedek yalnızca görünüm ve profil adlarını taşır; " +
                                "kaynak adresleri ve kimlik bilgileri yedeğe girmez.",
                            style = NovaType.Small.copy(fontSize = 12.sp, color = NovaColors.TextDim),
                        )
                    }
                }

                state.message?.let { message ->
                    item(key = "message") {
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
}

/** Başlık ve üç gezinme düğmesi: düğmeler dar ekranda alt satıra sarar. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompactSettingsHeader(
    onProfiles: () -> Unit,
    onSearch: () -> Unit,
    onBack: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        NovaLabel("Ayarlar")
        Text(text = "Görünüm ve sistem", style = NovaType.Title)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CompactSettingPill(label = "Profiller", onClick = onProfiles, testTag = "settings-profiles")
            CompactSettingPill(label = "Ara", onClick = onSearch, testTag = "settings-search")
            CompactSettingPill(label = "Canlı TV", onClick = onBack, testTag = "settings-back")
        }
    }
}

/** Bölüm yüzeyi: TV'nin sade yerleşimi, dokunmaya uygun dolguyla. */
@Composable
private fun CompactSettingsCard(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NovaColors.Surface, shape)
            .border(1.dp, NovaColors.Border.copy(alpha = 0.75f), shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

/** Sığmayan düğme kümesi: kaydırmak yerine alt satıra sarar, 8 dp boşluk. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompactWrapRow(content: @Composable androidx.compose.foundation.layout.FlowRowScope.() -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/** Sayaç: etiket üstte, eksi/değer/artı altta; her düğme ayrı hedef. */
@Composable
private fun CompactCounterRow(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, style = NovaType.Small.copy(color = NovaColors.TextMuted))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CompactSettingPill(label = "−", onClick = onMinus)
            Box(modifier = Modifier.width(84.dp), contentAlignment = Alignment.Center) {
                Text(text = value, style = NovaType.Number, maxLines = 1)
            }
            CompactSettingPill(label = "+", onClick = onPlus)
        }
    }
}

/** Düğme olmayan okunacak değer; satır yüksekliğine hizalanır. */
@Composable
private fun CompactValue(text: String) {
    Box(
        modifier = Modifier.heightIn(min = CompactSettingHeight),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = NovaType.Number, maxLines = 1)
    }
}

/** 56 dp dokunma hedefi; `fillWidth` ise satırı kaplar. */
@Composable
private fun CompactSettingPill(
    label: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    accent: Boolean = false,
    enabled: Boolean = true,
    fillWidth: Boolean = false,
    testTag: String? = null,
) {
    NovaPill(
        label = label,
        onClick = onClick,
        selected = selected,
        accent = accent,
        enabled = enabled,
        modifier = (if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = CompactSettingHeight),
        testTag = testTag,
    )
}

/** Ayarlardaki her dokunma hedefi: TV'nin 56 dp satır ölçüsü. */
internal val CompactSettingHeight = 56.dp
