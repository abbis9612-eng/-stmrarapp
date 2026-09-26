package app.sanad.coach.ui.screens

import androidx.compose.foundation.layout.fillMaxHeight
import app.sanad.core.WEEK_THEMES
import app.sanad.core.LESSONS
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import app.sanad.core.SLEEP_TARGET_H
import app.sanad.core.sleepBank
import app.sanad.core.SleepBank
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import app.sanad.coach.data.AppStore
import app.sanad.coach.data.latestWeight
import app.sanad.coach.data.targets
import app.sanad.coach.data.today
import app.sanad.coach.ui.Routes
import app.sanad.coach.ui.components.BtnStyle
import app.sanad.coach.ui.components.Eyebrow
import app.sanad.coach.ui.components.Ico
import app.sanad.coach.ui.components.Meter
import app.sanad.coach.ui.components.SButton
import app.sanad.coach.ui.components.SCard
import app.sanad.coach.ui.components.SIcon
import app.sanad.coach.ui.components.Stat
import app.sanad.coach.ui.components.glass
import app.sanad.coach.ui.components.press
import app.sanad.coach.ui.theme.Sanad
import app.sanad.coach.ui.theme.Type
import app.sanad.core.AppState
import app.sanad.core.Pacing
import app.sanad.core.TrendPoint
import app.sanad.core.WeeklyReview
import app.sanad.core.weighInWeather
import app.sanad.core.WeighIn
import app.sanad.core.addDays
import app.sanad.core.ar
import app.sanad.core.computeThread
import app.sanad.core.isCounted
import app.sanad.core.parseNum
import app.sanad.core.trendWeights
import app.sanad.core.weeklyReview
import app.sanad.core.weightPoints
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

@Composable
fun ProgressScreen(store: AppStore, state: AppState, nav: NavHostController) {
    val c = Sanad.colors
    val p = state.profile ?: return
    val t = state.targets() ?: return
    val day = state.today()
    val rise = rememberRise()
    var w by rememberSaveable { mutableStateOf("") }
    var confirmReset by remember { mutableStateOf(false) }
    val trend = trendWeights(weightPoints(state.days.values))
    val current = trend.lastOrNull()?.trend ?: p.startWeightKg
    val lost = Math.round((p.startWeightKg - current) * 10) / 10.0
    val toGo = max(0.0, Math.round((current - p.goalWeightKg) * 10) / 10.0)
    val weeks = if (t.weeklyLossKg > 0) ceil(toGo / t.weeklyLossKg).toInt() else 0
    val thread = computeThread(state.days, day.date)
    val workouts = state.days.values.sumOf { it.workouts.size }

    Page {
        item {
            Column(Modifier.rise(rise, 0)) {
                Eyebrow("تقدّمك")
                Text("الاتجاه أهم من الميزان", style = Type.h1.copy(color = c.ink))
            }
        }
        item {
            Column(Modifier.fillMaxWidth().rise(rise, 1).glass().padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 12.dp)) {
                Eyebrow("وزنك الاتجاهي")
                val shown = remember { Animatable(p.startWeightKg.toFloat()) }
                LaunchedEffect(current) { shown.animateTo(current.toFloat(), tween(1400, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f))) }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(ar(Math.round(shown.value * 10) / 10.0), style = Type.hero.copy(color = c.ink))
                    Spacer(Modifier.width(8.dp))
                    Text("كغ", style = Type.body.copy(color = c.inkSoft), modifier = Modifier.padding(bottom = 10.dp))
                    Spacer(Modifier.width(10.dp))
                    if (lost > 0) Box(Modifier.padding(bottom = 12.dp).clip(CircleShape).background(c.oasis.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text("نزلت ${ar(lost)} كغ", style = Type.label.copy(color = c.oasis))
                    }
                }
                Spacer(Modifier.height(6.dp))
                if (trend.size >= 2) TrendChart(trend.takeLast(60))
                else Text("سجّل وزنك مرتين على الأقل عشان يطلع مسارك.", style = Type.small.copy(color = c.inkSoft), modifier = Modifier.padding(vertical = 18.dp))
            }
        }
        item {
            Row(Modifier.rise(rise, 2), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat(ar(toGo), "كغ للهدف", Modifier.weight(1f))
                Stat(if (toGo > 0) ar(weeks) else "وصلت", if (toGo > 0) "أسبوع تقريباً" else "الهدف", Modifier.weight(1f))
                Stat(ar(workouts), "تمرين", Modifier.weight(1f))
            }
        }
        item { WeekReviewCard(weeklyReview(p, state.days, t, day.date), Modifier.rise(rise, 3)) { nav.navigate(Routes.coach("كيف كان أسبوعي؟")) } }
        sleepBank(state.days, day.date)?.let { b -> item(key = "sleep-bank") { SleepBankCard(b) } }
        item(key = "journey") { JourneyCard(state.lessonsRead.size) }
        item {
            SCard(Modifier.rise(rise, 4)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("خريطة الاستمرار", style = Type.h2.copy(color = c.ink), modifier = Modifier.weight(1f))
                    Text("٥ أسابيع", style = Type.small.copy(color = c.inkSoft))
                }
                Spacer(Modifier.height(12.dp))
                Heatmap(state, day.date)
            }
        }
        item {
            SCard(Modifier.rise(rise, 5)) {
                Text("إنجازاتك", style = Type.h2.copy(color = c.ink))
                Spacer(Modifier.height(14.dp))
                val proteinDays = state.days.values.count { it.protein >= t.protein * 0.9 }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Medal("أول ٧ أيام", thread.best >= 7, c.saffron, c.ember, Ico.STAR, Modifier.weight(1f))
                    Medal("أول كيلو", lost >= 1, c.oasis, Color(0xFF1FB093), Ico.SCALE, Modifier.weight(1f))
                    Medal("١٠ تمارين", workouts >= 10, Color(0xFF8FB0FF), Color(0xFF4B74F0), Ico.MOVE, Modifier.weight(1f))
                    Medal("بطل البروتين", proteinDays >= 5, Color(0xFFFF8DA3), c.rose, Ico.EAT, Modifier.weight(1f))
                }
            }
        }
        item {
            SCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SIcon(Ico.SCALE, tint = c.saffron)
                    Spacer(Modifier.size(8.dp))
                    Text("وزن اليوم", style = Type.h2.copy(color = c.ink), modifier = Modifier.weight(1f))
                    day.weightKg?.let { Text("${ar(it)} كغ", style = Type.bodyStrong.copy(color = c.oasis)) }
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
        weighInWeather(state, day.date, t)?.let { w -> item(key = "weather-${w.todayKg}") { WeighInCard(w) } }
        item {
            SCard {
                Text("حرقك الحقيقي", style = Type.h2.copy(color = c.ink))
                Spacer(Modifier.height(6.dp))
                val a = t.adaptive
                if (a != null) {
                    Text("تعلّم سند من ${ar(a.loggedDays)} يوم مسجل إن حرقك تقريباً ${ar(a.tdee)} سعرة يومياً، فعدّل هدفك إلى ${ar(t.kcal)} سعرة.", style = Type.body.copy(color = c.ink))
                    Spacer(Modifier.height(8.dp))
                    Text("الثقة بالتقدير ${ar((a.confidence * 100).toInt())}٪", style = Type.label.copy(color = c.inkSoft))
                    Spacer(Modifier.height(6.dp))
                    Meter(a.confidence.toFloat(), color = c.oasis, height = 8.dp)
                } else {
                    Text("حالياً نستخدم تقدير المعادلة (${ar(t.tdee)} سعرة). بعد ٧ أيام تسجيل أكل و٣ أوزان، سند يحسب حرقك الفعلي من بياناتك ويعدّل هدفك، مثل أخصائي يتابعك أسبوعياً.", style = Type.body.copy(color = c.ink))
                }
            }
        }
        item {
            SCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("وضع رمضان", style = Type.h2.copy(color = c.ink))
                        Text("يوزّع هدفك على الفطور والسحور، والتمرين بعد الفطور.", style = Type.small.copy(color = c.inkSoft))
                    }
                    Switch(
                        checked = p.ramadan,
                        onCheckedChange = store::setRamadan,
                        colors = SwitchDefaults.colors(checkedTrackColor = c.oasis, checkedThumbColor = c.bg, uncheckedTrackColor = c.glass2, uncheckedBorderColor = c.line),
                        modifier = Modifier.semantics { contentDescription = "وضع رمضان" },
                    )
                }
            }
        }
        item {
            SCard {
                Text("خططك «إذا… فأنا…»", style = Type.h2.copy(color = c.ink))
                if (p.ifThens.isEmpty()) Text("اطلب من سند يجهز لك خطة لأصعب موقف عندك.", style = Type.small.copy(color = c.inkSoft))
                p.ifThens.forEach { r ->
                    Row(Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.05f)).padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
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
            Text("سند أداة تدريب سلوكي ومعلومات عامة، ولا يغني عن الطبيب أو أخصائي التغذية.", style = Type.label.copy(color = c.faint))
        }
    }
}

/**
 * منحنى الوزن الاتجاهي يرسم نفسه من اليمين (الأقدم) لليسار (اليوم)،
 * مع قراءات الميزان اليومية نقاطاً خافتة وتعبئة متدرجة تحته.
 */
@Composable
private fun TrendChart(points: List<TrendPoint>) {
    val c = Sanad.colors
    val draw = remember { Animatable(0f) }
    val measurer = rememberTextMeasurer()
    LaunchedEffect(points.size) { draw.snapTo(0f); draw.animateTo(1f, tween(1600, easing = CubicBezierEasing(0.6f, 0f, 0.2f, 1f))) }
    val ys = points.flatMap { listOf(it.kg, it.trend) }
    val minY = floor(ys.min() - 0.6)
    val maxY = ceil(ys.max() + 0.6)
    val d0 = LocalDate.parse(points.first().date).toEpochDay()
    val d1 = max(d0 + 1, LocalDate.parse(points.last().date).toEpochDay())
    val weeksBack = ((d1 - d0) / 7).toInt()
    Canvas(
        Modifier.fillMaxWidth().aspectRatio(2f).semantics {
            contentDescription = "الوزن الاتجاهي من ${ar(points.first().trend)} إلى ${ar(points.last().trend)} كغ"
        },
    ) {
        val pad = 10.dp.toPx()
        val bottom = size.height - 24.dp.toPx()
        fun x(date: String) = size.width - pad - (LocalDate.parse(date).toEpochDay() - d0).toFloat() / (d1 - d0) * (size.width - 2 * pad)
        fun y(kg: Double) = pad + ((maxY - kg) / (maxY - minY)).toFloat() * (bottom - pad)
        val label = Type.label.copy(color = c.faint, fontSize = 10.sp)
        // خطوط الشبكة: ٣ قيم صحيحة ضمن المدى
        val step = max(1.0, ((maxY - minY) / 3).let { kotlin.math.round(it) })
        var g = ceil(minY)
        while (g <= maxY) {
            val gy = y(g)
            drawLine(Color.White.copy(alpha = 0.06f), Offset(pad, gy), Offset(size.width - pad, gy), 1.dp.toPx())
            drawLabel(measurer, ar(g.toInt()), Offset(pad, gy - 16.dp.toPx()), label)
            g += step
        }
        points.forEach { drawCircle(Color.White.copy(alpha = 0.26f), 2.dp.toPx(), Offset(x(it.date), y(it.kg))) }
        val line = Path()
        points.forEachIndexed { i, pt ->
            val px = x(pt.date); val py = y(pt.trend)
            if (i == 0) line.moveTo(px, py) else {
                val q = points[i - 1]; val mx = (x(q.date) + px) / 2
                line.cubicTo(mx, y(q.trend), mx, py, px, py)
            }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(x(points.last().date), bottom); lineTo(x(points.first().date), bottom); close()
        }
        val reveal = size.width - size.width * draw.value
        clipRect(left = reveal) {
            drawPath(area, Brush.verticalGradient(listOf(c.oasis.copy(alpha = 0.28f), Color.Transparent), startY = pad, endY = bottom))
            drawPath(line, Brush.horizontalGradient(listOf(c.oasis, c.saffron)), style = Stroke(3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        if (draw.value > 0.97f) {
            val last = points.last()
            val e = Offset(x(last.date), y(last.trend))
            drawCircle(c.oasis.copy(alpha = 0.2f), 10.dp.toPx(), e)
            drawCircle(c.bg, 6.5.dp.toPx(), e)
            drawCircle(c.oasis, 5.dp.toPx(), e)
        }
        drawLabel(measurer, if (weeksBack >= 1) "قبل ${ar(weeksBack)} أسابيع" else "البداية", Offset(size.width - pad, size.height - 16.dp.toPx()), label, alignEnd = true)
        drawLabel(measurer, "اليوم", Offset(pad, size.height - 16.dp.toPx()), label)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLabel(
    m: TextMeasurer, text: String, at: Offset, style: androidx.compose.ui.text.TextStyle, alignEnd: Boolean = false,
) {
    val r = m.measure(text, style)
    val x = if (alignEnd) at.x - r.size.width else at.x
    drawText(r, topLeft = Offset(x, at.y))
}

/** مراجعة الأسبوع: أرقام قليلة، نجاح واحد، وتركيز واحد للأسبوع الجاي. */
@Composable
private fun WeekReviewCard(r: WeeklyReview, modifier: Modifier, onAsk: () -> Unit) {
    val c = Sanad.colors
    SCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("مراجعة الأسبوع", style = Type.h2.copy(color = c.ink), modifier = Modifier.weight(1f))
            Text("آخر ٧ أيام", style = Type.small.copy(color = c.inkSoft))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniStat("${ar(r.activeDays)}/٧", "أيام حضور", Modifier.weight(1f))
            MiniStat(ar(r.workouts), "تمارين", Modifier.weight(1f))
            val change = r.trendChangeKg
            MiniStat(if (change != null) "${ar(abs(change))}${if (change <= 0) "-" else "+"}" else "—", "كغ اتجاه", Modifier.weight(1f), if (r.pacing == Pacing.ON_TRACK) c.oasis else c.ink)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.saffron.copy(alpha = 0.10f)).padding(12.dp)) {
            SIcon(Ico.STAR, size = 20.dp, tint = c.saffron)
            Spacer(Modifier.width(10.dp))
            Text(r.win, style = Type.small.copy(color = c.ink), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.oasis.copy(alpha = 0.10f)).padding(12.dp)) {
            SIcon(Ico.SPARK, size = 20.dp, tint = c.oasis)
            Spacer(Modifier.width(10.dp))
            Text(r.focusText, style = Type.small.copy(color = c.ink), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        SButton("ناقشها مع المدرب", onAsk, Modifier.fillMaxWidth(), style = BtnStyle.SOFT)
    }
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier, color: Color = Sanad.colors.ink) {
    val c = Sanad.colors
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.05f)).padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = Type.number.copy(fontSize = 22.sp, color = color))
        Text(label, style = Type.label.copy(color = c.inkSoft, fontSize = 11.sp))
    }
}

/** ٣٥ يوم: كل مربع يتوهّج حسب كمية الإنجاز. الأقدم يمين الصف الأول. */
@Composable
private fun Heatmap(state: AppState, today: String) {
    val c = Sanad.colors
    val cells = (34 downTo 0).map { back ->
        val d = state.days[addDays(today, -back.toLong())]
        when {
            !isCounted(d) -> 0
            d!!.done.size >= 3 || (d.workouts.isNotEmpty() && d.done.size >= 2) -> 3
            d.done.size >= 2 || d.workouts.isNotEmpty() -> 2
            else -> 1
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        cells.chunked(7).forEachIndexed { row, week ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEachIndexed { col, level ->
                    val i = row * 7 + col
                    val s = remember { Animatable(0f) }
                    LaunchedEffect(Unit) { kotlinx.coroutines.delay(200L + i * 18L); s.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 300f)) }
                    val color = when (level) {
                        3 -> c.oasis
                        2 -> c.oasis.copy(alpha = 0.55f)
                        1 -> c.oasis.copy(alpha = 0.28f)
                        else -> Color.White.copy(alpha = 0.06f)
                    }
                    Box(
                        Modifier.weight(1f).aspectRatio(1f).graphicsLayer { scaleX = s.value; scaleY = s.value }
                            .clip(RoundedCornerShape(8.dp)).background(color),
                    )
                }
            }
        }
    }
}

/** وسام سداسي؛ يدور لمن تلمسه. المقفول باهت. */
@Composable
private fun Medal(name: String, earned: Boolean, a: Color, b: Color, icon: Ico, modifier: Modifier) {
    val c = Sanad.colors
    var turns by remember { mutableFloatStateOf(0f) }
    val rot by animateFloatAsState(turns, spring(dampingRatio = 0.55f, stiffness = 120f), label = "medal")
    Column(
        modifier.graphicsLayer { alpha = if (earned) 1f else 0.4f }.press({ turns += 360f })
            .semantics { contentDescription = if (earned) "وسام: $name" else "وسام مقفول: $name" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(62.dp).graphicsLayer { rotationY = rot; cameraDistance = 14f * density }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(62.dp)) {
                fun hex(inset: Float): Path {
                    val w = size.width; val h = size.height
                    return Path().apply {
                        moveTo(w * 0.5f, inset); lineTo(w - inset, h * 0.27f); lineTo(w - inset, h * 0.73f)
                        lineTo(w * 0.5f, h - inset); lineTo(inset, h * 0.73f); lineTo(inset, h * 0.27f); close()
                    }
                }
                drawPath(hex(2.dp.toPx()), if (earned) Brush.linearGradient(listOf(a, b)) else Brush.linearGradient(listOf(Color(0xFF9AA3B2), Color(0xFF5E6776))))
                drawPath(hex(7.dp.toPx()), Color(0x590A0D12))
            }
            SIcon(icon, size = 24.dp, tint = Color.White)
        }
        Spacer(Modifier.height(6.dp))
        Text(name, style = Type.label.copy(color = c.inkSoft, fontSize = 11.sp), textAlign = TextAlign.Center)
    }
}

/** طقس الميزان: يفسّر رقم اليوم حتى ما تترك الميزان بعد قفزة. */
@Composable
private fun WeighInCard(w: WeighIn) {
    val c = Sanad.colors
    val tint = when (w.kind) {
        WeighIn.Kind.JUMP -> c.saffron
        WeighIn.Kind.DROP -> c.oasis
        else -> c.sky
    }
    Column(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp), tint).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("طقس الميزان", style = Type.label.copy(color = tint))
        Text(w.headline, style = Type.h2.copy(color = c.ink))
        Text(w.body, style = Type.body.copy(color = c.ink))
    }
}

/** بنك النوم: ٧ ليالي كأعمدة، والخط = ٧ ساعات. */
@Composable
private fun SleepBankCard(b: SleepBank) {
    val c = Sanad.colors
    val tint = if (b.debt < 0.5) c.oasis else if (b.debt < 3) c.sky else c.saffron
    Column(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SIcon(Ico.MOON, size = 20.dp, tint = c.sky)
            Spacer(Modifier.size(8.dp))
            Text("بنك النوم", style = Type.h2.copy(color = c.ink), modifier = Modifier.weight(1f))
            Text("متوسط ${ar(b.avg)} س", style = Type.small.copy(color = c.inkSoft))
        }
        Text(b.headline, style = Type.bodyStrong.copy(color = tint))
        val ink = c.ink
        val faint = c.faint
        Canvas(Modifier.fillMaxWidth().height(90.dp)) {
            val n = b.nights.size
            val gap = 10.dp.toPx()
            val bw = (size.width - gap * (n - 1)) / n
            val maxH = 10.0
            val goalY = size.height * (1f - (SLEEP_TARGET_H / maxH).toFloat())
            b.nights.forEachIndexed { i, h ->
                // الأحدث على اليمين (قراءة عربية)
                val x = size.width - (i + 1) * bw - i * gap
                if (h == null) {
                    drawRoundRect(faint.copy(alpha = 0.25f), Offset(x, size.height - 6.dp.toPx()), Size(bw, 6.dp.toPx()), CornerRadius(3.dp.toPx()))
                } else {
                    val bh = size.height * (h.coerceAtMost(maxH) / maxH).toFloat()
                    val col = if (h >= SLEEP_TARGET_H) tint else tint.copy(alpha = 0.45f)
                    drawRoundRect(col, Offset(x, size.height - bh), Size(bw, bh), CornerRadius(8.dp.toPx()))
                }
            }
            drawLine(ink.copy(alpha = 0.35f), Offset(0f, goalY), Offset(size.width, goalY), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
        }
        Text(b.tip, style = Type.small.copy(color = c.inkSoft))
    }
}

/** رحلة ١٢ أسبوع: ١٢ نقطة، كل نقطة تمتلي بقد دروس أسبوعها. */
@Composable
private fun JourneyCard(read: Int) {
    val c = Sanad.colors
    val total = LESSONS.size
    val week = (read / 7 + 1).coerceAtMost(12)
    Column(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("رحلة ١٢ أسبوع", style = Type.h2.copy(color = c.ink), modifier = Modifier.weight(1f))
            Text("${ar(read)} من ${ar(total)} درس", style = Type.small.copy(color = c.inkSoft))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            (0 until 12).forEach { i ->
                val f = ((read - i * 7) / 7f).coerceIn(0f, 1f)
                Box(Modifier.weight(1f).height(10.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f))) {
                    Box(Modifier.fillMaxWidth(f).fillMaxHeight().clip(CircleShape).background(c.sky))
                }
            }
        }
        Text(
            if (read >= total) "خلصت الرحلة كاملة. العادات صارت مالتك."
            else "الأسبوع ${ar(week)}: ${WEEK_THEMES[week - 1]}",
            style = Type.small.copy(color = c.sky),
        )
    }
}
