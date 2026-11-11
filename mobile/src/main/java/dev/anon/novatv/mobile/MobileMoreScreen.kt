package dev.anon.novatv.mobile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import dev.anon.novatv.ui.component.NovaBackground
import dev.anon.novatv.ui.component.NovaLabel
import dev.anon.novatv.ui.component.NovaTapSurface
import dev.anon.novatv.ui.theme.NovaColors
import dev.anon.novatv.ui.theme.NovaType

/**
 * "Daha" sekmesinin menüsü.
 *
 * Sekme sayısı beşte kalırken ekran sayısı artıyor; arama, çoklu ekran,
 * kaynak yönetimi ve ayarlar bu tek menüden açılır. Satırlar Ayarlar
 * ekranındaki bölüm diliyle gruplanır ([Section] başlıkları aynı kelime
 * dağarcığından) böylece iki ekran aynı kabuğun parçası gibi okunur.
 *
 * Düzen tek kolon ve kaydırılabilir: 411 dp'de 56 dp'lik altı satır artı
 * bölüm başlıkları ekran yüksekliğini aşabilir, kaydırma bunu karşılar.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MobileMoreScreen(
    onSearch: () -> Unit,
    onMultiwall: () -> Unit,
    onSources: () -> Unit,
    onSettings: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    message: String? = null,
    modifier: Modifier = Modifier,
) {
    NovaBackground(modifier = modifier.testTag("more-screen")) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                NovaLabel("Daha")
                Text(text = "Araçlar ve ayarlar", style = NovaType.Title)
            }

            Section("Araçlar") {
                MoreRow(
                    label = "Ara",
                    subtitle = "Kanal, film ve dizi içinde ara",
                    onClick = onSearch,
                    testTag = "more-search",
                )
                MoreRow(
                    label = "Çoklu ekran",
                    subtitle = "Dört kanalı aynı anda izle",
                    onClick = onMultiwall,
                    testTag = "more-multiwall",
                )
            }

            Section("Kaynak") {
                MoreRow(
                    label = "Kaynaklar",
                    subtitle = "Listeler, güncelleme ve sıralama",
                    onClick = onSources,
                    testTag = "more-sources",
                )
                MoreRow(
                    label = "Ayarlar",
                    subtitle = "Görünüm, yayın ve sistem",
                    onClick = onSettings,
                    testTag = "more-settings",
                )
            }

            Section("Yedekleme") {
                MoreRow(
                    label = "Yedek al",
                    subtitle = "Görünüm ve profil adlarını dosyaya yaz",
                    onClick = onBackup,
                    testTag = "more-backup",
                )
                MoreRow(
                    label = "Geri yükle",
                    subtitle = "TV yedeğini bu cihaza uygula",
                    onClick = onRestore,
                    testTag = "more-restore",
                )
                // Ayarlar ekranındaki mesaj satırının aynısı: yedek alma ve
                // geri yükleme sonucu burada okunur.
                message?.let { text ->
                    Text(
                        text = text,
                        style = NovaType.Small.copy(color = NovaColors.Accent),
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .testTag("more-message"),
                    )
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        NovaLabel(title)
        content()
    }
}

/**
 * Menü satırı: etiket ve kısa açıklama, sağda ince ok glifi.
 *
 * Dokunma hedefi en az 56 dp; odak dolgusu yalnız kumanda/klavye odağında
 * görünür (`NovaTapSurface` sözleşmesi).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MoreRow(
    label: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String,
) {
    NovaTapSurface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        containerColor = NovaColors.Surface,
        contentColor = NovaColors.Text,
        focusedContainerColor = NovaColors.SurfaceHigh,
        focusedContentColor = NovaColors.Text,
        pressedContainerColor = NovaColors.SurfacePressed,
        pressedContentColor = NovaColors.Text,
        border = BorderStroke(1.dp, NovaColors.Border.copy(alpha = 0.8f)),
        focusedBorder = BorderStroke(2.dp, NovaColors.Focus),
        focusedScale = 1f,
        pressedScale = 0.99f,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag(testTag),
    ) { state ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = NovaType.Body.copy(color = state.contentColor),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = NovaType.Small.copy(fontSize = 13.sp, color = NovaColors.TextMuted),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "›",
                fontSize = 20.sp,
                color = if (state.focused) state.contentColor else NovaColors.TextDim,
                maxLines = 1,
            )
        }
    }
}
