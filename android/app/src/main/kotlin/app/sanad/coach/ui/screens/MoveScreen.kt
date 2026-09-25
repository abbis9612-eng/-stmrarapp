package app.sanad.coach.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import app.sanad.coach.data.AppStore
import app.sanad.coach.data.today
import app.sanad.coach.ui.Routes
import app.sanad.coach.ui.components.Badge
import app.sanad.coach.ui.components.BtnStyle
import app.sanad.coach.ui.components.ExerciseFigure
import app.sanad.coach.ui.components.Ico
import app.sanad.coach.ui.components.NightCard
import app.sanad.coach.ui.components.SButton
import app.sanad.coach.ui.components.SCard
import app.sanad.coach.ui.components.SChip
import app.sanad.coach.ui.components.SIcon
import app.sanad.coach.ui.components.SaduBand
import app.sanad.coach.ui.components.SaduLogo
import app.sanad.coach.ui.components.SectionTitle
import app.sanad.coach.ui.components.press
import app.sanad.coach.ui.theme.Sanad
import app.sanad.coach.ui.theme.Type
import app.sanad.core.AppState
import app.sanad.core.EXERCISES
import app.sanad.core.Exercise
import app.sanad.core.ROUTINES
import app.sanad.core.Routine
import app.sanad.core.ar
import app.sanad.core.exerciseById
import app.sanad.core.routineById
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoveScreen(state: AppState, nav: NavHostController) {
    val c = Sanad.colors
    val day = state.today()
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val list = ROUTINES.filter { filter == 0 || (if (filter == 2) it.minutes <= 5 else it.minutes == filter) }
    Page {
        item {
            Text("الحركة", style = Type.h1.copy(color = c.ink))
            Text("وجبات حركة قصيرة بدون أدوات. تمارين القوة أهم شي يحمي عضلك وحرقك وأنت تنزل.", style = Type.small.copy(color = c.inkSoft))
        }
        if (day.workouts.isNotEmpty()) item {
            SCard(color = c.palmSoft, pad = 14.dp) {
                Text("أنجزت اليوم: ${day.workouts.joinToString("، ") { it.name }} 💪", style = Type.bodyStrong.copy(color = c.palm))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "الكل", 2 to "٥ دقائق وأقل", 10 to "١٠ دقائق", 20 to "٢٠ دقيقة").forEach { (v, l) -> SChip(l, filter == v, { filter = v }) }
            }
        }
        list.forEach { r ->
            item(key = r.id) {
                val fits = day.energy != null && r.energy <= day.energy!!.level && (day.time == null || r.minutes <= day.time!!.minutes)
                RoutineCard(r, fits) { nav.navigate(Routes.player(r.id)) }
            }
        }
        item { SectionTitle("مكتبة التمارين — ${ar(EXERCISES.size)}") }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = 2) {
                EXERCISES.forEach { e ->
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(c.surface).press({ nav.navigate(Routes.exercise(e.id)) }).padding(10.dp),
                    ) {
                        ExerciseFigure(e, Modifier.fillMaxWidth().aspectRatio(1.2f).clip(RoundedCornerShape(14.dp)).background(c.surface2))
                        Spacer(Modifier.height(8.dp))
                        Text(e.name, style = Type.bodyStrong.copy(color = c.ink))
                        Text(e.muscles.take(2).joinToString("، ") { it.label }, style = Type.label.copy(color = c.inkSoft))
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutineCard(r: Routine, fits: Boolean, onStart: () -> Unit) {
    val c = Sanad.colors
    SCard(pad = 0.dp) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(r.title, style = Type.h2.copy(color = c.ink))
                }
                Text("${ar(r.minutes)} د — ${r.tag}", style = Type.label.copy(color = c.inkSoft))
                if (fits) { Spacer(Modifier.height(6.dp)); Badge("يناسب طاقتك اليوم") }
            }
            val first = r.exercises.firstOrNull()
            if (first != null) ExerciseFigure(first, Modifier.size(width = 104.dp, height = 84.dp).clip(RoundedCornerShape(16.dp)).background(c.surface2))
        }
        Text(r.why, style = Type.small.copy(color = c.inkSoft), modifier = Modifier.padding(horizontal = 16.dp))
        LazyRow(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)) {
            items(r.exercises) { e ->
                Box(Modifier.clip(CircleShape).background(c.surface2).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(e.name, style = Type.label.copy(color = c.ink))
                }
            }
        }
        SButton("ابدأ الجلسة", onStart, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp).fillMaxWidth(), icon = Ico.PLAY)
    }
}

/* ------------------------------ صفحة التمرين ------------------------------ */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseScreen(id: String, nav: NavHostController) {
    val c = Sanad.colors
    val e = exerciseById(id) ?: return
    Page(bottom = false) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(c.surface2).press({ nav.popBackStack() }), contentAlignment = Alignment.Center) {
                    SIcon(Ico.BACK, description = "رجوع")
                }
                Spacer(Modifier.width(12.dp))
                Text(e.name, style = Type.h1.copy(color = c.ink))
            }
        }
        item {
            NightCard(pad = 0.dp) {
                ExerciseFigure(e, Modifier.fillMaxWidth().aspectRatio(1.15f).padding(18.dp), onDark = true)
                SaduBand(height = 8.dp)
            }
        }
        item {
            SCard {
                Text("الطريقة", style = Type.h3.copy(color = c.inkSoft))
                Text(e.cue, style = Type.body.copy(color = c.ink))
                if (e.jointFriendly) { Spacer(Modifier.height(8.dp)); Badge("لطيف على المفاصل") }
            }
        }
        item {
            SCard {
                Text("العضلات المستهدفة", style = Type.h3.copy(color = c.inkSoft))
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    e.muscles.forEach { m -> Badge(m.label, gold = true) }
                }
            }
        }
        if (e.mistakes.isNotEmpty()) item {
            SCard {
                Text("أخطاء شائعة", style = Type.h3.copy(color = c.inkSoft))
                e.mistakes.forEach { Text("✕  $it", style = Type.body.copy(color = c.ink)) }
            }
        }
        val alts = listOfNotNull(e.easier?.let { "أسهل" to it }, e.harder?.let { "أصعب" to it })
        if (alts.isNotEmpty()) item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                alts.forEach { (label, altId) ->
                    val alt = exerciseById(altId) ?: return@forEach
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(c.surface).press({ nav.navigate(Routes.exercise(alt.id)) }).padding(10.dp)) {
                        ExerciseFigure(alt, Modifier.fillMaxWidth().aspectRatio(1.3f).clip(RoundedCornerShape(14.dp)).background(c.surface2))
                        Text(label, style = Type.label.copy(color = c.sadu))
                        Text(alt.name, style = Type.bodyStrong.copy(color = c.ink))
                    }
                }
            }
        }
    }
}

/* ------------------------------ المشغّل ------------------------------ */

@Composable
fun PlayerScreen(routineId: String, store: AppStore, nav: NavHostController) {
    val c = Sanad.colors
    val r = routineById(routineId) ?: return
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    var idx by rememberSaveable { mutableIntStateOf(0) }
    var left by rememberSaveable { mutableIntStateOf(r.moves[0].seconds) }
    var running by rememberSaveable { mutableStateOf(true) }
    var finished by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    LaunchedEffect(running, idx, finished) {
        while (running && !finished) {
            delay(1000)
            if (left > 1) {
                left -= 1
                if (left <= 3) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } else if (idx < r.moves.lastIndex) {
                idx += 1; left = r.moves[idx].seconds
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } else {
                left = 0; finished = true
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    val move = r.moves[idx]
    val ex = move.exerciseId?.let(::exerciseById)
    val next = r.moves.getOrNull(idx + 1)?.let { m -> m.exerciseId?.let(::exerciseById)?.name ?: "راحة" }
    val elapsed = r.moves.take(idx).sumOf { it.seconds } + (move.seconds - left)
    val frac by animateFloatAsState(if (move.seconds == 0) 1f else (move.seconds - left) / move.seconds.toFloat(), tween(900), label = "ring")
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(c.night2, c.night)))) {
        Column(Modifier.fillMaxSize().padding(top = top + 8.dp).navigationBarsPadding().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).press({ nav.popBackStack() }), contentAlignment = Alignment.Center) {
                    SIcon(Ico.CLOSE, tint = Color.White, description = "إغلاق")
                }
                Spacer(Modifier.weight(1f))
                Text("${r.title} — ${ar(elapsed / 60)}:${ar(elapsed % 60).padStart(2, '٠')} من ${ar(r.minutes)} د", style = Type.small.copy(color = c.onNightSoft))
            }
            // شريط تقدّم الجلسة كاملة
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().height(4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                r.moves.forEachIndexed { i, m ->
                    Box(Modifier.weight(m.seconds.toFloat()).fillMaxSize().clip(CircleShape).background(if (i < idx || finished) c.date else if (i == idx) c.date.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.15f)))
                }
            }

            AnimatedContent(if (finished) -1 else idx, Modifier.weight(1f), transitionSpec = { (fadeIn(tween(300)) + scaleIn(initialScale = 0.96f)) togetherWith fadeOut(tween(200)) }, label = "move") { i ->
                if (i == -1) {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        SaduLogo(size = 120.dp)
                        Spacer(Modifier.height(16.dp))
                        Text("خلصت! يعطيك العافية", style = Type.h1.copy(color = Color.White), textAlign = TextAlign.Center)
                        Text("كل دقيقة حركة تنحسب. خيطك انمسك اليوم.", style = Type.body.copy(color = c.onNightSoft), textAlign = TextAlign.Center)
                    }
                } else {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(if (move.isRest) "راحة" else "تمرين ${ar(r.moves.take(i + 1).count { !it.isRest })}", style = Type.small.copy(color = c.onNightSoft))
                        Text(ex?.name ?: "خذ نفس", style = Type.h1.copy(color = Color.White))
                        Spacer(Modifier.height(10.dp))
                        Box(Modifier.fillMaxWidth(0.86f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                            Canvas(Modifier.fillMaxSize()) {
                                val stroke = 10.dp.toPx()
                                drawArc(Color.White.copy(alpha = 0.10f), 0f, 360f, false, Offset(stroke, stroke), Size(size.width - 2 * stroke, size.height - 2 * stroke), style = Stroke(stroke))
                                drawArc(if (move.isRest) c.onNightSoft else c.date, -90f, 360f * frac, false, Offset(stroke, stroke), Size(size.width - 2 * stroke, size.height - 2 * stroke), style = Stroke(stroke, cap = StrokeCap.Round))
                            }
                            if (ex != null) ExerciseFigure(ex, Modifier.fillMaxSize(0.74f), playing = running, onDark = true)
                            else Text(ar(left), style = Type.hero.copy(color = Color.White, fontSize = 72.sp))
                        }
                        if (ex != null) Text(ar(left), style = Type.hero.copy(color = Color.White, fontSize = 54.sp))
                        Text(move.cue ?: ex?.cue ?: "تنفّس بعمق من الأنف.", style = Type.body.copy(color = Color.White), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 12.dp))
                        if (next != null) Text("التالي: $next", style = Type.small.copy(color = c.onNightSoft))
                    }
                }
            }

            if (finished) {
                SButton("سجّلها وارجع ليومي", { store.logWorkout(r.id); nav.popBackStack() }, Modifier.fillMaxWidth(), style = BtnStyle.GOLD, icon = Ico.CHECK)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)).press({ running = !running }), contentAlignment = Alignment.Center) {
                        SIcon(if (running) Ico.PAUSE else Ico.PLAY, tint = Color.White, description = if (running) "إيقاف مؤقت" else "استئناف")
                    }
                    Box(Modifier.weight(1f).height(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)).press({
                        if (idx < r.moves.lastIndex) { idx += 1; left = r.moves[idx].seconds } else finished = true
                    }), contentAlignment = Alignment.Center) { Text("التالي", style = Type.h3.copy(color = Color.White)) }
                    SButton("أنهيت", { finished = true }, Modifier.weight(1f), style = BtnStyle.GOLD)
                }
            }
        }
    }
}
