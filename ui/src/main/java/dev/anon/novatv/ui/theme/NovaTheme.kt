package dev.anon.novatv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme

/**
 * NovaTV renk paleti: gece yayını hissi. Sıcak kehribar tek vurgu rengi,
 * geri kalanı soğuk arduvaz tonları — ekranda renk yarışı olmasın.
 *
 * Renkler anlık durum (snapshot state) olarak tutulur: Ayarlar ekranından
 * tema/vurgu değişince tüm ekranlar yeniden çizilir.
 */
object NovaColors {

    var Background by mutableStateOf(Color(0xFF070A0E))
        private set
    var BackgroundRaised by mutableStateOf(Color(0xFF0C1116))
        private set
    var Surface by mutableStateOf(Color(0xFF121821))
        private set
    var SurfaceHigh by mutableStateOf(Color(0xFF18202A))
        private set
    var SurfacePressed by mutableStateOf(Color(0xFF1E2732))
        private set
    var Border by mutableStateOf(Color(0xFF26313D))
        private set
    var BorderStrong by mutableStateOf(Color(0xFF3A4856))
        private set
    var Text by mutableStateOf(Color(0xFFEDF1F5))
        private set
    var TextMuted by mutableStateOf(Color(0xFF94A3B1))
        private set
    var TextDim by mutableStateOf(Color(0xFF66727E))
        private set
    var Accent by mutableStateOf(Color(0xFFE7B45A))
        private set
    var AccentInk by mutableStateOf(Color(0xFF17110A))
        private set
    var AccentSoft by mutableStateOf(Color(0x2EE7B45A))
        private set
    var Live by mutableStateOf(Color(0xFF63D08E))
        private set
    var Warning by mutableStateOf(Color(0xFFE0A458))
        private set
    var Danger by mutableStateOf(Color(0xFFE5737B))
        private set

    /** Tema ve vurgu rengini uygular; aynı değerlerde gereksiz yazma yapmaz. */
    fun apply(dark: Boolean, accent: Color) {
        val palette = if (dark) DarkPalette else LightPalette
        Background = palette.background
        BackgroundRaised = palette.raised
        Surface = palette.surface
        SurfaceHigh = palette.surfaceHigh
        SurfacePressed = palette.surfacePressed
        Border = palette.border
        BorderStrong = palette.borderStrong
        Text = palette.text
        TextMuted = palette.textMuted
        TextDim = palette.textDim
        Accent = accent
        AccentInk = Color(0xFF17110A)
        AccentSoft = accent.copy(alpha = 0.18f)
        Live = palette.live
        Warning = palette.warning
        Danger = palette.danger
    }

    private data class Palette(
        val background: Color,
        val raised: Color,
        val surface: Color,
        val surfaceHigh: Color,
        val surfacePressed: Color,
        val border: Color,
        val borderStrong: Color,
        val text: Color,
        val textMuted: Color,
        val textDim: Color,
        val live: Color,
        val warning: Color,
        val danger: Color,
    )

    private val DarkPalette = Palette(
        background = Color(0xFF070A0E),
        raised = Color(0xFF0C1116),
        surface = Color(0xFF121821),
        surfaceHigh = Color(0xFF18202A),
        surfacePressed = Color(0xFF1E2732),
        border = Color(0xFF26313D),
        borderStrong = Color(0xFF3A4856),
        text = Color(0xFFEDF1F5),
        textMuted = Color(0xFF94A3B1),
        textDim = Color(0xFF66727E),
        live = Color(0xFF63D08E),
        warning = Color(0xFFE0A458),
        danger = Color(0xFFE5737B),
    )

    private val LightPalette = Palette(
        background = Color(0xFFF4F1EA),
        raised = Color(0xFFFBF8F1),
        surface = Color(0xFFFFFFFF),
        surfaceHigh = Color(0xFFEDE7DB),
        surfacePressed = Color(0xFFE3DBCD),
        border = Color(0xFFD6CDBC),
        borderStrong = Color(0xFFB9AE9A),
        text = Color(0xFF1B1A17),
        textMuted = Color(0xFF5C574E),
        textDim = Color(0xFF837C70),
        live = Color(0xFF1F8A52),
        warning = Color(0xFF9A6516),
        danger = Color(0xFFB23A45),
    )

    /** Ayarlardaki vurgu adı için hazır tonlar. */
    val accents: List<Pair<String, Color>> = listOf(
        "Kehribar" to Color(0xFFE7B45A),
        "Turkuaz" to Color(0xFF5AC7C4),
        "Gül" to Color(0xFFE79AA8),
        "Mavi" to Color(0xFF7FB4E7),
    )
}

/**
 * 10 ayak mesafesi için tipografi: gövde 18 sp'nin altına inmez, başlıklar
 * sıkı harf aralığıyla ağırlaşır, etiketler geniş aralıkla sakinleşir.
 *
 * Renkler `get()` ile okunur; tema değişince stiller de güncel rengi alır.
 */
object NovaType {
    val Display: TextStyle
        get() = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 34.sp,
            lineHeight = 40.sp,
            letterSpacing = (-0.6).sp,
            color = NovaColors.Text,
        )

    val Title: TextStyle
        get() = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            letterSpacing = (-0.3).sp,
            color = NovaColors.Text,
        )

    val Body: TextStyle
        get() = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            color = NovaColors.Text,
        )

    val BodyMuted: TextStyle
        get() = Body.copy(color = NovaColors.TextMuted)

    val Small: TextStyle
        get() = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            color = NovaColors.TextMuted,
        )

    /** Bölüm etiketi: "CANLI TV", "KAYNAKLAR" gibi geniş aralıklı küçük yazı. */
    val Label: TextStyle
        get() = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 1.6.sp,
            color = NovaColors.TextDim,
        )

    /** Sayısal rozetler: tabular rakamlar hizalansın. */
    val Number: TextStyle
        get() = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            letterSpacing = 0.4.sp,
            fontFeatureSettings = "tnum",
            color = NovaColors.TextMuted,
        )

    val Button: TextStyle
        get() = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            letterSpacing = 0.2.sp,
        )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NovaTheme(
    dark: Boolean = true,
    typeScale: Float = 1f,
    accent: Color? = null,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    // Paleti şemadan önce uygula: tüm ekranlar NovaColors'ı doğrudan okur.
    NovaColors.apply(dark = dark, accent = accent ?: NovaColors.accents.first().second)
    val scheme = if (dark) {
        darkColorScheme(
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
    } else {
        lightColorScheme(
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
    }

    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density,
            fontScale = density.fontScale * typeScale.coerceIn(0.85f, 1.35f),
        ),
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
