package app.sanad.coach.ui.screens

import java.time.LocalDateTime
import app.sanad.core.welcomeBack
import app.sanad.core.lapseRisk
import app.sanad.core.lapseRecovery
import app.sanad.core.Welcome
import app.sanad.core.RiskLevel
import app.sanad.core.Risk
import app.sanad.core.LapseKind
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.os.Build
import android.content.pm.PackageManager
import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import app.sanad.coach.data.AppStore
import app.sanad.coach.data.targets
import app.sanad.coach.data.today
import app.sanad.coach.ui.LocalCelebration
import app.sanad.coach.ui.Routes
import app.sanad.coach.ui.components.BtnStyle
import app.sanad.coach.ui.components.Eyebrow
import app.sanad.coach.ui.components.Ico
import app.sanad.coach.ui.components.LivingOrb
import app.sanad.coach.ui.components.LocalConfetti
import app.sanad.coach.ui.components.Note
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
import app.sanad.core.Energy
import app.sanad.core.Mission
import app.sanad.core.MissionKind
import app.sanad.core.RamadanPlan
import app.sanad.core.TimeBudget
import app.sanad.core.WeaveCell
import app.sanad.core.addDays
import app.sanad.core.ar
import app.sanad.core.computeThread
import app.sanad.core.dayMissions
import app.sanad.core.ramadanPlan
import app.sanad.core.routineById
import app.sanad.core.weaveCells
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.max

private fun greeting(): String {
    val h = LocalTime.now().hour
    return when {
        h < 5 -> "سهرانين"
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

    Page {
        item {
            Row(Modifier.rise(rise, 0), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Eyebrow(arabicDate(LocalDate.parse(todayKey)))
                    Text(greeting(), style = Type.h1.copy(color = c.ink))
                }
                Box(
                    Modifier.size(52.dp).press({ nav.navigate(Routes.COACH) }).semantics { contentDescription = "افتح المدرب" },
                    contentAlignment = Alignment.Center,
                ) { LivingOrb(46.dp, kick = kick) }
            }
        }

        welcomeBack(state, todayKey)?.let { w -> item(key = "welcome") { WelcomeCard(w) } }

        item {
            EnergyCard(day.energy ?: Energy.MID, Modifier.rise(rise, 1)) { e ->
                store.checkIn(e, timeFor(e)); kick++
            }
        }

        val risk = lapseRisk(state, t, LocalDateTime.now())
        if (risk.level != RiskLevel.LOW) item(key = "radar") {
            RadarCard(risk, onTool = { nav.navigate(Routes.player(risk.toolRoutineId)) }, onLapse = { lapseOpen = true })
        }

        item {
            RingsCard(
                day.intake, day.protein, day.water, t.kcal, t.protein, t.water,
                onWater = { store.addWater(1) }, onOpen = { nav.navigate(Routes.EAT) },
                modifier = Modifier.rise(rise, 2),
            )
        }

        item { StreakCard(thread.length, thread.best, weaveCells(state.days, todayKey, 7, p.createdAt), todayKey, Modifier.rise(rise, 3)) }

        if (thread.rescueToday) item {
            Note("أمس فات، وهذا عادي. اليوم مهمة وحدة بس تمسك سلسلتك." + if (p.why.isNotBlank()) "\nتذكّر ليش بديت: ${p.why}" else "")
        }

        if (p.ramadan) item { RamadanCard(ramadanPlan(p, t)) }

        // مثل التصميم: الخطة تظهر دائماً؛ بدون تسجيل طاقة نعتبر اليوم "عادي"
        run {
            val e = day.energy ?: Energy.MID
            val missions = dayMissions(e, timeFor(e), t, p)
            val doneCount = day.done.count { it in setOf("move", "eat", "restore") }
            item {
                Row(Modifier.padding(horizontal = 4.dp).rise(rise, 4), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (p.ramadan) "خطتك الرمضانية اليوم" else planTitle(e), style = Type.h2.copy(color = c.ink), modifier = Modifier.weight(1f))
                    Text("${ar(doneCount)} من ٣", style = Type.small.copy(color = c.inkSoft))
                }
            }
            missions.forEachIndexed { i, m ->
                item(key = "${e.name}-${m.id}") {
                    MissionCard(
                        m, done = m.id in day.done, index = i,
                        onToggle = {
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
                        },
                        onStart = { m.routineId?.let { nav.navigate(Routes.player(it)) } },
                    )
                }
            }
        }

        item(key = "lapse") {
            LapseCard(
                open = lapseOpen, onToggle = { lapseOpen = !lapseOpen }, targets = t,
                onLog = { kind -> store.logLapse(kind.name); kick++ },
            )
        }

        item {
            Row(
                Modifier.fillMaxWidth().rise(rise, 6).glass(RoundedCornerShape(24.dp)).press({ nav.navigate(Routes.COACH) }).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LivingOrb(30.dp, glow = false)
                Spacer(Modifier.width(12.dp))
                Text("اسأل سند… «شنو آكل على العشا؟»", style = Type.body.copy(color = c.inkSoft), modifier = Modifier.weight(1f))
                SIcon(Ico.NEXT, size = 20.dp, tint = c.inkSoft)
            }
        }
    }
}

/** الوقت المتاح للحركة يتبع الطاقة: تعبان دقيقتين، عادي ١٠، نشيط ٢٠. */
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

/* ------------------------------ الطاقة ------------------------------ */

@Composable
private fun EnergyCard(energy: Energy, modifier: Modifier, onPick: (Energy) -> Unit) {
    val c = Sanad.colors
    val m = Sanad.mood
    val labels = listOf(Energy.LOW to "تعبان", Energy.MID to "عادي", Energy.HIGH to "نشيط")
    BoxWithConstraints(modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp)).padding(5.dp).semantics { contentDescription = "طاقتي اليوم" }) {
        val slot = (maxWidth - 12.dp) / 3
        val idx = labels.indexOfFirst { it.first == energy }
        val x by animateDpAsState((slot + 6.dp) * idx, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow), label = "pill")
        Box(
            Modifier.offset(x = x).width(slot).height(58.dp)
                .shadow(16.dp, RoundedCornerShape(17.dp), ambientColor = m.a, spotColor = m.a)
                .background(Color(0xFF232833), RoundedCornerShape(17.dp))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(17.dp)),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            labels.forEach { (e, label) ->
                val sel = energy == e
                val col by animateColorAsState(if (sel) c.ink else c.inkSoft, label = "e-col")
                Column(
                    Modifier.width(slot).height(58.dp).clip(RoundedCornerShape(17.dp))
                        .press({ if (!sel) onPick(e) }, role = Role.RadioButton)
                        .semantics { selected = sel; stateDescription = if (sel) "مختار" else "" },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    app.sanad.coach.ui.components.Battery(e.level, Modifier.scale(0.78f), color = col)
                    Spacer(Modifier.height(4.dp))
                    Text(label, style = Type.small.copy(color = col, fontWeight = FontWeight.Medium))
                }
            }
        }
    }
}

/* ------------------------------ الحلقات ------------------------------ */

@Composable
private fun RingsCard(
    kcal: Int, protein: Int, water: Int, kcalGoal: Int, proteinGoal: Int, waterGoal: Int,
    onWater: () -> Unit, onOpen: () -> Unit, modifier: Modifier,
) {
    val c = Sanad.colors
    val fk by animateFloatAsState((kcal / kcalGoal.toFloat()).coerceIn(0f, 1f), tween(1600), label = "rk")
    val fp by animateFloatAsState((protein / proteinGoal.toFloat()).coerceIn(0f, 1f), tween(1600), label = "rp")
    val fw by animateFloatAsState((water / waterGoal.toFloat()).coerceIn(0f, 1f), tween(1600), label = "rw")
    val left by animateIntAsState(kcalGoal - kcal, tween(1200), label = "left")
    Row(
        modifier
            .fillMaxWidth()
            .glass()
            .press(onOpen, haptic = false)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(168.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(168.dp).semantics { contentDescription = "أكلت ${ar(kcal)} من ${ar(kcalGoal)} سعرة، بروتين ${ar(protein)} غ، ماء ${ar(water)} أكواب" }) {
                val sw = 11.dp.toPx()
                fun ring(r: Float, frac: Float, a: Color, b: Color) {
                    val tl = Offset(center.x - r, center.y - r)
                    val sz = Size(r * 2, r * 2)
                    drawArc(Color.White.copy(alpha = 0.07f), 0f, 360f, false, tl, sz, style = Stroke(sw))
                    if (frac > 0.001f) {
                        // اتجاه عربي: من الأعلى عكس عقارب الساعة
                        drawArc(b.copy(alpha = 0.35f), -90f, -360f * frac, false, tl, sz, style = Stroke(sw * 1.9f, cap = StrokeCap.Round))
                        drawArc(Brush.sweepGradient(listOf(a, b, a), center), -90f, -360f * frac, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
                    }
                }
                val R = size.minDimension / 2 - sw
                ring(R, fk, c.saffron, c.ember)
                ring(R - sw * 1.55f, fp, Color(0xFF5CF0CF), Color(0xFF1FB093))
                ring(R - sw * 3.1f, fw, Color(0xFF8FB0FF), Color(0xFF4B74F0))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(ar(if (left >= 0) left else -left), style = Type.number.copy(fontSize = 26.sp, color = if (left >= 0) c.ink else c.saffron))
                Text(if (left >= 0) "سعرة باقية" else "فوق الهدف", style = Type.label.copy(fontSize = 10.sp, color = c.inkSoft))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Legend(c.saffron, "أكلت", ar(kcal), "من ${ar(kcalGoal)}")
            Legend(c.oasis, "بروتين", "${ar(protein)} غ", "من ${ar(proteinGoal)}")
            // لمسة على سطر الماء تضيف كوب
            Legend(c.sky, "ماء", ar(water), "من ${ar(waterGoal)} أكواب", Modifier.press(onWater).semantics { contentDescription = "ماء ${ar(water)} من ${ar(waterGoal)}، المس لإضافة كوب" })
        }
    }
}

@Composable
private fun Legend(dot: Color, key: String, value: String, of: String, modifier: Modifier = Modifier) {
    val c = Sanad.colors
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(7.dp))
            Text(key, style = Type.label.copy(color = c.inkSoft))
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = Type.number.copy(fontSize = 18.sp, color = c.ink))
            Spacer(Modifier.width(4.dp))
            Text(of, style = Type.label.copy(color = c.inkSoft), modifier = Modifier.padding(bottom = 2.dp))
        }
    }
}

/* ------------------------------ السلسلة ------------------------------ */

@Composable
private fun StreakCard(length: Int, best: Int, week: List<WeaveCell>, today: String, modifier: Modifier) {
    val c = Sanad.colors
    val spin = rememberInfiniteTransition(label = "sun")
    val deg by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(16000, easing = LinearEasing)), label = "rays")
    val breathe by spin.animateFloat(4f, 8f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "breathe")
    SCard(modifier, pad = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(44.dp)) {
                val cx = center
                rotate(deg, cx) {
                    for (i in 0 until 8) {
                        val a = (i * PI / 4).toFloat()
                        val r1 = size.minDimension * 0.34f; val r2 = size.minDimension * 0.46f
                        drawLine(c.saffron.copy(alpha = 0.85f), Offset(cx.x + kotlin.math.cos(a) * r1, cx.y + kotlin.math.sin(a) * r1), Offset(cx.x + kotlin.math.cos(a) * r2, cx.y + kotlin.math.sin(a) * r2), 2.4.dp.toPx(), StrokeCap.Round)
                    }
                }
                drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE3A8), c.saffron, c.ember), center = cx, radius = size.minDimension * 0.22f), size.minDimension * 0.2f, cx)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(ar(length), style = Type.number.copy(color = c.ink))
                    Spacer(Modifier.width(6.dp))
                    Text("يوم", style = Type.h3.copy(color = c.ink), modifier = Modifier.padding(bottom = 3.dp))
                }
                Text("سلسلة الاستمرار", style = Type.label.copy(color = c.inkSoft))
            }
            Spacer(Modifier.weight(1f))
            Text("فاتك يوم؟ عادي، بس لا تفوّت يومين ورا بعض.", style = Type.label.copy(color = c.inkSoft), textAlign = TextAlign.End, modifier = Modifier.width(140.dp))
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            // الأقدم يمين (بداية السطر في العربي)
            week.forEachIndexed { i, cell ->
                val date = LocalDate.parse(addDays(today, (i - 6).toLong()))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    DayDot(cell, if (cell == WeaveCell.TODAY) breathe.dp else 0.dp)
                    Spacer(Modifier.height(6.dp))
                    Text(DAY_LETTER.getValue(date.dayOfWeek), style = Type.label.copy(color = c.inkSoft, fontSize = 11.sp))
                }
            }
        }
    }
}

@Composable
private fun DayDot(cell: WeaveCell, halo: Dp) {
    val c = Sanad.colors
    val m = Modifier.size(34.dp)
    when (cell) {
        WeaveCell.WOVEN -> Box(
            m.clip(CircleShape).background(Brush.linearGradient(listOf(c.saffron, c.ember))).semantics { contentDescription = "يوم منجز" },
            contentAlignment = Alignment.Center,
        ) { SIcon(Ico.CHECK, size = 16.dp, tint = c.onGold) }
        WeaveCell.HELD -> Canvas(m.semantics { contentDescription = "يوم فائت ما كسر السلسلة" }) {
            drawCircle(c.saffron.copy(alpha = 0.55f), size.minDimension / 2 - 1.dp.toPx(), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))))
        }
        WeaveCell.TODAY -> Box(
            m.border(halo, c.saffron.copy(alpha = 0.1f), CircleShape).border(1.5.dp, c.saffron, CircleShape).semantics { contentDescription = "اليوم" },
        )
        WeaveCell.BROKEN -> Box(m.border(1.5.dp, c.rose.copy(alpha = 0.35f), CircleShape).semantics { contentDescription = "يوم فائت" })
        WeaveCell.BEFORE -> Box(m.background(Color.White.copy(alpha = 0.03f), CircleShape))
    }
}

/* ------------------------------ المهمات ------------------------------ */

@Composable
private fun MissionCard(m: Mission, done: Boolean, index: Int, onToggle: () -> Unit, onStart: () -> Unit) {
    val c = Sanad.colors
    val haptic = LocalHapticFeedback.current
    val confetti = LocalConfetti.current
    val point = rememberBurstPoint()
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 90L)
        enter.animateTo(1f, tween(700, easing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)))
    }
    val checkScale by animateFloatAsState(if (done) 1.08f else 1f, spring(dampingRatio = 0.4f, stiffness = 500f), label = "check")
    val tick by animateFloatAsState(if (done) 1f else 0f, tween(450, delayMillis = 100), label = "tick")
    val fill by animateColorAsState(if (done) c.oasis else Color.Transparent, label = "fill")
    val (tileColor, icon) = when (m.kind) {
        MissionKind.MOVE -> c.saffron to Ico.MOVE
        MissionKind.EAT -> c.oasis to Ico.EAT
        MissionKind.RESTORE -> c.sky to if ("ماء" in m.title || "أكواب" in m.title) Ico.DROP else Ico.MOON
    }
    val routine = m.routineId?.let(::routineById)
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = enter.value
                translationX = (1f - enter.value) * 40.dp.toPx()
                val s = 0.96f + 0.04f * enter.value
                scaleX = s; scaleY = s
            }
            .glass(RoundedCornerShape(24.dp))
            .padding(start = 14.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(tileColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            SIcon(icon, size = 24.dp, tint = tileColor)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(m.title, style = Type.h3.copy(color = if (done) c.inkSoft else c.ink, fontWeight = FontWeight.SemiBold))
            Text(m.detail, style = Type.small.copy(color = c.inkSoft))
        }
        if (routine != null && !done) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.clip(CircleShape).background(c.ink).press(onStart).padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text("ابدأ", style = Type.label.copy(color = c.bg, fontWeight = FontWeight.SemiBold))
            }
        }
        Spacer(Modifier.width(10.dp))
        Canvas(
            Modifier
                .size(34.dp)
                .burstFrom(point)
                .graphicsLayer { scaleX = checkScale; scaleY = checkScale }
                .press({
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (!done) confetti.burst(point.center, 40, 0.8f)
                    onToggle()
                }, role = Role.Checkbox, haptic = false)
                .semantics { contentDescription = if (done) "إلغاء: ${m.title}" else "تم: ${m.title}" },
        ) {
            val r = size.minDimension / 2
            if (done) drawCircle(c.oasis.copy(alpha = 0.15f), r * 1.35f)
            drawCircle(fill, r)
            drawCircle(if (done) c.oasis else Color.White.copy(alpha = 0.2f), r - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
            if (tick > 0f) {
                // علامة صح ترسم نفسها
                val p1 = Offset(size.width * 0.28f, size.height * 0.52f)
                val p2 = Offset(size.width * 0.44f, size.height * 0.68f)
                val p3 = Offset(size.width * 0.74f, size.height * 0.36f)
                val k1 = (tick * 2f).coerceAtMost(1f)
                val k2 = ((tick - 0.5f) * 2f).coerceIn(0f, 1f)
                val sw = 3.dp.toPx()
                drawLine(Color(0xFF04221C), p1, p1 + (p2 - p1) * k1, sw, StrokeCap.Round)
                if (k2 > 0f) drawLine(Color(0xFF04221C), p2, p2 + (p3 - p2) * k2, sw, StrokeCap.Round)
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
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(c.saffron.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                SIcon(Ico.MOON, size = 22.dp, tint = c.saffron)
            }
            Spacer(Modifier.width(12.dp))
            Text("خطتك الرمضانية", style = Type.h2.copy(color = c.ink), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        plan.meals.forEach { m ->
            Row(
                Modifier.padding(bottom = 8.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.05f)).padding(horizontal = 14.dp, vertical = 10.dp),
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

@Composable
private fun WelcomeCard(w: Welcome) {
    val c = Sanad.colors
    Column(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp), c.oasis).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LivingOrb(34.dp, glow = false)
            Spacer(Modifier.width(10.dp))
            Text(w.headline, style = Type.h2.copy(color = c.ink))
        }
        Text(w.body, style = Type.body.copy(color = c.ink))
        Text("مهمة اليوم: ${w.mission}", style = Type.small.copy(color = c.oasis, fontWeight = FontWeight.SemiBold))
    }
}

/** رادار الزلّة: يطلع بس لما اللحظة حساسة، ويعرض خطتك أنت وأداة سريعة. */
@Composable
private fun RadarCard(r: Risk, onTool: () -> Unit, onLapse: () -> Unit) {
    val c = Sanad.colors
    val tint = if (r.level == RiskLevel.HIGH) c.rose else c.saffron
    val pulse = rememberInfiniteTransition(label = "radar")
    val ring by pulse.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "ring")
    Column(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp), tint).padding(18.dp),
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
                Text("رادار سند", style = Type.label.copy(color = tint))
                Text(r.headline, style = Type.h3.copy(color = c.ink, fontWeight = FontWeight.Bold))
            }
        }
        r.plan?.let { plan ->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.05f)).padding(12.dp)) {
                Text("خطتك أنت:", style = Type.label.copy(color = c.inkSoft))
                Text("${plan.whenText} ← ${plan.thenText}", style = Type.body.copy(color = c.ink))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SButton("أداة ٣ دقايق", onTool, Modifier.weight(1f), style = BtnStyle.GOLD, small = true, icon = Ico.PLAY)
            SButton("زلّيت", onLapse, style = BtnStyle.SOFT, small = true)
        }
    }
}

/** "زلّيت": تسجيل صادق بدون حكم، وخطة رجوع فورية بدل "خلاص خربت". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LapseCard(open: Boolean, onToggle: () -> Unit, targets: app.sanad.core.Targets, onLog: (LapseKind) -> Unit) {
    val c = Sanad.colors
    var kind by rememberSaveable { mutableStateOf<LapseKind?>(null) }
    var logged by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(24.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth().press(onToggle, haptic = false), verticalAlignment = Alignment.CenterVertically) {
            Text("صار شي اليوم؟", style = Type.h3.copy(color = c.ink), modifier = Modifier.weight(1f))
            Text(if (open) "سكّر" else "زلّيت", style = Type.label.copy(color = c.saffron, fontWeight = FontWeight.SemiBold))
        }
        AnimatedVisibility(open, enter = fadeIn() + expandVertically()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("ما في حكم هنا. اختار اللي صار، وسند يعطيك الخطوة الجاية.", style = Type.small.copy(color = c.inkSoft))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LapseKind.entries.forEach { k ->
                        val sel = kind == k
                        Box(
                            Modifier.clip(CircleShape).background(if (sel) c.ink else c.glassTop).border(1.dp, if (sel) Color.Transparent else c.line, CircleShape)
                                .press({ kind = k; logged = false }).padding(horizontal = 14.dp, vertical = 8.dp),
                        ) { Text(k.label, style = Type.small.copy(color = if (sel) c.bg else c.ink)) }
                    }
                }
                kind?.let { k ->
                    val r = lapseRecovery(k, targets)
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.oasis.copy(alpha = 0.08f)).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(r.title, style = Type.h3.copy(color = c.ink, fontWeight = FontWeight.Bold))
                        r.steps.forEach { Text("• $it", style = Type.body.copy(color = c.ink)) }
                        Text(r.reframe, style = Type.small.copy(color = c.oasis))
                    }
                    if (!logged) SButton("سجّلها وكمّل يومي", { onLog(k); logged = true }, Modifier.fillMaxWidth(), style = BtnStyle.SOFT, small = true)
                    else Text("انسجلت. التسجيل الصادق نفسه التزام، وسلسلتك محفوظة.", style = Type.small.copy(color = c.oasis))
                }
            }
        }
    }
}
