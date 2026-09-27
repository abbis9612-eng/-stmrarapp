package app.sanad.coach.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.sanad.coach.data.AppStore
import app.sanad.coach.ui.components.BtnStyle
import app.sanad.coach.ui.components.Ico
import app.sanad.coach.ui.components.NightCard
import app.sanad.coach.ui.components.Moon
import app.sanad.coach.ui.components.HandNote
import app.sanad.coach.ui.components.Wordmark
import app.sanad.coach.ui.components.glass
import app.sanad.coach.ui.components.Note
import app.sanad.coach.ui.components.SButton
import app.sanad.coach.ui.components.SCard
import app.sanad.coach.ui.components.SChip
import app.sanad.coach.ui.components.SIcon
import app.sanad.coach.ui.components.Stat
import app.sanad.coach.ui.components.press
import app.sanad.coach.ui.theme.Sanad
import app.sanad.coach.ui.theme.Type
import app.sanad.core.Activity
import app.sanad.core.Barrier
import app.sanad.core.IfThen
import app.sanad.core.Pace
import app.sanad.core.Profile
import app.sanad.core.SafetyFlag
import app.sanad.core.Sex
import androidx.compose.ui.unit.sp
import app.sanad.core.ar
import app.sanad.core.Forecast
import app.sanad.core.forecast
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import app.sanad.core.assessSafety
import app.sanad.core.bmi
import app.sanad.core.computeTargets
import app.sanad.core.parseNum
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.roundToInt

private val WHY = listOf("أتحرك بخفة مع أطفالي", "صحتي وتحاليلي", "ثقتي بنفسي", "ألبس ما أحب", "طاقة أكثر في العمل", "مناسبة قريبة")
private val BARRIERS = listOf(
    Barrier.TIME to "ليس عندي وقت", Barrier.ENERGY to "طاقتي منخفضة دائماً", Barrier.NIGHT to "أكل الليل",
    Barrier.SOCIAL to "العزائم والخروجات", Barrier.STRESS to "آكل عندما أتضايق", Barrier.SWEETS to "الحلويات والسكريات",
)
private val FLAGS = listOf(
    SafetyFlag.DIABETES_MEDS to "آخذ أدوية للسكري", SafetyFlag.GLP1 to "آخذ إبر تنحيف (GLP-1)", SafetyFlag.HEART to "قلب أو ضغط غير منضبط",
    SafetyFlag.PREGNANT to "حامل أو مرضع", SafetyFlag.EATING_DISORDER to "لدي تاريخ مع اضطراب الأكل",
)
private val STARTER_RULES = mapOf(
    Barrier.TIME to ("إذا لم يكن عندي وقت" to "أتمرن دقيقتين فقط وأسجّل وجبة واحدة"),
    Barrier.ENERGY to ("إذا استيقظت متعباً" to "أختار طاقة منخفضة وأكتفي بخطة الحد الأدنى"),
    Barrier.NIGHT to ("إذا جعت بعد الساعة ٩" to "أشرب شاياً أو ماءً، وإن استمر الجوع آكل زبادي"),
    Barrier.SOCIAL to ("إذا كانت عندي عزيمة" to "آكل بروتيناً خفيفاً قبلها وآخذ صحناً واحداً"),
    Barrier.STRESS to ("إذا تضايقت واشتهيت الأكل" to "أمشي ٥ دقائق أو أكلّم سند قبل أن آكل"),
    Barrier.SWEETS to ("إذا اشتهيت الحلو" to "آخذ قطعة صغيرة بعد وجبة فيها بروتين وأسجّلها"),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(store: AppStore, demo: Boolean = false, onDone: () -> Unit) {
    val c = Sanad.colors
    // demo: نسخة المطوّر فقط، تفتح «شاشة الصراحة» ببيانات جاهزة للقطات الآلية
    var step by rememberSaveable { mutableIntStateOf(if (demo) HONEST_STEP else 0) }
    var name by rememberSaveable { mutableStateOf(if (demo) "سارة" else "") }
    var sex by rememberSaveable { mutableStateOf(if (demo) Sex.F else null) }
    var age by rememberSaveable { mutableStateOf(if (demo) "34" else "") }
    var height by rememberSaveable { mutableStateOf(if (demo) "162" else "") }
    var weight by rememberSaveable { mutableStateOf(if (demo) "91" else "") }
    var goal by rememberSaveable { mutableStateOf(if (demo) "80" else "") }
    var pace by rememberSaveable { mutableStateOf(Pace.STEADY) }
    var activity by rememberSaveable { mutableStateOf(Activity.SEDENTARY) }
    var why by rememberSaveable { mutableStateOf("") }
    var barriers by rememberSaveable { mutableStateOf(listOf<Barrier>()) }
    var flags by rememberSaveable { mutableStateOf(listOf<SafetyFlag>()) }

    val a = parseNum(age)?.toInt() ?: 0
    val h = parseNum(height) ?: 0.0
    val w = parseNum(weight) ?: 0.0
    val g = parseNum(goal) ?: 0.0
    val basicsOk = name.isNotBlank() && sex != null && a in 10..90 && h in 130.0..220.0 && w in 40.0..300.0
    val minGoal = if (h > 0) ceil(20 * (h / 100) * (h / 100)) else 0.0
    val milestone = (w * 0.93).roundToInt().toDouble()
    val goalOk = g >= minGoal && g < w

    val profile = if (basicsOk) Profile(
        name = name.trim(), sex = sex!!, age = a, heightCm = h, startWeightKg = w, goalWeightKg = if (goalOk) g else milestone,
        activity = activity, pace = pace, why = why, barriers = barriers,
        ifThens = barriers.take(3).mapIndexed { i, b -> STARTER_RULES.getValue(b).let { (x, y) -> IfThen("starter-$i", x, y) } },
        flags = flags, createdAt = LocalDate.now().toString(),
    ) else null
    val safety = profile?.let { assessSafety(a, w, h, flags) }
    val targets = profile?.let { computeTargets(it, w) }
    val canNext = listOf(true, basicsOk, goalOk, true, true, true, profile != null && safety?.block != true)[step]
    val section = STEP_SECTION[step]
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Column(Modifier.fillMaxSize().imePadding().padding(top = top).navigationBarsPadding()) {
        if (step > 0) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(12.dp)).press({ step-- }),
                    contentAlignment = Alignment.Center,
                ) { SIcon(Ico.BACK, description = "رجوع") }
                Spacer(Modifier.width(12.dp))
                // شريط بأقسام مسمّاة: المستخدم يعرف وين هو وكم باقي
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SECTIONS.forEachIndexed { i, label ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(if (i <= section) c.primary else c.line))
                            Spacer(Modifier.height(4.dp))
                            Text(label, style = Type.label.copy(fontSize = 11.sp, color = if (i == section) c.ink else c.faint))
                        }
                    }
                }
            }
        }
        AnimatedContent(
            step, Modifier.weight(1f),
            transitionSpec = {
                val fwd = targetState > initialState
                (slideInHorizontally(tween(320)) { if (fwd) -it / 5 else it / 5 } + fadeIn()) togetherWith (slideOutHorizontally(tween(260)) { if (fwd) it / 5 else -it / 5 } + fadeOut())
            },
            label = "onboarding",
        ) { s ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when (s) {
                    0 -> Welcome()
                    1 -> {
                        Text("لنتعرّف عليك", style = Type.h1.copy(color = c.ink))
                        SField(name, { name = it }, "بماذا نناديك؟", Modifier.fillMaxWidth())
                        Text("الجنس (يؤثر في حساب الحرق)", style = Type.small.copy(color = c.inkSoft))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SChip("ذكر", sex == Sex.M, { sex = Sex.M }); SChip("أنثى", sex == Sex.F, { sex = Sex.F })
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumBox("العمر", age, { age = it }, "سنة", Modifier.weight(1f))
                            NumBox("الطول", height, { height = it }, "سم", Modifier.weight(1f))
                            NumBox("الوزن", weight, { weight = it }, "كغ", Modifier.weight(1f))
                        }
                        if (w > 0 && h > 0) Text("مؤشر كتلة الجسم: ${ar(bmi(w, h))}", style = Type.small.copy(color = c.inkSoft))
                    }
                    2 -> {
                        Text("إلى أين تريد أن تصل؟", style = Type.h1.copy(color = c.ink))
                        Note("نزول ٥–١٠٪ من وزنك يحسّن السكر والضغط والمفاصل بوضوح. أول محطة مقترحة: ${ar(milestone)} كغ.")
                        NumBox("الوزن المستهدف", goal, { goal = it }, "كغ", Modifier.fillMaxWidth())
                        if (goal.isBlank()) SButton("اعتمد المحطة المقترحة", { goal = milestone.toInt().toString() }, style = BtnStyle.SOFT, small = true)
                        else if (!goalOk) Note(if (g >= w) "يجب أن يكون الهدف أقل من وزنك الحالي." else "أقل وزن صحي لطولك تقريباً ${ar(minGoal)} كغ.", alert = true)
                        Text("السرعة", style = Type.h3.copy(color = c.inkSoft))
                        listOf(Pace.GENTLE to ("هادئة" to "أسهل للاستمرار، جوع أقل"), Pace.STEADY to ("ثابتة" to "التوازن الموصى به"), Pace.BRISK to ("أسرع" to "تحتاج التزاماً أعلى بالبروتين وتمارين القوة")).forEach { (pc, txt) ->
                            val sel = pace == pc
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (sel) c.primaryTint else c.surface)
                                    .border(if (sel) 2.dp else 1.dp, if (sel) c.primary else c.line, RoundedCornerShape(18.dp)).press({ pace = pc }).padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(txt.first, style = Type.bodyStrong.copy(color = c.ink))
                                    Text(txt.second, style = Type.label.copy(color = c.inkSoft))
                                }
                                if (w > 0) Text("نحو ${ar(pc.weeklyRate * w)} كغ/أسبوع", style = Type.label.copy(color = c.primary))
                            }
                        }
                        Text("يومك العادي", style = Type.h3.copy(color = c.inkSoft))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(Activity.SEDENTARY to "جالس معظم اليوم", Activity.LIGHT to "حركة خفيفة", Activity.MODERATE to "حركة متوسطة", Activity.ACTIVE to "نشيط").forEach { (ac, l) ->
                                SChip(l, activity == ac, { activity = ac })
                            }
                        }
                    }
                    HONEST_STEP -> forecast(w, if (goalOk) g else milestone, pace, LocalDate.now().toString())?.let { HonestForecast(it) }
                    4 -> {
                        Text("لماذا هذه المرة مختلفة؟", style = Type.h1.copy(color = c.ink))
                        Text("سببك الشخصي هو ما يعيدك حين يذهب الحماس. سيذكّرك سند به في الأيام الصعبة.", style = Type.small.copy(color = c.inkSoft))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            WHY.forEach { x -> SChip(x, why == x, { why = x }) }
                        }
                        SField(if (why in WHY) "" else why, { why = it }, "أو اكتب سببك بكلماتك…", Modifier.fillMaxWidth())
                        Text("ما الذي يوقفك عادةً؟", style = Type.h3.copy(color = c.inkSoft))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BARRIERS.forEach { (b, l) -> SChip(l, b in barriers, { barriers = if (b in barriers) barriers - b else barriers + b }) }
                        }
                        if (barriers.isNotEmpty()) Text("سنجهّز لك خطة «إذا… فإني…» لكل عائق، وهي من أقوى أدوات تغيير السلوك.", style = Type.small.copy(color = c.primary))
                    }
                    5 -> {
                        Text("سلامتك أولاً", style = Type.h1.copy(color = c.ink))
                        Text("اختر ما ينطبق عليك (أو تجاوز). سنعدّل الخطة بناءً عليه.", style = Type.small.copy(color = c.inkSoft))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FLAGS.forEach { (f, l) -> SChip(l, f in flags, { flags = if (f in flags) flags - f else flags + f }) }
                        }
                        safety?.notes?.forEach { Note(it, alert = safety.block) }
                    }
                    else -> if (profile != null && targets != null) {
                        FirstCrescent()
                        Text("خطتك جاهزة يا ${profile.name}", style = Type.h1.copy(color = c.ink))
                        if (safety?.block == true) safety.notes.forEach { Note(it, alert = true) } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Stat(ar(targets.kcal), "سعرة يومياً", Modifier.weight(1f))
                                Stat(ar(targets.protein), "غ بروتين", Modifier.weight(1f))
                                Stat(ar(targets.steps), "خطوة", Modifier.weight(1f))
                            }
                            Text("حرقك التقديري ${ar(targets.tdee)} سعرة. بعد أسبوعين من التسجيل يتعلّم سند حرقك الحقيقي ويعدّل الهدف تلقائياً." + if (targets.floorApplied) " ثبّتنا الهدف عند الحد الأدنى الآمن." else "", style = Type.small.copy(color = c.inkSoft))
                            HandNote("قاعدة واحدة فقط: كل صباح أخبرني بطاقتك، فتصغر الخطة أو تكبر على قدرك. يوم التعب يكفيه دقيقتان. المهم ألّا يمرّ يومان فارغان متتاليان.")
                            if (profile.ifThens.isNotEmpty()) SCard {
                                Text("خططك الجاهزة", style = Type.h3.copy(color = c.ink))
                                profile.ifThens.forEach { r -> Text("${r.whenText} ← ${r.thenText}", style = Type.small.copy(color = c.ink)) }
                            }
                            safety?.notes?.forEach { Note(it) }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        Box(Modifier.padding(16.dp)) {
            when (step) {
                0 -> SButton("لنبدأ — ٣ دقائق", { step = 1 }, Modifier.fillMaxWidth(), style = BtnStyle.GOLD, icon = Ico.SPARK)
                6 -> SButton("ابدأ ليلتي الأولى", { profile?.let { store.saveProfile(it); onDone() } }, Modifier.fillMaxWidth(), enabled = canNext, style = BtnStyle.GOLD)
                else -> SButton(
                    when (step) { 5 -> "اعرض خطتي"; HONEST_STEP -> "مفهوم، أكمل"; else -> "التالي" },
                    { step++ }, Modifier.fillMaxWidth(), enabled = canNext,
                )
            }
        }
    }
}

private val SECTIONS = listOf("أنت", "هدفك", "عوائقك", "سلامتك", "قمرك")

/** بعد الهدف مباشرة: متى تصل تقريباً، بصراحة. */
private const val HONEST_STEP = 3

/** القسم الظاهر في الشريط لكل خطوة (الترحيب بلا قسم، والصراحة جزء من «هدفك»). */
private val STEP_SECTION = listOf(-1, 0, 1, 1, 2, 3, 4)

private val AR_DIGITS = "٠١٢٣٤٥٦٧٨٩"
private fun arYear(y: Int) = y.toString().map { AR_DIGITS[it - '0'] }.joinToString("")
private fun monthAr(d: LocalDate) = d.month.getDisplayName(TextStyle.FULL, Locale("ar"))

/** «بين فبراير وأبريل ٢٠٢٧»، أو بسنتين إذا اختلفت السنة. */
private fun rangeText(a: LocalDate, b: LocalDate): String = when {
    a.year == b.year && a.month == b.month -> "في ${monthAr(a)} ${arYear(a.year)}"
    a.year == b.year -> "بين ${monthAr(a)} و${monthAr(b)} ${arYear(b.year)}"
    else -> "بين ${monthAr(a)} ${arYear(a.year)} و${monthAr(b)} ${arYear(b.year)}"
}

/**
 * شاشة الصراحة: النزول ليس خطاً مستقيماً. مسار متوقّع يتموّج داخل شريط التذبذب الطبيعي،
 * ومنطقة الوصول مدى زمني لا تاريخ واحد. الأحدث يمين (قراءة عربية).
 */
@Composable
private fun HonestForecast(f: Forecast) {
    val c = Sanad.colors
    val today = LocalDate.now()
    val earliest = LocalDate.parse(f.earliest)
    val latest = LocalDate.parse(f.latest)
    val draw = remember { Animatable(0f) }
    LaunchedEffect(Unit) { draw.animateTo(1f, tween(1600, easing = FastOutSlowInEasing)) }
    Text("بصراحة", style = Type.label.copy(color = c.sky, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
    Text("النزول ليس خطاً مستقيماً", style = Type.h1.copy(color = c.ink))
    Text(
        "بعض الأسابيع يثبت فيها الميزان أو يصعد قليلاً: ماء، ملح، قلة نوم. سند ينظر إلى الاتجاه، لا إلى رقم اليوم.",
        style = Type.body.copy(color = c.inkSoft),
    )
    Column(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp)).padding(16.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "من ${ar(f.startKg)} إلى ${ar(f.goalKg)} كغ، ${rangeText(earliest, latest)}"
            },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text("الآن ${ar(f.startKg)}", style = Type.bodyStrong.copy(color = c.ink), modifier = Modifier.weight(1f))
            Text("${ar(f.goalKg)} كغ", style = Type.bodyStrong.copy(color = c.saffron))
        }
        val band = Color(0xFFDCE8F3)
        val goalZone = c.amberTint
        Canvas(Modifier.fillMaxWidth().height(170.dp)) {
            val pad = 10.dp.toPx()
            val bandKg = 0.9
            val top = f.startKg + bandKg + 0.3
            val bottom = f.goalKg - bandKg - 0.3
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
            // شريط التذبذب الطبيعي حول الاتجاه
            val area = Path().apply {
                for (i in 0..n) { val t = tMid * i / n; if (i == 0) moveTo(x(t), y(trend(t) + bandKg)) else lineTo(x(t), y(trend(t) + bandKg)) }
                for (i in n downTo 0) { val t = tMid * i / n; lineTo(x(t), y(trend(t) - bandKg)) }
                close()
            }
            drawPath(area, band)
            // منطقة الوصول: من أسرع أسبوع إلى أبطأه
            drawRoundRect(
                goalZone, Offset(x(1f), y(f.goalKg + bandKg * 0.8)), Size(x(tFast) - x(1f), y(f.goalKg - bandKg * 0.8) - y(f.goalKg + bandKg * 0.8)),
                CornerRadius(10.dp.toPx()),
            )
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
        Row(Modifier.fillMaxWidth()) {
            Text("${monthAr(today)} ${arYear(today.year)}", style = Type.label.copy(color = c.inkSoft), modifier = Modifier.weight(1f))
            Text(rangeText(earliest, latest), style = Type.label.copy(color = c.saffron, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
        }
    }
    Text(
        "تقدير على نزول ${ar(f.slowKgWeek, 2)}–${ar(f.fastKgWeek, 2)} كغ في الأسبوع. الشريط الأزرق هو المدى الطبيعي للتذبذب، والمنطقة الصفراء هي وقت الوصول المتوقَّع.",
        style = Type.small.copy(color = c.faint),
    )
}

/** «هلالك الأول»: القمر يولد أمامك، وكل يوم تسجّله يضيف له ليلة. */
@Composable
private fun FirstCrescent() {
    val c = Sanad.colors
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { grow.animateTo(0.08f, tween(1400, easing = FastOutSlowInEasing)) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.nightSky).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Moon(72.dp, phase = grow.value, night = true, description = "هلالك الأول")
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text("هلالك الأول", style = Type.h2.copy(color = Color.White))
            Text("كل يوم تسجّل فيه يضيف ليلة. بعد ٣٠ ليلة يكتمل بدرك.", style = Type.small.copy(color = Color(0xFFC9D3E0)))
        }
    }
}

@Composable
private fun NumBox(label: String, value: String, onChange: (String) -> Unit, unit: String, modifier: Modifier = Modifier) {
    val c = Sanad.colors
    Column(modifier) {
        Text(label, style = Type.label.copy(color = c.inkSoft))
        Spacer(Modifier.height(4.dp))
        SField(value, onChange, unit, Modifier.fillMaxWidth(), number = true)
    }
}

@Composable
private fun Welcome() {
    val c = Sanad.colors
    val grow = remember { Animatable(0.04f) }
    LaunchedEffect(Unit) { grow.animateTo(0.3f, tween(1600, easing = FastOutSlowInEasing)) }
    Spacer(Modifier.height(12.dp))
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Moon(110.dp, phase = grow.value)
        Spacer(Modifier.height(14.dp))
        Wordmark(72.sp)
        Text("مدرّب تنحيف يمشي على قدر طاقتك.", style = Type.body.copy(color = c.inkSoft), textAlign = TextAlign.Center)
    }
    Spacer(Modifier.height(10.dp))
    listOf(
        Triple(Ico.SPARK, "أخبرنا بطاقتك، فتأتي الخطة على قدرها", "متعب؟ دقيقتان تكفيان. نشيط؟ نبني العضل."),
        Triple(Ico.CAMERA, "صوّر صحنك أو اكتبه بجملة", "«تغدّيت كبسة دجاج وزبادي» وسند يحسبها لك."),
        Triple(Ico.MOVE, "تمارين تراها تتحرك", "كل تمرين برسم متحرك، بلا أدوات، ولطيف على الركبتين."),
        Triple(Ico.FLAME, "كل يوم تسجّله يكبر قمرك", "يوم واحد فائت لا يطفئه. نعود غداً بلا لوم."),
    ).forEach { (icon, t, s) ->
        Row(Modifier.fillMaxWidth().glass(RoundedCornerShape(18.dp)).padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(c.primaryTint), contentAlignment = Alignment.Center) {
                SIcon(icon, size = 20.dp, tint = c.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(t, style = Type.bodyStrong.copy(color = c.ink))
                Text(s, style = Type.small.copy(color = c.inkSoft))
            }
        }
    }
    Text("بياناتك تبقى على جهازك. سند مدرّب سلوكي، وليس بديلاً عن الطبيب.", style = Type.label.copy(color = c.faint), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
}
