package dev.anon.novatv.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Yüzey çerçevesi: çizgi kalınlığı ve rengi. `BorderStroke` yerine kendi
 * tipimiz — bu modülde sıcak derleme, geçişli foundation üyesini bazen
 * çözemiyor; tipler burada açık ve sabit.
 */
@Immutable
data class NovaStroke(val width: Dp, val color: Color)

/**
 * Yüzey: tek bir ilkel, iki işaretçi — TV kumandası (D-pad/ENTER) ve parmak.
 *
 * tv-material'ın `ClickableSurface`'ı `tvClickable` üzerinden yalnız odaktaki
 * Enter tuşunu işler; işaretçi (dokunma) olayı işlemez. `NovaTapSurface`
 * foundation `clickable`'ı kullanır: dokunma, erişilebilirlik "tıkla" eylemi
 * ve odaktaki ENTER/DPAD_CENTER/boşluk tuşu aynı `onClick`'e gider (foundation
 * zaten odaktaki Enter'ı yakalar; tuş işleme burada tekrar yazılmaz).
 *
 * Odaklı ve basılı durum yalnız [interactionSource]'tan okunur; `clickable`
 * kendi odak/pres olaylarını aynı kaynağa yayar. Ayrı bir `focusable` veya
 * `onFocusChanged` eklenmez — tek mekanizma, ikinci bir odak kaynağı yok.
 * Dokunmak odak **vermez**: krem odak dolgusu yalnız gerçek D-pad odağında
 * görünür.
 *
 * Görünüm sözleşmesi: dolgu ve çerçeve renkleri durumdan seçilip animasyonla
 * geçer; ölçek `focusedScale`/`pressedScale` ile büyür/küçülür. Yazı rengi
 * (`contentColor`) içeriğe [NovaTapState] ile döner: tv-material'ın `Text`
 * rengi stil üzerinden alır, bu yüzden içerik kendi yazı rengini bu state'ten
 * okur (tek renk mekanizması, ikinci bir CompositionLocal yok). Ripple yok —
 * kimlik mürekkep ve kremden ibaret.
 */
@Composable
fun NovaTapSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    containerColor: Color = Color.Transparent,
    contentColor: Color = Color.Unspecified,
    focusedContainerColor: Color = containerColor,
    focusedContentColor: Color = contentColor,
    pressedContainerColor: Color = focusedContainerColor,
    pressedContentColor: Color = focusedContentColor,
    border: NovaStroke? = null,
    focusedBorder: NovaStroke? = null,
    pressedBorder: NovaStroke? = null,
    enabled: Boolean = true,
    focusedScale: Float = 1f,
    pressedScale: Float = 1f,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable (NovaTapState) -> Unit,
) {
    val focused by interactionSource.collectIsFocusedAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    // Basılı, odaktan önce gelir: Enter'a basılı tutarken de dokunurken de
    // "basıldı" hissi odak dilinin üstünde çalışır.
    val container by animateColorAsState(
        targetValue = when {
            !enabled -> containerColor
            pressed -> pressedContainerColor
            focused -> focusedContainerColor
            else -> containerColor
        },
        label = "nova-tap-container",
    )
    val resolvedContentColor by animateColorAsState(
        targetValue = when {
            !enabled -> contentColor
            pressed -> pressedContentColor
            focused -> focusedContentColor
            else -> contentColor
        },
        label = "nova-tap-content",
    )
    val stroke = when {
        !enabled -> border
        pressed -> pressedBorder ?: focusedBorder ?: border
        focused -> focusedBorder ?: border
        else -> border
    }
    val strokeColor by animateColorAsState(
        targetValue = stroke?.color ?: Color.Transparent,
        label = "nova-tap-border",
    )
    val strokeWidth by animateFloatAsState(
        targetValue = (stroke?.width ?: 0.dp).value,
        label = "nova-tap-border-width",
    )
    val scale by animateFloatAsState(
        targetValue = when {
            !enabled -> 1f
            pressed -> pressedScale
            focused -> focusedScale
            else -> 1f
        },
        label = "nova-tap-scale",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(color = container, shape = shape)
            .then(
                if (strokeWidth > 0f) {
                    Modifier.border(BorderStroke(strokeWidth.dp, strokeColor), shape)
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
        propagateMinConstraints = true,
    ) {
        content(
            NovaTapState(
                containerColor = container,
                contentColor = resolvedContentColor,
                focused = focused,
                pressed = pressed,
                enabled = enabled,
            ),
        )
    }
}

/** Yüzeyin çözülmüş hâli: içerik yazı/glif rengini buradan okur. */
@Immutable
data class NovaTapState(
    val containerColor: Color = Color.Transparent,
    val contentColor: Color = Color.Unspecified,
    val focused: Boolean = false,
    val pressed: Boolean = false,
    val enabled: Boolean = true,
)
