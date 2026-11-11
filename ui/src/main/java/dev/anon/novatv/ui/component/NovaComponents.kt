package dev.anon.novatv.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import dev.anon.novatv.ui.theme.NovaColors
import dev.anon.novatv.ui.theme.NovaType

/**
 * Uygulamanın zemini: dikey gece gradyanı ve sol üstte çok hafif bir ışık.
 * Düz siyah yerine derinlik verir; içerik kartları bu zeminin üstünde durur.
 */
@Composable
fun NovaBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to NovaColors.BackgroundRaised,
                    0.55f to NovaColors.Background,
                    // Açık temada dip siyaha kaçmasın: paletin kendi koyusu.
                    1f to NovaColors.SurfacePressed,
                ),
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(NovaColors.Accent.copy(alpha = 0.04f), Color.Transparent),
                    center = Offset.Zero,
                    radius = 1_100f,
                ),
            ),
    ) {
        content()
    }
}

/** Tüm bölüm başlıklarının üstünde duran geniş aralıklı küçük etiket. */
@Composable
fun NovaLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = NovaType.Label,
        modifier = modifier,
    )
}

/** İnce ayırıcı; kartları bölmek için, gölge yerine çizgi. */
@Composable
fun NovaRule(modifier: Modifier = Modifier, color: Color = NovaColors.Border.copy(alpha = 0.6f)) {
    Box(
        modifier = modifier
            .height(1.dp)
            .background(color),
    )
}

/**
 * Araç çubuğu ve diyalog düğmesi. TV'de dolgu yerine çerçeve kullanır.
 *
 * Odak her zaman temanın tersi: krem dolgu + mürekkep yazı. Kırmızı dolgu
 * yok — kırmızı yalnızca birincil eylemde ince bir halka olarak kalır, böylece
 * ekranın en parlak şeyi hep "neredesin" sorusunun cevabı olur.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NovaPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    accent: Boolean = false,
    danger: Boolean = false,
    enabled: Boolean = true,
    testTag: String? = null,
) {
    val shape = RoundedCornerShape(50)
    val idleContainer = when {
        !enabled -> Color.Transparent
        accent -> NovaColors.Accent.copy(alpha = 0.10f)
        selected -> NovaColors.Focus.copy(alpha = 0.07f)
        danger -> NovaColors.Danger.copy(alpha = 0.10f)
        else -> Color.Transparent
    }
    val idleContent = when {
        !enabled -> NovaColors.TextDim
        accent -> NovaColors.Accent
        selected -> NovaColors.Focus
        danger -> NovaColors.Danger
        else -> NovaColors.TextMuted
    }
    val idleBorder = when {
        !enabled -> NovaColors.Border.copy(alpha = 0.5f)
        accent -> NovaColors.Accent.copy(alpha = 0.45f)
        danger -> NovaColors.Danger.copy(alpha = 0.5f)
        selected -> NovaColors.Focus.copy(alpha = 0.55f)
        else -> NovaColors.Border
    }
    val focusContainer = NovaColors.Focus
    val focusContent = NovaColors.FocusInk
    val focusBorder = when {
        danger -> NovaColors.Danger
        accent -> NovaColors.Accent
        else -> NovaColors.Focus
    }
    NovaTapSurface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        containerColor = idleContainer,
        contentColor = idleContent,
        focusedContainerColor = focusContainer,
        focusedContentColor = focusContent,
        pressedContainerColor = focusContainer,
        pressedContentColor = focusContent,
        border = BorderStroke(1.dp, idleBorder),
        focusedBorder = BorderStroke(2.dp, focusBorder),
        pressedBorder = BorderStroke(2.dp, focusBorder),
        focusedScale = 1.02f,
        pressedScale = 0.98f,
        modifier = modifier.then(
            if (testTag != null) Modifier.testTag(testTag) else Modifier,
        ),
    ) { state ->
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = label,
                style = NovaType.Button.copy(color = state.contentColor),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Küçük durum rozeti: "AKTİF", "XTREAM", "ARŞİV 7G" gibi. */
@Composable
fun NovaBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = NovaColors.TextMuted,
    filled: Boolean = true,
) {
    Box(
        modifier = modifier
            .background(
                color = if (filled) color.copy(alpha = 0.14f) else Color.Transparent,
                shape = CircleShape,
            )
            .then(
                if (filled) Modifier.padding(horizontal = 9.dp, vertical = 3.dp) else Modifier,
            ),
    ) {
        Text(
            text = text.uppercase(),
            style = NovaType.Label.copy(color = color, fontSize = 11.sp, letterSpacing = 1.1.sp),
        )
    }
}

/** Diyalog paneli: koyu yüzey, saç teli çerçeve, geniş iç boşluk. */
@Composable
fun NovaPanel(
    modifier: Modifier = Modifier,
    width: Dp = 440.dp,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .width(width)
            .background(NovaColors.SurfaceHigh, RoundedCornerShape(16.dp))
            .border(1.dp, NovaColors.BorderStrong.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .padding(26.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        content()
    }
}
