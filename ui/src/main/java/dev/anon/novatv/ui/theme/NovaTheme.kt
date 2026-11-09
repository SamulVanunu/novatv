package dev.anon.novatv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

/**
 * NovaTV renk paleti: gece yayını hissi. Sıcak kehribar tek vurgu rengi,
 * geri kalanı soğuk arduvaz tonları — ekranda renk yarışı olmasın.
 */
object NovaColors {
    val Background = Color(0xFF070A0E)
    val BackgroundRaised = Color(0xFF0C1116)
    val Surface = Color(0xFF121821)
    val SurfaceHigh = Color(0xFF18202A)
    val SurfacePressed = Color(0xFF1E2732)
    val Border = Color(0xFF26313D)
    val BorderStrong = Color(0xFF3A4856)
    val Text = Color(0xFFEDF1F5)
    val TextMuted = Color(0xFF94A3B1)
    val TextDim = Color(0xFF66727E)
    val Accent = Color(0xFFE7B45A)
    val AccentInk = Color(0xFF17110A)
    val AccentSoft = Color(0x2EE7B45A)
    val Live = Color(0xFF63D08E)
    val Warning = Color(0xFFE0A458)
    val Danger = Color(0xFFE5737B)
}

/**
 * 10 ayak mesafesi için tipografi: gövde 18 sp'nin altına inmez, başlıklar
 * sıkı harf aralığıyla ağırlaşır, etiketler geniş aralıkla sakinleşir.
 */
object NovaType {
    val Display = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.6).sp,
        color = NovaColors.Text,
    )

    val Title = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp,
        color = NovaColors.Text,
    )

    val Body = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = NovaColors.Text,
    )

    val BodyMuted = Body.copy(color = NovaColors.TextMuted)

    val Small = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        color = NovaColors.TextMuted,
    )

    /** Bölüm etiketi: "CANLI TV", "KAYNAKLAR" gibi geniş aralıklı küçük yazı. */
    val Label = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.6.sp,
        color = NovaColors.TextDim,
    )

    /** Sayısal rozetler: tabular rakamlar hizalansın. */
    val Number = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        letterSpacing = 0.4.sp,
        fontFeatureSettings = "tnum",
        color = NovaColors.TextMuted,
    )

    val Button = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 0.2.sp,
    )
}

private val NovaColorScheme = darkColorScheme(
    primary = NovaColors.Accent,
    onPrimary = NovaColors.AccentInk,
    primaryContainer = NovaColors.Accent,
    onPrimaryContainer = NovaColors.AccentInk,
    secondary = NovaColors.TextMuted,
    onSecondary = NovaColors.Background,
    background = NovaColors.Background,
    onBackground = NovaColors.Text,
    surface = NovaColors.Surface,
    onSurface = NovaColors.Text,
    surfaceVariant = NovaColors.SurfaceHigh,
    onSurfaceVariant = NovaColors.TextMuted,
    border = NovaColors.Border,
    error = NovaColors.Danger,
    onError = NovaColors.Background,
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NovaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NovaColorScheme, content = content)
}
