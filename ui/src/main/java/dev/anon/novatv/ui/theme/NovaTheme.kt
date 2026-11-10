package dev.anon.novatv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme
import dev.anon.novatv.ui.R

/**
 * "Yayın" kimliği: mürekkep zemin, krem harf, tek sinyal kırmızısı.
 *
 * Kırmızı büyük yüzey doldurmaz; damga olarak yaşar — ikon, canlı/kayıt
 * işareti, rehberde şu anki program, birincil eylem. Ekranın en parlak şeyi
 * odaktır: krem dolgu + mürekkep yazı. Böylece kırmızı her göründüğünde
 * bir anlam taşır.
 *
 * Renkler anlık durum (snapshot state): tema değişince tüm ekranlar yeniden
 * çizilir.
 */
object NovaColors {

    var Background by mutableStateOf(Color(0xFF0C0C0D))
        private set
    var BackgroundRaised by mutableStateOf(Color(0xFF131314))
        private set
    var Surface by mutableStateOf(Color(0xFF191919))
        private set
    var SurfaceHigh by mutableStateOf(Color(0xFF212121))
        private set
    var SurfacePressed by mutableStateOf(Color(0xFF2A2A2A))
        private set
    var Border by mutableStateOf(Color(0xFF2E2E2F))
        private set
    var BorderStrong by mutableStateOf(Color(0xFF454546))
        private set
    var Text by mutableStateOf(Color(0xFFF6F1E7))
        private set
    var TextMuted by mutableStateOf(Color(0xFFA5A29A))
        private set
    var TextDim by mutableStateOf(Color(0xFF7D7A73))
        private set

    /** Sinyal kırmızısı: canlı, kayıt, şu an, birincil eylem. */
    var Accent by mutableStateOf(Color(0xFFE8322A))
        private set

    /** Kırmızı zemin üstündeki yazı. */
    var AccentInk by mutableStateOf(Color(0xFF14110D))
        private set
    var AccentSoft by mutableStateOf(Color(0x24E8322A))
        private set

    /** Odak: temanın tersi. Karanlıkta krem dolgu, açıkta mürekkep dolgu. */
    var Focus by mutableStateOf(Color(0xFFF6F1E7))
        private set
    var FocusInk by mutableStateOf(Color(0xFF14110D))
        private set

    var Live by mutableStateOf(Color(0xFF4FBF7A))
        private set
    var Warning by mutableStateOf(Color(0xFFE8B33A))
        private set
    var Danger by mutableStateOf(Color(0xFFE5737B))
        private set

    /** Tema paletini uygular; aynı değerlerde gereksiz yazma yapmaz. */
    fun apply(dark: Boolean) {
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
        Accent = palette.accent
        AccentInk = palette.accentInk
        AccentSoft = palette.accent.copy(alpha = 0.14f)
        Focus = palette.focus
        FocusInk = palette.focusInk
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
        val accent: Color,
        val accentInk: Color,
        val focus: Color,
        val focusInk: Color,
        val live: Color,
        val warning: Color,
        val danger: Color,
    )

    private val DarkPalette = Palette(
        background = Color(0xFF0C0C0D),
        raised = Color(0xFF131314),
        surface = Color(0xFF191919),
        surfaceHigh = Color(0xFF212121),
        surfacePressed = Color(0xFF2A2A2A),
        border = Color(0xFF2E2E2F),
        borderStrong = Color(0xFF454546),
        text = Color(0xFFF6F1E7),
        textMuted = Color(0xFFA5A29A),
        textDim = Color(0xFF7D7A73),
        accent = Color(0xFFE8322A),
        accentInk = Color(0xFF14110D),
        focus = Color(0xFFF6F1E7),
        focusInk = Color(0xFF14110D),
        live = Color(0xFF4FBF7A),
        warning = Color(0xFFE8B33A),
        danger = Color(0xFFE5737B),
    )

    /** Açık tema mürekkep değil krem kâğıt: basılı yayın rehberi gibi. */
    private val LightPalette = Palette(
        background = Color(0xFFF2EAD9),
        raised = Color(0xFFFBF6EC),
        surface = Color(0xFFFFFCF5),
        surfaceHigh = Color(0xFFEAE1CF),
        surfacePressed = Color(0xFFDFD4BF),
        border = Color(0xFFD8CDB9),
        borderStrong = Color(0xFFB3A68D),
        text = Color(0xFF14110D),
        textMuted = Color(0xFF6B6459),
        textDim = Color(0xFF7F776A),
        accent = Color(0xFFC92C24),
        accentInk = Color(0xFFFBF6EC),
        focus = Color(0xFF14110D),
        focusInk = Color(0xFFFBF6EC),
        live = Color(0xFF1F7A4A),
        warning = Color(0xFF8A5E10),
        danger = Color(0xFFA8323F),
    )
}

/**
 * 10 ayak için tipografi: IBM Plex. Sıkışık (condensed) başlıklar yayın
 * grafiği gibi durur, gövde Plex Sans, saat/tarih/rakamlar Plex Mono —
 * rehber ızgarasında sayılar kaymaz.
 */
object NovaType {

    private val Sans = FontFamily(
        Font(R.font.plex_sans_regular, FontWeight.Normal),
        Font(R.font.plex_sans_medium, FontWeight.Medium),
        Font(R.font.plex_sans_semibold, FontWeight.SemiBold),
    )

    private val Condensed = FontFamily(
        Font(R.font.plex_sans_condensed_semibold, FontWeight.SemiBold),
        Font(R.font.plex_sans_condensed_semibold, FontWeight.Bold),
    )

    private val Mono = FontFamily(
        Font(R.font.plex_mono_medium, FontWeight.Medium),
        Font(R.font.plex_mono_medium, FontWeight.SemiBold),
    )

    val Display: TextStyle
        get() = TextStyle(
            fontFamily = Condensed,
            fontWeight = FontWeight.SemiBold,
            fontSize = 36.sp,
            lineHeight = 42.sp,
            letterSpacing = (-0.2).sp,
            color = NovaColors.Text,
        )

    val Title: TextStyle
        get() = TextStyle(
            fontFamily = Condensed,
            fontWeight = FontWeight.SemiBold,
            fontSize = 25.sp,
            lineHeight = 31.sp,
            letterSpacing = 0.sp,
            color = NovaColors.Text,
        )

    val Body: TextStyle
        get() = TextStyle(
            fontFamily = Sans,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            color = NovaColors.Text,
        )

    val BodyMuted: TextStyle
        get() = Body.copy(color = NovaColors.TextMuted)

    val Small: TextStyle
        get() = TextStyle(
            fontFamily = Sans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            color = NovaColors.TextMuted,
        )

    /** Bölüm etiketi: "CANLI TV", "KAYNAKLAR" gibi geniş aralıklı küçük yazı. */
    val Label: TextStyle
        get() = TextStyle(
            fontFamily = Condensed,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 1.8.sp,
            color = NovaColors.TextDim,
        )

    /** Saat, tarih, kanal numarası: eşit genişlikli rakam. */
    val Number: TextStyle
        get() = TextStyle(
            fontFamily = Mono,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.sp,
            color = NovaColors.TextMuted,
        )

    val Button: TextStyle
        get() = TextStyle(
            fontFamily = Sans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            letterSpacing = 0.1.sp,
        )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NovaTheme(
    dark: Boolean = true,
    typeScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    // Paleti şemadan önce uygula: tüm ekranlar NovaColors'ı doğrudan okur.
    NovaColors.apply(dark = dark)
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
