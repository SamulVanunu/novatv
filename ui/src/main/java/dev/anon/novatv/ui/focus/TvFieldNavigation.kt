package dev.anon.novatv.ui.focus

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager

/**
 * TV metin alanları tek başına D-pad ile alandan çıkılamaz: odaktaki alan
 * yön tuşlarını kendine saklar. Bu değiştirici tuşu önizlemede yakalayıp
 * odağı bir sonraki öğeye taşır. [horizontal] yatay geçişi de açar; tek
 * satırlık arama alanı gibi yerlerde kullanılır.
 */
@Composable
fun Modifier.tvFieldNavigation(horizontal: Boolean = false): Modifier {
    val focusManager = LocalFocusManager.current
    return onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) {
            false
        } else {
            val direction = when (event.key) {
                Key.DirectionUp -> FocusDirection.Up
                Key.DirectionDown -> FocusDirection.Down
                Key.DirectionLeft -> if (horizontal) FocusDirection.Left else null
                Key.DirectionRight -> if (horizontal) FocusDirection.Right else null
                else -> null
            }
            if (direction == null) {
                false
            } else {
                focusManager.moveFocus(direction)
                true
            }
        }
    }
}
