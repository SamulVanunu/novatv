package dev.anon.novatv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val NovaColorScheme = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF00202E),
    secondary = Color(0xFF9CCC65),
    onSecondary = Color(0xFF102000),
    background = Color(0xFF0B0F13),
    onBackground = Color(0xFFE6EDF3),
    surface = Color(0xFF101418),
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF1C232B),
    onSurfaceVariant = Color(0xFFB6C2CF),
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NovaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NovaColorScheme, content = content)
}
