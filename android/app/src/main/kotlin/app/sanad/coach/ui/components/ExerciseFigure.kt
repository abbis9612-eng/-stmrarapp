package app.sanad.coach.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.sanad.coach.ui.theme.Sanad
import app.sanad.coach.ui.theme.SanadColors
import app.sanad.core.Exercise
import app.sanad.core.Joint
import app.sanad.core.Muscle
import app.sanad.core.Pose
import app.sanad.core.Prop
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.PI

private fun ease(t: Float) = (1 - cos(t * PI.toFloat())) / 2

/** الوضعية عند لحظة [phase] (0..عدد الإطارات) مع انتقال ناعم بين الإطارات. */
fun poseAt(frames: List<Pose>, phase: Float): Pose {
    val i = floor(phase).toInt() % frames.size
    val t = ease(phase - floor(phase))
    return frames[i].lerp(frames[(i + 1) % frames.size], t)
}

/**
 * شخصية التمرين بستايل الحبر: أطراف بخط حبر وحشوة بيضاء، والعضلات الشغالة بالكهرماني.
 * الأطراف البعيدة رمادية فاتحة لتعطي عمقاً. تُرسم معها الأداة (كرسي/جدار/حصيرة).
 */
@Composable
fun ExerciseFigure(exercise: Exercise, modifier: Modifier = Modifier, playing: Boolean = true, onDark: Boolean = false) {
    val c = Sanad.colors
    val n = exercise.frames.size
    val transition = rememberInfiniteTransition(label = "figure-${exercise.id}")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = n.toFloat(),
        animationSpec = infiniteRepeatable(tween(exercise.tempoMs * n, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val pose = if (playing) poseAt(exercise.frames, phase) else exercise.frames.last()
    Canvas(modifier.semantics { contentDescription = "رسم متحرك: ${exercise.name}" }) {
        drawFigure(pose, exercise.prop, c, exercise.muscles)
    }
}

fun DrawScope.drawFigure(f: Pose, prop: Prop, c: SanadColors, muscles: List<Muscle> = emptyList()) {
    val s = size.minDimension / 100f
    val ox = (size.width - 100 * s) / 2
    val oy = (size.height - 100 * s) / 2
    fun pt(j: Joint) = Offset(ox + f.x(j) * s, oy + f.y(j) * s)
    fun o(x: Float, y: Float) = Offset(ox + x * s, oy + y * s)
    val ink = c.ink
    val propInk = c.faint
    val legsWork = muscles.any { it == Muscle.QUADS || it == Muscle.GLUTES || it == Muscle.HAMSTRINGS || it == Muscle.CALVES }
    val armsWork = muscles.any { it == Muscle.ARMS || it == Muscle.CHEST || it == Muscle.SHOULDERS || it == Muscle.BACK }
    val coreWork = Muscle.CORE in muscles
    val nearLeg = if (legsWork) c.amber else Color.White
    val nearArm = if (armsWork) c.amber else Color.White
    val farFill = c.surface2

    // خط الأرض
    val hipX = f.x(Joint.HIP)
    drawLine(c.line, o(hipX - 30, 95f), o(hipX + 30, 95f), 1.4f * s, StrokeCap.Round)

    when (prop) {
        Prop.CHAIR -> {
            drawLine(propInk, o(25f, 64.5f), o(49f, 64.5f), 2.2f * s, StrokeCap.Round)
            drawLine(propInk, o(27f, 65f), o(27f, 94f), 2.2f * s, StrokeCap.Round)
            drawLine(propInk, o(47f, 65f), o(47f, 94f), 2.2f * s, StrokeCap.Round)
            drawLine(propInk, o(26f, 64f), o(23f, 37f), 2.2f * s, StrokeCap.Round)
        }
        Prop.WALL -> drawRoundRect(c.surface2, o(86f, 5f), Size(6 * s, 89.6f * s), CornerRadius(2f * s))
        Prop.MAT -> drawRoundRect(c.amberTint, o(6f, 92.4f), Size(88 * s, 2.6f * s), CornerRadius(1.3f * s))
        Prop.NONE -> Unit
    }

    // طرف بخط حبر: خط غامق عريض ثم حشوة أرفع فوقه
    fun seg(a: Offset, b: Offset, w: Float, fill: Color) {
        drawLine(ink, a, b, (w + 2.2f) * s, StrokeCap.Round)
        drawLine(fill, a, b, w * s, StrokeCap.Round)
    }
    fun limb(a: Joint, b: Joint, w: Float, fill: Color) = seg(pt(a), pt(b), w, fill)
    fun shoe(knee: Joint, foot: Joint, w: Float, fill: Color) {
        val k = pt(knee); val ft = pt(foot)
        val dx = ft.x - k.x; val dy = ft.y - k.y
        val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
        // القدم عمودية على الساق وتتجه للأمام (+x)
        var px = -dy / len; var py = dx / len
        if (px < 0) { px = -px; py = -py }
        seg(ft, Offset(ft.x + px * 6f * s, ft.y + py * 6f * s), w, fill)
    }

    // الأطراف البعيدة خلف الجسم
    limb(Joint.NECK, Joint.ELBOW_F, 4f, farFill); limb(Joint.ELBOW_F, Joint.HAND_F, 3.4f, farFill)
    limb(Joint.HIP, Joint.KNEE_F, 5.6f, farFill); limb(Joint.KNEE_F, Joint.FOOT_F, 5f, farFill)
    shoe(Joint.KNEE_F, Joint.FOOT_F, 3.4f, farFill)

    // الجذع
    val torso = Path().apply {
        moveTo(pt(Joint.NECK).x, pt(Joint.NECK).y)
        quadraticTo(pt(Joint.MID).x, pt(Joint.MID).y, pt(Joint.HIP).x, pt(Joint.HIP).y)
    }
    drawPath(torso, ink, style = Stroke(11.6f * s, cap = StrokeCap.Round))
    drawPath(torso, if (coreWork) c.amberTint else Color.White, style = Stroke(9.4f * s, cap = StrokeCap.Round))

    // الأطراف القريبة
    limb(Joint.HIP, Joint.KNEE_N, 6f, nearLeg); limb(Joint.KNEE_N, Joint.FOOT_N, 5.4f, nearLeg)
    shoe(Joint.KNEE_N, Joint.FOOT_N, 3.8f, Color.White)
    limb(Joint.NECK, Joint.ELBOW_N, 4.4f, nearArm); limb(Joint.ELBOW_N, Joint.HAND_N, 3.8f, nearArm)

    // الرأس
    val head = pt(Joint.HEAD)
    drawCircle(Color.White, 6f * s, head)
    drawCircle(ink, 6f * s, head, style = Stroke(1.6f * s))
}

/** خريطة الجسم من قدّام وورا: العضلة الأساسية كهرمانية، والمساعدة فاتحة. */
@Composable
fun BodyMap(primary: List<Muscle>, secondary: List<Muscle>, modifier: Modifier = Modifier) {
    val c = Sanad.colors
    Canvas(modifier.semantics { contentDescription = "العضلات: " + (primary + secondary).joinToString("، ") { it.label } }) {
        val w = size.width / 2
        drawBody(front = true, left = 0f, width = w, primary = primary, secondary = secondary, c = c)
        drawBody(front = false, left = w, width = w, primary = primary, secondary = secondary, c = c)
    }
}

private fun DrawScope.drawBody(front: Boolean, left: Float, width: Float, primary: List<Muscle>, secondary: List<Muscle>, c: SanadColors) {
    val s = minOf(width / 120f, size.height / 280f)
    val ox = left + (width - 120 * s) / 2
    val oy = (size.height - 280 * s) / 2
    fun o(x: Float, y: Float) = Offset(ox + x * s, oy + y * s)
    val ink = c.ink
    fun limb(x1: Float, y1: Float, x2: Float, y2: Float, w: Float) {
        drawLine(ink, o(x1, y1), o(x2, y2), (w + 2.6f) * s, StrokeCap.Round)
    }
    fun limbFill(x1: Float, y1: Float, x2: Float, y2: Float, w: Float) {
        drawLine(Color.White, o(x1, y1), o(x2, y2), w * s, StrokeCap.Round)
    }
    val limbs = listOf(
        floatArrayOf(35f, 55f, 28f, 98f, 12.4f), floatArrayOf(28f, 98f, 25f, 136f, 10.4f),
        floatArrayOf(85f, 55f, 92f, 98f, 12.4f), floatArrayOf(92f, 98f, 95f, 136f, 10.4f),
        floatArrayOf(50f, 142f, 47f, 202f, 21.4f), floatArrayOf(47f, 202f, 46f, 256f, 14.4f),
        floatArrayOf(70f, 142f, 73f, 202f, 21.4f), floatArrayOf(73f, 202f, 74f, 256f, 14.4f),
    )
    limbs.forEach { limb(it[0], it[1], it[2], it[3], it[4]) }
    val torso = Path().apply {
        moveTo(ox + 36 * s, oy + 50 * s)
        quadraticTo(ox + 60 * s, oy + 42 * s, ox + 84 * s, oy + 50 * s)
        quadraticTo(ox + 88 * s, oy + 58 * s, ox + 86 * s, oy + 70 * s)
        quadraticTo(ox + 82 * s, oy + 96 * s, ox + 78 * s, oy + 118 * s)
        lineTo(ox + 80 * s, oy + 140 * s)
        quadraticTo(ox + 60 * s, oy + 150 * s, ox + 40 * s, oy + 140 * s)
        lineTo(ox + 42 * s, oy + 118 * s)
        quadraticTo(ox + 38 * s, oy + 96 * s, ox + 34 * s, oy + 70 * s)
        quadraticTo(ox + 32 * s, oy + 58 * s, ox + 36 * s, oy + 50 * s)
        close()
    }
    drawPath(torso, Color.White)
    drawPath(torso, ink, style = Stroke(1.6f * s))
    limbs.forEach { limbFill(it[0], it[1], it[2], it[3], it[4]) }
    drawCircle(Color.White, 13 * s, o(60f, 23f)); drawCircle(ink, 13 * s, o(60f, 23f), style = Stroke(1.6f * s))

    // مناطق العضلات: (العضلة، مركز، نصف قطر) — مرآة لليمين واليسار
    data class Zone(val m: Muscle, val cx: Float, val cy: Float, val rx: Float, val ry: Float)
    val zones = if (front) listOf(
        Zone(Muscle.SHOULDERS, 38f, 56f, 6.5f, 6.5f), Zone(Muscle.CHEST, 51f, 64f, 9f, 6.5f),
        Zone(Muscle.ARMS, 30.5f, 80f, 4.2f, 10f), Zone(Muscle.CORE, 60f, 93f, 7f, 19f),
        Zone(Muscle.QUADS, 49f, 170f, 8.5f, 24f), Zone(Muscle.CALVES, 46.5f, 228f, 4.2f, 13f),
    ) else listOf(
        Zone(Muscle.BACK, 48f, 84f, 9f, 16f), Zone(Muscle.SHOULDERS, 38f, 56f, 6.5f, 6.5f),
        Zone(Muscle.ARMS, 30.5f, 80f, 4.2f, 10f), Zone(Muscle.GLUTES, 51.5f, 134f, 9.5f, 9f),
        Zone(Muscle.HAMSTRINGS, 49f, 176f, 8f, 21f), Zone(Muscle.CALVES, 46.5f, 226f, 5.5f, 14f),
    )
    val shift = 1.6f * s
    zones.forEach { z ->
        val fill = when (z.m) {
            in primary -> c.amber
            in secondary -> c.amberTint
            else -> null
        }
        val mirrored = z.cx != 60f
        listOfNotNull(z.cx, if (mirrored) 120f - z.cx else null).forEach { cx ->
            val tl = o(cx - z.rx, z.cy - z.ry)
            val sz = Size(z.rx * 2 * s, z.ry * 2 * s)
            if (fill != null) drawOval(fill, tl + Offset(shift, shift), sz)
            drawOval(if (fill != null) ink else c.line, tl, sz, style = Stroke(if (fill != null) 1.2f * s else 0.9f * s))
        }
    }
}
