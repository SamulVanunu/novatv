package dev.anon.novatv.feature.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.ui.component.NovaBadge
import dev.anon.novatv.ui.component.NovaLabel
import dev.anon.novatv.ui.component.NovaRule
import dev.anon.novatv.ui.component.NovaTapSurface
import dev.anon.novatv.ui.theme.NovaColors
import dev.anon.novatv.ui.theme.NovaTextFieldTheme
import dev.anon.novatv.ui.theme.NovaType
import dev.anon.novatv.ui.theme.novaTextFieldColors

/**
 * Kanal listesi: videonun üstünde tam genişlikte panel (ayrı pencere değil).
 *
 * Zapping sırası ([order]) gösterilir; oynatılan kanal 3 dp kırmızı damga ile
 * işaretlenir (kimlik: kırmızı zemin doldurmaz, damga olarak yaşar). Arama
 * alanı kanal adına ve numarasına göre süzer; her satır en az 56 dp yüksekliğinde
 * dokunma hedefidir. Kanala dokunmak [onSelect] çağırır; kapanmayı çağıran yönetir.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun ChannelPickerSheet(
    order: List<Channel>,
    current: Channel?,
    onSelect: (Channel) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    val filtered = remember(order, query) { filterChannels(order, query) }
    val searching = query.isNotBlank()

    // Arama alanı kendiliğinden odaklanmaz: odaklanınca klavye açılıyor ve
    // ekranın yarısını kaplayıp listeyi seçilemez hale getiriyordu. Klavye
    // yalnız kullanıcı alana dokununca açılır; `imePadding` listeyi üstte tutar.
    // Arama metni panelle birlikte yaşar; panel durumunu tutan `pickerOpen` de
    // kaydedilmediğinden ikisi aynı.

    Column(
        modifier = modifier
            .background(NovaColors.Background.copy(alpha = 0.97f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("channel-picker"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NovaLabel("Kanallar")
            Text(
                text = "${filtered.size} / ${order.size}",
                style = NovaType.Small.copy(color = NovaColors.TextDim),
            )
            Box(modifier = Modifier.weight(1f))
            NovaTapSurface(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
                contentColor = NovaColors.TextMuted,
                focusedContainerColor = NovaColors.Focus,
                focusedContentColor = NovaColors.FocusInk,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("picker-close"),
            ) { state ->
                Text(
                    text = "Kapat",
                    style = NovaType.Button.copy(color = state.contentColor),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        NovaTextFieldTheme {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = {
                    Text("Kanal ara", style = NovaType.Small.copy(color = NovaColors.TextDim))
                },
                textStyle = NovaType.Body,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                colors = novaTextFieldColors(),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .testTag("picker-search"),
            )
        }

        NovaRule(Modifier.fillMaxWidth().padding(top = 10.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (searching) "Kanal bulunamadı" else "Kanal listesi boş",
                    style = NovaType.Title.copy(color = NovaColors.TextMuted),
                    modifier = Modifier.testTag("picker-empty"),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("picker-list"),
            ) {
                itemsIndexed(filtered, key = { _, channel -> channel.id }) { index, channel ->
                    ChannelRow(
                        channel = channel,
                        isCurrent = current != null && channel.id == current.id,
                        onSelect = {
                            keyboard?.hide()
                            onSelect(channel)
                        },
                        testTag = "picker-row-$index",
                    )
                }
            }
        }
    }
}

/** Tek kanal satırı: numara, ad, grup; oynatılan kanalda kırmızı damga. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ChannelRow(
    channel: Channel,
    isCurrent: Boolean,
    onSelect: () -> Unit,
    testTag: String,
) {
    NovaTapSurface(
        onClick = onSelect,
        containerColor = if (isCurrent) NovaColors.Surface else NovaColors.Background,
        // İçerik rengi açıkça verilir: varsayılan `Unspecified` ışığın
        // `LocalContentColor`ına düşüyor ve kanal adı mürekkep renginde,
        // yani koyu zeminde okunmaz çiziliyordu (cihazda görüldü).
        contentColor = NovaColors.Text,
        focusedContainerColor = NovaColors.Focus,
        focusedContentColor = NovaColors.FocusInk,
        pressedContainerColor = NovaColors.Focus,
        pressedContentColor = NovaColors.FocusInk,
        border = BorderStroke(1.dp, NovaColors.Border.copy(alpha = 0.5f)),
        focusedBorder = BorderStroke(2.dp, NovaColors.Focus),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag(testTag),
    ) { state ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Kırmızı damga: 3 dp dikey çubuk, yalnız oynatılan kanalda görünür.
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(28.dp)
                    .background(
                        if (isCurrent) NovaColors.Accent else androidx.compose.ui.graphics.Color.Transparent,
                        RoundedCornerShape(50),
                    )
                    .testTag(if (isCurrent) "picker-current" else "picker-stamp"),
            )
            channel.number?.let { number ->
                Text(
                    text = number.toString(),
                    style = NovaType.Number.copy(color = NovaColors.TextDim),
                    modifier = Modifier.width(44.dp),
                )
            }
            Text(
                text = channel.name,
                style = NovaType.Body.copy(color = state.contentColor),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (isCurrent) {
                NovaBadge(text = "şimdi", color = NovaColors.Accent)
            } else if (channel.groupName.isNotBlank()) {
                Text(
                    text = channel.groupName,
                    style = NovaType.Small.copy(
                        color = if (state.focused) NovaColors.FocusInk else NovaColors.TextDim,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Kanal süzgeci: ad ya da numara eşleşir, büyük/küçük harf duyarsız.
 * Boş aramada liste olduğu gibi döner; sıra korunur.
 */
internal fun filterChannels(order: List<Channel>, query: String): List<Channel> {
    val needle = query.trim().takeIf { it.isNotEmpty() } ?: return order
    return order.filter { channel ->
        channel.name.contains(needle, ignoreCase = true) ||
            channel.number?.toString()?.contains(needle) == true
    }
}
