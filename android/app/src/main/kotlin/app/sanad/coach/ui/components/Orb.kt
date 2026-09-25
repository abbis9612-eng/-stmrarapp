package app.sanad.coach.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import app.sanad.coach.ui.theme.Sanad
import app.sanad.coach.ui.theme.Type
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** ساعة مشتركة بالثواني تتقدّم مع كل إطار. */
@Composable
fun rememberClock(): State<Float> = produceState(0f) {
    val start = withInfiniteAnimationFrameMillis { it }
    while (true) withInfiniteAnimationFrameMillis { value = (it - start) / 1000f }
}

/**
 * الكرة الحيّة: حضور المدرب. تتنفس دائماً، تنبض وهي "تتكلم"، وتتلوّن بطاقة اليوم.
 * [kick] أي تغيّر فيه يعطي نبضة فرح قصيرة. تُرسم الهالة خارج حدودها عمداً.
 */
@Composable
fun LivingOrb(
    size: Dp,
    modifier: Modifier = Modifier,
    speaking: Boolean = false,
    breathe: Boolean = false,
    glow: Boolean = true,
    kick: Int = 0,
    description: String? = null,
) {
    val mood = Sanad.mood
    val time by rememberClock()
    val speak by animateFloatAsState(if (speaking) 1f else 0f, tween(500), label = "speak")
    val breath by animateFloatAsState(if (breathe) 1f else 0f, tween(800), label = "breath")
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(kick) {
        if (kick != 0) { pulse.snapTo(0.6f); pulse.animateTo(0f, tween(900)) }
    }
    val seed = remember { (Math.random() * 10).toFloat() }
    val cols = listOf(mood.a, mood.b, mood.c)
    Canvas(modifier.size(size).then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier)) {
        val t = time + seed
        val base = this.size.minDimension / 2
        val r = base * (1 + sin(t * 1.6f) * 0.025f + speak * (0.05f + sin(t * 13f) * 0.05f) + pulse.value * 0.25f + breath * sin(time * 1.25f) * 0.12f)
        val c = center
        if (glow) {
            drawCircle(
                Brush.radialGradient(listOf(mood.a.copy(alpha = 0.42f + speak * 0.2f), mood.a.copy(alpha = 0f)), center = c, radius = r * 1.85f),
                radius = r * 1.85f, center = c,
            )
        }
        val clip = Path().apply { addOval(Rect(c, r)) }
        clipPath(clip) {
            drawCircle(Color(0xFF0D1320), r, c)
            val sp = 1 + speak * 2.5f
            for (i in 0 until 4) {
                val col = cols[i % 3]
                val bx = c.x + sin(t * (0.7f + i * 0.23f) * sp + i * 2.1f) * r * 0.5f
                val by = c.y + cos(t * (0.55f + i * 0.31f) * sp + i * 1.3f) * r * 0.5f
                val rr = r * (0.8f + 0.18f * sin(t * 0.9f + i))
                drawCircle(
                    Brush.radialGradient(listOf(col.copy(alpha = 0.9f), col.copy(alpha = 0f)), center = Offset(bx, by), radius = rr),
                    radius = rr, center = Offset(bx, by), blendMode = BlendMode.Plus,
                )
            }
            val hl = Offset(c.x - r * 0.35f, c.y - r * 0.45f)
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent), center = hl, radius = r * 0.75f), radius = r * 0.75f, center = hl)
            drawCircle(Brush.radialGradient(0.7f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.25f), center = c, radius = r), radius = r, center = c)
        }
        drawCircle(Color.White.copy(alpha = 0.18f), r - 0.5f, c, style = Stroke(1.dp.toPx()))
    }
}

/**
 * شعار سند: كلمة «سند» بالكوفي، ونقطة النون هي الكرة الحيّة.
 * نكتب النون بدون نقطة (ٮ) ونضع الكرة مكانها بقياس الحرف الفعلي.
 * [reveal] من ٠ إلى ١ يكتب الكلمة من اليمين لليسار.
 */
@Composable
fun Wordmark(
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = Sanad.colors.ink,
    reveal: Float = 1f,
    showDot: Boolean = true,
    onDot: (center: Offset, sizePx: Float) -> Unit = { _, _ -> },
) {
    val density = LocalDensity.current
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val dotPx = with(density) { fontSize.toPx() } * 0.17f
    Box(modifier.semantics { contentDescription = "سند" }) {
        Text(
            "سٮد",
            style = Type.hero.copy(fontSize = fontSize, color = color, lineHeight = fontSize * 1.2f),
            onTextLayout = { layout = it },
            modifier = Modifier.drawWithContent {
                clipRect(left = size.width * (1f - reveal.coerceIn(0f, 1f))) { this@drawWithContent.drawContent() }
            },
        )
        val l = layout
        if (l != null) {
            val box = l.getBoundingBox(1)
            val center = Offset(box.center.x, box.top + box.height * 0.3f)
            LaunchedEffect(center, dotPx) { onDot(center, dotPx) }
            if (showDot) {
                val d = with(density) { dotPx.toDp() }
                LivingOrb(
                    d,
                    Modifier.align(AbsoluteAlignment.TopLeft).absoluteOffset { IntOffset((center.x - dotPx / 2).roundToInt(), (center.y - dotPx / 2).roundToInt()) },
                )
            }
        }
    }
}
