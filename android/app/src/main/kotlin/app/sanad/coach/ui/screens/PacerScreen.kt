package app.sanad.coach.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.compose.material3.Text
import app.sanad.coach.data.AppStore
import app.sanad.coach.ui.components.BtnStyle
import app.sanad.coach.ui.components.Eyebrow
import app.sanad.coach.ui.components.Ico
import app.sanad.coach.ui.components.LivingOrb
import app.sanad.coach.ui.components.SButton
import app.sanad.coach.ui.components.SIcon
import app.sanad.coach.ui.components.glass
import app.sanad.coach.ui.components.press
import app.sanad.coach.ui.theme.Sanad
import app.sanad.coach.ui.theme.Type
import app.sanad.core.CueKind
import app.sanad.core.PACER_TOTAL_SEC
import app.sanad.core.ar
import app.sanad.core.cueAt
import app.sanad.core.fullnessAdvice
import app.sanad.core.pacerCues
import kotlinx.coroutines.delay

/** مؤقت الأكل على مهل: ٢٠ دقيقة، إيقاع لقمات هادئ، وسؤال شبع بالنص. */
@Composable
fun PacerScreen(store: AppStore, nav: NavHostController) {
    val c = Sanad.colors
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val cues = remember { pacerCues() }
    var elapsed by rememberSaveable { mutableIntStateOf(0) }
    var running by rememberSaveable { mutableStateOf(true) }
    var finished by rememberSaveable { mutableStateOf(false) }
    var fullness by rememberSaveable { mutableIntStateOf(0) }
    var kick by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    LaunchedEffect(running, finished) {
        while (running && !finished) {
            delay(1000)
            val before = cueAt(cues, elapsed)
            elapsed += 1
            val now = cueAt(cues, elapsed)
            if (now != before) {
                haptic.performHapticFeedback(if (now.kind == CueKind.CHECK) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove)
                kick++
                if (now.kind == CueKind.CHECK) running = false
                if (now.kind == CueKind.END) finished = true
            }
        }
    }

    val cue = cueAt(cues, elapsed)
    val left = (PACER_TOTAL_SEC - elapsed).coerceAtLeast(0)
    val frac by animateFloatAsState(elapsed / PACER_TOTAL_SEC.toFloat(), tween(900), label = "pacer")
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val askFullness = cue.kind == CueKind.CHECK && fullness == 0

    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(
            Modifier.fillMaxSize().padding(top = top + 10.dp).navigationBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(c.glass2).border(1.dp, c.line, CircleShape).press({ nav.popBackStack() }),
                    contentAlignment = Alignment.Center,
                ) { SIcon(Ico.CLOSE, size = 20.dp, description = "إغلاق") }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Eyebrow("كل على مهلك")
                    Text("٢٠ دقيقة للشبع", style = Type.h2.copy(color = c.ink))
                }
            }

            Spacer(Modifier.height(8.dp))
            // الساعة: حلقة تمتلي مع الوقت والكرة تتنفس بنصها
            Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val sw = 8.dp.toPx()
                    val tl = Offset(sw / 2, sw / 2)
                    val sz = Size(size.width - sw, size.height - sw)
                    drawArc(Color.White.copy(alpha = 0.07f), 0f, 360f, false, tl, sz, style = Stroke(sw))
                    drawArc(Brush.sweepGradient(listOf(c.oasis, c.sky, c.oasis)), -90f, 360f * frac, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
                }
                LivingOrb(170.dp, breathe = true, kick = kick)
            }
            Text(
                "${ar(left / 60)}:${ar(left % 60).padStart(2, '٠')}",
                style = Type.number.copy(fontSize = 40.sp, color = c.ink),
                modifier = Modifier.semantics { contentDescription = "باقي ${left / 60} دقيقة" },
            )
            AnimatedContent(cue.text, transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(250)) }, label = "cue") { t ->
                Text(t, style = Type.h1.copy(fontSize = 26.sp, color = c.ink), textAlign = TextAlign.Center)
            }

            if (askFullness) {
                Column(
                    Modifier.fillMaxWidth().glass(RoundedCornerShape(24.dp), c.oasis).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("شبعان كم من ٥؟", style = Type.h3.copy(color = c.ink))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..5).forEach { n ->
                            Box(
                                Modifier.weight(1f).height(44.dp).clip(CircleShape).background(c.glassTop).border(1.dp, c.line, CircleShape)
                                    .press({ fullness = n; running = true }),
                                contentAlignment = Alignment.Center,
                            ) { Text(ar(n), style = Type.bodyStrong.copy(color = c.ink)) }
                        }
                    }
                    Text("١ جوعان · ٣ مرتاح · ٥ متروس", style = Type.label.copy(color = c.inkSoft))
                }
            } else if (fullness > 0) {
                Text(fullnessAdvice(fullness, elapsed), style = Type.body.copy(color = c.oasis), textAlign = TextAlign.Center)
            }

            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(58.dp).clip(RoundedCornerShape(22.dp)).background(c.glass2).border(1.dp, c.line, RoundedCornerShape(22.dp)).press({ running = !running }),
                    contentAlignment = Alignment.Center,
                ) { SIcon(if (running) Ico.PAUSE else Ico.PLAY, size = 22.dp, description = if (running) "إيقاف مؤقت" else "استئناف") }
                SButton("شبعت، خلصت", { finished = true }, Modifier.weight(1f), style = BtnStyle.GOLD, icon = Ico.CHECK)
            }
        }

        AnimatedVisibility(finished, enter = fadeIn(tween(600)), exit = fadeOut()) {
            val minutes = (elapsed / 60).coerceAtLeast(1)
            Column(
                Modifier.fillMaxSize().background(c.bg).navigationBarsPadding().padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
            ) {
                LivingOrb(110.dp, kick = kick)
                Text("أكلت على مهلك ${ar(minutes)} دقيقة", style = Type.h1.copy(color = c.ink), textAlign = TextAlign.Center)
                Text(
                    if (elapsed >= 15 * 60) "هذا اللي يخلّي الشبع يوصل قبل ما تتروس. كرّرها بوجبة العشا."
                    else "حتى الإبطاء القليل يفرق. المرة الجاية حاول توصل ١٥ دقيقة.",
                    style = Type.body.copy(color = c.inkSoft), textAlign = TextAlign.Center,
                )
                SButton("سجّلها وارجع", { store.logSlowMeal(); nav.popBackStack() }, Modifier.fillMaxWidth(), style = BtnStyle.GOLD, icon = Ico.CHECK)
            }
        }
    }
}
