package app.sanad.coach.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import app.sanad.coach.ui.BottomBarSpace
import kotlinx.coroutines.delay

/** قالب صفحة موحّد: هوامش ١٨، مسافات ١٦، ومساحة فوق شريط التنقل العائم. */
@Composable
fun Page(state: LazyListState = rememberLazyListState(), bottom: Boolean = true, content: LazyListScope.() -> Unit) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = top + 18.dp, bottom = if (bottom) BottomBarSpace else 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

/** يتذكّر العناصر اللي ظهرت حتى تطلع بحركة مرة وحدة بس لكل زيارة للشاشة. */
class RiseState { internal val seen = HashSet<Int>() }

@Composable
fun rememberRise() = remember { RiseState() }

private val Out = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

/** دخول ناعم: يطلع العنصر من تحت بتأخير متدرّج. */
@Composable
fun Modifier.rise(state: RiseState, index: Int): Modifier {
    val first = remember { index !in state.seen }
    val a = remember { Animatable(if (first) 0f else 1f) }
    LaunchedEffect(Unit) {
        state.seen += index
        if (a.value < 1f) {
            delay(80L + index * 70L)
            a.animateTo(1f, tween(800, easing = Out))
        }
    }
    return this.graphicsLayer {
        alpha = a.value
        translationY = (1f - a.value) * 18.dp.toPx()
        val s = 0.98f + 0.02f * a.value
        scaleX = s; scaleY = s
    }
}
