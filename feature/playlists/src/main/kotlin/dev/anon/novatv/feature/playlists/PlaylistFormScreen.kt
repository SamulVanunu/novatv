package dev.anon.novatv.feature.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import dev.anon.novatv.ui.focus.tvFieldNavigation
import dev.anon.novatv.ui.layout.NovaWindow
import dev.anon.novatv.ui.theme.NovaColors
import dev.anon.novatv.ui.theme.NovaTextFieldTheme
import dev.anon.novatv.ui.theme.NovaType
import dev.anon.novatv.ui.theme.novaTextFieldColors

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlaylistFormScreen(
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
    onSaved: (Long) -> Unit = {},
    window: NovaWindow = NovaWindow.Expanded,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(state.savedId) {
        state.savedId?.let(onSaved)
    }

    when (window) {
        NovaWindow.Compact, NovaWindow.Medium -> {
            CompactPlaylistForm(
                state = state,
                onName = onName,
                onType = onType,
                onUrl = onUrl,
                onUsername = onUsername,
                onPassword = onPassword,
                onEpgUrl = onEpgUrl,
                onUserAgent = onUserAgent,
                onReferer = onReferer,
                onSyncInterval = onSyncInterval,
                onTest = onTest,
                onSave = onSave,
                onBack = onBack,
                modifier = modifier,
            )
        }

        NovaWindow.Expanded -> TvPlaylistForm(
            state = state,
            onName = onName,
            onType = onType,
            onUrl = onUrl,
            onUsername = onUsername,
            onPassword = onPassword,
            onEpgUrl = onEpgUrl,
            onUserAgent = onUserAgent,
            onReferer = onReferer,
            onSyncInterval = onSyncInterval,
            onTest = onTest,
            onSave = onSave,
            onBack = onBack,
            modifier = modifier,
        )
    }
}

/**
 * TV gövdesi: iki sütun ve sabit düğme çubuğu bugünkü gibi.
 * `window == Expanded` dışında çizilmez.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvPlaylistForm(
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
    NovaBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 52.dp, vertical = 26.dp),
        ) {
            NovaLabel("Kaynak")
            Text(
                text = if (state.id == null) "Kaynak ekle" else "Kaynağı düzenle",
                style = NovaType.Display,
            )
            Text(
                text = "Kimlik bilgileri cihazda şifreli saklanır, yayın adreslerine gömülmez.",
                style = NovaType.BodyMuted,
            )
            Spacer(Modifier.height(18.dp))
            NovaRule(Modifier.fillMaxWidth())
            Spacer(Modifier.height(18.dp))

            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                        FormSection(modifier = Modifier.width(550.dp)) {
                            NovaLabel("Bağlantı")

                            Row(
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                PlaylistType.entries.forEach { type ->
                                    NovaPill(
                                        label = formTypeLabel(type),
                                        onClick = { onType(type) },
                                        selected = state.type == type,
                                        testTag = "type-${type.name}",
                                    )
                                }
                            }

                            NovaTextFieldTheme {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    FormField(
                                        value = state.name,
                                        onValueChange = onName,
                                        label = "Ad",
                                        placeholder = "Örn. Ev listesi",
                                        testTag = "field-name",
                                    )
                                    FormField(
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
                                        FormField(
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
                                        FormField(
                                            value = state.password,
                                            onValueChange = onPassword,
                                            label = "Şifre",
                                            visualTransformation = PasswordVisualTransformation(),
                                            testTag = "field-password",
                                        )
                                    }
                                    if (state.type == PlaylistType.M3U) {
                                        FormField(
                                            value = state.epgUrl,
                                            onValueChange = onEpgUrl,
                                            label = "EPG URL (isteğe bağlı)",
                                            placeholder = "https://ornek.com/epg.xml",
                                            testTag = "field-epg",
                                        )
                                    }
                                    FormField(
                                        value = state.userAgent,
                                        onValueChange = onUserAgent,
                                        label = "User-Agent (isteğe bağlı)",
                                        placeholder = "Varsayılan: ExoPlayer",
                                        testTag = "field-user-agent",
                                    )
                                    FormField(
                                        value = state.referer,
                                        onValueChange = onReferer,
                                        label = "Referer (isteğe bağlı)",
                                        placeholder = "Yalnızca isteyen sağlayıcılar için",
                                        testTag = "field-referer",
                                    )
                                    FormField(
                                        value = state.syncIntervalHours,
                                        onValueChange = onSyncInterval,
                                        label = "Yenileme aralığı (saat)",
                                        placeholder = "24",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        testTag = "field-interval",
                                    )
                                }
                            }

                        }

                        FormSection(modifier = Modifier.width(280.dp)) {
                            NovaLabel("Özet")
                            Text(
                                text = formTypeLabel(state.type),
                                style = NovaType.Title,
                                modifier = Modifier.testTag("form-type-label"),
                            )
                            Text(
                                text = state.typeHint,
                                style = NovaType.Small,
                            )
                            Spacer(Modifier.height(2.dp))
                            NovaBadge(
                                text = if (state.canSave) "hazır" else "eksik alan",
                                color = if (state.canSave) NovaColors.Live else NovaColors.TextDim,
                            )
                            Spacer(Modifier.height(6.dp))
                            NovaRule(Modifier.fillMaxWidth())
                            NovaLabel("Not")
                            Note("Kanallar kaydedilince arka planda çekilir, Canlı TV'de görünür.")
                            Note("Kimlik bilgileri cihazda şifreli saklanır.")
                            Note("Şifre taşıyan liste adresi yalnızca şifreli depoda tutulur.")

                            state.connectionStatus?.let { status ->
                                Spacer(Modifier.height(4.dp))
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
                    }
                    Spacer(Modifier.height(64.dp))
                }

                // Alt kenar sönümü: içerik devam ediyor, kesik görünmesin.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                1f to NovaColors.Background,
                            ),
                        ),
                )
            }

            Spacer(Modifier.height(12.dp))
            NovaRule(Modifier.fillMaxWidth())
            // Kaydet/Test et her zaman erişilebilir kalsın; form uzun.
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                NovaPill(
                    label = if (state.testing) "Test ediliyor…" else "Test et",
                    onClick = onTest,
                    enabled = state.canSave && !state.testing,
                    testTag = "test-connection",
                )
                NovaPill(
                    label = if (state.saving) "Kaydediliyor…" else "Kaydet",
                    onClick = onSave,
                    enabled = state.canSave && !state.saving,
                    accent = true,
                    testTag = "save-source",
                )
                NovaPill(
                    label = "Geri",
                    onClick = onBack,
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FormSection(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .background(NovaColors.Surface, RoundedCornerShape(14.dp))
            .border(1.dp, NovaColors.Border.copy(alpha = 0.75f), RoundedCornerShape(14.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        content()
    }
}

/** Küçük madde işareti: kehribar nokta + sönük açıklama. */
@Composable
private fun Note(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("·", style = NovaType.Small.copy(color = NovaColors.Accent))
        Text(text, style = NovaType.Small.copy(color = NovaColors.TextMuted))
    }
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    testTag: String,
    placeholder: String = "",
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        NovaLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = if (placeholder.isBlank()) null else {
                { Text(placeholder, style = NovaType.Small) }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            colors = novaTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .tvFieldNavigation()
                .testTag(testTag),
        )
    }
}

internal fun formTypeLabel(type: PlaylistType): String = when (type) {
    PlaylistType.XTREAM -> "Xtream Codes"
    PlaylistType.M3U -> "M3U / M3U8"
    PlaylistType.STALKER -> "Stalker Portal"
}

internal val PlaylistFormState.typeHint: String
    get() = when (type) {
        PlaylistType.XTREAM -> "Sunucu adresi, kullanıcı adı ve şifre ile kanal listesi alınır."
        PlaylistType.M3U -> "Tek bir oynatma listesi bağlantısı; EPG isteğe bağlı."
        PlaylistType.STALKER -> "Portal adresi ve cihaz MAC adresi ile giriş yapılır."
    }

object PlaylistsRoutes {
    const val LIST = "playlists"
    const val FORM = "playlists/form?playlistId={playlistId}"

    fun form(playlistId: Long? = null): String =
        if (playlistId == null) "playlists/form" else "playlists/form?playlistId=$playlistId"
}
