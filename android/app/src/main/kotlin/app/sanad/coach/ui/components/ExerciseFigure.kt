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
import app.sanad.core.Pose
import app.sanad.core.Prop
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.PI

private fun ease(t: Float) = (1 - cos(t * PI.toFloat())) / 2

/** الوضعية عند لحظة [phase] (٠..عدد الإطارات) مع انتقال ناعم بين الإطارات. */
fun poseAt(frames: List<Pose>, phase: Float): Pose {
    val i = floor(phase).toInt() % frames.size
    val t = ease(phase - floor(phase))
    return frames[i].lerp(frames[(i + 1) % frames.size], t)
}

/**
 * شخصية سند المتحركة لتمرين: جسم بخطوط مستديرة سميكة، الأطراف البعيدة
 * أفتح (عمق)، الجذع بلون السدو، والرأس ذهبي. تُرسم معها الأداة (كرسي/جدار/حصيرة).
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
        drawFigure(pose, exercise.prop, c, onDark)
    }
}

private val FAR_ARM = Color(0xFF178A74)
private val FAR_SKIN = Color(0xFFA8744F)
private val FAR_LEG = Color(0xFF1C2433)
private val FAR_SHOE = Color(0xFFC98A2E)
private val NEAR_LEG = Color(0xFF2B3650)
private val SHOE = Color(0xFFFFB648)
private val SLEEVE = Color(0xFF2FC4A6)
private val SKIN = Color(0xFFD39A72)
private val HAIR = Color(0xFF2B1D16)
private val TORSO = Brush.verticalGradient(listOf(Color(0xFF5CF0CF), Color(0xFF1FB093)))

/**
 * شخصية التمرين بهوية سند: قميص فيروزي، بنطلون كحلي، حذاء زعفراني، وعصابة رياضية.
 * الأطراف البعيدة أغمق لتعطي عمق.
 */
fun DrawScope.drawFigure(f: Pose, prop: Prop, @Suppress("UNUSED_PARAMETER") c: SanadColors, onDark: Boolean) {
    val s = size.minDimension / 100f
    val ox = (size.width - 100 * s) / 2
    val oy = (size.height - 100 * s) / 2
    fun pt(j: Joint) = Offset(ox + f.x(j) * s, oy + f.y(j) * s)
    fun o(x: Float, y: Float) = Offset(ox + x * s, oy + y * s)
    val propColor = Color(0x73A0AFC8)

    // ظل ناعم تحت الجسم
    val hipX = f.x(Joint.HIP)
    drawOval(
        Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent), center = o(hipX, 95f), radius = 26 * s),
        topLeft = o(hipX - 26, 93f), size = Size(52 * s, 4.4f * s),
    )

    when (prop) {
        Prop.CHAIR -> {
            drawLine(propColor, o(25f, 64.5f), o(49f, 64.5f), 3f * s, StrokeCap.Round)
            drawLine(propColor, o(27f, 65f), o(27f, 94f), 3f * s, StrokeCap.Round)
            drawLine(propColor, o(47f, 65f), o(47f, 94f), 3f * s, StrokeCap.Round)
            drawLine(propColor, o(26f, 64f), o(23f, 37f), 3f * s, StrokeCap.Round)
        }
        Prop.WALL -> drawRoundRect(propColor.copy(alpha = 0.3f), o(86f, 5f), Size(6 * s, 89.6f * s), CornerRadius(2f * s))
        Prop.MAT -> drawRoundRect(SHOE.copy(alpha = if (onDark) 0.28f else 0.22f), o(6f, 92.4f), Size(88 * s, 2.6f * s), CornerRadius(1.3f * s))
        Prop.NONE -> Unit
    }

    fun seg(a: Offset, b: Offset, w: Float, col: Color) = drawLine(col, a, b, w * s, StrokeCap.Round)
    fun limb(a: Joint, b: Joint, w: Float, col: Color) = seg(pt(a), pt(b), w, col)
    fun shoe(knee: Joint, foot: Joint, w: Float, col: Color) {
        val k = pt(knee); val ft = pt(foot)
        val dx = ft.x - k.x; val dy = ft.y - k.y
        val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
        // القدم عمودية على الساق وتتجه للأمام (+x)
        var px = -dy / len; var py = dx / len
        if (px < 0) { px = -px; py = -py }
        seg(ft, Offset(ft.x + px * 7f * s, ft.y + py * 7f * s), w, col)
    }

    // الأطراف البعيدة خلف الجسم
    limb(Joint.NECK, Joint.ELBOW_F, 5f, FAR_ARM); limb(Joint.ELBOW_F, Joint.HAND_F, 4.4f, FAR_SKIN)
    limb(Joint.HIP, Joint.KNEE_F, 6.6f, FAR_LEG); limb(Joint.KNEE_F, Joint.FOOT_F, 6f, FAR_LEG)
    shoe(Joint.KNEE_F, Joint.FOOT_F, 4.6f, FAR_SHOE)

    // الجذع
    val torso = Path().apply {
        moveTo(pt(Joint.NECK).x, pt(Joint.NECK).y)
        quadraticTo(pt(Joint.MID).x, pt(Joint.MID).y, pt(Joint.HIP).x, pt(Joint.HIP).y)
    }
    drawPath(torso, TORSO, style = Stroke(10.5f * s, cap = StrokeCap.Round))

    // الأطراف القريبة
    limb(Joint.HIP, Joint.KNEE_N, 7f, NEAR_LEG); limb(Joint.KNEE_N, Joint.FOOT_N, 6.4f, NEAR_LEG)
    shoe(Joint.KNEE_N, Joint.FOOT_N, 4.8f, SHOE)
    limb(Joint.NECK, Joint.ELBOW_N, 5.4f, SLEEVE); limb(Joint.ELBOW_N, Joint.HAND_N, 4.6f, SKIN)

    // الرأس + الشعر + العصابة، مائلة مع اتجاه الرقبة
    val head = pt(Joint.HEAD)
    val neck = pt(Joint.NECK)
    val ang = kotlin.math.atan2(head.y - neck.y, head.x - neck.x) + (kotlin.math.PI / 2).toFloat()
    fun rot(x: Float, y: Float): Offset {
        val cs = kotlin.math.cos(ang); val sn = kotlin.math.sin(ang)
        return Offset(head.x + (x * cs - y * sn) * s, head.y + (x * sn + y * cs) * s)
    }
    drawCircle(SKIN, 6.4f * s, head)
    val hair = Path().apply {
        val a = rot(-6.5f, -0.6f); moveTo(a.x, a.y)
        val c1 = rot(-5.2f, -6.2f); val c2 = rot(3.2f, -7.8f); val e = rot(6.5f, -2.6f)
        cubicTo(c1.x, c1.y, c2.x, c2.y, e.x, e.y)
        val q = rot(0f, -4.6f); quadraticTo(q.x, q.y, a.x, a.y)
        close()
    }
    drawPath(hair, HAIR)
    val band = Path().apply {
        val a = rot(-6.2f, -1.6f); moveTo(a.x, a.y)
        val m = rot(0f, -3.4f); val b = rot(6.2f, -1.6f)
        quadraticTo(m.x, m.y, b.x, b.y)
    }
    drawPath(band, SHOE, style = Stroke(2.3f * s, cap = StrokeCap.Round))
}
