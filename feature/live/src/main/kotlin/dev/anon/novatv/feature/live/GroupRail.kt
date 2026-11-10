package dev.anon.novatv.feature.live

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
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
import dev.anon.novatv.core.sync.GroupItem
import dev.anon.novatv.ui.component.NovaLabel
import dev.anon.novatv.ui.theme.NovaColors
import dev.anon.novatv.ui.theme.NovaType

/**
 * Sol grup rayı: "Tümü" en üstte, sonra alfabetik gruplar ve kanal sayıları.
 * Seçili grup kehribar bir çizgiyle işaretlenir; odak ayrı bir yüzey rengiyle.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun GroupRail(
    groups: List<GroupItem>,
    selectedGroup: String?,
    onSelect: (String?) -> Unit,
    firstItemFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = modifier
            .width(232.dp)
            .fillMaxHeight()
            .testTag("group-rail")
            .padding(start = 14.dp, end = 10.dp, top = 22.dp, bottom = 16.dp),
    ) {
        NovaLabel("Gruplar", Modifier.padding(start = 12.dp, bottom = 8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            itemsIndexed(groups, key = { _, item -> item.key }) { index, item ->
                val selected = (selectedGroup ?: LiveUiState.ALL_GROUPS_KEY) == item.key
                Surface(
                    onClick = { onSelect(item.key.ifBlank { null }) },
                    shape = ClickableSurfaceDefaults.shape(shape = shape),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color.Transparent,
                        contentColor = NovaColors.TextMuted,
                        focusedContainerColor = NovaColors.SurfaceHigh,
                        focusedContentColor = NovaColors.Text,
                        pressedContainerColor = NovaColors.SurfacePressed,
                        pressedContentColor = NovaColors.Text,
                    ),
                    border = ClickableSurfaceDefaults.border(
                        border = Border(BorderStroke(1.dp, Color.Transparent), shape = shape),
                        focusedBorder = Border(BorderStroke(2.dp, NovaColors.Focus), shape = shape),
                        pressedBorder = Border(BorderStroke(1.dp, NovaColors.Border), shape = shape),
                    ),
                    scale = ClickableSurfaceDefaults.scale(
                        scale = 1f,
                        focusedScale = 1f,
                        pressedScale = 0.99f,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier)
                        .testTag(if (item.key.isEmpty()) "group-all" else "group-${item.key}"),
                ) {
                    Row(
                        modifier = Modifier.padding(start = 10.dp, end = 12.dp, top = 9.dp, bottom = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(26.dp)
                                .background(
                                    color = if (selected) NovaColors.Accent else Color.Transparent,
                                    shape = CircleShape,
                                ),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.label,
                                fontSize = 17.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (selected) NovaColors.Text else NovaColors.TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${item.count} kanal",
                                style = NovaType.Number.copy(color = NovaColors.TextDim),
                            )
                        }
                    }
                }
            }
        }
    }
}
