package app.sanad.core.ai

import app.sanad.core.AppState
import app.sanad.core.ChatRole
import app.sanad.core.CoachAction
import app.sanad.core.CoachReply
import app.sanad.core.ROUTINES
import app.sanad.core.Targets
import app.sanad.core.trendWeights
import app.sanad.core.weeklyReview
import app.sanad.core.weightPoints
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

enum class ProviderKind { CLAUDE, OPENAI_COMPAT }

/** إعدادات المزوّد كما يحفظها المستخدم (المفتاح يُخزَّن مشفّراً في التطبيق). */
data class ProviderConfig(val kind: ProviderKind, val apiKey: String, val model: String, val baseUrl: String = "")

data class ProviderPreset(
    val id: String,
    val label: String,
    val kind: ProviderKind,
    val baseUrl: String,
    val defaultModel: String,
    val keyUrl: String,
    val note: String,
)

/** قوالب جاهزة: اختر المزوّد والصق المفتاح. أي خدمة متوافقة مع OpenAI تشتغل عبر "مخصّص". */
val PRESETS = listOf(
    ProviderPreset("claude", "Claude", ProviderKind.CLAUDE, "", "claude-opus-5", "https://console.anthropic.com/settings/keys", "أقوى فهم للهجة وأدق تقدير للأكل."),
    ProviderPreset("gemini", "Google Gemini", ProviderKind.OPENAI_COMPAT, "https://generativelanguage.googleapis.com/v1beta/openai/", "gemini-2.5-flash", "https://aistudio.google.com/apikey", "عنده خطة مجانية."),
    ProviderPreset("groq", "Groq", ProviderKind.OPENAI_COMPAT, "https://api.groq.com/openai/v1/", "llama-3.3-70b-versatile", "https://console.groq.com/keys", "سريع جداً، وعنده خطة مجانية."),
    ProviderPreset("openrouter", "OpenRouter", ProviderKind.OPENAI_COMPAT, "https://openrouter.ai/api/v1/", "openrouter/auto", "https://openrouter.ai/keys", "بوابة لنماذج كثيرة بعضها مجاني."),
    ProviderPreset("custom", "مخصّص (OpenAI-compatible)", ProviderKind.OPENAI_COMPAT, "", "", "", "أي خدمة تدعم /chat/completions."),
)

data class Turn(val role: ChatRole, val text: String)

/** صورة وجبة مرفقة بآخر رسالة (JPEG/PNG/WEBP). */
class MealImage(val bytes: ByteArray, val mime: String = "image/jpeg") {
    init { require(mime in setOf("image/jpeg", "image/png", "image/webp")) { "unsupported image type" } }
    val base64: String get() = java.util.Base64.getEncoder().encodeToString(bytes)
}

class CoachException(val code: String, message: String, cause: Throwable? = null) : Exception(message, cause)

/** المدرب الذكي: يستقبل آخر الرسائل + سياق التطبيق، ويرجع رداً وإجراءات مقترحة. تُستدعى من خيط خلفي. */
interface CoachAI {
    fun reply(history: List<Turn>, context: String, image: MealImage? = null): CoachReply
    fun listModels(): List<String> = emptyList()
}

fun createCoach(cfg: ProviderConfig): CoachAI = when (cfg.kind) {
    ProviderKind.CLAUDE -> ClaudeCoach(cfg.apiKey, cfg.model)
    ProviderKind.OPENAI_COMPAT -> OpenAICompatCoach(cfg.baseUrl, cfg.apiKey, cfg.model)
}

/** آخر ١٦ رسالة تبدأ برسالة مستخدم، والسياق يُلصق بآخر رسالة مستخدم. */
fun prepareTurns(history: List<Turn>, context: String): List<Turn> {
    val recent = history.takeLast(16).dropWhile { it.role != ChatRole.USER }.toMutableList()
    require(recent.isNotEmpty() && recent.last().role == ChatRole.USER) { "last turn must be from the user" }
    val last = recent.removeAt(recent.lastIndex)
    recent += last.copy(text = "<app_context>\n$context\n</app_context>\n\n${last.text.take(2000)}")
    return recent
}

private val lenient = Json { ignoreUnknownKeys = true; isLenient = true }
private val ROUTINE_IDS = ROUTINES.map { it.id }.toSet()

/**
 * يحلّل رد النموذج بصرامة: يقبل كائن JSON (حتى لو محاط بنص أو ```)،
 * ويتجاهل أي إجراء ناقص أو خارج الحدود. لو ما فيه JSON، النص كله رد.
 */
fun parseCoachJson(raw: String): CoachReply {
    val start = raw.indexOf('{')
    val end = raw.lastIndexOf('}')
    val obj = if (start >= 0 && end > start) runCatching { lenient.parseToJsonElement(raw.substring(start, end + 1)) as? JsonObject }.getOrNull() else null
    val reply = obj?.get("reply")?.jsonPrimitive?.contentOrNull?.trim()
    if (obj == null || reply.isNullOrEmpty()) {
        val text = raw.replace(Regex("```[a-z]*"), "").trim()
        if (text.isEmpty()) throw CoachException("empty", "empty model reply")
        return CoachReply(text.take(2000), emptyList())
    }
    val actions = (obj["actions"] as? JsonArray).orEmpty().mapNotNull { el ->
        val a = el as? JsonObject ?: return@mapNotNull null
        fun s(k: String) = a[k]?.jsonPrimitive?.contentOrNull?.trim()
        fun i(k: String) = a[k]?.jsonPrimitive?.intOrNull
        when (s("type")) {
            "log_meal" -> {
                val name = s("name"); val kcal = i("kcal"); val protein = i("protein")
                if (name.isNullOrEmpty() || name.length > 80 || kcal == null || kcal !in 0..4000 || protein == null || protein !in 0..300) null
                else CoachAction.LogMeal(name, kcal, protein)
            }
            "log_water" -> i("cups")?.takeIf { it in 1..10 }?.let { CoachAction.LogWater(it) }
            "start_workout" -> s("routineId")?.takeIf { it in ROUTINE_IDS }?.let { CoachAction.StartWorkout(it) }
            "add_if_then" -> {
                val w = s("when"); val t = s("then")
                if (w.isNullOrEmpty() || t.isNullOrEmpty() || w.length > 160 || t.length > 160) null else CoachAction.AddIfThen(w, t)
            }
            else -> null
        }
    }.take(8)
    return CoachReply(reply.take(2000), actions)
}

/** بيانات التطبيق اللي يشوفها المدرب (نفس سياق نسخة الويب). */
fun coachContext(s: AppState, t: Targets, today: String, localTime: String): String {
    val p = s.profile ?: return ""
    val d = s.days[today]
    val tr = trendWeights(weightPoints(s.days.values))
    return listOf(
        "name: ${p.name}; sex: ${p.sex}; age: ${p.age}; height_cm: ${p.heightCm}",
        "start_kg: ${p.startWeightKg}; trend_kg: ${tr.lastOrNull()?.trend ?: p.startWeightKg}; goal_kg: ${p.goalWeightKg}",
        "targets: ${t.kcal} kcal, ${t.protein} g protein, ${t.steps} steps, ${t.water} cups water",
        "today: eaten ${d?.intake ?: 0} kcal, protein ${d?.protein ?: 0} g, water ${d?.water ?: 0} cups, energy ${d?.energy?.level ?: "not checked in"} (1 low–3 high), free minutes ${d?.time?.minutes ?: "?"}, slept ${d?.sleepHours?.let { "$it h" } ?: "not logged"}",
        "today_meals: ${d?.meals?.joinToString(", ") { "${it.name} ${it.kcal}kcal" }?.ifEmpty { null } ?: "none"}",
        "why: ${p.why.ifBlank { "-" }}; barriers: ${p.barriers.joinToString(",").ifEmpty { "-" }}; ramadan_mode: ${p.ramadan}",
        "health_flags: ${p.flags.joinToString(",").ifEmpty { "none" }}",
        "if_then_plans: ${p.ifThens.joinToString(" | ") { "${it.whenText} → ${it.thenText}" }.ifEmpty { "none" }}",
        weeklyReview(p, s.days, t, today).let { r ->
            "last_7_days: active ${r.activeDays}/7, food_logged ${r.foodDays}, avg_kcal ${r.avgKcal ?: "-"}, avg_protein ${r.avgProtein ?: "-"} g, protein_target_days ${r.proteinDays}, workouts ${r.workouts} (${r.workoutMinutes} min), trend_change_kg ${r.trendChangeKg ?: "unknown"} (plan ${r.expectedChangeKg}), pacing ${r.pacing}, suggested_focus ${r.focus}"
        },
        "local_time: $localTime",
    ).joinToString("\n")
}
