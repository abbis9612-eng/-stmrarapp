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

fun DrawScope.drawFigure(f: Pose, prop: Prop, c: SanadColors, onDark: Boolean) {
    val s = size.minDimension / 100f
    val ox = (size.width - 100 * s) / 2
    val oy = (size.height - 100 * s) / 2
    fun pt(j: Joint) = Offset(ox + f.x(j) * s, oy + f.y(j) * s)
    fun o(x: Float, y: Float) = Offset(ox + x * s, oy + y * s)

    // في الوضع الداكن الأطراف فاتحة حتى تبان على الخلفيات الداكنة
    val light = onDark || c.isDark
    val near = if (light) c.wool else c.night
    val far = if (light) c.onNightSoft.copy(alpha = 0.55f) else c.inkSoft.copy(alpha = 0.45f)
    val propColor = if (onDark) Color.White.copy(alpha = 0.16f) else c.line

    // الأرض + ظل ناعم تحت الجسم
    drawLine(propColor, o(4f, 94.6f), o(96f, 94.6f), 1.2f * s, cap = StrokeCap.Round)
    val hipX = f.x(Joint.HIP)
    drawOval(
        Brush.radialGradient(listOf(Color.Black.copy(alpha = if (onDark) 0.35f else 0.12f), Color.Transparent), center = o(hipX, 95f), radius = 22 * s),
        topLeft = o(hipX - 24, 92.5f), size = Size(48 * s, 5 * s),
    )

    when (prop) {
        Prop.CHAIR -> {
            drawRoundRect(propColor, o(26f, 62f), Size(24 * s, 4 * s), CornerRadius(1.5f * s))
            drawLine(propColor, o(28f, 66f), o(28f, 94f), 2.4f * s, StrokeCap.Round)
            drawLine(propColor, o(48f, 66f), o(48f, 94f), 2.4f * s, StrokeCap.Round)
            drawLine(propColor, o(27f, 64f), o(24f, 36f), 2.4f * s, StrokeCap.Round)
        }
        Prop.WALL -> drawRect(propColor, o(86f, 6f), Size(4 * s, 88.6f * s))
        Prop.MAT -> drawRoundRect(if (onDark) c.date.copy(alpha = 0.25f) else c.dateSoft, o(6f, 92.2f), Size(88 * s, 2.6f * s), CornerRadius(1.3f * s))
        Prop.NONE -> Unit
    }

    fun limb(a: Joint, b: Joint, w: Float, col: Color) = drawLine(col, pt(a), pt(b), w * s, StrokeCap.Round)

    // الأطراف البعيدة خلف الجسم
    limb(Joint.HIP, Joint.KNEE_F, 7.4f, far); limb(Joint.KNEE_F, Joint.FOOT_F, 6.4f, far)
    limb(Joint.NECK, Joint.ELBOW_F, 6f, far); limb(Joint.ELBOW_F, Joint.HAND_F, 5.2f, far)

    // الجذع: منحنى سميك بلون السدو مع خط ذهبي رفيع (زخرفة الثوب)
    val torso = Path().apply {
        moveTo(pt(Joint.NECK).x, pt(Joint.NECK).y)
        quadraticTo(pt(Joint.MID).x, pt(Joint.MID).y, pt(Joint.HIP).x, pt(Joint.HIP).y)
    }
    drawPath(torso, c.sadu, style = Stroke(12f * s, cap = StrokeCap.Round))
    drawPath(torso, c.date.copy(alpha = 0.8f), style = Stroke(1.2f * s, cap = StrokeCap.Round))

    limb(Joint.HIP, Joint.KNEE_N, 8.2f, near); limb(Joint.KNEE_N, Joint.FOOT_N, 7.2f, near)
    limb(Joint.NECK, Joint.ELBOW_N, 6.8f, near); limb(Joint.ELBOW_N, Joint.HAND_N, 6f, near)

    // الرأس
    val head = pt(Joint.HEAD)
    drawCircle(c.date, 6.8f * s, head)
    drawCircle(Color.White.copy(alpha = 0.35f), 2.2f * s, head + Offset(-1.8f * s, -2f * s))
}
