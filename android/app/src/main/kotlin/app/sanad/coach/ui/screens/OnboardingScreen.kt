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
import app.sanad.coach.ui.components.Note
import app.sanad.coach.ui.components.SButton
import app.sanad.coach.ui.components.SCard
import app.sanad.coach.ui.components.SChip
import app.sanad.coach.ui.components.SIcon
import app.sanad.coach.ui.components.SaduBand
import app.sanad.coach.ui.components.SaduLogo
import app.sanad.coach.ui.components.SaduWeave
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
import app.sanad.core.WeaveCell
import app.sanad.core.ar
import app.sanad.core.assessSafety
import app.sanad.core.bmi
import app.sanad.core.computeTargets
import app.sanad.core.parseNum
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.roundToInt

private val WHY = listOf("أتحرك بخفة مع عيالي", "صحتي وتحاليلي", "ثقتي بنفسي", "ألبس اللي أحبه", "طاقة أكثر بالدوام", "مناسبة قريبة")
private val BARRIERS = listOf(
    Barrier.TIME to "ما عندي وقت", Barrier.ENERGY to "طاقتي دايماً تحت", Barrier.NIGHT to "أكل الليل",
    Barrier.SOCIAL to "العزايم والطلعات", Barrier.STRESS to "آكل لما أتضايق", Barrier.SWEETS to "الحلا والسكريات",
)
private val FLAGS = listOf(
    SafetyFlag.DIABETES_MEDS to "آخذ أدوية سكري", SafetyFlag.GLP1 to "آخذ إبر تنحيف (GLP-1)", SafetyFlag.HEART to "قلب أو ضغط غير منضبط",
    SafetyFlag.PREGNANT to "حامل أو مرضع", SafetyFlag.EATING_DISORDER to "عندي تاريخ اضطراب أكل",
)
private val STARTER_RULES = mapOf(
    Barrier.TIME to ("إذا ما عندي وقت" to "أسوي تمرين الدقيقتين وأسجل وجبة وحدة بس"),
    Barrier.ENERGY to ("إذا صحيت تعبان" to "أختار طاقة منخفضة وأكتفي بخطة الحد الأدنى"),
    Barrier.NIGHT to ("إذا جاني جوع بعد الساعة ٩" to "أشرب شاي أو ماء، وإذا استمر آكل زبادي يوناني"),
    Barrier.SOCIAL to ("إذا عندي عزيمة" to "آكل بروتين خفيف قبلها وآخذ صحن واحد"),
    Barrier.STRESS to ("إذا تضايقت وجاني اشتهاء" to "أمشي ٥ دقائق أو أكلم سند قبل ما آكل"),
    Barrier.SWEETS to ("إذا اشتهيت حلا" to "آخذ قطعة صغيرة بعد وجبة فيها بروتين وأسجلها"),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(store: AppStore, onDone: () -> Unit) {
    val c = Sanad.colors
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var sex by rememberSaveable { mutableStateOf<Sex?>(null) }
    var age by rememberSaveable { mutableStateOf("") }
    var height by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var goal by rememberSaveable { mutableStateOf("") }
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
    val canNext = listOf(true, basicsOk, goalOk, true, true, profile != null && safety?.block != true)[step]
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Column(Modifier.fillMaxSize().imePadding().padding(top = top).navigationBarsPadding()) {
        if (step > 0) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(c.surface2).press({ step-- }), contentAlignment = Alignment.Center) {
                    SIcon(Ico.BACK, description = "رجوع")
                }
                Spacer(Modifier.width(12.dp))
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(5) { i -> Box(Modifier.weight(1f).height(5.dp).clip(CircleShape).background(if (i < step) c.sadu else c.line)) }
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
                        Text("نتعرف عليك", style = Type.h1.copy(color = c.ink))
                        SField(name, { name = it }, "وش نناديك؟", Modifier.fillMaxWidth())
                        Text("الجنس (يأثر على حساب الحرق)", style = Type.small.copy(color = c.inkSoft))
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
                        Text("وين تبي توصل؟", style = Type.h1.copy(color = c.ink))
                        Note("نزول ٥–١٠٪ من وزنك يحسّن السكر والضغط والمفاصل بوضوح. أول محطة مقترحة: ${ar(milestone)} كغ.")
                        NumBox("الوزن المستهدف", goal, { goal = it }, "كغ", Modifier.fillMaxWidth())
                        if (goal.isBlank()) SButton("خذ المحطة المقترحة", { goal = milestone.toInt().toString() }, style = BtnStyle.SOFT, small = true)
                        else if (!goalOk) Note(if (g >= w) "الهدف لازم يكون أقل من وزنك الحالي." else "أقل وزن صحي لطولك تقريباً ${ar(minGoal)} كغ.", alert = true)
                        Text("السرعة", style = Type.h3.copy(color = c.inkSoft))
                        listOf(Pace.GENTLE to ("هادئة" to "أسهل للاستمرار، جوع أقل"), Pace.STEADY to ("ثابتة" to "التوازن الموصى به"), Pace.BRISK to ("أسرع" to "تحتاج التزام أعلى بالبروتين والقوة")).forEach { (pc, txt) ->
                            val sel = pace == pc
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (sel) c.dateSoft else c.surface)
                                    .border(1.5.dp, if (sel) c.date else c.line, RoundedCornerShape(18.dp)).press({ pace = pc }).padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(txt.first, style = Type.bodyStrong.copy(color = c.ink))
                                    Text(txt.second, style = Type.label.copy(color = c.inkSoft))
                                }
                                if (w > 0) Text("حوالي ${ar(pc.weeklyRate * w)} كغ/أسبوع", style = Type.label.copy(color = c.sadu))
                            }
                        }
                        Text("يومك العادي", style = Type.h3.copy(color = c.inkSoft))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(Activity.SEDENTARY to "جالس أغلب اليوم", Activity.LIGHT to "حركة خفيفة", Activity.MODERATE to "حركة متوسطة", Activity.ACTIVE to "نشيط").forEach { (ac, l) ->
                                SChip(l, activity == ac, { activity = ac })
                            }
                        }
                    }
                    3 -> {
                        Text("ليش هالمرة غير؟", style = Type.h1.copy(color = c.ink))
                        Text("السبب الشخصي هو اللي يرجعك لما يروح الحماس. سند بيذكّرك فيه بالأيام الصعبة.", style = Type.small.copy(color = c.inkSoft))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            WHY.forEach { x -> SChip(x, why == x, { why = x }) }
                        }
                        SField(if (why in WHY) "" else why, { why = it }, "أو اكتب سببك بكلامك…", Modifier.fillMaxWidth())
                        Text("وش اللي يوقفك عادة؟", style = Type.h3.copy(color = c.inkSoft))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BARRIERS.forEach { (b, l) -> SChip(l, b in barriers, { barriers = if (b in barriers) barriers - b else barriers + b }) }
                        }
                        if (barriers.isNotEmpty()) Text("بنجهز لك خطة \"إذا… فأنا…\" لكل عائق — من أقوى أدوات تغيير السلوك.", style = Type.small.copy(color = c.palm))
                    }
                    4 -> {
                        Text("سلامتك أول", style = Type.h1.copy(color = c.ink))
                        Text("اختر اللي ينطبق عليك (أو تجاوز). نعدّل الخطة على أساسه.", style = Type.small.copy(color = c.inkSoft))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FLAGS.forEach { (f, l) -> SChip(l, f in flags, { flags = if (f in flags) flags - f else flags + f }) }
                        }
                        safety?.notes?.forEach { Note(it, alert = safety.block) }
                    }
                    else -> if (profile != null && targets != null) {
                        Text("خطتك جاهزة، ${profile.name}", style = Type.h1.copy(color = c.ink))
                        if (safety?.block == true) safety.notes.forEach { Note(it, alert = true) } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Stat(ar(targets.kcal), "سعرة يومياً", Modifier.weight(1f))
                                Stat(ar(targets.protein), "غ بروتين", Modifier.weight(1f))
                                Stat(ar(targets.steps), "خطوة", Modifier.weight(1f))
                            }
                            Text("حرقك التقديري ${ar(targets.tdee)} سعرة. بعد أسبوعين من التسجيل، سند يتعلم حرقك الحقيقي ويعدّل الهدف تلقائياً." + if (targets.floorApplied) " ثبّتنا الهدف عند الحد الأدنى الآمن." else "", style = Type.small.copy(color = c.inkSoft))
                            NightCard {
                                Text("قاعدة سند الوحيدة", style = Type.h2.copy(color = Color.White))
                                Text("كل صباح تقول طاقتك ووقتك، والخطة تصغر أو تكبر على قدّك. يوم التعب = دقيقتين. المهم ما يصير عندك يوم صفر مرتين ورا بعض.", style = Type.body.copy(color = c.onNightSoft))
                            }
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
                0 -> SButton("نبدأ — ٣ دقائق", { step = 1 }, Modifier.fillMaxWidth(), icon = Ico.SPARK)
                5 -> SButton("ابدأ يومي الأول", { profile?.let { store.saveProfile(it); onDone() } }, Modifier.fillMaxWidth(), enabled = canNext, style = BtnStyle.GOLD)
                else -> SButton(if (step == 4) "اعرض خطتي" else "التالي", { step++ }, Modifier.fillMaxWidth(), enabled = canNext)
            }
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
    val weave = remember { Animatable(0f) }
    LaunchedEffect(Unit) { weave.animateTo(1f, tween(1400, easing = FastOutSlowInEasing)) }
    Spacer(Modifier.height(8.dp))
    NightCard(shape = RoundedCornerShape(topStart = 200.dp, topEnd = 200.dp, bottomStart = 28.dp, bottomEnd = 28.dp), pad = 0.dp) {
        Column(Modifier.fillMaxWidth().padding(top = 36.dp, start = 18.dp, end = 18.dp, bottom = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            SaduLogo(size = 108.dp, progress = weave.value)
            Text("سَنَد", style = Type.hero.copy(color = Color.White))
            Text("مدرب تنحيف يمشي على قد طاقتك.", style = Type.body.copy(color = c.onNightSoft), textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            val demo = listOf(1, 1, 1, 0, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1)
            SaduWeave(demo.map { if (it == 1) WeaveCell.WOVEN else WeaveCell.HELD })
        }
        SaduBand(height = 8.dp)
    }
    listOf(
        "قل طاقتك، نعطيك خطة بحجمها" to "يوم تعبان؟ دقيقتين تكفي. يوم فل؟ نبني عضل.",
        "قل وش أكلت بجملة" to "\"تغديت كبسة ولبن\" — سند يحسبها لك.",
        "تمارين تشوفها تتحرك" to "كل تمرين برسم متحرك، بدون أدوات، ولطيف على الركب.",
        "لا تفوّت مرتين" to "يوم واحد ما يقطع خيطك. نرجع بكرة بدون تأنيب.",
    ).forEach { (t, s) ->
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.surface).padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(c.palmSoft), contentAlignment = Alignment.Center) { SIcon(Ico.CHECK, size = 18.dp, tint = c.palm) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(t, style = Type.bodyStrong.copy(color = c.ink))
                Text(s, style = Type.small.copy(color = c.inkSoft))
            }
        }
    }
    Text("بياناتك تبقى على جهازك. سند مدرب سلوكي، مو بديل عن الطبيب.", style = Type.label.copy(color = c.inkSoft), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
}
