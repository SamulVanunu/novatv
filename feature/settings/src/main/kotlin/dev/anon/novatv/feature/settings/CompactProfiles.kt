package dev.anon.novatv.feature.settings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
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
import dev.anon.novatv.ui.component.NovaPill
import dev.anon.novatv.ui.component.NovaRule
import dev.anon.novatv.ui.component.NovaTapSurface
import dev.anon.novatv.ui.layout.rememberNovaSpacing
import dev.anon.novatv.ui.theme.NovaColors
import dev.anon.novatv.ui.theme.NovaTextFieldTheme
import dev.anon.novatv.ui.theme.NovaType
import dev.anon.novatv.ui.theme.novaTextFieldColors

/**
 * Profiller ekranının telefon/tablet düzeni.
 *
 * Tek sütun; her profil tam genişlikte, en az 56 dp, dokununca seçilir.
 * Düzenle ve Sil satırın ikinci katmanındadır, her zaman görünür ve 48 dp'dir.
 * Yeni profil ve geri dönüş düğmeleri başlığın altında sarar.
 *
 * PIN akışı TV ile ortak: `PinDialog` ve `ProfileFormDialog` aynen kullanılır;
 * kompakt yol yalnız listeyi dokunmaya uygun kurar.
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun CompactProfiles(
    state: ProfilesUiState,
    onSelect: (Profile) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Profile) -> Unit,
    onDelete: (Profile) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = rememberNovaSpacing()

    NovaBackground(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .imePadding()
                    .testTag("profiles-compact"),
                contentPadding = PaddingValues(
                    start = spacing.screen,
                    end = spacing.screen,
                    top = spacing.screen,
                    bottom = spacing.screen,
                ),
                verticalArrangement = Arrangement.spacedBy(spacing.gap),
            ) {
                item(key = "header") {
                    CompactProfilesHeader(onAdd = onAdd, onBack = onBack)
                }

                item(key = "rule") {
                    NovaRule(Modifier.fillMaxWidth())
                }

                if (state.profiles.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = "Profil yok",
                            style = NovaType.Title,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    }
                } else {
                    items(state.profiles, key = { it.id }) { profile ->
                        CompactProfileRow(
                            profile = profile,
                            active = profile.id == state.activeId,
                            canDelete = state.profiles.size > 1,
                            onSelect = { onSelect(profile) },
                            onEdit = { onEdit(profile) },
                            onDelete = { onDelete(profile) },
                        )
                    }
                }

                state.message?.let { message ->
                    item(key = "message") {
                        Text(
                            text = message,
                            style = NovaType.Small.copy(color = NovaColors.Accent),
                            modifier = Modifier.testTag("profiles-message"),
                        )
                    }
                }
            }
        }
    }
}

/** Başlık, yeni profil ve geri dönüş: dar ekranda alt satıra sarar. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompactProfilesHeader(onAdd: () -> Unit, onBack: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        NovaLabel("Profiller")
        Text(text = "Kim izliyor?", style = NovaType.Title)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NovaPill(
                label = "Yeni profil",
                onClick = onAdd,
                modifier = Modifier.heightIn(min = CompactProfileControlHeight),
                testTag = "profile-add",
            )
            NovaPill(
                label = "Ayarlar",
                onClick = onBack,
                modifier = Modifier.heightIn(min = CompactProfileControlHeight),
                testTag = "profiles-back",
            )
        }
    }
}

/**
 * Profil kartı: yüzeyine dokunmak profili seçer; rozetler adın altındadır
 * ki 411 dp'de sığsın. Düzenle/Sil ayrı 48 dp hedefler, silme yalnız etkin
 * olmayan ve tek kalmayan profilde çizilir (TV ile aynı kural).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CompactProfileRow(
    profile: Profile,
    active: Boolean,
    canDelete: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NovaColors.Surface, shape)
            .border(
                1.dp,
                if (active) NovaColors.Accent.copy(alpha = 0.75f) else NovaColors.Border.copy(alpha = 0.8f),
                shape,
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        NovaTapSurface(
            onClick = onSelect,
            shape = RoundedCornerShape(8.dp),
            containerColor = Color.Transparent,
            contentColor = NovaColors.Text,
            focusedContainerColor = NovaColors.SurfacePressed,
            focusedContentColor = NovaColors.Text,
            border = null,
            focusedBorder = BorderStroke(2.dp, NovaColors.Focus),
            focusedScale = 1f,
            pressedScale = 0.995f,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = CompactProfileRowHeight)
                .testTag("profile-${profile.id}"),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = profile.name,
                    style = NovaType.Body.copy(fontSize = 17.sp, lineHeight = 22.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                CompactProfileBadges(profile = profile, active = active)
            }
        }

        CompactPillRow {
            NovaPill(
                label = "Düzenle",
                onClick = onEdit,
                modifier = Modifier.heightIn(min = CompactProfileControlHeight),
                testTag = "profile-edit-${profile.id}",
            )
            if (canDelete) {
                NovaPill(
                    label = "Sil",
                    onClick = onDelete,
                    danger = true,
                    enabled = !active,
                    modifier = Modifier.heightIn(min = CompactProfileControlHeight),
                    testTag = "profile-delete-${profile.id}",
                )
            }
        }
    }
}

/** PIN, çocuk profili, içerik sınırı ve etkinlik damgaları. */
@Composable
private fun CompactProfileBadges(profile: Profile, active: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (profile.pinHash != null) NovaBadge(text = "PIN", color = NovaColors.Accent)
        if (profile.isKids) NovaBadge(text = "ÇOCUK", color = NovaColors.Live)
        profile.maxRating?.let { limit -> NovaBadge(text = "$limit+", color = NovaColors.TextMuted) }
        if (active) NovaBadge(text = "AKTİF", color = NovaColors.Accent)
    }
}

/** Tek satır, yatay kaydırılan eylem şeridi; kırpılmaz. */
@Composable
private fun CompactPillRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

/**
 * Profil formu: TV'de sabit 520 dp panel; 411 dp'de paneller ekranı taşırır.
 * Kompakt yol içeriği kaydırılabilir, tam genişlik bir karta yerleştirir.
 * Alanlar ve düğmeler TV ile aynı etiket ve tag'leri taşır; PIN akışı
 * (kaydet / kaldır / vazgeç) değişmez.
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun CompactProfileFormDialog(
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(NovaColors.SurfaceHigh, RoundedCornerShape(16.dp))
                .border(1.dp, NovaColors.BorderStrong.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NovaLabel(title)
            NovaTextFieldTheme {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profil adı", style = NovaType.Small) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = novaTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = CompactProfileControlHeight),
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { value -> pin = value.filter(Char::isDigit).take(6) },
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = CompactProfileControlHeight),
                )
            }

            Text(text = "İçerik sınırı", style = NovaType.Small.copy(color = NovaColors.TextMuted))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NovaPill(
                    label = "Yok",
                    onClick = { limit = null },
                    selected = limit == null,
                    modifier = Modifier.heightIn(min = CompactProfileControlHeight),
                )
                ParentalPolicy.LIMITS.forEach { value ->
                    NovaPill(
                        label = "$value+",
                        onClick = { limit = value },
                        selected = limit == value,
                        modifier = Modifier.heightIn(min = CompactProfileControlHeight),
                    )
                }
            }

            NovaPill(
                label = if (kids) "Çocuk profili · açık" else "Çocuk profili · kapalı",
                onClick = { kids = !kids },
                selected = kids,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = CompactProfileControlHeight),
                testTag = "profile-kids",
            )

            Spacer(Modifier.height(2.dp))
            NovaPill(
                label = "Kaydet",
                onClick = { onSave(name, pin.ifBlank { null }, limit, kids) },
                accent = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = CompactProfilePrimaryHeight),
                testTag = "profile-save",
            )
            NovaPill(
                label = "Vazgeç",
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = CompactProfilePrimaryHeight),
                testTag = "profile-cancel",
            )
            if (profile?.pinHash != null && onRemovePin != null) {
                NovaPill(
                    label = "PIN'i kaldır",
                    onClick = {
                        onRemovePin()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = CompactProfileControlHeight),
                )
            }
        }
    }
}

/** Profil satırının alt sınırı; ad ve rozetlerle 56 dp'yi aşar. */
internal val CompactProfileRowHeight = 56.dp

/** Profildeki düğme ve alan dokunma hedefi. */
internal val CompactProfileControlHeight = 48.dp

/** Profil formunun birincil düğmeleri (Kaydet, Vazgeç). */
internal val CompactProfilePrimaryHeight = 56.dp
