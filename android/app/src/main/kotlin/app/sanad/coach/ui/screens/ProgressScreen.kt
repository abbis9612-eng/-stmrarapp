package app.sanad.coach.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import app.sanad.coach.data.AppStore
import app.sanad.coach.data.latestWeight
import app.sanad.coach.data.targets
import app.sanad.coach.data.today
import app.sanad.coach.ui.components.BtnStyle
import app.sanad.coach.ui.components.Ico
import app.sanad.coach.ui.components.Meter
import app.sanad.coach.ui.components.NightCard
import app.sanad.coach.ui.components.SButton
import app.sanad.coach.ui.components.SCard
import app.sanad.coach.ui.components.SIcon
import app.sanad.coach.ui.components.SaduBand
import app.sanad.coach.ui.components.Stat
import app.sanad.coach.ui.components.press
import app.sanad.coach.ui.theme.Sanad
import app.sanad.coach.ui.theme.Type
import app.sanad.core.AppState
import app.sanad.core.TrendPoint
import app.sanad.core.ar
import app.sanad.core.computeThread
import app.sanad.core.parseNum
import app.sanad.core.trendWeights
import app.sanad.core.weightPoints
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

@Composable
fun ProgressScreen(store: AppStore, state: AppState, nav: NavHostController) {
    val c = Sanad.colors
    val p = state.profile ?: return
    val t = state.targets() ?: return
    val day = state.today()
    var w by rememberSaveable { mutableStateOf("") }
    var confirmReset by remember { mutableStateOf(false) }
    val trend = trendWeights(weightPoints(state.days.values))
    val current = trend.lastOrNull()?.trend ?: p.startWeightKg
    val lost = Math.round((p.startWeightKg - current) * 10) / 10.0
    val toGo = max(0.0, Math.round((current - p.goalWeightKg) * 10) / 10.0)
    val weeks = if (t.weeklyLossKg > 0) ceil(toGo / t.weeklyLossKg).toInt() else 0
    val pct = ((p.startWeightKg - current) / (p.startWeightKg - p.goalWeightKg)).toFloat().coerceIn(0f, 1f)
    val thread = computeThread(state.days, day.date)

    Page {
        item {
            Text("التقدّم", style = Type.h1.copy(color = c.ink))
            Text("الميزان اليومي يتذبذب بالماء والملح. نتابع الوزن الاتجاهي — الخط الهادي اللي يبين الحقيقة.", style = Type.small.copy(color = c.inkSoft))
        }
        item {
            NightCard(pad = 0.dp) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("وزنك الاتجاهي", style = Type.small.copy(color = c.onNightSoft))
                            Text("${ar(current)} كغ", style = Type.hero.copy(color = Color.White))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("الهدف", style = Type.small.copy(color = c.onNightSoft))
                            Text("${ar(p.goalWeightKg)} كغ", style = Type.h2.copy(color = c.date))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Meter(pct, color = c.date, height = 10.dp)
                    Spacer(Modifier.height(14.dp))
                    if (trend.size >= 2) TrendChart(trend.takeLast(60), p.goalWeightKg)
                    else Text("سجّل وزنك مرتين على الأقل عشان يطلع مسارك.", style = Type.small.copy(color = c.onNightSoft))
                }
                SaduBand(height = 8.dp)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat(ar(max(0.0, lost)), "كغ نزلت", Modifier.weight(1f))
                Stat(ar(toGo), "كغ للهدف", Modifier.weight(1f))
                Stat(if (toGo > 0) ar(weeks) else "✓", if (toGo > 0) "أسبوع تقريباً" else "وصلت!", Modifier.weight(1f))
            }
        }
        item {
            SCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SIcon(Ico.SCALE, tint = c.sadu)
                    Spacer(Modifier.size(8.dp))
                    Text("وزن اليوم", style = Type.h2.copy(color = c.ink), modifier = Modifier.weight(1f))
                    day.weightKg?.let { Text("${ar(it)} كغ", style = Type.bodyStrong.copy(color = c.palm)) }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SField(w, { w = it }, ar(state.latestWeight() ?: p.startWeightKg), Modifier.weight(1f), number = true)
                    val kg = parseNum(w)
                    SButton("احفظ", { store.logWeight(kg!!); w = "" }, enabled = kg != null && kg in 30.0..350.0)
                }
                Text("أفضل وقت: الصبح بعد الحمام وقبل الأكل. ٣ مرات بالأسبوع تكفي.", style = Type.label.copy(color = c.inkSoft), modifier = Modifier.padding(top = 8.dp))
            }
        }
        item {
            SCard {
                Text("حرقك الحقيقي", style = Type.h2.copy(color = c.ink))
                Spacer(Modifier.height(6.dp))
                val a = t.adaptive
                if (a != null) {
                    Text("تعلّم سند من ${ar(a.loggedDays)} يوم مسجل إن حرقك تقريباً ${ar(a.tdee)} سعرة يومياً، فعدّل هدفك إلى ${ar(t.kcal)} سعرة.", style = Type.body.copy(color = c.ink))
                    Spacer(Modifier.height(8.dp))
                    Text("الثقة بالتقدير ${ar((a.confidence * 100).toInt())}٪", style = Type.label.copy(color = c.inkSoft))
                    Meter(a.confidence.toFloat(), color = c.palm, height = 8.dp)
                } else {
                    Text("حالياً نستخدم تقدير المعادلة (${ar(t.tdee)} سعرة). بعد ٧ أيام تسجيل أكل و٣ أوزان، سند يحسب حرقك الفعلي من بياناتك ويعدّل هدفك — مثل أخصائي يتابعك أسبوعياً.", style = Type.body.copy(color = c.ink))
                }
            }
        }
        item {
            SCard {
                Text("الخيط", style = Type.h2.copy(color = c.ink))
                Text("خيطك الحالي ${ar(thread.length)} يوم، وأطول خيط ${ar(thread.best)}.", style = Type.body.copy(color = c.ink))
            }
        }
        item {
            SCard {
                Text("خططك \"إذا… فأنا…\"", style = Type.h2.copy(color = c.ink))
                if (p.ifThens.isEmpty()) Text("اطلب من سند يجهز لك خطة لأصعب موقف عندك.", style = Type.small.copy(color = c.inkSoft))
                p.ifThens.forEach { r ->
                    Row(Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface2).padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${r.whenText} ← ${r.thenText}", style = Type.small.copy(color = c.ink), modifier = Modifier.weight(1f).padding(vertical = 10.dp))
                        Box(Modifier.size(48.dp).press({ store.removeIfThen(r.id) }).semantics { contentDescription = "احذف الخطة" }, contentAlignment = Alignment.Center) {
                            SIcon(Ico.TRASH, size = 20.dp, tint = c.inkSoft)
                        }
                    }
                }
            }
        }
        item {
            SCard {
                Text("بياناتك", style = Type.h2.copy(color = c.ink))
                Text("كل بياناتك محفوظة على هذا الجهاز فقط.", style = Type.small.copy(color = c.inkSoft))
                Spacer(Modifier.height(10.dp))
                if (!confirmReset) SButton("امسح كل البيانات", { confirmReset = true }, Modifier.fillMaxWidth(), style = BtnStyle.GHOST)
                else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SButton("تراجع", { confirmReset = false }, Modifier.weight(1f), style = BtnStyle.SOFT)
                    SButton("نعم، امسح", { store.reset(); nav.navigate("start") { popUpTo(0) } }, Modifier.weight(1f))
                }
            }
        }
        item {
            Text("سند أداة تدريب سلوكي ومعلومات عامة، ولا يغني عن الطبيب أو أخصائي التغذية.", style = Type.label.copy(color = c.inkSoft))
        }
    }
}

/** منحنى الوزن الاتجاهي يرسم نفسه، مع نقاط القراءات اليومية وخط الهدف. RTL: الأقدم يمين. */
@Composable
private fun TrendChart(points: List<TrendPoint>, goal: Double) {
    val c = Sanad.colors
    val draw = remember { Animatable(0f) }
    LaunchedEffect(points.size) { draw.snapTo(0f); draw.animateTo(1f, tween(1200, easing = FastOutSlowInEasing)) }
    val ys = points.flatMap { listOf(it.kg, it.trend) }.toMutableList()
    if (ys.min() - goal < 3) ys += goal
    val minY = kotlin.math.floor(ys.min() - 0.5)
    val maxY = ceil(ys.max() + 0.5)
    val d0 = LocalDate.parse(points.first().date).toEpochDay()
    val d1 = max(d0 + 1, LocalDate.parse(points.last().date).toEpochDay())
    Canvas(
        Modifier.fillMaxWidth().aspectRatio(2.1f).semantics {
            contentDescription = "الوزن الاتجاهي من ${ar(points.first().trend)} إلى ${ar(points.last().trend)} كغ"
        },
    ) {
        val pad = 8.dp.toPx()
        fun x(date: String) = size.width - pad - (LocalDate.parse(date).toEpochDay() - d0).toFloat() / (d1 - d0) * (size.width - 2 * pad)
        fun y(kg: Double) = pad + ((maxY - kg) / (maxY - minY)).toFloat() * (size.height - 2 * pad)
        if (goal in minY..maxY) drawLine(c.palm, Offset(pad, y(goal)), Offset(size.width - pad, y(goal)), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
        points.forEach { drawCircle(Color.White.copy(alpha = 0.35f), 3.dp.toPx(), Offset(x(it.date), y(it.kg))) }
        val path = Path()
        points.forEachIndexed { i, pt -> if (i == 0) path.moveTo(x(pt.date), y(pt.trend)) else path.lineTo(x(pt.date), y(pt.trend)) }
        // يرسم من اليمين (الأقدم) لليسار حسب التقدّم
        val revealX = size.width - size.width * draw.value
        clipRect(left = min(revealX, size.width)) {
            drawPath(path, Brush.horizontalGradient(listOf(c.date, c.sadu2)), style = Stroke(3.5.dp.toPx(), cap = StrokeCap.Round))
        }
        val last = points.last()
        if (draw.value > 0.95f) {
            drawCircle(c.date, 6.dp.toPx(), Offset(x(last.date), y(last.trend)))
            drawCircle(c.night, 3.dp.toPx(), Offset(x(last.date), y(last.trend)))
        }
    }
}
