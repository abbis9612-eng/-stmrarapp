package app.sanad.coach.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import app.sanad.coach.ui.theme.Sanad
import app.sanad.coach.ui.theme.Type
import kotlin.math.abs
import kotlin.math.max

/**
 * الجزء المضيء من القمر عند طور [phase] (٠ محاق، ٠٫٥ تربيع، ١ بدر).
 * يضيء من اليمين (قمر متزايد): نصف دائرة يمين + منحنى الفاصل.
 */
fun moonLitPath(phase: Float, center: Offset, r: Float): Path? {
    val f = phase.coerceIn(0f, 1f)
    if (f < 0.02f) return null
    val outer = Rect(center, r)
    if (f > 0.985f) return Path().apply { addOval(outer) }
    val rx = max(0.5f, r * abs(1 - 2 * f))
    val inner = Rect(center.x - rx, center.y - r, center.x + rx, center.y + r)
    return Path().apply {
        arcTo(outer, -90f, 180f, forceMoveTo = true)
        // الرجوع من الأسفل للأعلى: هلال عبر اليمين، أحدب عبر اليسار
        arcTo(inner, 90f, if (f < 0.5f) -180f else 180f, forceMoveTo = false)
        close()
    }
}

/** يرسم القمر بستايل الحبر: قرص معتم، ضوء مزاح قليلاً، وخط حبر فوقهما. */
fun DrawScope.drawInkMoon(phase: Float, dark: Color, light: Color, ink: Color, strokePx: Float, shiftPx: Float) {
    val r = size.minDimension / 2 - strokePx - shiftPx
    val c = Offset(size.width / 2 - shiftPx / 2, size.height / 2 - shiftPx / 2)
    drawCircle(dark, r, c)
    val lit = moonLitPath(phase, c, r)
    if (lit != null) {
        translate(shiftPx, shiftPx) { drawPath(lit, light) }
        drawPath(lit, ink, style = Stroke(strokePx * 0.8f, join = StrokeJoin.Round))
    }
    drawCircle(ink, r, c, style = Stroke(strokePx))
}

/**
 * قمر سند: رمز التقدّم بدل الكرة. يكبر ليلة مع كل يوم تسجّل فيه ويصير بدراً بعد شهر.
 * [night] للسماء الزرقاء في صفحة التقدّم. [kick] أي تغيّر فيه يعطي نبضة فرح قصيرة.
 * [breathe] تنفّس بطيء (مؤقت الأكل)، [speaking] تمايل خفيف وهو يرد.
 */
@Composable
fun Moon(
    size: Dp,
    modifier: Modifier = Modifier,
    phase: Float = 0.3f,
    night: Boolean = false,
    kick: Int = 0,
    breathe: Boolean = false,
    speaking: Boolean = false,
    description: String? = null,
) {
    val c = Sanad.colors
    val shown by animateFloatAsState(phase, spring(dampingRatio = 0.8f, stiffness = 60f), label = "phase")
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(kick) {
        if (kick != 0) { pulse.snapTo(0.18f); pulse.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 300f)) }
    }
    val loop = rememberInfiniteTransition(label = "moon")
    val slow by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(4200), RepeatMode.Reverse), label = "breath")
    val bob by loop.animateFloat(-1f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "bob")
    val ink = if (night) Color(0xFFF7EBD0) else c.ink
    val dark = if (night) c.nightSky2 else c.moonDark
    Canvas(
        modifier
            .size(size)
            .graphicsLayer {
                val s = 1f + pulse.value + (if (breathe) 0.08f * slow else 0f)
                scaleX = s; scaleY = s
                rotationZ = if (speaking) bob * 4f else 0f
            }
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
    ) {
        val sw = (this.size.minDimension * 0.045f).coerceIn(1.5f, 5f)
        drawInkMoon(shown, dark, c.moonLight, ink, sw, this.size.minDimension * 0.035f)
    }
}

/** اسم سند بخط الرقعة: شعار بسيط يُكتب مثل خط اليد. */
@Composable
fun Wordmark(fontSize: TextUnit, modifier: Modifier = Modifier, color: Color = Sanad.colors.ink) {
    Text(
        "سند",
        style = Type.handTitle.copy(fontSize = fontSize, color = color, lineHeight = fontSize * 1.4f),
        modifier = modifier.semantics { contentDescription = "سند" },
    )
}
