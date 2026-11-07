package dev.anon.novatv.feature.live

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.anon.novatv.core.sync.GroupItem

/** Sol grup rayı: "Tümü" en üstte, sonra alfabetik gruplar ve kanal sayıları. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun GroupRail(
    groups: List<GroupItem>,
    selectedGroup: String?,
    onSelect: (String?) -> Unit,
    firstItemFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .width(280.dp)
            .fillMaxHeight()
            .testTag("group-rail")
            .padding(vertical = 8.dp),
    ) {
        itemsIndexed(groups, key = { _, item -> item.key }) { index, item ->
            val selected = (selectedGroup ?: LiveUiState.ALL_GROUPS_KEY) == item.key
            Surface(
                onClick = { onSelect(item.key.ifBlank { null }) },
                selected = selected,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .then(if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier)
                    .testTag(if (item.key.isEmpty()) "group-all" else "group-${item.key}"),
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(
                        text = item.label,
                        fontSize = 18.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${item.count} kanal",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
