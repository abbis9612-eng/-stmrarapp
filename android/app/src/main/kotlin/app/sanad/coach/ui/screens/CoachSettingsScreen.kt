package app.sanad.coach.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import app.sanad.coach.data.CoachSettings
import app.sanad.coach.ui.components.BtnStyle
import app.sanad.coach.ui.components.Ico
import app.sanad.coach.ui.components.NightCard
import app.sanad.coach.ui.components.Note
import app.sanad.coach.ui.components.SButton
import app.sanad.coach.ui.components.SCard
import app.sanad.coach.ui.components.SChip
import app.sanad.coach.ui.components.SIcon
import app.sanad.coach.ui.components.press
import app.sanad.coach.ui.theme.Sanad
import app.sanad.coach.ui.theme.Type
import app.sanad.core.ChatRole
import app.sanad.core.ai.CoachException
import app.sanad.core.ai.PRESETS
import app.sanad.core.ai.ProviderConfig
import app.sanad.core.ai.ProviderKind
import app.sanad.core.ai.Turn
import app.sanad.core.ai.createCoach
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun coachErrorText(e: Throwable): String = when ((e as? CoachException)?.code) {
    "limit" -> "انتهت رسائل المدرب الذكي لهذا اليوم. تعود غداً، والمدرب المحلي معك الآن."
    "auth" -> "المفتاح غير صحيح أو ليست له صلاحية."
    "busy" -> "المزوّد مشغول أو تجاوزت حد الاستخدام المجاني. جرّب بعد قليل."
    "network" -> "لم نستطع الوصول إلى المزوّد. تأكد من الإنترنت."
    "bad_output" -> "أعاد المزوّد رداً غير مفهوم. جرّب نموذجاً آخر."
    else -> "حدث خطأ من المزوّد: ${e.message?.take(120) ?: "غير معروف"}"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CoachSettingsScreen(settings: CoachSettings, nav: NavHostController) {
    val c = Sanad.colors
    val current by settings.state.collectAsStateWithLifecycle()
    val uri = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var presetId by rememberSaveable { mutableStateOf(current?.preset?.id ?: "gemini") }
    val preset = PRESETS.first { it.id == presetId }
    var model by rememberSaveable(presetId) { mutableStateOf(if (current?.preset?.id == presetId) current!!.model else preset.defaultModel) }
    var base by rememberSaveable(presetId) { mutableStateOf(if (current?.preset?.id == presetId) current!!.baseUrl else preset.baseUrl) }
    var key by rememberSaveable { mutableStateOf("") }
    var status by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var models by remember { mutableStateOf(listOf<String>()) }
    val keyReady = key.isNotBlank() || (current?.hasKey == true && current?.preset?.id == presetId)

    fun cfg(): ProviderConfig? {
        val k = key.ifBlank { settings.config()?.takeIf { current?.preset?.id == presetId }?.apiKey.orEmpty() }
        return if (k.isBlank()) null else ProviderConfig(preset.kind, k, model.trim(), base.trim())
    }

    Page(bottom = false) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(c.surface2).press({ nav.popBackStack() }), contentAlignment = Alignment.Center) {
                    SIcon(Ico.BACK, description = "رجوع")
                }
                Spacer(Modifier.width(12.dp))
                Text("المدرب الذكي", style = Type.h1.copy(color = c.ink))
            }
        }
        item {
            NightCard {
                if (settings.cloudAvailable) {
                    Text("المدرب الذكي يعمل", style = Type.h2.copy(color = c.ink))
                    Text("المدرب السحابي مفعّل للجميع بلا مفتاح ولا إعداد. إن أردت استعمال مفتاحك الخاص بدلاً منه، فاختره بالأسفل (اختياري).", style = Type.small.copy(color = c.inkSoft))
                } else {
                    Text("اربط المدرب بعقل حقيقي", style = Type.h2.copy(color = c.ink))
                    Text("اختر المزوّد، والصق مفتاح الـ API، واختبر. بلا مفتاح يعمل المدرب المحلي.", style = Type.small.copy(color = c.inkSoft))
                }
                if (current != null) {
                    Spacer(Modifier.height(8.dp))
                    Text("الحالي: ${current!!.label}، ${current!!.model}" + if (current!!.hasKey) " ✓" else " (بلا مفتاح)", style = Type.label.copy(color = c.saffron))
                }
            }
        }
        item {
            SCard {
                Text("المزوّد", style = Type.h3.copy(color = c.inkSoft))
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PRESETS.forEach { p -> SChip(p.label, presetId == p.id, { presetId = p.id; models = emptyList(); status = null }) }
                }
                Spacer(Modifier.height(8.dp))
                Text(preset.note, style = Type.small.copy(color = c.inkSoft))
                if (preset.keyUrl.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    SButton("احصل على مفتاح", { uri.openUri(preset.keyUrl) }, style = BtnStyle.GHOST, small = true, icon = Ico.NEXT)
                }
            }
        }
        item {
            SCard {
                Text("مفتاح الـ API", style = Type.h3.copy(color = c.inkSoft))
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 54.dp).clip(RoundedCornerShape(16.dp)).background(c.surface).border(1.5.dp, c.line, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (key.isEmpty()) Text(if (keyReady) "المفتاح محفوظ، الصق مفتاحاً جديداً لتغييره" else "الصق المفتاح هنا", style = Type.body.copy(color = c.inkSoft))
                    BasicTextField(
                        key, { key = it }, singleLine = true, textStyle = Type.body.copy(color = c.ink), cursorBrush = SolidColor(c.primary),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "مفتاح الـ API" },
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text("النموذج", style = Type.h3.copy(color = c.inkSoft))
                Spacer(Modifier.height(6.dp))
                SField(model, { model = it }, "اسم النموذج", Modifier.fillMaxWidth())
                if (preset.kind == ProviderKind.OPENAI_COMPAT) {
                    Spacer(Modifier.height(12.dp))
                    Text("رابط الخدمة", style = Type.h3.copy(color = c.inkSoft))
                    Spacer(Modifier.height(6.dp))
                    SField(base, { base = it }, "https://…/v1/", Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    SButton("اعرض النماذج المتاحة", {
                        val cf = cfg() ?: return@SButton
                        busy = true
                        scope.launch {
                            models = withContext(Dispatchers.IO) { runCatching { createCoach(cf).listModels() }.getOrDefault(emptyList()) }
                            if (models.isEmpty()) status = false to "لم نستطع جلب القائمة؛ اكتب اسم النموذج يدوياً."
                            busy = false
                        }
                    }, style = BtnStyle.SOFT, small = true, enabled = keyReady && !busy)
                    if (models.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            models.take(40).forEach { m -> SChip(m, model == m, { model = m }) }
                        }
                    }
                }
            }
        }
        status?.let { (ok, msg) -> item { Note(msg, alert = !ok) } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SButton("اختبر الاتصال", {
                    val cf = cfg() ?: return@SButton
                    busy = true; status = null
                    scope.launch {
                        status = withContext(Dispatchers.IO) {
                            runCatching { createCoach(cf).reply(listOf(Turn(ChatRole.USER, "قل: جاهز. بكلمة واحدة.")), "test") }
                                .fold({ true to "يعمل ✓ رد المدرب: ${it.text.take(80)}" }, { false to coachErrorText(it) })
                        }
                        busy = false
                    }
                }, Modifier.weight(1f), style = BtnStyle.SOFT, enabled = keyReady && model.isNotBlank() && !busy)
                SButton("احفظ", {
                    settings.save(preset, model, base, key.ifBlank { null })
                    key = ""
                    status = true to "حُفظ. المدرب الذكي مفعّل الآن."
                }, Modifier.weight(1f), enabled = keyReady && model.isNotBlank() && (preset.kind == ProviderKind.CLAUDE || base.startsWith("https://")))
            }
        }
        if (current != null) item {
            SButton("افصل المدرب الذكي", { settings.clear(); status = true to "عدنا إلى المدرب المحلي." }, Modifier.fillMaxWidth(), style = BtnStyle.GHOST)
        }
        item {
            Text(
                "خصوصيتك: رسائلك وملخص يومك (السعرات، الوزن الاتجاهي، الطاقة) تُرسل إلى المزوّد الذي تختاره فقط، والمفتاح مشفّر داخل جهازك بـ Android Keystore.",
                style = Type.label.copy(color = c.inkSoft),
            )
        }
    }
}
