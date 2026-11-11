package dev.anon.novatv.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import dev.anon.novatv.ui.theme.NovaColors
import dev.anon.novatv.ui.theme.NovaType

/**
 * D-pad ile girilebilen PIN paneli.
 *
 * Sistem klavyesi TV'de hantal olduğu için 3×4 tuş takımı çizilir; 4 hane
 * girilince Tamam etkinleşir. Doğrulama çağırana aittir.
 *
 * [touch] doğrulama: dokunmatik cihazda tuşlar 48 dp tabanına çekilir. TV
 * ölçüleri değişmesin diye varsayılan kapalı.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PinDialog(
    title: String,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
    subtitle: String? = null,
    error: Boolean = false,
    touch: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var digits by remember { mutableStateOf("") }
    val keyModifier = if (touch) Modifier.heightIn(min = 48.dp) else Modifier

    Dialog(onDismissRequest = onDismiss) {
        NovaPanel(width = 340.dp) {
            NovaLabel("PIN")
            Text(
                text = title,
                style = NovaType.Title.copy(fontSize = 22.sp),
                modifier = modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = NovaType.Small.copy(color = NovaColors.TextMuted),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }

            Text(
                text = dots(digits),
                style = NovaType.Display.copy(fontSize = 30.sp, letterSpacing = 10.sp),
                color = if (error) NovaColors.Danger else NovaColors.Accent,
                modifier = Modifier.fillMaxWidth().testTag("pin-dots"),
                textAlign = TextAlign.Center,
            )
            if (error) {
                Text(
                    text = "PIN hatalı",
                    style = NovaType.Small.copy(color = NovaColors.Danger),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }

            for (row in 0 until 4) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (column in 0 until 3) {
                        val label = PAD[row * 3 + column]
                        NovaPill(
                            label = label,
                            onClick = {
                                when (label) {
                                    "Sil" -> digits = digits.dropLast(1)
                                    "Tamam" -> if (digits.length >= 4) onSubmit(digits)
                                    else -> if (digits.length < MAX_DIGITS) digits += label
                                }
                            },
                            modifier = Modifier.weight(1f).then(keyModifier),
                            accent = label == "Tamam",
                            enabled = label != "Tamam" || digits.length >= 4,
                            testTag = "pin-key-$label",
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            NovaPill(
                label = "Vazgeç",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().then(keyModifier),
                testTag = "pin-cancel",
            )
        }
    }
}

private fun dots(digits: String): String =
    if (digits.isEmpty()) "· · · ·" else digits.map { "●" }.joinToString(" ")

private const val MAX_DIGITS = 6

private val PAD = listOf(
    "1", "2", "3",
    "4", "5", "6",
    "7", "8", "9",
    "Sil", "0", "Tamam",
)

/** PIN hatalıysa paneli sıfırlayıp yeniden açmak için ortak yardımcı. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PinGate(
    title: String,
    subtitle: String? = null,
    error: Boolean = false,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    PinDialog(
        title = title,
        subtitle = subtitle,
        error = error,
        onSubmit = onSubmit,
        onDismiss = onDismiss,
    )
}

/** PIN alanı için basit maske; testlerde ve ayarlarda ortak. */
fun maskPin(value: String): String = "●".repeat(value.length.coerceAtMost(MAX_DIGITS))

private val pinTag = "pin-panel"

/** Testlerin paneli bulması için ortak etiket. */
val PinTestTag: String get() = pinTag
