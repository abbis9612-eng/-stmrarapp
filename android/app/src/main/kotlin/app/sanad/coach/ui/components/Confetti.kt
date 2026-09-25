package app.sanad.coach.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private val CONFETTI = listOf(
    Color(0xFFFFB648), Color(0xFFFF7A45), Color(0xFF34D7B8), Color(0xFF5B8CFF), Color(0xFFFF5C7A), Color(0xFFF2F0EB),
)

private class Bit(
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    var r: Float, val vr: Float, val w: Float, val h: Float,
    val color: Color, val round: Boolean, var life: Float = 1f,
)

/** احتفال بقصاصات ملوّنة فوق كل الشاشات. يُطلق من أي مكان عبر [LocalConfetti]. */
class ConfettiState {
    private val bits = mutableListOf<Bit>()
    internal var frame by mutableIntStateOf(0)
    internal var active by mutableStateOf(false)

    fun burst(at: Offset, count: Int = 70, power: Float = 1f) {
        repeat(count) { i ->
            val a = Random.nextFloat() * 6.283f
            val v = (5f + Random.nextFloat() * 17f) * power
            bits += Bit(
                at.x, at.y, cos(a) * v, sin(a) * v - 10f * power,
                Random.nextFloat() * 6.28f, (Random.nextFloat() - 0.5f) * 0.4f,
                10f + Random.nextFloat() * 12f, 18f + Random.nextFloat() * 14f,
                CONFETTI[i % CONFETTI.size], Random.nextFloat() < 0.3f,
            )
        }
        active = true
    }

    internal fun step() {
        val it = bits.iterator()
        while (it.hasNext()) {
            val b = it.next()
            b.vy += 0.55f; b.vx *= 0.985f; b.vy *= 0.985f
            b.x += b.vx; b.y += b.vy; b.r += b.vr; b.life -= 0.009f
            if (b.life <= 0f) it.remove()
        }
        if (bits.isEmpty()) active = false
        frame++
    }

    internal fun draw(scope: androidx.compose.ui.graphics.drawscope.DrawScope) = with(scope) {
        for (b in bits) {
            val alpha = min(1f, b.life * 2f)
            rotateRad(b.r, Offset(b.x, b.y)) {
                if (b.round) drawCircle(b.color.copy(alpha = alpha), b.w / 2, Offset(b.x, b.y))
                else drawRect(b.color.copy(alpha = alpha), Offset(b.x - b.w / 2, b.y - b.h / 2), Size(b.w, b.h * abs(cos(b.r * 2)) + 2f))
            }
        }
    }
}

val LocalConfetti = staticCompositionLocalOf { ConfettiState() }

@Composable
fun ConfettiLayer(state: ConfettiState) {
    LaunchedEffect(state.active) {
        while (state.active) withFrameNanos { state.step() }
    }
    if (state.active) {
        Canvas(Modifier.fillMaxSize()) {
            state.frame // يعيد الرسم كل إطار
            state.draw(this)
        }
    }
}

/** نقطة إطلاق: يحفظ مركز العنصر على الشاشة لنطلق منه الاحتفال. */
class BurstPoint { var center = Offset.Zero }

fun Modifier.burstFrom(point: BurstPoint): Modifier = onGloballyPositioned { c ->
    val p = c.positionInRoot()
    point.center = Offset(p.x + c.size.width / 2f, p.y + c.size.height / 2f)
}

@Composable
fun rememberBurstPoint() = remember { BurstPoint() }
