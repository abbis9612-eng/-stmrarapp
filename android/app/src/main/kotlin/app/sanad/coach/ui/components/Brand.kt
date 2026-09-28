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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import app.sanad.coach.ui.theme.Sanad
import app.sanad.coach.ui.theme.Type

private val MARK_INK = Color(0xFF15231C)

/**
 * يرسم علامة «تحرّك» داخل مربع طوله [side] يبدأ من [origin]: مربع ليموني وسهمان وأمام كل سهم أصابع قدم.
 * [backT]/[frontT] من 0 إلى 1 لدخول كل سهم، و[backDx]/[frontDx] إزاحة أفقية بوحدات المربع (من 180).
 */
private fun DrawScope.drawMark(
    origin: Offset, side: Float, lime: Color, olive: Color, tile: Boolean,
    backT: Float = 1f, frontT: Float = 1f, backDx: Float = 0f, frontDx: Float = 0f,
) {
    val u = side / 180f
    if (tile) drawRoundRect(lime, origin, Size(side, side), CornerRadius(41f * u))
    fun chevron(x: Float, col: Color, t: Float, shift: Float) {
        if (t <= 0f) return
        val a = t.coerceIn(0f, 1f)
        val s = 34f * u
        val cx = origin.x + (x + shift + (1f - a) * 40f) * u
        val cy = origin.y + 90f * u
        val p = Path().apply { moveTo(cx + s, cy - s); lineTo(cx, cy); lineTo(cx + s, cy + s) }
        drawPath(p, col.copy(alpha = a), style = Stroke(22f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
        listOf(Triple(-22f, -15f, 6f), Triple(-27f, 0f, 7.5f), Triple(-22f, 15f, 6f)).forEach { (ox, oy, r) ->
            drawCircle(col.copy(alpha = a), r * u, Offset(cx + ox * u, cy + oy * u))
        }
    }
    // الخلفي زيتوني والأمامي حبر؛ ألوان ثابتة لأن المربع ليموني بكل النماذج
    chevron(105f, olive, backT, backDx)
    chevron(65f, MARK_INK, frontT, frontDx)
}

/**
 * علامة «تحرّك»: سهمان للأمام (من شدّة الاسم) وأمام كل سهم أصابع قدم، على مربع ليموني.
 * [progress] من 0 إلى 1 يُدخل السهمين من اليمين لليسار (للأمام في العربي).
 */
@Composable
fun BrandMark(size: Dp, modifier: Modifier = Modifier, progress: Float = 1f, tile: Boolean = true) {
    val c = Sanad.colors
    Canvas(modifier.size(size).semantics { contentDescription = "شعار تحرّك" }) {
        drawMark(
            Offset.Zero, this.size.minDimension, c.brand, c.brandOlive, tile,
            backT = (progress / 0.6f).coerceIn(0f, 1f),
            frontT = ((progress - 0.3f) / 0.7f).coerceIn(0f, 1f),
        )
    }
}

/**
 * رمز التقدّم: أيقونة «تحرّك» داخل حلقة تمتلئ مع كل يوم تسجّل فيه، وتكتمل بعد 30 يوماً.
 * [progress] من 0 إلى 1. [dark] لبطاقة غامقة. [ring] يطفئ الحلقة إذا الشاشة عندها حلقتها الخاصة.
 * [kick] أي تغيّر فيه يعطي نبضة فرح. [breathe] تنفّس بطيء. [speaking] السهمان يتناوبان كأنهما يمشيان.
 */
@Composable
fun MoveMark(
    size: Dp,
    modifier: Modifier = Modifier,
    progress: Float = 0f,
    dark: Boolean = false,
    kick: Int = 0,
    breathe: Boolean = false,
    speaking: Boolean = false,
    ring: Boolean = true,
    description: String? = null,
) {
    val c = Sanad.colors
    val shown by animateFloatAsState(progress.coerceIn(0f, 1f), spring(dampingRatio = 0.8f, stiffness = 60f), label = "progress")
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(kick) {
        if (kick != 0) { pulse.snapTo(0.18f); pulse.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 300f)) }
    }
    val loop = rememberInfiniteTransition(label = "mark")
    val slow by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(4200), RepeatMode.Reverse), label = "breath")
    val step by loop.animateFloat(-1f, 1f, infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "step")
    val track = if (dark || c.dark) Color.White.copy(alpha = 0.14f) else c.line
    val fill = if (dark || c.dark) c.brand else c.oasis
    Canvas(
        modifier
            .size(size)
            .graphicsLayer {
                val s = 1f + pulse.value + (if (breathe) 0.07f * slow else 0f)
                scaleX = s; scaleY = s
            }
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
    ) {
        val d = this.size.minDimension
        var inset = 0f
        if (ring) {
            val sw = (d * 0.075f).coerceAtLeast(2.5f)
            val tl = Offset(sw / 2, sw / 2)
            val sz = Size(d - sw, d - sw)
            drawArc(track, 0f, 360f, false, tl, sz, style = Stroke(sw))
            if (shown > 0.005f) drawArc(fill, -90f, (360f * shown).coerceAtLeast(8f), false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
            inset = sw * 2.1f
        }
        val side = d - inset * 2
        val walk = if (speaking) step * 7f else 0f
        drawMark(Offset(inset, inset), side, c.brand, c.brandOlive, tile = true, backDx = walk, frontDx = -walk)
    }
}

/** اسم «تحرّك» بخط عريض: الشعار المكتوب. */
@Composable
fun Wordmark(fontSize: TextUnit, modifier: Modifier = Modifier, color: Color = Sanad.colors.ink) {
    Text(
        "تحرّك",
        style = Type.h1.copy(fontSize = fontSize, color = color, lineHeight = fontSize * 1.3f),
        modifier = modifier.semantics { contentDescription = "تحرّك" },
    )
}
