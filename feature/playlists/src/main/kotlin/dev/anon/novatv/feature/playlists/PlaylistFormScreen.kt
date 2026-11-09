package dev.anon.novatv.feature.playlists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.anon.novatv.core.model.PlaylistType
import dev.anon.novatv.ui.focus.tvFieldNavigation
import dev.anon.novatv.ui.theme.NovaTextFieldTheme

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
    onSyncInterval: (String) -> Unit,
    onTest: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(state.savedId) {
        state.savedId?.let(onSaved)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = if (state.id == null) "Kaynak ekle" else "Kaynağı düzenle",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        NovaTextFieldTheme {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onName,
                    label = { Text("Ad") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().tvFieldNavigation().testTag("field-name"),
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Tür", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PlaylistType.entries.forEach { type ->
                        Button(
                            onClick = { onType(type) },
                            modifier = Modifier.testTag("type-${type.name}"),
                        ) {
                            Text(if (type == PlaylistType.XTREAM) "Xtream" else type.name)
                        }
                    }
                }

                OutlinedTextField(
                    value = state.url,
                    onValueChange = onUrl,
                    label = { Text("URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().tvFieldNavigation().testTag("field-url"),
                )

                OutlinedTextField(
                    value = state.username,
                    onValueChange = onUsername,
                    label = { Text(if (state.type == PlaylistType.STALKER) "MAC adresi" else "Kullanıcı adı") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().tvFieldNavigation().testTag("field-username"),
                )

                if (state.type == PlaylistType.XTREAM) {
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = onPassword,
                        label = { Text("Şifre") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().tvFieldNavigation().testTag("field-password"),
                    )
                }

                if (state.type == PlaylistType.M3U) {
                    OutlinedTextField(
                        value = state.epgUrl,
                        onValueChange = onEpgUrl,
                        label = { Text("EPG URL (isteğe bağlı)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().tvFieldNavigation().testTag("field-epg"),
                    )
                }

                OutlinedTextField(
                    value = state.userAgent,
                    onValueChange = onUserAgent,
                    label = { Text("User-Agent (isteğe bağlı)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().tvFieldNavigation().testTag("field-user-agent"),
                )

                OutlinedTextField(
                    value = state.syncIntervalHours,
                    onValueChange = onSyncInterval,
                    label = { Text("Yenileme aralığı (saat)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().tvFieldNavigation().testTag("field-interval"),
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onTest,
                enabled = state.canSave && !state.testing,
                modifier = Modifier.testTag("test-connection"),
            ) {
                Text(if (state.testing) "Test ediliyor…" else "Test et")
            }
            Button(
                onClick = onSave,
                enabled = state.canSave && !state.saving,
                modifier = Modifier.testTag("save-source"),
            ) {
                Text(if (state.saving) "Kaydediliyor…" else "Kaydet")
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onBack) { Text("Geri") }
        }

        state.connectionStatus?.let { status ->
            Text(
                text = "Hesap: $status",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.testTag("connection-status"),
            )
        }

        state.error?.let { message ->
            Text(
                text = message,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("form-error"),
            )
        }
    }
}

object PlaylistsRoutes {
    const val LIST = "playlists"
    const val FORM = "playlists/form?playlistId={playlistId}"

    fun form(playlistId: Long? = null): String =
        if (playlistId == null) "playlists/form" else "playlists/form?playlistId=$playlistId"
}
