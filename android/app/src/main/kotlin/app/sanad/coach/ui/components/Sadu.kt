package app.sanad.coach.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.sanad.coach.ui.theme.Sanad
import app.sanad.core.WeaveCell
import app.sanad.core.ar

/** معيّن السدو: شكل واحد يتكرر في الشعار والخيط والزخارف. */
fun DrawScope.diamond(cx: Float, cy: Float, halfW: Float, halfH: Float, color: Color) {
    val p = Path().apply {
        moveTo(cx, cy - halfH); lineTo(cx + halfW, cy); lineTo(cx, cy + halfH); lineTo(cx - halfW, cy); close()
    }
    drawPath(p, color)
}

/**
 * الخيط: كل يوم ملتزم = صف منسوج بزخرفة السدو؛ يوم فائت = خيط رفيع؛ يومين = انقطاع.
 * الزمن من اليمين (الأقدم) لليسار (اليوم) كما نقرأ.
 */
@Composable
fun SaduWeave(cells: List<WeaveCell>, modifier: Modifier = Modifier, animateLast: Boolean = false) {
    val c = Sanad.colors
    val reveal = remember { Animatable(if (animateLast) 0f else 1f) }
    LaunchedEffect(animateLast) {
        if (animateLast) { reveal.snapTo(0f); reveal.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    }
    val woven = cells.count { it == WeaveCell.WOVEN }
    val palette = listOf(c.sadu2, c.date, c.wool)
    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(cells.size * 13f / 56f)
            .semantics { contentDescription = "نسيج آخر ${ar(cells.size)} يوم: ${ar(woven)} يوم ملتزم" },
    ) {
        val w = size.width / cells.size
        val h = size.height
        cells.forEachIndexed { i, cell ->
            // RTL: الأحدث يسار
            val x0 = size.width - (i + 1) * w
            val cx = x0 + w / 2
            val isLast = i == cells.lastIndex
            when (cell) {
                WeaveCell.BEFORE -> drawLine(Color.White.copy(alpha = 0.12f), Offset(x0, h / 2), Offset(x0 + w, h / 2), 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 6f)))
                WeaveCell.BROKEN -> drawCircle(Color.White.copy(alpha = 0.25f), 1.4.dp.toPx(), Offset(cx, h / 2))
                WeaveCell.HELD, WeaveCell.TODAY -> {
                    drawLine(c.date, Offset(x0, h / 2), Offset(x0 + w, h / 2), 1.6.dp.toPx())
                    if (cell == WeaveCell.TODAY) drawRoundRect(
                        Color.White.copy(alpha = 0.55f), Offset(x0 + 1, h * 0.12f), Size(w - 2, h * 0.76f),
                        style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))),
                    )
                }
                WeaveCell.WOVEN -> {
                    val t = if (animateLast && isLast) reveal.value else 1f
                    withTransform({ scale(1f, t, Offset(cx, h / 2)) }) {
                        drawRect(c.sadu, Offset(x0, h * 0.07f), Size(w + 0.6f, h * 0.11f))
                        drawRect(c.sadu, Offset(x0, h * 0.82f), Size(w + 0.6f, h * 0.11f))
                        drawRect(c.wool.copy(alpha = 0.85f), Offset(x0, h * 0.21f), Size(w + 0.6f, h * 0.035f))
                        drawRect(c.wool.copy(alpha = 0.85f), Offset(x0, h * 0.755f), Size(w + 0.6f, h * 0.035f))
                        diamond(cx, h / 2, w / 2 - 0.5f, h * 0.2f, palette[i % 3])
                        diamond(cx, h / 2, w * 0.2f, h * 0.1f, c.night)
                    }
                }
            }
        }
    }
}

/** شريط زخرفي رفيع بمعيّنات السدو — للرؤوس والبطاقات الفاخرة. */
@Composable
fun SaduBand(modifier: Modifier = Modifier, height: Dp = 10.dp, onDark: Boolean = true) {
    val c = Sanad.colors
    Canvas(modifier.fillMaxWidth().height(height)) {
        val h = size.height
        drawRect(c.sadu, size = Size(size.width, h * 0.28f))
        drawRect(c.sadu, Offset(0f, h * 0.72f), Size(size.width, h * 0.28f))
        val step = h * 1.6f
        var x = step / 2
        var k = 0
        clipRect {
            while (x < size.width + step) {
                diamond(x, h / 2, h * 0.42f, h * 0.36f, if (k % 2 == 0) c.date else if (onDark) c.wool else c.night)
                x += step; k++
            }
        }
    }
}

/**
 * شعار سند: قوس (بيت/سند) تنسجه خيوط السدو. [progress] من ٠ إلى ١ يرسمه تدريجياً.
 */
@Composable
fun SaduLogo(modifier: Modifier = Modifier, size: Dp = 96.dp, progress: Float = 1f, onDark: Boolean = true) {
    val c = Sanad.colors
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val u = s / 512f
        fun p(v: Float) = v * u
        val arch = Path().apply {
            moveTo(p(96f), p(360f)); lineTo(p(96f), p(250f))
            cubicTo(p(96f), p(162f), p(168f), p(90f), p(256f), p(90f))
            cubicTo(p(344f), p(90f), p(416f), p(162f), p(416f), p(250f))
            lineTo(p(416f), p(360f)); close()
        }
        val a = progress.coerceIn(0f, 1f)
        drawPath(arch, (if (onDark) c.night2 else c.night).copy(alpha = (a * 2).coerceAtMost(1f)))
        // الخيوط تنسدل من المنتصف للأطراف
        val bandT = ((a - 0.2f) / 0.4f).coerceIn(0f, 1f)
        val half = p(160f) * bandT
        drawRect(c.sadu, Offset(p(256f) - half, p(148f)), Size(half * 2, p(26f)))
        drawRect(c.sadu, Offset(p(256f) - half, p(338f)), Size(half * 2, p(26f)))
        val dT = ((a - 0.5f) / 0.5f).coerceIn(0f, 1f)
        diamond(p(256f), p(256f), p(62f) * dT, p(66f) * dT, c.date)
        diamond(p(256f), p(256f), p(22f) * dT, p(34f) * dT, c.night)
        val sT = ((a - 0.7f) / 0.3f).coerceIn(0f, 1f)
        diamond(p(184f), p(256f), p(24f) * sT, p(26f) * sT, c.wool)
        diamond(p(328f), p(256f), p(24f) * sT, p(26f) * sT, c.wool)
    }
}
