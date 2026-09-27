package app.sanad.coach.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import app.sanad.coach.data.AppStore
import app.sanad.coach.data.targets
import app.sanad.coach.data.today
import app.sanad.coach.ui.LocalCelebration
import app.sanad.coach.ui.Routes
import app.sanad.coach.ui.components.BtnStyle
import app.sanad.coach.ui.components.Eyebrow
import app.sanad.coach.ui.components.HandNote
import app.sanad.coach.ui.components.HeroCard
import app.sanad.coach.ui.components.Ico
import app.sanad.coach.ui.components.LocalConfetti
import app.sanad.coach.ui.components.Meter
import app.sanad.coach.ui.components.Moon
import app.sanad.coach.ui.components.SButton
import app.sanad.coach.ui.components.SCard
import app.sanad.coach.ui.components.SIcon
import app.sanad.coach.ui.components.burstFrom
import app.sanad.coach.ui.components.glass
import app.sanad.coach.ui.components.press
import app.sanad.coach.ui.components.rememberBurstPoint
import app.sanad.coach.ui.theme.Sanad
import app.sanad.coach.ui.theme.Type
import app.sanad.core.AppState
import app.sanad.core.DayLog
import app.sanad.core.DayMark
import app.sanad.core.Energy
import app.sanad.core.GatheringPlan
import app.sanad.core.LapseKind
import app.sanad.core.Lesson
import app.sanad.core.Mission
import app.sanad.core.MissionKind
import app.sanad.core.RamadanPlan
import app.sanad.core.Risk
import app.sanad.core.RiskLevel
import app.sanad.core.Targets
import app.sanad.core.TimeBudget
import app.sanad.core.WEEK_THEMES
import app.sanad.core.Welcome
import app.sanad.core.addDays
import app.sanad.core.ar
import app.sanad.core.computeThread
import app.sanad.core.dayMarks
import app.sanad.core.dayMissions
import app.sanad.core.gatheringPlan
import app.sanad.core.lapseRecovery
import app.sanad.core.lapseRisk
import app.sanad.core.lessonForToday
import app.sanad.core.occasionBank
import app.sanad.core.ramadanPlan
import app.sanad.core.routineById
import app.sanad.core.skyOf
import app.sanad.core.stepsNote
import app.sanad.core.welcomeBack
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

private fun greeting(): String {
    val h = LocalTime.now().hour
    return when {
        h < 5 -> "تأخر الوقت"
        h < 12 -> "صباح الخير"
        h < 17 -> "نهارك سعيد"
        else -> "مساء الخير"
    }
}

private val DAY_LETTER = mapOf(
    DayOfWeek.SATURDAY to "س", DayOfWeek.SUNDAY to "ح", DayOfWeek.MONDAY to "ن", DayOfWeek.TUESDAY to "ث",
    DayOfWeek.WEDNESDAY to "ر", DayOfWeek.THURSDAY to "خ", DayOfWeek.FRIDAY to "ج",
)

private fun arabicDate(d: LocalDate): String {
    val loc = Locale.forLanguageTag("ar")
    return "${d.dayOfWeek.getDisplayName(TextStyle.FULL, loc)}، ${ar(d.dayOfMonth)} ${d.month.getDisplayName(TextStyle.FULL, loc)}"
}

@Composable
fun TodayScreen(store: AppStore, state: AppState, nav: NavHostController) {
    val p = state.profile ?: return
    val t = state.targets() ?: return
    val c = Sanad.colors
    val day = state.today()
    val todayKey = day.date
    val thread = computeThread(state.days, todayKey)
    val sky = skyOf(state.days, todayKey, p.createdAt)
    val rise = rememberRise()
    val celebration = LocalCelebration.current
    var kick by remember { mutableIntStateOf(0) }
    var lapseOpen by rememberSaveable { mutableStateOf(false) }

    // إذن التنبيهات (أندرويد ١٣+): نطلبه مرة وحدة بعد ما يدخل يومه الأول
    val context = LocalContext.current
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { app.sanad.coach.notify.Reminders.schedule(context) }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
        else app.sanad.coach.notify.Reminders.schedule(context)
    }

    val e = day.energy ?: Energy.MID
    val missions = dayMissions(e, timeFor(e), t, p)
    fun toggle(m: Mission) {
        val wasDone = m.id in day.done
        store.toggleMission(m.id)
        if (!wasDone) {
            kick++
            val s = store.state.value
            val d = s.days[todayKey]
            if (d != null && d.done.count { it in setOf("move", "eat", "restore") } == 3) {
                celebration.streak = computeThread(s.days, todayKey).length
            }
        }
    }

    Page {
        // الرأس: التاريخ، التحية بخط اليد، المدرب، والقمر
        item {
            Row(Modifier.rise(rise, 0), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Eyebrow(arabicDate(LocalDate.parse(todayKey)))
                    Text("${greeting()} يا ${p.name}", style = Type.handTitle.copy(color = c.ink, fontSize = 30.sp))
                }
                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.size(50.dp).clip(RoundedCornerShape(14.dp)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(14.dp))
                            .press({ nav.navigate(Routes.COACH) }).semantics { contentDescription = "تحدّث مع سند" },
                        contentAlignment = Alignment.Center,
                    ) { SIcon(Ico.COACH, size = 23.dp) }
                    Box(
                        Modifier.size(50.dp).clip(RoundedCornerShape(14.dp)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(14.dp))
                            .press({ nav.navigate(Routes.PROGRESS) }).semantics { contentDescription = "قمرك: ${ar(sky.nights)} ليلة" },
                        contentAlignment = Alignment.Center,
                    ) { Moon(36.dp, phase = sky.phase, kick = kick) }
                }
            }
        }

        item { WeekStrip(dayMarks(state.days, todayKey, t.kcal, p.createdAt), todayKey, thread.length, Modifier.rise(rise, 1)) }

        welcomeBack(state, todayKey)?.let { w -> item(key = "welcome") { WelcomeCard(w, sky.phase) } }

        item { EnergyPicker(e, Modifier.rise(rise, 2)) { picked -> store.checkIn(picked, timeFor(picked)); kick++ } }

        if (day.sleepHours == null) item(key = "sleep-ask") { SleepAsk { h -> store.setSleep(h); kick++ } }

        val risk = lapseRisk(state, t, LocalDateTime.now())
        if (risk.level != RiskLevel.LOW) item(key = "radar") {
            RadarCard(risk, onTool = { nav.navigate(Routes.player(risk.toolRoutineId)) }, onLapse = { lapseOpen = true })
        }

        if (day.gathering) item(key = "gathering") {
            GatheringCard(gatheringPlan(t, day), onCancel = { store.setGathering(false) }, onPacer = { nav.navigate(Routes.PACER) })
        }

        // الخطوة الوحيدة المهمة الآن
        val next = missions.firstOrNull { it.kind == MissionKind.MOVE && it.id !in day.done } ?: missions.firstOrNull { it.id !in day.done }
        item(key = "hero-${next?.id}-${e.name}") {
            if (next != null) NextStep(next, Modifier.rise(rise, 3),
                onStart = { next.routineId?.let { nav.navigate(Routes.player(it)) } ?: toggle(next) },
                onSwap = { nav.navigate(Routes.MOVE) },
            ) else AllDone(Modifier.rise(rise, 3))
        }

        item {
            val tracker = app.sanad.coach.Graph.steps
            var granted by remember { mutableStateOf(tracker.hasPermission()) }
            val askSteps = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> granted = ok; if (ok) tracker.start() }
            NumbersCard(
                day, t, Modifier.rise(rise, 4),
                stepsNeedPermission = tracker.available && !granted,
                onEnableSteps = { if (Build.VERSION.SDK_INT >= 29) askSteps.launch(Manifest.permission.ACTIVITY_RECOGNITION) },
                onWater = { d -> store.addWater(d); if (d > 0) kick++ },
                onOpen = { nav.navigate(Routes.EAT) },
            )
        }

        val bank = occasionBank(state.days, todayKey, t.kcal)
        if (bank.saved >= 50 && !day.gathering) item(key = "bank") {
            BankCard(bank.saved, bank.cap) { store.setGathering(true); kick++ }
        }

        item(key = "note") {
            HandNote(
                if (thread.rescueToday) "أمس فات، وهذا عادي. اليوم خطوة واحدة فقط تحفظ سلسلتك." + if (p.why.isNotBlank()) "\nتذكّر لماذا بدأت: ${p.why}" else ""
                else coachNote(day, t),
                Modifier.rise(rise, 5),
                action = "ردّ عليه",
                onAction = { nav.navigate(Routes.COACH) },
            )
        }

        lessonForToday(state.lessonsRead, p.createdAt, todayKey, day.lesson != null)?.let { lesson ->
            item(key = "lesson-${lesson.id}") { LessonCard(lesson) { store.readLesson(lesson.id); kick++ } }
        }

        if (p.ramadan) item { RamadanCard(ramadanPlan(p, t)) }

        item {
            val doneCount = day.done.count { it in setOf("move", "eat", "restore") }
            Row(Modifier.padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (p.ramadan) "خطتك الرمضانية اليوم" else planTitle(e), style = Type.h2.copy(color = c.ink), modifier = Modifier.weight(1f))
                Text("${ar(doneCount)} من ٣", style = Type.small.copy(color = c.inkSoft))
            }
        }
        item {
            Column {
                missions.forEachIndexed { i, m ->
                    PlanRow(
                        m, done = m.id in day.done, last = i == missions.lastIndex,
                        onToggle = { toggle(m) },
                        onStart = { m.routineId?.let { nav.navigate(Routes.player(it)) } },
                    )
                }
            }
        }

        item(key = "lapse") {
            LapseCard(
                open = lapseOpen, onToggle = { lapseOpen = !lapseOpen }, targets = t,
                gathering = day.gathering, onGathering = { store.setGathering(true); kick++ },
                onLog = { kind -> store.logLapse(kind.name); kick++ },
            )
        }
    }
}

/** الوقت المتاح للحركة يتبع الطاقة: متعب دقيقتين، عادي ١٠، نشيط ٢٠. */
private fun timeFor(e: Energy) = when (e) {
    Energy.LOW -> TimeBudget.TWO
    Energy.MID -> TimeBudget.TEN
    Energy.HIGH -> TimeBudget.TWENTY
}

private fun planTitle(e: Energy) = when (e) {
    Energy.LOW -> "خطة الطاقة القليلة"
    Energy.MID -> "خطتك اليوم"
    Energy.HIGH -> "يوم الطاقة العالية"
}

/** ملاحظة سند اليومية: جملة واحدة عملية حسب ما سجّلته حتى الآن. */
private fun coachNote(d: DayLog, t: Targets): String {
    val left = t.kcal - d.intake
    return when {
        d.intake == 0 -> "ابدأ يومك بوجبة فيها بروتين: بيضتان أو زبادي أو فول. تشبعك لوقت أطول."
        d.protein < t.protein * 0.35 && d.intake > t.kcal * 0.4 -> "بروتينك قليل حتى الآن. اجعل وجبتك القادمة فيها دجاج أو سمك أو بيض."
        left > 250 -> "متبقٍ لك ${ar(left)} سعرة. صحن متوازن: نصفه خضار، وربعه بروتين، وربعه أرز أو خبز."
        left >= 0 -> "أنت قريب من خطتك اليوم. عشاء خفيف ومشية قصيرة بعده ويكتمل يومك."
        else -> "تجاوزت خطتك قليلاً، وهذا يحدث. مشية ١٥ دقيقة بعد الأكل تساعد، وغداً يوم جديد."
    }
}

/* ------------------------------ شريط الأسبوع ------------------------------ */

@Composable
private fun WeekStrip(marks: List<DayMark>, today: String, streak: Int, modifier: Modifier) {
    val c = Sanad.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            // الأقدم يمين (بداية السطر في العربي)
            marks.forEachIndexed { i, mark ->
                val date = LocalDate.parse(addDays(today, (i - 6).toLong()))
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (mark == DayMark.TODAY) "اليوم" else DAY_LETTER.getValue(date.dayOfWeek),
                        style = Type.label.copy(color = if (mark == DayMark.TODAY) c.primary else c.inkSoft, fontSize = 12.sp, fontWeight = if (mark == DayMark.TODAY) FontWeight.Bold else FontWeight.Medium),
                    )
                    DayRing(mark, ar(date.dayOfMonth))
                }
            }
        }
        Text(
            "سلسلتك ${ar(streak)} يوم · أخضر ضمن خطتك، أصفر زيادة بسيطة، بنفسجي مناسبة، منقّط لم تسجّل",
            style = Type.label.copy(color = c.inkSoft, fontSize = 12.sp),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DayRing(mark: DayMark, number: String) {
    val c = Sanad.colors
    val size = 40.dp
    val label = when (mark) {
        DayMark.ON_PLAN -> "ضمن الخطة"
        DayMark.OVER -> "زيادة بسيطة"
        DayMark.OCCASION -> "مناسبة"
        DayMark.EMPTY -> "لم تسجّل"
        DayMark.TODAY -> "اليوم"
        DayMark.BEFORE -> "قبل البداية"
    }
    Box(Modifier.size(size).semantics { contentDescription = "$number: $label" }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val sw = 3.dp.toPx()
            val r = this.size.minDimension / 2 - sw / 2
            when (mark) {
                DayMark.TODAY -> drawCircle(c.primary, this.size.minDimension / 2)
                DayMark.ON_PLAN -> drawCircle(c.primary, r, style = Stroke(sw))
                DayMark.OVER -> drawCircle(c.amber, r, style = Stroke(sw))
                DayMark.OCCASION -> { drawCircle(c.violet, r, style = Stroke(sw)); drawCircle(c.violet, r - sw * 1.6f, style = Stroke(sw * 0.45f)) }
                DayMark.EMPTY -> drawCircle(c.faint, r, style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))))
                DayMark.BEFORE -> drawCircle(c.surface2, this.size.minDimension / 2)
            }
        }
        Text(
            number,
            style = Type.label.copy(
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                color = when (mark) { DayMark.TODAY -> c.onPrimary; DayMark.EMPTY, DayMark.BEFORE -> c.inkSoft; else -> c.ink },
            ),
        )
    }
}

/* ------------------------------ الطاقة ------------------------------ */

@Composable
private fun EnergyPicker(energy: Energy, modifier: Modifier, onPick: (Energy) -> Unit) {
    val c = Sanad.colors
    val labels = listOf(Energy.LOW to "متعب", Energy.MID to "عادي", Energy.HIGH to "نشيط")
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("كيف طاقتك الآن؟", style = Type.body.copy(color = c.inkSoft), modifier = Modifier.padding(horizontal = 4.dp))
        Row(Modifier.fillMaxWidth().semantics { contentDescription = "طاقتي اليوم" }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            labels.forEach { (en, label) ->
                val sel = energy == en
                val bg by animateColorAsState(if (sel) c.primaryTint else c.surface, label = "e-bg")
                Row(
                    Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(14.dp)).background(bg)
                        .border(if (sel) 2.dp else 1.dp, if (sel) c.primary else c.line, RoundedCornerShape(14.dp))
                        .press({ if (!sel) onPick(en) }, role = Role.RadioButton)
                        .semantics { selected = sel; stateDescription = if (sel) "مختار" else "" },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    app.sanad.coach.ui.components.Battery(en.level, Modifier.graphicsLayer { scaleX = 0.72f; scaleY = 0.72f }, color = c.ink)
                    Spacer(Modifier.width(4.dp))
                    Text(label, style = Type.body.copy(color = c.ink, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal))
                }
            }
        }
    }
}

/* ------------------------------ الخطوة التالية ------------------------------ */

@Composable
private fun NextStep(m: Mission, modifier: Modifier, onStart: () -> Unit, onSwap: () -> Unit) {
    val c = Sanad.colors
    val minutes = when {
        m.kind != MissionKind.MOVE -> null
        m.routineId != null -> routineById(m.routineId!!)?.minutes
        else -> null
    }
    HeroCard(modifier) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "خطوتك التالية" + (minutes?.let { " · ${ar(it)} دقائق" } ?: ""),
                    style = Type.label.copy(color = c.onPrimary.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold),
                )
                Text(m.title, style = Type.h1.copy(color = c.onPrimary, fontSize = 26.sp))
                Text(m.detail, style = Type.body.copy(color = c.onPrimary.copy(alpha = 0.92f)))
            }
            Spacer(Modifier.width(8.dp))
            InkFigureSquat(Modifier.size(width = 70.dp, height = 90.dp), c.onPrimary, c.amber)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(14.dp)).background(c.onPrimary).press(onStart),
                contentAlignment = Alignment.Center,
            ) { Text(if (m.routineId != null) "ابدأ الآن" else "تم", style = Type.h3.copy(color = c.primary, fontWeight = FontWeight.Bold, fontSize = 17.sp)) }
            if (m.kind == MissionKind.MOVE) Box(
                Modifier.width(96.dp).height(52.dp).clip(RoundedCornerShape(14.dp)).border(1.5.dp, c.onPrimary.copy(alpha = 0.6f), RoundedCornerShape(14.dp)).press(onSwap),
                contentAlignment = Alignment.Center,
            ) { Text("غيّرها", style = Type.body.copy(color = c.onPrimary)) }
        }
    }
}

/** رسمة حبر بسيطة لوضعية القرفصاء: الفخذ والساق بلون العضلة الشغالة مزاحة قليلاً. */
@Composable
fun InkFigureSquat(modifier: Modifier, ink: Color, accent: Color) {
    Canvas(modifier) {
        val sx = size.width / 60f; val sy = size.height / 92f
        fun o(x: Float, y: Float) = Offset(x * sx, y * sy)
        val w = 2.6f * sx
        val shift = 2f * sx
        drawLine(accent, o(22f, 58f) + Offset(shift, shift), o(42f, 64f) + Offset(shift, shift), w * 2f, StrokeCap.Round)
        drawLine(accent, o(42f, 64f) + Offset(shift, shift), o(34f, 86f) + Offset(shift, shift), w * 2f, StrokeCap.Round)
        drawCircle(ink, 6f * sx, o(34f, 30f), style = Stroke(w))
        drawLine(ink, o(32f, 37f), o(22f, 58f), w * 1.2f, StrokeCap.Round)
        drawLine(ink, o(31f, 41f), o(50f, 40f), w, StrokeCap.Round)
        drawLine(ink, o(22f, 58f), o(42f, 64f), w, StrokeCap.Round)
        drawLine(ink, o(42f, 64f), o(34f, 86f), w, StrokeCap.Round)
        drawLine(ink, o(34f, 86f), o(42f, 86f), w, StrokeCap.Round)
        drawLine(ink.copy(alpha = 0.5f), o(8f, 90f), o(52f, 90f), 1.5f * sx)
    }
}

@Composable
private fun AllDone(modifier: Modifier) {
    val c = Sanad.colors
    SCard(modifier, color = c.primary) {
        Text("خطة اليوم اكتملت", style = Type.h2.copy(color = c.ink))
        Text("أحسنت. ارتح الآن، وقمرك كبر ليلة.", style = Type.body.copy(color = c.inkSoft))
    }
}

/* ------------------------------ الأرقام ------------------------------ */

@Composable
private fun NumbersCard(
    day: DayLog, t: Targets, modifier: Modifier,
    stepsNeedPermission: Boolean, onEnableSteps: () -> Unit,
    onWater: (Int) -> Unit, onOpen: () -> Unit,
) {
    val c = Sanad.colors
    val left by animateIntAsState(t.kcal - day.intake, tween(900), label = "left")
    Column(modifier.fillMaxWidth().glass().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().press(onOpen, haptic = false), verticalAlignment = Alignment.Bottom) {
            Text(ar(if (left >= 0) left else -left), style = Type.hero.copy(fontSize = 40.sp, color = if (left >= 0) c.ink else c.saffron))
            Spacer(Modifier.width(8.dp))
            Text(
                if (left >= 0) "سعرة متبقية من ${ar(t.kcal)}" else "فوق خطتك اليوم",
                style = Type.body.copy(color = c.inkSoft), modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        Meter(day.intake / t.kcal.toFloat(), height = 12.dp, color = if (left >= 0) c.primary else c.amber)
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniStat("بروتين", ar(day.protein), " / ${ar(t.protein)}غ", day.protein / t.protein.toFloat(), c.oasis, Modifier.weight(1f).fillMaxHeight())
            Column(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(14.dp)).background(c.bg).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("ماء", style = Type.label.copy(color = c.inkSoft))
                Text(
                    "${ar(day.water)} / ${ar(t.water)}",
                    style = Type.number.copy(fontSize = 18.sp, color = c.ink),
                    modifier = Modifier.semantics { contentDescription = "ماء ${ar(day.water)} من ${ar(t.water)} أكواب" },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(9.dp)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(9.dp))
                            .press({ onWater(-1) }).semantics { contentDescription = "قلّل كوب ماء" },
                        contentAlignment = Alignment.Center,
                    ) { SIcon(Ico.MINUS, size = 16.dp) }
                    Box(
                        Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(9.dp)).background(c.sky)
                            .press({ onWater(1) }).semantics { contentDescription = "أضف كوب ماء" },
                        contentAlignment = Alignment.Center,
                    ) { SIcon(Ico.PLUS, size = 16.dp, tint = c.onPrimary) }
                }
            }
            if (stepsNeedPermission) Column(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(14.dp)).background(c.bg).press(onEnableSteps).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("خطوات", style = Type.label.copy(color = c.inkSoft))
                Text("فعّل العدّاد", style = Type.small.copy(color = c.primary, fontWeight = FontWeight.Bold))
                Text("بلا إنترنت", style = Type.label.copy(color = c.inkSoft))
            } else MiniStat("خطوات", ar(day.steps), "", day.steps / t.steps.toFloat(), c.amber, Modifier.weight(1f).fillMaxHeight())
        }
        if (!stepsNeedPermission && day.steps > 0) Text(stepsNote(day.steps, t.steps), style = Type.small.copy(color = c.inkSoft))
    }
}

@Composable
private fun MiniStat(label: String, value: String, of: String, progress: Float, color: Color, modifier: Modifier) {
    val c = Sanad.colors
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(c.bg).padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = Type.label.copy(color = c.inkSoft))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = Type.number.copy(fontSize = 18.sp, color = c.ink))
            if (of.isNotEmpty()) Text(of, style = Type.label.copy(color = c.inkSoft, fontSize = 11.sp), modifier = Modifier.padding(bottom = 2.dp))
        }
        Spacer(Modifier.weight(1f))
        Meter(progress, height = 5.dp, color = color)
    }
}

/** رصيد المناسبات: ما وفّرته هذا الأسبوع لعزومة قادمة. */
@Composable
private fun BankCard(saved: Int, cap: Int, onUse: () -> Unit) {
    val c = Sanad.colors
    Row(
        Modifier.fillMaxWidth().glass().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(c.violetTint), contentAlignment = Alignment.Center) {
            SIcon(Ico.PEOPLE, size = 24.dp, tint = c.violet)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("رصيد المناسبات", style = Type.h3.copy(color = c.ink), modifier = Modifier.weight(1f))
                Text("${ar(saved)} / ${ar(cap)}", style = Type.label.copy(color = c.violet, fontWeight = FontWeight.Bold))
            }
            Text("ما بقي تحت خطتك هذا الأسبوع محفوظ لعزومة أو مناسبة، بلا ذنب.", style = Type.small.copy(color = c.inkSoft))
            Meter(saved / cap.toFloat(), height = 6.dp, color = c.violet)
            Text(
                "عندي مناسبة اليوم",
                style = Type.small.copy(color = c.violet, fontWeight = FontWeight.Bold),
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).press(onUse).padding(vertical = 6.dp),
            )
        }
    }
}

/* ------------------------------ الخطة كخط زمني ------------------------------ */

@Composable
private fun PlanRow(m: Mission, done: Boolean, last: Boolean, onToggle: () -> Unit, onStart: () -> Unit) {
    val c = Sanad.colors
    val haptic = LocalHapticFeedback.current
    val confetti = LocalConfetti.current
    val point = rememberBurstPoint()
    val checkScale by animateFloatAsState(if (done) 1.1f else 1f, spring(dampingRatio = 0.4f, stiffness = 500f), label = "check")
    val tick by animateFloatAsState(if (done) 1f else 0f, tween(420, delayMillis = 80), label = "tick")
    val routine = m.routineId?.let(::routineById)
    val kindLabel = when (m.kind) {
        MissionKind.MOVE -> "حركة"
        MissionKind.EAT -> "أكل"
        MissionKind.RESTORE -> "راحة"
    }
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // عمود الخط الزمني: دائرة تنقر فيها لتعلّمها تمّت
        Column(Modifier.width(40.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(
                Modifier
                    .size(32.dp)
                    .burstFrom(point)
                    .graphicsLayer { scaleX = checkScale; scaleY = checkScale }
                    .press({
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (!done) confetti.burst(point.center, 36, 0.8f)
                        onToggle()
                    }, role = Role.Checkbox, haptic = false)
                    .semantics { contentDescription = if (done) "إلغاء: ${m.title}" else "تم: ${m.title}" },
            ) {
                val r = size.minDimension / 2
                if (done) drawCircle(c.primary, r) else drawCircle(c.faint, r - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
                if (tick > 0f) {
                    val p1 = Offset(size.width * 0.28f, size.height * 0.52f)
                    val p2 = Offset(size.width * 0.44f, size.height * 0.68f)
                    val p3 = Offset(size.width * 0.74f, size.height * 0.36f)
                    val k1 = (tick * 2f).coerceAtMost(1f)
                    val k2 = ((tick - 0.5f) * 2f).coerceIn(0f, 1f)
                    val sw = 3.dp.toPx()
                    drawLine(c.onPrimary, p1, p1 + (p2 - p1) * k1, sw, StrokeCap.Round)
                    if (k2 > 0f) drawLine(c.onPrimary, p2, p2 + (p3 - p2) * k2, sw, StrokeCap.Round)
                }
            }
            if (!last) Box(Modifier.width(2.dp).weight(1f).background(if (done) c.primary else c.line))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f).padding(bottom = if (last) 0.dp else 18.dp)) {
            Text(kindLabel, style = Type.label.copy(color = c.inkSoft))
            Text(m.title, style = Type.h3.copy(color = if (done) c.inkSoft else c.ink, fontWeight = FontWeight.SemiBold))
            Text(m.detail, style = Type.small.copy(color = c.inkSoft))
            if (routine != null && !done) {
                Spacer(Modifier.height(8.dp))
                SButton("ابدأ", onStart, small = true, icon = Ico.PLAY)
            }
        }
    }
}

/* ------------------------------ رمضان ------------------------------ */

@Composable
private fun RamadanCard(plan: RamadanPlan) {
    val c = Sanad.colors
    SCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(c.amberTint), contentAlignment = Alignment.Center) {
                SIcon(Ico.MOON, size = 22.dp, tint = c.saffron)
            }
            Spacer(Modifier.width(12.dp))
            Text("خطتك الرمضانية", style = Type.h2.copy(color = c.ink), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        plan.meals.forEach { m ->
            Row(
                Modifier.padding(bottom = 8.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.bg).padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(m.name, style = Type.bodyStrong.copy(color = c.ink), modifier = Modifier.weight(1f))
                Text("${ar(m.kcal)} سعرة، ${ar(m.protein)} غ بروتين", style = Type.small.copy(color = c.inkSoft))
            }
        }
        Text(plan.water, style = Type.small.copy(color = c.ink))
        Spacer(Modifier.height(6.dp))
        plan.cautions.forEach { Text(it, style = Type.label.copy(color = c.inkSoft), modifier = Modifier.padding(top = 2.dp)) }
    }
}

/* ------------------------------ الحارس ------------------------------ */

/** خطة المناسبة: قبل، الصحن، بعد — بلا حرمان وبلا تعويض. */
@Composable
private fun GatheringCard(g: GatheringPlan, onCancel: () -> Unit, onPacer: () -> Unit) {
    val c = Sanad.colors
    Column(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp), c.violet).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("خطة المناسبة", style = Type.label.copy(color = c.violet, fontWeight = FontWeight.Bold))
                Text("تذهب مرتاحاً وتعود مرتاحاً", style = Type.h2.copy(color = c.ink))
            }
            Text("ليس اليوم", style = Type.small.copy(color = c.inkSoft), modifier = Modifier.clip(RoundedCornerShape(10.dp)).press(onCancel, haptic = false).padding(10.dp))
        }
        Text(g.note, style = Type.body.copy(color = c.ink))
        listOf("قبل" to g.before, "الصحن" to g.plate, "بعد" to g.after).forEach { (title, steps) ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(title, style = Type.label.copy(color = c.violet, fontWeight = FontWeight.Bold))
                steps.forEach { Text("• $it", style = Type.small.copy(color = c.ink)) }
            }
        }
        SButton("مؤقت الأكل على مهل", onPacer, Modifier.fillMaxWidth(), style = BtnStyle.SOFT, small = true, icon = Ico.PLAY)
    }
}

/** درس اليوم من رحلة الـ١٢ أسبوعاً: دقيقة قراءة وخطوة واحدة. */
@Composable
private fun LessonCard(l: Lesson, onRead: () -> Unit) {
    val c = Sanad.colors
    Column(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp), c.sky).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("درس اليوم · الأسبوع ${ar(l.week)}: ${WEEK_THEMES[l.week - 1]}", style = Type.label.copy(color = c.sky, fontWeight = FontWeight.Bold))
        Text(l.title, style = Type.h2.copy(color = c.ink))
        Text(l.body, style = Type.body.copy(color = c.ink))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SIcon(Ico.CHECK, size = 18.dp, tint = c.oasis)
            Spacer(Modifier.width(8.dp))
            Text(l.action, style = Type.small.copy(color = c.ink), modifier = Modifier.weight(1f))
        }
        SButton("قرأته، سأجرّبه اليوم", onRead, Modifier.fillMaxWidth(), style = BtnStyle.SOFT, small = true)
    }
}

/** سؤال الصباح: كم نمت؟ ضغطة واحدة، ويغذّي الرادار وبنك النوم. */
@Composable
private fun SleepAsk(onPick: (Double) -> Unit) {
    val c = Sanad.colors
    val opts = listOf(5.0 to "٥ أو أقل", 6.0 to "٦", 7.0 to "٧", 8.0 to "٨", 9.0 to "٩+")
    Column(
        Modifier.fillMaxWidth().glass().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SIcon(Ico.MOON, size = 20.dp, tint = c.violet)
            Spacer(Modifier.width(8.dp))
            Text("كم ساعة نمت البارحة؟", style = Type.h3.copy(color = c.ink), modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            opts.forEach { (h, label) ->
                Box(
                    Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp)).background(c.bg)
                        .border(1.dp, c.line, RoundedCornerShape(12.dp)).press({ onPick(h) })
                        .semantics { contentDescription = "نمت $label ساعات" },
                    contentAlignment = Alignment.Center,
                ) { Text(label, style = Type.small.copy(color = c.ink, fontWeight = FontWeight.Medium)) }
            }
        }
    }
}

@Composable
private fun WelcomeCard(w: Welcome, phase: Float) {
    val c = Sanad.colors
    Column(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp), c.oasis).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Moon(34.dp, phase = phase)
            Spacer(Modifier.width(10.dp))
            Text(w.headline, style = Type.h2.copy(color = c.ink))
        }
        Text(w.body, style = Type.body.copy(color = c.ink))
        Text("مهمة اليوم: ${w.mission}", style = Type.small.copy(color = c.oasis, fontWeight = FontWeight.SemiBold))
    }
}

/** رادار سند: يظهر فقط حين تكون اللحظة حساسة، ويعرض خطتك أنت وأداة سريعة. */
@Composable
private fun RadarCard(r: Risk, onTool: () -> Unit, onLapse: () -> Unit) {
    val c = Sanad.colors
    val tint = if (r.level == RiskLevel.HIGH) c.rose else c.saffron
    val pulse = rememberInfiniteTransition(label = "radar")
    val ring by pulse.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "ring")
    Column(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp), tint).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(34.dp)) {
                val r0 = size.minDimension / 2
                drawCircle(tint.copy(alpha = (1f - ring) * 0.5f), r0 * (0.4f + ring * 0.6f), style = Stroke(2.dp.toPx()))
                drawCircle(tint, r0 * 0.28f)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("رادار سند", style = Type.label.copy(color = tint, fontWeight = FontWeight.Bold))
                Text(r.headline, style = Type.h3.copy(color = c.ink, fontWeight = FontWeight.Bold))
            }
        }
        r.plan?.let { plan ->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface).padding(12.dp)) {
                Text("خطتك أنت:", style = Type.label.copy(color = c.inkSoft))
                Text("${plan.whenText} ← ${plan.thenText}", style = Type.body.copy(color = c.ink))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SButton("أداة ٣ دقائق", onTool, Modifier.weight(1f), small = true, icon = Ico.PLAY)
            SButton("تعثّرت", onLapse, style = BtnStyle.SOFT, small = true)
        }
    }
}

/** «تعثّرت»: تسجيل صادق بلا حكم، وخطة رجوع فورية بدل «خلاص خربت». */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LapseCard(
    open: Boolean, onToggle: () -> Unit, targets: Targets,
    gathering: Boolean, onGathering: () -> Unit, onLog: (LapseKind) -> Unit,
) {
    val c = Sanad.colors
    var kind by rememberSaveable { mutableStateOf<LapseKind?>(null) }
    var logged by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().glass().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("حدث شيء اليوم؟", style = Type.h3.copy(color = c.ink), modifier = Modifier.weight(1f))
            if (!gathering) SButton("عندي مناسبة", onGathering, style = BtnStyle.SOFT, small = true)
            SButton(if (open) "إغلاق" else "تعثّرت", onToggle, style = BtnStyle.GHOST, small = true)
        }
        AnimatedVisibility(open, enter = fadeIn() + expandVertically()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("لا حكم هنا. اختر ما حدث، وسند يعطيك الخطوة التالية.", style = Type.small.copy(color = c.inkSoft))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LapseKind.entries.forEach { k ->
                        val sel = kind == k
                        Box(
                            Modifier.clip(RoundedCornerShape(12.dp)).background(if (sel) c.primaryTint else c.surface)
                                .border(if (sel) 2.dp else 1.dp, if (sel) c.primary else c.line, RoundedCornerShape(12.dp))
                                .press({ kind = k; logged = false }).padding(horizontal = 14.dp, vertical = 10.dp),
                        ) { Text(k.label, style = Type.small.copy(color = c.ink, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)) }
                    }
                }
                kind?.let { k ->
                    val r = lapseRecovery(k, targets)
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.primaryTint).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(r.title, style = Type.h3.copy(color = c.ink, fontWeight = FontWeight.Bold))
                        r.steps.forEach { Text("• $it", style = Type.body.copy(color = c.ink)) }
                        Text(r.reframe, style = Type.small.copy(color = c.primary))
                    }
                    if (!logged) SButton("سجّلها وأكمل يومي", { onLog(k); logged = true }, Modifier.fillMaxWidth(), style = BtnStyle.SOFT, small = true)
                    else Text("تم التسجيل. التسجيل الصادق نفسه التزام، وسلسلتك محفوظة.", style = Type.small.copy(color = c.primary))
                }
            }
        }
    }
}
