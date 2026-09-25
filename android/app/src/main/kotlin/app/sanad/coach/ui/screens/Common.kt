package app.sanad.coach.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.sanad.coach.ui.BottomBarSpace

/** قالب صفحة موحّد: هوامش ١٦، مسافات ١٤، ومساحة فوق شريط التنقل. */
@Composable
fun Page(state: LazyListState = rememberLazyListState(), bottom: Boolean = true, content: LazyListScope.() -> Unit) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = top + 14.dp, bottom = if (bottom) BottomBarSpace else 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}
