package ru.anseranser.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ru.anseranser.android.ui.theme.GameLogBg
import ru.anseranser.android.ui.theme.TextWhite

@Composable
fun GameLogView(
    logLines: List<String>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(logLines.size) {
        if (logLines.isNotEmpty()) {
            listState.animateScrollToItem(logLines.lastIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .background(GameLogBg)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        items(logLines) { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.labelSmall,
                color = TextWhite.copy(alpha = 0.7f),
                modifier = Modifier.padding(vertical = 1.dp)
            )
        }
    }
}
