package app.sanad.coach.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import app.sanad.coach.data.AppStore
import app.sanad.coach.data.latestWeight
import app.sanad.coach.data.today
import app.sanad.coach.ui.Routes
import app.sanad.coach.ui.components.Badge
import app.sanad.coach.ui.components.BtnStyle
import app.sanad.coach.ui.components.Eyebrow
import app.sanad.coach.ui.components.ExerciseFigure
import app.sanad.coach.ui.components.Ico
import app.sanad.coach.ui.components.LivingOrb
import app.sanad.coach.ui.components.LocalConfetti
import app.sanad.coach.ui.components.SButton
import app.sanad.coach.ui.components.SCard
import app.sanad.coach.ui.components.SChip
import app.sanad.coach.ui.components.SIcon
import app.sanad.coach.ui.components.SectionTitle
import app.sanad.coach.ui.components.Stat
import app.sanad.coach.ui.components.burstFrom
import app.sanad.coach.ui.components.glass
import app.sanad.coach.ui.components.press
import app.sanad.coach.ui.components.rememberBurstPoint
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
import kotlin.math.roundToInt

private val TINTS = listOf(Color(0xFF34D7B8), Color(0xFF5B8CFF), Color(0xFFFF7A45), Color(0xFFA57BFF), Color(0xFFFFB648))

/** خلفية "مسرح" للشخصية: توهّج ناعم وخطوط أرضية خفيفة. */
@Composable
private fun Modifier.stage(tint: Color, radius: Int = 18): Modifier {
    val shape = RoundedCornerShape(radius.dp)
    return this
        .clip(shape)
        .background(Brush.radialGradient(listOf(tint.copy(alpha = 0.2f), Color.Transparent)))
        .background(Color.White.copy(alpha = 0.03f))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoveScreen(state: AppState, nav: NavHostController) {
    val c = Sanad.colors
    val day = state.today()
    val rise = rememberRise()
    val level = day.energy?.level ?: 2
    val featured = ROUTINES.firstOrNull { it.energy == level && it.minutes == when (level) { 1 -> 2; 3 -> 20; else -> 10 } }
        ?: ROUTINES.first { it.id == "low-impact-10" }

    Page {
        item {
            Column(Modifier.rise(rise, 0)) {
                Eyebrow("مكتبة الحركة")
                Text("تمارين تناسب يومك", style = Type.h1.copy(color = c.ink))
            }
        }
        if (day.workouts.isNotEmpty()) item {
            SCard(color = c.oasis, pad = 14.dp) {
                Text("أنجزت اليوم: ${day.workouts.joinToString("، ") { it.name }}", style = Type.bodyStrong.copy(color = c.oasis))
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().rise(rise, 1).glass(RoundedCornerShape(30.dp)).press({ nav.navigate(Routes.player(featured.id)) }).padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                featured.exercises.firstOrNull()?.let {
                    ExerciseFigure(it, Modifier.size(120.dp).stage(c.saffron))
                    Spacer(Modifier.width(14.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Badge("مقترح لك اليوم", gold = true)
                    Text(featured.title, style = Type.h2.copy(color = c.ink))
                    Text(featured.why, style = Type.small.copy(color = c.inkSoft))
                }
            }
        }
        // شبكة عمودين مثل التصميم: كل جلسة بطاقة فيها رسمها المتحرك
        item {
            FlowRow(
                Modifier.rise(rise, 2),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), maxItemsInEachRow = 2,
            ) {
                ROUTINES.filter { it.id != featured.id }.forEachIndexed { i, r ->
                    Column(
                        Modifier.weight(1f).glass(RoundedCornerShape(26.dp)).press({ nav.navigate(Routes.player(r.id)) }).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        r.exercises.firstOrNull()?.let { ExerciseFigure(it, Modifier.fillMaxWidth().height(128.dp).stage(TINTS[i % TINTS.size])) }
                        Spacer(Modifier.height(4.dp))
                        Text(r.title, style = Type.bodyStrong.copy(color = c.ink))
                        Text("${ar(r.minutes)} د — ${r.tag}", style = Type.label.copy(color = c.inkSoft))
                    }
                }
            }
        }
        item { SectionTitle("كل التمارين — ${ar(EXERCISES.size)}") }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), maxItemsInEachRow = 2) {
                EXERCISES.forEachIndexed { i, e ->
                    Column(
                        Modifier.weight(1f).glass(RoundedCornerShape(26.dp)).press({ nav.navigate(Routes.exercise(e.id)) }).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        ExerciseFigure(e, Modifier.fillMaxWidth().height(128.dp).stage(TINTS[i % TINTS.size]))
                        Spacer(Modifier.height(4.dp))
                        Text(e.name, style = Type.bodyStrong.copy(color = c.ink))
                        Text(e.muscles.take(2).joinToString("، ") { it.label }, style = Type.label.copy(color = c.inkSoft))
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutineCard(r: Routine, fits: Boolean, tint: Color, onStart: () -> Unit) {
    val c = Sanad.colors
    Row(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp)).press(onStart).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        r.exercises.firstOrNull()?.let { ExerciseFigure(it, Modifier.size(width = 96.dp, height = 88.dp).stage(tint, 18)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(r.title, style = Type.h3.copy(color = c.ink, fontWeight = FontWeight.Bold))
            Text("${ar(r.minutes)} د — ${r.tag}", style = Type.label.copy(color = c.inkSoft))
            Text(r.exercises.joinToString("، ") { it.name }, style = Type.label.copy(color = c.faint), maxLines = 1)
            if (fits) Badge("يناسب طاقتك اليوم")
        }
        Box(Modifier.size(42.dp).clip(CircleShape).background(c.ink), contentAlignment = Alignment.Center) {
            SIcon(Ico.PLAY, size = 18.dp, tint = c.bg)
        }
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
                Box(Modifier.size(44.dp).clip(CircleShape).background(c.glass2).border(1.dp, c.line, CircleShape).press({ nav.popBackStack() }), contentAlignment = Alignment.Center) {
                    SIcon(Ico.BACK, size = 20.dp, description = "رجوع")
                }
                Spacer(Modifier.width(12.dp))
                Text(e.name, style = Type.h1.copy(color = c.ink), modifier = Modifier.weight(1f))
                if (e.jointFriendly) Badge("لطيف على المفاصل")
            }
        }
        item {
            Box(Modifier.fillMaxWidth().height(330.dp).glass(RoundedCornerShape(34.dp)).stage(c.oasis, 34), contentAlignment = Alignment.Center) {
                FloorLines()
                ExerciseFigure(e, Modifier.fillMaxSize().padding(10.dp), onDark = true)
            }
        }
        item {
            SCard {
                Eyebrow("الطريقة")
                Text(e.cue, style = Type.body.copy(color = c.ink))
            }
        }
        item {
            SCard {
                Eyebrow("العضلات المستهدفة")
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    e.muscles.forEach { m -> Badge(m.label, gold = true) }
                }
            }
        }
        if (e.mistakes.isNotEmpty()) item {
            SCard {
                Eyebrow("انتبه من")
                e.mistakes.forEach {
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Top) {
                        SIcon(Ico.CLOSE, size = 16.dp, tint = c.rose, modifier = Modifier.padding(top = 4.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(it, style = Type.body.copy(color = c.ink))
                    }
                }
            }
        }
        val alts = listOfNotNull(e.easier?.let { "أسهل" to it }, e.harder?.let { "أصعب" to it })
        if (alts.isNotEmpty()) item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                alts.forEachIndexed { i, (label, altId) ->
                    val alt = exerciseById(altId) ?: return@forEachIndexed
                    Column(Modifier.weight(1f).glass(RoundedCornerShape(24.dp)).press({ nav.navigate(Routes.exercise(alt.id)) }).padding(12.dp)) {
                        ExerciseFigure(alt, Modifier.fillMaxWidth().aspectRatio(1.3f).stage(TINTS[i + 1]))
                        Spacer(Modifier.height(6.dp))
                        Text(label, style = Type.label.copy(color = c.saffron))
                        Text(alt.name, style = Type.bodyStrong.copy(color = c.ink))
                    }
                }
            }
        }
    }
}

@Composable
private fun FloorLines() {
    Canvas(Modifier.fillMaxSize()) {
        val top = size.height - 90.dp.toPx()
        var x = 0f
        val step = 36.dp.toPx()
        while (x < size.width) {
            drawLine(
                Brush.verticalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.05f)), startY = top, endY = size.height),
                Offset(x, top), Offset(x, size.height), 1.dp.toPx(),
            )
            x += step
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
    val confetti = LocalConfetti.current
    val finPoint = rememberBurstPoint()
    var idx by rememberSaveable { mutableIntStateOf(0) }
    var left by rememberSaveable { mutableIntStateOf(r.moves[0].seconds) }
    var running by rememberSaveable { mutableStateOf(true) }
    var finished by rememberSaveable { mutableStateOf(false) }
    var reps by rememberSaveable { mutableIntStateOf(0) }
    var kick by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    LaunchedEffect(running, idx, finished) {
        while (running && !finished) {
            delay(1000)
            val mv = r.moves[idx]
            val ex = mv.exerciseId?.let(::exerciseById)
            if (ex != null) {
                val repSec = ex.tempoMs * ex.frames.size / 1000f
                val before = ((mv.seconds - left) / repSec).toInt()
                val after = ((mv.seconds - left + 1) / repSec).toInt()
                if (after > before) reps += after - before
            }
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
    LaunchedEffect(finished) {
        if (finished) { delay(350); kick++; confetti.burst(finPoint.center, 130, 1.2f) }
    }

    val move = r.moves[idx]
    val ex = move.exerciseId?.let(::exerciseById)
    val nextName = r.moves.getOrNull(idx + 1)?.let { m -> m.exerciseId?.let(::exerciseById)?.name ?: "راحة" }
    val frac by animateFloatAsState(if (move.seconds == 0) 1f else (move.seconds - left) / move.seconds.toFloat(), tween(900), label = "ring")
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val exCount = r.moves.count { !it.isRest }
    val exIndex = r.moves.take(idx + 1).count { !it.isRest }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(
            Modifier.fillMaxSize().padding(top = top + 10.dp).navigationBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(c.glass2).border(1.dp, c.line, CircleShape).press({ nav.popBackStack() }), contentAlignment = Alignment.Center) {
                    SIcon(Ico.CLOSE, size = 20.dp, description = "إغلاق")
                }
                Spacer(Modifier.width(12.dp))
                // شريط الجلسة: كل حركة جزء بطول مدتها
                Row(Modifier.weight(1f).height(5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    r.moves.forEachIndexed { i, m ->
                        val f = when { finished || i < idx -> 1f; i == idx -> frac; else -> 0f }
                        Box(Modifier.weight(m.seconds.toFloat()).fillMaxHeight().clip(CircleShape).background(Color.White.copy(alpha = 0.1f))) {
                            Box(Modifier.fillMaxWidth(f).fillMaxHeight().background(Brush.horizontalGradient(listOf(c.oasis, c.saffron))))
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Eyebrow(if (move.isRest) "راحة" else "الحركة ${ar(exIndex)} من ${ar(exCount)}")
                    Text(ex?.name ?: "خذ نفس", style = Type.h1.copy(fontSize = 28.sp, color = c.ink))
                }
                ex?.muscles?.firstOrNull()?.let {
                    Box(Modifier.clip(CircleShape).background(c.glass2).padding(horizontal = 11.dp, vertical = 6.dp)) {
                        Text(ex.muscles.take(2).joinToString(" و") { m -> m.label }, style = Type.label.copy(color = c.ink))
                    }
                }
            }

            // المسرح
            Box(
                Modifier.fillMaxWidth().height(330.dp).glass(RoundedCornerShape(34.dp)).stage(c.oasis, 34),
                contentAlignment = Alignment.Center,
            ) {
                FloorLines()
                AnimatedContent(ex, transitionSpec = { (fadeIn(tween(300)) + scaleIn(initialScale = 0.95f)) togetherWith fadeOut(tween(200)) }, label = "stage") { e ->
                    if (e != null) ExerciseFigure(e, Modifier.fillMaxSize().padding(8.dp), playing = running, onDark = true)
                    else Box(Modifier.fillMaxSize())
                }
                val restAlpha by animateFloatAsState(if (move.isRest && !finished) 1f else 0f, tween(500), label = "rest")
                if (restAlpha > 0.01f) {
                    Column(
                        Modifier.fillMaxSize().graphicsLayer { alpha = restAlpha }.background(Color(0xB80A0D12)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        LivingOrb(120.dp, breathe = true)
                        Spacer(Modifier.height(18.dp))
                        Text(if ((left / 3) % 2 == 0) "خذ نفس…" else "طلّعه بهدوء…", style = Type.h1.copy(fontSize = 22.sp, color = c.ink))
                        nextName?.let { Text("التالي: $it", style = Type.small.copy(color = c.inkSoft)) }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val sw = 7.dp.toPx()
                        val tl = Offset(sw / 2, sw / 2)
                        val sz = Size(size.width - sw, size.height - sw)
                        drawArc(Color.White.copy(alpha = 0.08f), 0f, 360f, false, tl, sz, style = Stroke(sw))
                        drawArc(Brush.sweepGradient(listOf(c.saffron, c.ember, c.saffron)), -90f, -360f * (1f - frac), false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(ar(left), style = Type.number.copy(fontSize = 34.sp, color = c.ink))
                        Text("ثانية", style = Type.label.copy(fontSize = 11.sp, color = c.inkSoft))
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!move.isRest) Text("العدّة ${ar(reps)}", style = Type.h3.copy(fontFamily = Type.number.fontFamily, color = c.saffron))
                    Text(move.cue ?: ex?.cue ?: "تنفّس بعمق من الأنف.", style = Type.body.copy(fontSize = 16.sp, color = c.ink))
                    if (!move.isRest && nextName != null) Text("التالي: $nextName", style = Type.label.copy(color = c.faint))
                }
            }

            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(58.dp).clip(RoundedCornerShape(22.dp)).background(c.glass2).border(1.dp, c.line, RoundedCornerShape(22.dp)).press({ running = !running }),
                    contentAlignment = Alignment.Center,
                ) { SIcon(if (running) Ico.PAUSE else Ico.PLAY, size = 22.dp, description = if (running) "إيقاف مؤقت" else "استئناف") }
                SButton("التالي", {
                    if (idx < r.moves.lastIndex) { idx += 1; left = r.moves[idx].seconds } else finished = true
                }, style = BtnStyle.SOFT)
                SButton(if (move.isRest) "كمّل" else "أنهيت الحركة", {
                    if (idx < r.moves.lastIndex) { idx += 1; left = r.moves[idx].seconds } else finished = true
                }, Modifier.weight(1f), style = BtnStyle.GOLD)
            }
        }

        // شاشة الإنجاز
        AnimatedVisibility(finished, enter = fadeIn(tween(600)), exit = fadeOut()) {
            val weight = store.state.value.latestWeight() ?: 80.0
            val minutes = (r.totalSeconds / 60f).roundToInt().coerceAtLeast(1)
            // MET ~٤ لتمارين وزن الجسم المعتدلة
            val kcal = (4.0 * 3.5 * weight / 200.0 * r.totalSeconds / 60.0).roundToInt()
            Column(
                Modifier.fillMaxSize().background(c.bg).navigationBarsPadding().padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
            ) {
                LivingOrb(110.dp, Modifier.burstFrom(finPoint), kick = kick)
                Text("أنهيت تمرينك!", style = Type.h1.copy(color = c.ink))
                Text("كل مرة تتمرن قوة، تحمي عضلك وتخلي النزول من الدهون مو من العضل.", style = Type.body.copy(color = c.inkSoft), textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Stat(ar(minutes), "دقيقة", Modifier.weight(1f))
                    Stat(ar(reps), "عدّة", Modifier.weight(1f))
                    Stat(ar(kcal), "سعرة تقريباً", Modifier.weight(1f))
                }
                SButton("سجّلها وارجع ليومي", { store.logWorkout(r.id); nav.popBackStack() }, Modifier.fillMaxWidth(), style = BtnStyle.GOLD, icon = Ico.CHECK)
            }
        }
    }
}
