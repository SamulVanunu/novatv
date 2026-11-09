package dev.anon.novatv.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import dev.anon.novatv.core.model.ParentalPolicy
import dev.anon.novatv.core.model.Profile
import dev.anon.novatv.ui.component.NovaBackground
import dev.anon.novatv.ui.component.NovaBadge
import dev.anon.novatv.ui.component.NovaLabel
import dev.anon.novatv.ui.component.NovaPanel
import dev.anon.novatv.ui.component.NovaPill
import dev.anon.novatv.ui.component.NovaRule
import dev.anon.novatv.ui.component.PinDialog
import dev.anon.novatv.ui.focus.tvFieldNavigation
import dev.anon.novatv.ui.theme.NovaColors
import dev.anon.novatv.ui.theme.NovaTextFieldTheme
import dev.anon.novatv.ui.theme.NovaType
import dev.anon.novatv.ui.theme.novaTextFieldColors

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ProfilesScreen(
    state: ProfilesUiState,
    onSelect: (Profile) -> Unit,
    onSubmitPin: (String) -> Unit,
    onCancelPin: () -> Unit,
    onAdd: (name: String, pin: String?, limit: String?, kids: Boolean) -> Unit,
    onUpdate: (Profile, String, String?, String?, Boolean) -> Unit,
    onRemovePin: (Profile) -> Unit,
    onDelete: (Profile) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<Profile?>(null) }
    var adding by remember { mutableStateOf(false) }

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
                    NovaLabel("Profiller")
                    Text(text = "Kim izliyor?", style = NovaType.Title)
                }
                NovaPill(label = "Yeni profil", onClick = { adding = true }, testTag = "profile-add")
                NovaPill(label = "Ayarlar", onClick = onBack, testTag = "profiles-back")
            }

            Spacer(Modifier.height(14.dp))
            NovaRule(Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))

            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.profiles.forEach { profile ->
                    ProfileRow(
                        profile = profile,
                        active = profile.id == state.activeId,
                        canDelete = state.profiles.size > 1,
                        onSelect = { onSelect(profile) },
                        onEdit = { editing = profile },
                        onDelete = { onDelete(profile) },
                    )
                }
            }

            state.message?.let { message ->
                Text(
                    text = message,
                    style = NovaType.Small.copy(color = NovaColors.Accent),
                    modifier = Modifier.padding(top = 8.dp).testTag("profiles-message"),
                )
            }
        }
    }

    state.pinTarget?.let { target ->
        PinDialog(
            title = "${target.name} profiline geç",
            subtitle = "Bu profil PIN ile korunuyor",
            error = state.pinError,
            onSubmit = onSubmitPin,
            onDismiss = onCancelPin,
        )
    }

    if (adding) {
        ProfileFormDialog(
            title = "Yeni profil",
            profile = null,
            onSave = { name, pin, limit, kids ->
                onAdd(name, pin, limit, kids)
                adding = false
            },
            onDismiss = { adding = false },
        )
    }

    editing?.let { profile ->
        ProfileFormDialog(
            title = "Profili düzenle",
            profile = profile,
            onSave = { name, pin, limit, kids ->
                onUpdate(profile, name, pin, limit, kids)
                editing = null
            },
            onRemovePin = { onRemovePin(profile) },
            onDismiss = { editing = null },
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProfileRow(
    profile: Profile,
    active: Boolean,
    canDelete: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        NovaPill(
            label = profile.name,
            onClick = onSelect,
            selected = active,
            modifier = Modifier.weight(1f),
            testTag = "profile-${profile.id}",
        )
        if (profile.pinHash != null) {
            NovaBadge(text = "PIN", color = NovaColors.Accent)
        }
        if (profile.isKids) {
            NovaBadge(text = "ÇOCUK", color = NovaColors.Live)
        }
        profile.maxRating?.let { limit ->
            NovaBadge(text = "$limit+", color = NovaColors.TextMuted)
        }
        if (active) {
            NovaBadge(text = "AKTİF", color = NovaColors.Accent)
        }
        NovaPill(label = "Düzenle", onClick = onEdit, testTag = "profile-edit-${profile.id}")
        if (canDelete) {
            NovaPill(
                label = "Sil",
                onClick = onDelete,
                danger = true,
                enabled = !active,
                testTag = "profile-delete-${profile.id}",
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProfileFormDialog(
    title: String,
    profile: Profile?,
    onSave: (name: String, pin: String?, limit: String?, kids: Boolean) -> Unit,
    onDismiss: () -> Unit,
    onRemovePin: (() -> Unit)? = null,
) {
    var name by remember { mutableStateOf(profile?.name.orEmpty()) }
    var pin by remember { mutableStateOf("") }
    var limit by remember { mutableStateOf(profile?.maxRating) }
    var kids by remember { mutableStateOf(profile?.isKids ?: false) }

    Dialog(onDismissRequest = onDismiss) {
        NovaPanel(width = 520.dp) {
            NovaLabel(title)
            NovaTextFieldTheme {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profil adı", style = NovaType.Small) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = novaTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().tvFieldNavigation(horizontal = false),
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { value ->
                        pin = value.filter(Char::isDigit).take(6)
                    },
                    label = {
                        Text(
                            if (profile == null) "PIN (isteğe bağlı)" else "Yeni PIN (boş = değişmez)",
                            style = NovaType.Small,
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(12.dp),
                    colors = novaTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().tvFieldNavigation(horizontal = false),
                )
            }

            Text(text = "İçerik sınırı", style = NovaType.Small.copy(color = NovaColors.TextMuted))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NovaPill(
                    label = "Yok",
                    onClick = { limit = null },
                    selected = limit == null,
                )
                ParentalPolicy.LIMITS.forEach { value ->
                    NovaPill(
                        label = "$value+",
                        onClick = { limit = value },
                        selected = limit == value,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NovaPill(
                    label = if (kids) "Çocuk profili · açık" else "Çocuk profili · kapalı",
                    onClick = { kids = !kids },
                    selected = kids,
                    testTag = "profile-kids",
                )
            }

            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NovaPill(
                    label = "Kaydet",
                    onClick = { onSave(name, pin.ifBlank { null }, limit, kids) },
                    accent = true,
                    modifier = Modifier.weight(1f),
                    testTag = "profile-save",
                )
                NovaPill(
                    label = "Vazgeç",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    testTag = "profile-cancel",
                )
            }
            if (profile?.pinHash != null && onRemovePin != null) {
                NovaPill(
                    label = "PIN'i kaldır",
                    onClick = {
                        onRemovePin()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Profil listesinin boş olmadığını varsayan küçük yardımcı; testlerde kullanılır. */
internal fun ProfilesUiState.hasProfiles(): Boolean = profiles.isNotEmpty()

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun ProfileEmpty() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Profil yok",
            style = NovaType.Display.copy(fontSize = 24.sp),
            modifier = Modifier.width(200.dp),
        )
    }
}
