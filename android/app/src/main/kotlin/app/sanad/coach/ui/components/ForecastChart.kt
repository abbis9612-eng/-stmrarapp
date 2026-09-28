package app.sanad.coach.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.sanad.coach.ui.theme.Sanad
import app.sanad.coach.ui.theme.Type
import app.sanad.core.Forecast
import app.sanad.core.ar
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

private const val AR_DIGITS = "٠١٢٣٤٥٦٧٨٩"
fun arYear(y: Int) = y.toString().map { AR_DIGITS[it - '0'] }.joinToString("")
fun monthAr(d: LocalDate): String = d.month.getDisplayName(TextStyle.FULL, Locale("ar"))

/** «بين فبراير وأبريل ٢٠٢٧»، أو بسنتين إذا اختلفت السنة. */
fun rangeText(a: LocalDate, b: LocalDate): String = when {
    a.year == b.year && a.month == b.month -> "في ${monthAr(a)} ${arYear(a.year)}"
    a.year == b.year -> "بين ${monthAr(a)} و${monthAr(b)} ${arYear(b.year)}"
    else -> "بين ${monthAr(a)} ${arYear(a.year)} و${monthAr(b)} ${arYear(b.year)}"
}

/**
 * مسار الوصول بصراحة: خط يتموّج داخل شريط التذبذب الطبيعي، ومنطقة وصول صفراء
 * تمتد من أسرع أسبوع إلى أبطأه. الأحدث يمين (قراءة عربية)، والهدف يسار.
 */
@Composable
fun ForecastCard(f: Forecast, modifier: Modifier = Modifier, startLabel: String = "الآن") {
    val c = Sanad.colors
    val today = LocalDate.now()
    val earliest = LocalDate.parse(f.earliest)
    val latest = LocalDate.parse(f.latest)
    val draw = remember { Animatable(0f) }
    LaunchedEffect(f) { draw.snapTo(0f); draw.animateTo(1f, tween(1600, easing = FastOutSlowInEasing)) }
    val band = Color(0xFFDCE8F3)
    Column(
        modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp)).padding(16.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "من ${ar(f.startKg)} إلى ${ar(f.goalKg)} كغ، ${rangeText(earliest, latest)}"
            },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("$startLabel ${ar(f.startKg)}", style = Type.bodyStrong.copy(color = c.ink))
        Box {
            Canvas(Modifier.fillMaxWidth().height(170.dp)) {
                val pad = 10.dp.toPx()
                val bandKg = 0.9
                val top = f.startKg + bandKg + 0.3
                val bottom = f.goalKg - bandKg * 1.6
                fun y(kg: Double) = pad + ((top - kg) / (top - bottom)).toFloat() * (size.height - 2 * pad)
                // t=0 اليوم يميناً، t=1 أبطأ وصول يساراً
                fun x(t: Float) = size.width - pad - t * (size.width - 2 * pad)
                val tFast = f.weeksFast.toFloat() / f.weeksSlow
                val tMid = (tFast + 1f) / 2f
                fun trend(t: Float): Double {
                    val u = (t / tMid).coerceIn(0f, 1f).toDouble()
                    return f.startKg - (f.startKg - f.goalKg) * (1 - (1 - u).pow(1.3))
                }
                val n = 90
                val area = Path().apply {
                    for (i in 0..n) { val t = tMid * i / n; if (i == 0) moveTo(x(t), y(trend(t) + bandKg)) else lineTo(x(t), y(trend(t) + bandKg)) }
                    for (i in n downTo 0) { val t = tMid * i / n; lineTo(x(t), y(trend(t) - bandKg)) }
                    close()
                }
                drawPath(area, band)
                // منطقة الوصول: من أسرع أسبوع إلى أبطأه، أعرض من الشريط حتى تُقرأ
                val zTop = y(f.goalKg + bandKg * 1.3)
                val zBottom = y(f.goalKg - bandKg * 1.3)
                drawRoundRect(c.amberTint, Offset(x(1f), zTop), Size(x(tFast) - x(1f), zBottom - zTop), CornerRadius(12.dp.toPx()))
                drawLine(c.faint.copy(alpha = 0.6f), Offset(pad, y(f.goalKg)), Offset(size.width - pad, y(f.goalKg)), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                // المسار الحقيقي يتموّج: أسابيع تثبت وأسابيع تنزل
                val shown = (n * draw.value).toInt()
                val line = Path()
                for (i in 0..shown) {
                    val t = tMid * i / n
                    val wig = 0.45 * sin(t / tMid * 7.0 * PI) * (1 - 0.4 * t / tMid)
                    val px = x(t); val py = y(trend(t) + wig)
                    if (i == 0) line.moveTo(px, py) else line.lineTo(px, py)
                }
                drawPath(line, c.primary, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                val start = Offset(x(0f), y(f.startKg))
                drawCircle(c.surface, 7.dp.toPx(), start)
                drawCircle(c.primary, 7.dp.toPx(), start, style = Stroke(3.dp.toPx()))
            }
            // رقم الهدف فوق منطقة الوصول مباشرة (يسار البطاقة في العربي)
            Text(
                "${ar(f.goalKg)} كغ",
                style = Type.bodyStrong.copy(color = c.saffron),
                modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 40.dp, end = 14.dp),
            )
        }
        Row(Modifier.fillMaxWidth()) {
            Text("${monthAr(today)} ${arYear(today.year)}", style = Type.label.copy(color = c.inkSoft), modifier = Modifier.weight(1f))
            Text(rangeText(earliest, latest), style = Type.label.copy(color = c.saffron, fontWeight = FontWeight.Bold))
        }
    }
}
