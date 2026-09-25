package app.sanad.coach.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import app.sanad.coach.data.AppStore
import app.sanad.coach.data.targets
import app.sanad.coach.data.today
import app.sanad.coach.ui.Routes
import app.sanad.coach.ui.components.Badge
import app.sanad.coach.ui.components.Battery
import app.sanad.coach.ui.components.BtnStyle
import app.sanad.coach.ui.components.ExerciseFigure
import app.sanad.coach.ui.components.Ico
import app.sanad.coach.ui.components.Meter
import app.sanad.coach.ui.components.NightCard
import app.sanad.coach.ui.components.Note
import app.sanad.coach.ui.components.SButton
import app.sanad.coach.ui.components.SCard
import app.sanad.coach.ui.components.SChip
import app.sanad.coach.ui.components.SIcon
import app.sanad.coach.ui.components.SaduBand
import app.sanad.coach.ui.components.SaduWeave
import app.sanad.coach.ui.components.press
import app.sanad.coach.ui.theme.Sanad
import app.sanad.coach.ui.theme.Type
import app.sanad.core.AppState
import app.sanad.core.ENERGY_COPY
import app.sanad.core.Energy
import app.sanad.core.Mission
import app.sanad.core.MissionKind
import app.sanad.core.TimeBudget
import app.sanad.core.ar
import app.sanad.core.computeThread
import app.sanad.core.dayMissions
import app.sanad.core.RamadanPlan
import app.sanad.core.ramadanPlan
import app.sanad.core.routineById
import app.sanad.core.weaveCells
import java.time.LocalTime

private fun greeting(): String {
    val h = LocalTime.now().hour
    return when {
        h < 5 -> "سهرانين"
        h < 12 -> "صباح الخير"
        h < 17 -> "يعطيك العافية"
        else -> "مساء الخير"
    }
}

@Composable
fun TodayScreen(store: AppStore, state: AppState, nav: NavHostController) {
    val p = state.profile ?: return
    val t = state.targets() ?: return
    val c = Sanad.colors
    val day = state.today()
    val todayKey = day.date
    val thread = computeThread(state.days, todayKey)
    var editing by rememberSaveable { mutableStateOf(false) }
    var justWove by remember { mutableStateOf(false) }
    val checkedIn = day.energy != null && day.time != null && !editing

    Page {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(greeting(), style = Type.small.copy(color = c.inkSoft))
                    Text(p.name, style = Type.h1.copy(color = c.ink))
                }
                Row(
                    Modifier.clip(CircleShape).background(c.dateSoft).press({ nav.navigate(Routes.PROGRESS) }).padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SIcon(Ico.FLAME, size = 18.dp, tint = c.sadu)
                    Spacer(Modifier.width(6.dp))
                    Text("${ar(thread.length)} يوم", style = Type.label.copy(color = c.ink))
                }
            }
        }

        item {
            AnimatedContent(checkedIn, transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) }, label = "checkin") { done ->
                if (!done) CheckInCard(day.energy, day.time) { e, tm -> store.checkIn(e, tm); editing = false }
                else {
                    val (label, line) = ENERGY_COPY.getValue(day.energy!!)
                    SCard(pad = 14.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Battery(day.energy!!.level, Modifier.scale(0.6f))
                            Column(Modifier.weight(1f).padding(horizontal = 6.dp)) {
                                Text(label, style = Type.bodyStrong.copy(color = c.ink))
                                Text(line, style = Type.small.copy(color = c.inkSoft))
                            }
                            SButton("غيّر", { editing = true }, style = BtnStyle.GHOST, small = true)
                        }
                    }
                }
            }
        }

        if (thread.rescueToday) item {
            Note("أمس فات، وهذا عادي. اليوم يوم الإنقاذ: مهمة وحدة بس تمسك خيطك." + if (p.why.isNotBlank()) "\nتذكّر ليش بديت: ${p.why}" else "")
        }

        item {
            NightCard(pad = 0.dp) {
                SaduBand(height = 8.dp)
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("خيطك", style = Type.h2.copy(color = Color.White), modifier = Modifier.weight(1f))
                        Text("أطول خيط ${ar(thread.best)}", style = Type.small.copy(color = c.onNightSoft))
                    }
                    Spacer(Modifier.height(12.dp))
                    SaduWeave(weaveCells(state.days, todayKey, 28, p.createdAt), animateLast = justWove)
                    Spacer(Modifier.height(8.dp))
                    Text("كل يوم تنجز فيه مهمة وحدة ينسج صف. يوم فائت يمسكه خيط؛ يومين ورا بعض يقطعونه.", style = Type.small.copy(color = c.onNightSoft))
                }
            }
        }

        if (p.ramadan) item { RamadanCard(ramadanPlan(p, t)) }

        if (checkedIn) {
            val missions = dayMissions(day.energy!!, day.time!!, t, p)
            val doneCount = day.done.count { it in setOf("move", "eat", "restore") }
            item {
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("مهمات اليوم", style = Type.h2.copy(color = c.ink), modifier = Modifier.weight(1f))
                    Badge("${ar(doneCount)} من ٣", gold = doneCount < 3)
                }
            }
            missions.forEach { m ->
                item(key = m.id) {
                    MissionCard(m, done = m.id in day.done, onToggle = {
                        if (m.id !in day.done && day.done.isEmpty()) justWove = true
                        store.toggleMission(m.id)
                    }, onStart = { m.routineId?.let { nav.navigate(Routes.player(it)) } })
                }
            }
        }

        item { FuelCard(day.intake, day.protein, day.water, t.kcal, t.protein, t.water, onWater = store::addWater, onLog = { nav.navigate(Routes.EAT) }) }

        item { AskCoach { q -> nav.navigate(Routes.coach(q)) } }
    }
}

@Composable
private fun CheckInCard(e0: Energy?, t0: TimeBudget?, onDone: (Energy, TimeBudget) -> Unit) {
    val c = Sanad.colors
    var energy by rememberSaveable { mutableStateOf(e0) }
    var time by rememberSaveable { mutableStateOf(t0) }
    SCard(shape = RoundedCornerShape(topStart = 160.dp, topEnd = 160.dp, bottomStart = 26.dp, bottomEnd = 26.dp), pad = 20.dp) {
        Spacer(Modifier.height(22.dp))
        Text("كيف طاقتك اليوم؟", style = Type.h1.copy(color = c.ink), modifier = Modifier.align(Alignment.CenterHorizontally))
        Text("صدق مع نفسك — الخطة تتفصّل على قدّك.", style = Type.small.copy(color = c.inkSoft), modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Energy.entries.forEach { e ->
                val sel = energy == e
                val bg by animateColorAsState(if (sel) c.dateSoft else c.surface, label = "ebg")
                val border by animateColorAsState(if (sel) c.date else c.line, label = "ebr")
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(bg)
                        .border(1.5.dp, border, RoundedCornerShape(20.dp))
                        .press({ energy = e })
                        .semantics { stateDescription = if (sel) "مختار" else "" }
                        .padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Battery(e.level)
                    Spacer(Modifier.height(8.dp))
                    Text(ENERGY_COPY.getValue(e).first, style = Type.label.copy(color = c.ink))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("كم دقيقة تقدر تعطي الحركة؟", style = Type.small.copy(color = c.inkSoft), modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(8.dp))
        Row(Modifier.align(Alignment.CenterHorizontally), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimeBudget.entries.forEach { tm ->
                SChip(mapOf(TimeBudget.TWO to "دقيقتين", TimeBudget.TEN to "١٠ دقائق", TimeBudget.TWENTY to "٢٠ دقيقة").getValue(tm), time == tm, { time = tm })
            }
        }
        Spacer(Modifier.height(18.dp))
        SButton("فصّل لي خطة اليوم", { onDone(energy!!, time!!) }, Modifier.fillMaxWidth(), enabled = energy != null && time != null, icon = Ico.SPARK)
    }
}

@Composable
private fun MissionCard(m: Mission, done: Boolean, onToggle: () -> Unit, onStart: () -> Unit) {
    val c = Sanad.colors
    val haptic = LocalHapticFeedback.current
    val checkScale by animateFloatAsState(if (done) 1f else 0.6f, spring(dampingRatio = 0.4f, stiffness = 500f), label = "check")
    val bg by animateColorAsState(if (done) c.palmSoft else c.surface, tween(350), label = "mbg")
    val kind = when (m.kind) { MissionKind.MOVE -> "حركة"; MissionKind.EAT -> "أكل"; MissionKind.RESTORE -> "راحة" }
    SCard(color = bg, pad = 16.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (done) c.palm else c.surface)
                    .border(2.dp, if (done) c.palm else c.line, RoundedCornerShape(18.dp))
                    .press({ haptic.performHapticFeedback(HapticFeedbackType.LongPress); onToggle() }, role = Role.Checkbox, haptic = false)
                    .semantics { contentDescription = if (done) "إلغاء إنجاز: ${m.title}" else "أنجزت: ${m.title}" },
                contentAlignment = Alignment.Center,
            ) { SIcon(Ico.CHECK, Modifier.scale(checkScale), size = 28.dp, tint = if (done) Color.White else c.inkSoft) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(kind, style = Type.label.copy(color = c.sadu))
                Text(m.title, style = Type.h3.copy(color = c.ink, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
                Text(m.detail, style = Type.small.copy(color = c.inkSoft))
                val routine = m.routineId?.let(::routineById)
                if (routine != null && !done) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SButton("ابدأ الآن", onStart, small = true, icon = Ico.PLAY)
                        Spacer(Modifier.weight(1f))
                        routine.exercises.firstOrNull()?.let {
                            ExerciseFigure(it, Modifier.size(width = 84.dp, height = 64.dp).clip(RoundedCornerShape(14.dp)).background(c.surface2))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FuelCard(kcal: Int, protein: Int, water: Int, kcalGoal: Int, proteinGoal: Int, waterGoal: Int, onWater: (Int) -> Unit, onLog: () -> Unit) {
    val c = Sanad.colors
    val left by animateIntAsState(kcalGoal - kcal, tween(600), label = "left")
    val p by animateIntAsState(protein, tween(600), label = "protein")
    SCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("وقود اليوم", style = Type.h2.copy(color = c.ink), modifier = Modifier.weight(1f))
            SButton("سجّل أكل", onLog, style = BtnStyle.SOFT, small = true, icon = Ico.PLUS)
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(if (left >= 0) ar(left) else ar(-left), style = Type.number.copy(color = if (left >= 0) c.ink else c.date))
            Spacer(Modifier.width(6.dp))
            Text(if (left >= 0) "سعرة باقية" else "فوق الهدف", style = Type.small.copy(color = c.inkSoft), modifier = Modifier.padding(bottom = 4.dp))
        }
        Meter(kcal / kcalGoal.toFloat(), color = if (kcal > kcalGoal) c.date else if (c.isDark) c.ink else c.night)
        Spacer(Modifier.height(14.dp))
        Row {
            Text("البروتين", style = Type.small.copy(color = c.ink), modifier = Modifier.weight(1f))
            Text("${ar(p)} / ${ar(proteinGoal)} غ", style = Type.small.copy(color = c.inkSoft))
        }
        Spacer(Modifier.height(6.dp))
        Meter(protein / proteinGoal.toFloat(), color = c.palm)
        Spacer(Modifier.height(14.dp))
        Row {
            Text("الماء", style = Type.small.copy(color = c.ink), modifier = Modifier.weight(1f))
            Text("${ar(water)} / ${ar(waterGoal)} أكواب", style = Type.small.copy(color = c.inkSoft))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(waterGoal) { i ->
                val full = i < water
                val fill by animateColorAsState(if (full) c.sky.copy(alpha = 0.75f) else Color.Transparent, label = "cup")
                Box(
                    Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 12.dp, bottomEnd = 12.dp))
                        .background(fill)
                        .border(2.dp, if (full) Color.Transparent else c.line, RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 12.dp, bottomEnd = 12.dp))
                        .press({ onWater(if (full) -1 else 1) })
                        .semantics { contentDescription = if (full) "احذف كوب" else "أضف كوب ماء" },
                )
            }
        }
    }
}

@Composable
private fun AskCoach(onAsk: (String) -> Unit) {
    val c = Sanad.colors
    var text by rememberSaveable { mutableStateOf("") }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(28.dp)).padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            if (text.isEmpty()) Text("قل لسند: \"تغديت مندي ولبن\"", style = Type.body.copy(color = c.inkSoft))
            BasicTextField(
                text, { text = it },
                textStyle = Type.body.copy(color = c.ink),
                cursorBrush = SolidColor(c.sadu),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (text.isNotBlank()) onAsk(text.trim()) }),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "اكتب لسند" },
            )
        }
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(if (c.isDark) c.date else c.night)
                .press({ if (text.isNotBlank()) onAsk(text.trim()) })
                .semantics { contentDescription = "أرسل لسند" },
            contentAlignment = Alignment.Center,
        ) { SIcon(Ico.SEND, size = 22.dp, tint = if (c.isDark) Color(0xFF1B1406) else Color.White) }
    }
}

/** خطة رمضان: هدف اليوم موزّع على نافذة الأكل. */
@Composable
private fun RamadanCard(plan: RamadanPlan) {
    val c = Sanad.colors
    SCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SIcon(Ico.MOON, tint = c.sadu)
            Spacer(Modifier.width(8.dp))
            Text("خطتك الرمضانية", style = Type.h2.copy(color = c.ink), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        plan.meals.forEach { m ->
            Row(
                Modifier.padding(bottom = 8.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface2).padding(horizontal = 14.dp, vertical = 10.dp),
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
