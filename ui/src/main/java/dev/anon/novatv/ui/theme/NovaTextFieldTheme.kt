package dev.anon.novatv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NovaTextFieldColors = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF00202E),
    background = Color(0xFF0B0F13),
    onBackground = Color(0xFFE6EDF3),
    surface = Color(0xFF101418),
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF1C232B),
    onSurfaceVariant = Color(0xFFB6C2CF),
    outline = Color(0xFF2A3441),
    error = Color(0xFFEF5350),
)

/** TV Material3'te metin alanı yok; material3 alanlarını Nova paletine bağlar. */
@Composable
fun NovaTextFieldTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NovaTextFieldColors, content = content)
}
