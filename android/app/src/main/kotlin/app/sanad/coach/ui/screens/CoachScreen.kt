package app.sanad.coach.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import app.sanad.coach.data.AppStore
import app.sanad.coach.data.CoachSettings
import app.sanad.coach.data.targets
import app.sanad.coach.ui.Routes
import app.sanad.coach.ui.components.BtnStyle
import app.sanad.coach.ui.components.Ico
import app.sanad.coach.ui.components.SButton
import app.sanad.coach.ui.components.SChip
import app.sanad.coach.ui.components.SIcon
import app.sanad.coach.ui.components.SaduLogo
import app.sanad.coach.ui.components.press
import app.sanad.coach.ui.theme.Sanad
import app.sanad.coach.ui.theme.Type
import app.sanad.core.AppState
import app.sanad.core.ChatMessage
import app.sanad.core.ChatRole
import app.sanad.core.CoachAction
import app.sanad.core.ar
import app.sanad.core.offlineReply
import app.sanad.core.routineById
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sanad.core.ai.Turn
import app.sanad.core.ai.coachContext
import app.sanad.core.ai.createCoach
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

private val STARTERS = listOf("تغديت كبسة دجاج ولبن", "اليوم تعبان مرة", "عندي عزيمة الليلة", "ليش وزني ما نزل؟", "يجيني جوع بالليل")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CoachScreen(store: AppStore, settings: CoachSettings, state: AppState, nav: NavHostController, initialQuestion: String?) {
    val c = Sanad.colors
    val p = state.profile ?: return
    var text by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val list = rememberLazyListState()
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val smart by settings.state.collectAsStateWithLifecycle()

    fun send(raw: String) {
        val msg = raw.trim()
        if (msg.isEmpty() || busy) return
        text = ""
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
                delay(400)
                val r = offlineReply(msg, s, t, AppStore.today())
                store.pushChat(ChatRole.COACH, r.text, r.actions, offline = true)
            }
            busy = false
        }
    }

    LaunchedEffect(initialQuestion) { initialQuestion?.let { if (it.isNotBlank()) send(it) } }
    LaunchedEffect(state.chat.size, busy) { if (state.chat.isNotEmpty()) list.animateScrollToItem(state.chat.size + 1) }

    Column(Modifier.fillMaxSize().imePadding()) {
        LazyColumn(
            state = list,
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = top + 14.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SaduLogo(size = 52.dp, onDark = false)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("سند", style = Type.h1.copy(color = c.ink))
                        Text(if (smart?.hasKey == true) "ذكي، ${smart!!.label}" else "المدرب المحلي", style = Type.label.copy(color = if (smart?.hasKey == true) c.palm else c.inkSoft))
                    }
                    if (state.chat.isNotEmpty()) SButton("جديدة", { store.clearChat() }, style = BtnStyle.GHOST, small = true)
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.size(44.dp).clip(CircleShape).background(c.surface2).press({ nav.navigate(Routes.COACH_SETTINGS) }).semantics { contentDescription = "إعدادات المدرب الذكي" }, contentAlignment = Alignment.Center) {
                        SIcon(Ico.SETTINGS, size = 22.dp)
                    }
                }
            }
            item {
                Bubble(ChatMessage("hello", ChatRole.COACH, "هلا ${p.name} 👋 أنا سند. قل لي وش أكلت وأحسبه لك، أو قل كيف يومك وأفصّل لك خطوة تناسبك.", 0), store, nav)
            }
            items(state.chat, key = { it.id }) { m -> Bubble(m, store, nav) }
            if (busy) item { Typing() }
            if (smart?.hasKey != true && state.chat.isEmpty()) item {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.dateSoft).press({ nav.navigate(Routes.COACH_SETTINGS) }).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SIcon(Ico.SPARK, tint = c.sadu)
                    Spacer(Modifier.width(10.dp))
                    Text("فعّل المدرب الذكي بمفتاح API (Claude أو Gemini المجاني…)", style = Type.small.copy(color = c.ink), modifier = Modifier.weight(1f))
                    SIcon(Ico.NEXT, size = 20.dp)
                }
            }
            if (state.chat.isEmpty()) item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    STARTERS.forEach { s -> SChip(s, false, { send(s) }) }
                }
            }
        }
        Row(
            Modifier
                // فوق شريط التنقل (ارتفاعه ~٨٨dp + شريط النظام)
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, bottom = 96.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(c.surface)
                .border(1.dp, c.line, RoundedCornerShape(28.dp))
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                if (text.isEmpty()) Text("اكتب لسند…", style = Type.body.copy(color = c.inkSoft))
                BasicTextField(text, { text = it }, textStyle = Type.body.copy(color = c.ink), cursorBrush = SolidColor(c.sadu), maxLines = 4,
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "رسالتك لسند" })
            }
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(if (c.isDark) c.date else c.night).press({ send(text) }).semantics { contentDescription = "أرسل" },
                contentAlignment = Alignment.Center,
            ) { SIcon(Ico.SEND, size = 22.dp, tint = if (c.isDark) Color(0xFF1B1406) else Color.White) }
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
    Box(Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(
            Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(topStart = if (mine) 22.dp else 6.dp, topEnd = if (mine) 6.dp else 22.dp, bottomStart = 22.dp, bottomEnd = 22.dp))
                .background(if (mine) (if (c.isDark) c.night2 else c.night) else c.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(m.text, style = Type.body.copy(color = if (mine) Color.White else c.ink))
            if (m.actions.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    m.actions.forEachIndexed { i, a ->
                        val done = i in m.applied
                        SButton(
                            if (done) "تم ✓" else label(a),
                            {
                                if (a is CoachAction.StartWorkout) nav.navigate(Routes.player(a.routineId))
                                else if (!done) { store.apply(a); store.markApplied(m.id, i) }
                            },
                            small = true,
                            style = if (done) BtnStyle.SOFT else BtnStyle.PRIMARY,
                            enabled = !done,
                            icon = if (a is CoachAction.StartWorkout) Ico.PLAY else if (done) null else Ico.PLUS,
                        )
                    }
                }
            }
            if (m.offline) {
                Spacer(Modifier.height(6.dp))
                Text("وضع محلي", style = Type.label.copy(color = c.inkSoft))
            }
        }
    }
}

@Composable
private fun Typing() {
    val c = Sanad.colors
    val t = rememberInfiniteTransition(label = "typing")
    Row(Modifier.clip(RoundedCornerShape(18.dp)).background(c.surface).padding(horizontal = 18.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(3) { i ->
            val a by t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(600, delayMillis = i * 160), RepeatMode.Reverse), label = "dot$i")
            Box(Modifier.size(8.dp).alpha(a).clip(CircleShape).background(c.inkSoft))
        }
    }
}
