package dev.anon.novatv.feature.live

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.SubcomposeAsyncImage
import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.ui.component.NovaRule
import dev.anon.novatv.ui.theme.NovaColors
import dev.anon.novatv.ui.theme.NovaType

/** Tek kanal satırı: logo, numara, ad ve favori işareti. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelRow(
    channel: Channel,
    favorite: Boolean,
    index: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFocused: () -> Unit,
    autoFocus: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(autoFocus) {
        if (autoFocus) runCatching { focusRequester.requestFocus() }
    }
    val shape = RoundedCornerShape(8.dp)
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = NovaColors.Text,
            focusedContainerColor = NovaColors.SurfaceHigh,
            focusedContentColor = NovaColors.Text,
            pressedContainerColor = NovaColors.SurfacePressed,
            pressedContentColor = NovaColors.Text,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, Color.Transparent), shape = shape),
            // Odaklı satır kehribar çerçeveyle işaretlenir; 3 metreden
            // bakınca hangi satırda olduğunuz bir bakışta belli olur.
            focusedBorder = Border(BorderStroke(2.dp, NovaColors.Accent), shape = shape),
            pressedBorder = Border(BorderStroke(2.dp, NovaColors.Accent), shape = shape),
        ),
        scale = ClickableSurfaceDefaults.scale(
            scale = 1f,
            focusedScale = 1f,
            pressedScale = 0.995f,
        ),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { state -> if (state.isFocused) onFocused() }
            .testTag("channel-$index"),
    ) {
        Column {
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 16.dp, top = 9.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChannelLogo(url = channel.logoUrl, name = channel.name, modifier = Modifier.size(44.dp))
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = channel.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = NovaColors.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        channel.number?.let { number ->
                            Text(
                                text = "#$number",
                                style = NovaType.Number.copy(color = NovaColors.Accent.copy(alpha = 0.8f)),
                            )
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .background(NovaColors.TextDim, RoundedCornerShape(50)),
                            )
                        }
                        if (channel.groupName.isNotBlank()) {
                            Text(
                                text = channel.groupName,
                                fontSize = 13.sp,
                                color = NovaColors.TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                if (favorite) {
                    Text(
                        text = "★",
                        fontSize = 16.sp,
                        color = NovaColors.Accent,
                    )
                }
            }
            NovaRule(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 70.dp, end = 16.dp),
                color = NovaColors.Border.copy(alpha = 0.45f),
            )
        }
    }
}

/** Izgara kartı: üstte logo ve numara, altta kanal adı. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelCard(
    channel: Channel,
    favorite: Boolean,
    index: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFocused: () -> Unit,
    autoFocus: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(autoFocus) {
        if (autoFocus) runCatching { focusRequester.requestFocus() }
    }
    val shape = RoundedCornerShape(10.dp)
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = NovaColors.Surface,
            contentColor = NovaColors.Text,
            focusedContainerColor = NovaColors.SurfaceHigh,
            focusedContentColor = NovaColors.Text,
            pressedContainerColor = NovaColors.SurfacePressed,
            pressedContentColor = NovaColors.Text,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, NovaColors.Border.copy(alpha = 0.8f)), shape = shape),
            focusedBorder = Border(BorderStroke(2.dp, NovaColors.Accent), shape = shape),
            pressedBorder = Border(BorderStroke(2.dp, NovaColors.Accent), shape = shape),
        ),
        scale = ClickableSurfaceDefaults.scale(
            scale = 1f,
            focusedScale = 1.02f,
            pressedScale = 0.99f,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(124.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { state -> if (state.isFocused) onFocused() }
            .testTag("channel-$index"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChannelLogo(url = channel.logoUrl, name = channel.name, modifier = Modifier.size(40.dp))
                Spacer(Modifier.weight(1f))
                if (favorite) {
                    Text(text = "★", fontSize = 15.sp, color = NovaColors.Accent)
                    Spacer(Modifier.width(8.dp))
                }
                channel.number?.let { number ->
                    Text(
                        text = "#$number",
                        style = NovaType.Number.copy(color = NovaColors.TextDim),
                    )
                }
            }
            Text(
                text = channel.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 21.sp,
                color = NovaColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (channel.groupName.isNotBlank()) {
                Text(
                    text = channel.groupName,
                    fontSize = 12.sp,
                    color = NovaColors.TextDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun ChannelLogo(url: String?, name: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    val base = modifier
        .clip(shape)
        .background(NovaColors.SurfaceHigh)
        .border(1.dp, NovaColors.Border.copy(alpha = 0.8f), shape)
    if (url.isNullOrBlank()) {
        Box(modifier = base, contentAlignment = Alignment.Center) {
            LogoLetter(name)
        }
    } else {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = null,
            modifier = base,
            contentScale = ContentScale.Fit,
            loading = { LogoLetter(name) },
            error = { LogoLetter(name) },
        )
    }
}

@Composable
private fun LogoLetter(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = name.firstOrNull()?.uppercase() ?: "?",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = NovaColors.TextMuted,
        )
    }
}
