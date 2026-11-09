package dev.anon.novatv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NovaTextFieldColors = darkColorScheme(
    primary = NovaColors.Accent,
    onPrimary = NovaColors.AccentInk,
    background = NovaColors.Background,
    onBackground = NovaColors.Text,
    surface = NovaColors.Surface,
    onSurface = NovaColors.Text,
    surfaceVariant = NovaColors.SurfaceHigh,
    onSurfaceVariant = NovaColors.TextMuted,
    outline = NovaColors.Border,
    outlineVariant = NovaColors.Border.copy(alpha = 0.6f),
    error = NovaColors.Danger,
    onError = NovaColors.Background,
)

/** TV Material3'te metin alanı yok; material3 alanlarını Nova paletine bağlar. */
@Composable
fun NovaTextFieldTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NovaTextFieldColors, content = content)
}

/** Tüm form alanlarının ortak rengi: koyu yüzey, odakta kehribar çerçeve. */
@Composable
fun novaTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NovaColors.Accent,
    unfocusedBorderColor = NovaColors.Border,
    disabledBorderColor = NovaColors.Border.copy(alpha = 0.5f),
    focusedContainerColor = NovaColors.Surface,
    unfocusedContainerColor = NovaColors.Surface,
    disabledContainerColor = NovaColors.Background,
    cursorColor = NovaColors.Accent,
    focusedTextColor = NovaColors.Text,
    unfocusedTextColor = NovaColors.Text,
    focusedLabelColor = NovaColors.Accent,
    unfocusedLabelColor = NovaColors.TextMuted,
    focusedPlaceholderColor = NovaColors.TextDim,
    unfocusedPlaceholderColor = NovaColors.TextDim,
)
