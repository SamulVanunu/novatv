package dev.anon.novatv.feature.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import dev.anon.novatv.core.model.PlaylistType
import dev.anon.novatv.ui.component.NovaBackground
import dev.anon.novatv.ui.component.NovaBadge
import dev.anon.novatv.ui.component.NovaLabel
import dev.anon.novatv.ui.component.NovaPill
import dev.anon.novatv.ui.component.NovaRule
import dev.anon.novatv.ui.layout.rememberNovaSpacing
import dev.anon.novatv.ui.theme.NovaColors
import dev.anon.novatv.ui.theme.NovaTextFieldTheme
import dev.anon.novatv.ui.theme.NovaType
import dev.anon.novatv.ui.theme.novaTextFieldColors

/**
 * Kaynak formunun telefon/tablet düzeni.
 *
 * TV'de iki sütun (550 + 280 dp) ve alt kenarda sabit düğme çubuğu vardır;
 * 411 dp'de bunlar sığmaz. Burada tek sütun: başlık, Özet künyesi, tek
 * yüzeyde tam genişlik alanlar, Not maddeleri. Tür seçici yatay kaydırılan
 * çip şerididir.
 *
 * Klavye açıldığında odaklı alanı gizlememek için tüm sayfa [imePadding] ile
 * sistem klavyesinin üstüne kalkar; düğmeler akışın sonundadır, her zaman
 * aynı yerde bulunmaz — alanların altına kaydırılarak erişilir. Test/Kaydet/Geri
 * en az 56 dp. İlk odak istenmez.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun CompactPlaylistForm(
    state: PlaylistFormState,
    onName: (String) -> Unit,
    onType: (PlaylistType) -> Unit,
    onUrl: (String) -> Unit,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
    onEpgUrl: (String) -> Unit,
    onUserAgent: (String) -> Unit,
    onReferer: (String) -> Unit,
    onSyncInterval: (String) -> Unit,
    onTest: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = rememberNovaSpacing()

    NovaBackground(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    // Sıra önemli: önce sınır, sonra doldurma. `fillMaxSize`
                    // dışta kalırsa genişlik sabitlenir, 720 dp sınırı işlemez.
                    .widthIn(max = 720.dp)
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = spacing.screen,
                        end = spacing.screen,
                        top = spacing.screen,
                        bottom = spacing.screen,
                    )
                    .testTag("playlist-form-compact"),
                verticalArrangement = Arrangement.spacedBy(spacing.gap),
            ) {
                CompactFormHeader(state)
                NovaRule(Modifier.fillMaxWidth())

                CompactFormSection {
                    NovaLabel("Bağlantı")
                    CompactTypeSelector(type = state.type, onType = onType)
                    NovaTextFieldTheme {
                        CompactFormField(
                            value = state.name,
                            onValueChange = onName,
                            label = "Ad",
                            placeholder = "Örn. Ev listesi",
                            testTag = "field-name",
                        )
                        CompactFormField(
                            value = state.url,
                            onValueChange = onUrl,
                            label = if (state.type == PlaylistType.M3U) "Liste adresi" else "Sunucu adresi",
                            placeholder = when (state.type) {
                                PlaylistType.XTREAM -> "http://sunucu:8080"
                                PlaylistType.M3U -> "https://ornek.com/liste.m3u"
                                PlaylistType.STALKER -> "http://sunucu:8080/c/"
                            },
                            testTag = "field-url",
                        )
                        // M3U listesinde kullanıcı adı yoktur; alan yalnızca
                        // Xtream (kullanıcı) ve Stalker (MAC) içindir.
                        if (state.type != PlaylistType.M3U) {
                            CompactFormField(
                                value = state.username,
                                onValueChange = onUsername,
                                label = if (state.type == PlaylistType.STALKER) "MAC adresi" else "Kullanıcı adı",
                                placeholder = if (state.type == PlaylistType.STALKER) {
                                    "00:1A:79:…"
                                } else {
                                    "Xtream kullanıcı adı"
                                },
                                testTag = "field-username",
                            )
                        }
                        if (state.type == PlaylistType.XTREAM) {
                            CompactFormField(
                                value = state.password,
                                onValueChange = onPassword,
                                label = "Şifre",
                                visualTransformation = PasswordVisualTransformation(),
                                testTag = "field-password",
                            )
                        }
                        if (state.type == PlaylistType.M3U) {
                            CompactFormField(
                                value = state.epgUrl,
                                onValueChange = onEpgUrl,
                                label = "EPG URL (isteğe bağlı)",
                                placeholder = "https://ornek.com/epg.xml",
                                testTag = "field-epg",
                            )
                        }
                        CompactFormField(
                            value = state.userAgent,
                            onValueChange = onUserAgent,
                            label = "User-Agent (isteğe bağlı)",
                            placeholder = "Varsayılan: ExoPlayer",
                            testTag = "field-user-agent",
                        )
                        CompactFormField(
                            value = state.referer,
                            onValueChange = onReferer,
                            label = "Referer (isteğe bağlı)",
                            placeholder = "Yalnızca isteyen sağlayıcılar için",
                            testTag = "field-referer",
                        )
                        CompactFormField(
                            value = state.syncIntervalHours,
                            onValueChange = onSyncInterval,
                            label = "Yenileme aralığı (saat)",
                            placeholder = "24",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            testTag = "field-interval",
                        )
                    }
                }

                CompactFormSection {
                    NovaLabel("Özet")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = formTypeLabel(state.type),
                            style = NovaType.Title,
                            modifier = Modifier.testTag("form-type-label"),
                        )
                        NovaBadge(
                            text = if (state.canSave) "hazır" else "eksik alan",
                            color = if (state.canSave) NovaColors.Live else NovaColors.TextDim,
                        )
                    }
                    Text(text = state.typeHint, style = NovaType.Small.copy(color = NovaColors.TextMuted))

                    state.connectionStatus?.let { status ->
                        NovaRule(Modifier.fillMaxWidth())
                        Text(text = "Hesap", style = NovaType.Label)
                        Text(
                            text = status,
                            style = NovaType.Small.copy(color = NovaColors.Live),
                            modifier = Modifier.testTag("connection-status"),
                        )
                    }

                    state.error?.let { message ->
                        Text(
                            text = message,
                            style = NovaType.Small.copy(color = NovaColors.Danger),
                            modifier = Modifier.testTag("form-error"),
                        )
                    }
                }

                CompactFormSection {
                    NovaLabel("Not")
                    CompactNote("Kanallar kaydedilince arka planda çekilir, Canlı TV'de görünür.")
                    CompactNote("Kimlik bilgileri cihazda şifreli saklanır.")
                    CompactNote("Şifre taşıyan liste adresi yalnızca şifreli depoda tutulur.")
                }

                NovaRule(Modifier.fillMaxWidth())

                // Klavyenin altında kalmasın diye düğmeler akışın sonunda, tam
                // genişlik; her biri ayrı satır (yan yana sığmaz, kırpılmaz).
                NovaPill(
                    label = if (state.saving) "Kaydediliyor…" else "Kaydet",
                    onClick = onSave,
                    enabled = state.canSave && !state.saving,
                    accent = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = CompactPrimaryHeight),
                    testTag = "save-source",
                )
                NovaPill(
                    label = if (state.testing) "Test ediliyor…" else "Test et",
                    onClick = onTest,
                    enabled = state.canSave && !state.testing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = CompactPrimaryHeight),
                    testTag = "test-connection",
                )
                NovaPill(
                    label = "Geri",
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = CompactPrimaryHeight),
                    testTag = "form-back",
                )
            }
        }
    }
}

/** Başlık ve giriş metni; TV ile aynı sözler, kompakt ölçek. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CompactFormHeader(state: PlaylistFormState) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        NovaLabel("Kaynak")
        Text(
            text = if (state.id == null) "Kaynak ekle" else "Kaynağı düzenle",
            style = NovaType.Display.copy(fontSize = 28.sp),
        )
        Text(
            text = "Kimlik bilgileri cihazda şifreli saklanır, yayın adreslerine gömülmez.",
            style = NovaType.Small.copy(color = NovaColors.TextMuted),
        )
    }
}

/** Yatay kaydırılan tür çipleri: Xtream, M3U, Stalker. */
@Composable
private fun CompactTypeSelector(type: PlaylistType, onType: (PlaylistType) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PlaylistType.entries.forEach { entry ->
            NovaPill(
                label = formTypeLabel(entry),
                onClick = { onType(entry) },
                selected = type == entry,
                modifier = Modifier.heightIn(min = CompactControlHeight),
                testTag = "type-${entry.name}",
            )
        }
    }
}

/** Kart yüzeyi: TV `FormSection` ile aynı çerçeve, kompakt dolgu. */
@Composable
private fun CompactFormSection(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NovaColors.Surface, shape)
            .border(1.dp, NovaColors.Border.copy(alpha = 0.75f), shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        content()
    }
}

/** Tam genişlik alan: etiket üstte, kutu 48 dp'den şaşmaz. */
@Composable
private fun CompactFormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    testTag: String,
    placeholder: String = "",
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        NovaLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = if (placeholder.isBlank()) {
                null
            } else {
                { Text(placeholder, style = NovaType.Small) }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            colors = novaTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = CompactControlHeight)
                .testTag(testTag),
        )
    }
}

/** Küçük madde işareti: kehribar nokta + sönük açıklama. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CompactNote(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("·", style = NovaType.Small.copy(color = NovaColors.Accent))
        Text(text, style = NovaType.Small.copy(color = NovaColors.TextMuted))
    }
}
