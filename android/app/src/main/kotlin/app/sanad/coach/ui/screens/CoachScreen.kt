package app.sanad.coach.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import app.sanad.coach.data.AppStore
import app.sanad.coach.data.CoachSettings
import app.sanad.coach.data.targets
import app.sanad.coach.ui.Routes
import app.sanad.coach.ui.components.Ico
import app.sanad.coach.ui.components.LivingOrb
import app.sanad.coach.ui.components.LocalConfetti
import app.sanad.coach.ui.components.SIcon
import app.sanad.coach.ui.components.burstFrom
import app.sanad.coach.ui.components.glass
import app.sanad.coach.ui.components.press
import app.sanad.coach.ui.components.rememberBurstPoint
import app.sanad.coach.ui.theme.Sanad
import app.sanad.coach.ui.theme.Type
import app.sanad.core.AppState
import app.sanad.core.ChatMessage
import app.sanad.core.ChatRole
import app.sanad.core.CoachAction
import app.sanad.core.ar
import app.sanad.core.offlineReply
import app.sanad.core.routineById
import app.sanad.core.ai.Turn
import app.sanad.core.ai.coachContext
import app.sanad.core.ai.createCoach
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val STARTERS = listOf("كيف كان أسبوعي؟", "تغديت كبسة دجاج ولبن", "عندي عزيمة الليلة", "تعبان اليوم", "ليش وزني ما نزل؟", "يجيني جوع بالليل")

@Composable
fun CoachScreen(store: AppStore, settings: CoachSettings, state: AppState, nav: NavHostController, initialQuestion: String?) {
    val c = Sanad.colors
    val p = state.profile ?: return
    var text by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val list = rememberLazyListState()
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val smart by settings.state.collectAsStateWithLifecycle()

    fun send(raw: String) {
        val msg = raw.trim()
        if (msg.isEmpty() || busy) return
        text = ""; hint = null
        store.pushChat(ChatRole.USER, msg)
        busy = true
        scope.launch {
            val s = store.state.value
            val t = s.targets()!!
            val cfg = settings.config()
            if (cfg != null) {
                val history = s.chat.takeLast(16).map { Turn(it.role, it.text) }
                val ctx = coachContext(s, t, AppStore.today(), LocalTime.now().toString().take(5))
                val result = withContext(Dispatchers.IO) { runCatching { createCoach(cfg).reply(history, ctx) } }
                result.onSuccess { r -> store.pushChat(ChatRole.COACH, r.text, r.actions) }
                result.onFailure { e ->
                    // المدرب المحلي يرد، مع توضيح سبب تعذّر الذكي
                    val local = offlineReply(msg, store.state.value, t, AppStore.today())
                    store.pushChat(ChatRole.COACH, "${coachErrorText(e)}\n\n${local.text}", local.actions, offline = true)
                }
            } else {
                delay(700)
                val r = offlineReply(msg, s, t, AppStore.today())
                store.pushChat(ChatRole.COACH, r.text, r.actions, offline = true)
            }
            busy = false
        }
    }

    // الإدخال الصوتي: خدمة التعرّف على الكلام في الجهاز (عربي)
    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val said = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!said.isNullOrBlank()) send(said)
    }
    fun listen() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar")
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "تكلّم، سند يسمعك")
        try { voice.launch(intent) } catch (_: ActivityNotFoundException) {
            hint = "التعرّف على الصوت غير متوفر بهذا الجهاز. اكتب رسالتك."
        }
    }

    LaunchedEffect(initialQuestion) { initialQuestion?.let { if (it.isNotBlank()) send(it) } }
    LaunchedEffect(state.chat.size, busy) { if (state.chat.isNotEmpty()) list.animateScrollToItem(state.chat.size + 1) }

    Column(Modifier.fillMaxSize().imePadding()) {
        // الرأس: الكرة الحيّة تتكلم لمن يرد سند
        Row(
            Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = top + 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LivingOrb(48.dp, speaking = busy)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("سند", style = Type.h2.copy(color = c.ink))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(if (smart?.hasKey == true) c.oasis else c.saffron))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (busy) "يكتب لك…" else if (smart?.hasKey == true) "ذكي، ${smart!!.label}" else "المدرب المحلي",
                        style = Type.label.copy(color = if (smart?.hasKey == true) c.oasis else c.saffron),
                    )
                }
            }
            if (state.chat.isNotEmpty()) {
                Box(Modifier.clip(CircleShape).border(1.dp, c.line, CircleShape).press({ store.clearChat() }).padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Text("محادثة جديدة", style = Type.label.copy(color = c.ink))
                }
                Spacer(Modifier.width(8.dp))
            }
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(c.glass2).press({ nav.navigate(Routes.COACH_SETTINGS) }).semantics { contentDescription = "إعدادات المدرب الذكي" },
                contentAlignment = Alignment.Center,
            ) { SIcon(Ico.SETTINGS, size = 21.dp) }
        }

        LazyColumn(
            state = list,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Bubble(ChatMessage("hello", ChatRole.COACH, "هلا ${p.name}! أنا سند. قول لي شنو أكلت وأحسبه لك، أو قول شلون طاقتك وأفصّل لك خطوة تناسبك.", 0), store, nav)
            }
            items(state.chat, key = { it.id }) { m -> Bubble(m, store, nav) }
            if (busy) item { Thinking() }
            if (smart?.hasKey != true && state.chat.isEmpty()) item {
                Row(
                    Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp), c.saffron).press({ nav.navigate(Routes.COACH_SETTINGS) }).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SIcon(Ico.SPARK, tint = c.saffron)
                    Spacer(Modifier.width(10.dp))
                    Text("فعّل المدرب الذكي بمفتاح API (Claude أو Gemini المجاني…)", style = Type.small.copy(color = c.ink), modifier = Modifier.weight(1f))
                    SIcon(Ico.NEXT, size = 20.dp)
                }
            }
        }

        // شريط الاقتراحات + مربع الكتابة
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 10.dp),
        ) {
            items(STARTERS) { s ->
                Box(
                    Modifier.clip(CircleShape).glass(CircleShape).press({ send(s) }).padding(horizontal = 14.dp, vertical = 9.dp),
                ) { Text(s, style = Type.small.copy(color = c.ink)) }
            }
        }
        hint?.let { Text(it, style = Type.label.copy(color = c.saffron), modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) }
        Row(
            Modifier
                .navigationBarsPadding()
                .padding(start = 14.dp, end = 14.dp, bottom = 96.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xD9161A22))
                .border(1.dp, c.line, RoundedCornerShape(26.dp))
                .padding(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(c.glass2).press({ nav.navigate(Routes.EAT) }).semantics { contentDescription = "سجّل أكل" },
                contentAlignment = Alignment.Center,
            ) { SIcon(Ico.PLUS, size = 21.dp) }
            Box(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                if (text.isEmpty()) Text("قول لسند شنو أكلت أو شلونك…", style = Type.body.copy(color = c.faint))
                BasicTextField(
                    text, { text = it },
                    textStyle = Type.body.copy(color = c.ink),
                    cursorBrush = SolidColor(c.saffron),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send(text) }),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "رسالتك لسند" },
                )
            }
            val typing = text.isNotBlank()
            AnimatedContent(typing, transitionSpec = { (scaleIn(spring(0.5f)) + fadeIn()) togetherWith (scaleOut() + fadeOut()) }, label = "send-mic") { t ->
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(Brush.linearGradient(listOf(c.saffron, c.ember)))
                        .press({ if (t) send(text) else listen() })
                        .semantics { contentDescription = if (t) "أرسل" else "تكلّم مع سند" },
                    contentAlignment = Alignment.Center,
                ) { SIcon(if (t) Ico.SEND else Ico.MIC, size = 21.dp, tint = c.onGold) }
            }
        }
    }
}

private fun label(a: CoachAction): String = when (a) {
    is CoachAction.LogMeal -> "سجّل ${a.name}، ${ar(a.kcal)} سعرة"
    is CoachAction.LogWater -> "سجّل ${ar(a.cups)} ماء"
    is CoachAction.StartWorkout -> "ابدأ: ${routineById(a.routineId)?.title ?: "تمرين"}"
    is CoachAction.AddIfThen -> "احفظ الخطة"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Bubble(m: ChatMessage, store: AppStore, nav: NavHostController) {
    val c = Sanad.colors
    val mine = m.role == ChatRole.USER
    val confetti = LocalConfetti.current
    val appear = remember { Animatable(if (m.id == "hello") 1f else 0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 400f)) }
    Box(
        Modifier.fillMaxWidth().graphicsLayer {
            alpha = appear.value.coerceIn(0f, 1f)
            translationY = (1f - appear.value) * 10.dp.toPx()
            val s = 0.94f + 0.06f * appear.value
            scaleX = s; scaleY = s
        },
        contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        val shape = RoundedCornerShape(
            topStart = 22.dp, topEnd = 22.dp,
            bottomStart = if (mine) 22.dp else 6.dp, bottomEnd = if (mine) 6.dp else 22.dp,
        )
        Column(
            Modifier
                .widthIn(max = 320.dp)
                .then(
                    if (mine) Modifier.clip(shape).background(Brush.linearGradient(listOf(Color(0xFF2A3142), Color(0xFF232937))))
                    else Modifier.glass(shape),
                )
                .padding(horizontal = 15.dp, vertical = 12.dp),
        ) {
            Text(m.text, style = Type.body.copy(color = c.ink))
            if (m.actions.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    m.actions.forEachIndexed { i, a ->
                        val done = i in m.applied
                        val point = rememberBurstPoint()
                        Row(
                            Modifier
                                .burstFrom(point)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (done) c.oasis.copy(alpha = 0.15f) else c.ink)
                                .press({
                                    if (a is CoachAction.StartWorkout) nav.navigate(Routes.player(a.routineId))
                                    else if (!done) { store.apply(a); store.markApplied(m.id, i); confetti.burst(point.center, 30, 0.7f) }
                                }, enabled = !done || a is CoachAction.StartWorkout)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SIcon(if (a is CoachAction.StartWorkout) Ico.PLAY else if (done) Ico.CHECK else Ico.PLUS, size = 16.dp, tint = if (done) c.oasis else c.bg)
                            Spacer(Modifier.width(6.dp))
                            Text(if (done) "انسجلت" else label(a), style = Type.label.copy(color = if (done) c.oasis else c.bg, fontWeight = FontWeight.SemiBold))
                        }
                    }
                }
            }
            if (m.offline) {
                Spacer(Modifier.height(6.dp))
                Text("وضع محلي", style = Type.label.copy(color = c.faint))
            }
        }
    }
}

@Composable
private fun Thinking() {
    val c = Sanad.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        LivingOrb(26.dp, speaking = true, glow = false)
        Spacer(Modifier.width(10.dp))
        Text("سند يفكر…", style = Type.small.copy(color = c.inkSoft))
    }
}
