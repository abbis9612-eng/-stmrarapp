package app.sanad.coach.data

import android.content.Context
import app.sanad.core.AppState
import app.sanad.core.ChatMessage
import app.sanad.core.ChatRole
import app.sanad.core.CoachAction
import app.sanad.core.DayLog
import app.sanad.core.Energy
import app.sanad.core.IfThen
import app.sanad.core.MealEntry
import app.sanad.core.MealSource
import app.sanad.core.Profile
import app.sanad.core.Targets
import app.sanad.core.TimeBudget
import app.sanad.core.WorkoutEntry
import app.sanad.core.adaptiveTdee
import app.sanad.core.computeTargets
import app.sanad.core.routineById
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate
import java.util.UUID

/** مخزن محلي: كل البيانات على الجهاز في ملف JSON واحد. */
class AppStore(context: Context) {
    private val file = File(context.filesDir, "sanad-state.json")
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private fun load(): AppState = try {
        if (file.exists()) json.decodeFromString(AppState.serializer(), file.readText()) else AppState()
    } catch (_: Exception) {
        AppState()
    }

    private fun persist(s: AppState) {
        io.launch {
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.writeText(json.encodeToString(AppState.serializer(), s))
            tmp.renameTo(file)
        }
    }

    private fun set(transform: (AppState) -> AppState) {
        _state.update(transform)
        persist(_state.value)
    }

    private fun updateDay(date: String = today(), fn: (DayLog) -> DayLog) = set { s ->
        val d = s.days[date] ?: DayLog(date)
        s.copy(days = s.days + (date to fn(d)))
    }

    fun saveProfile(p: Profile) {
        set { it.copy(profile = p) }
        updateDay { it.copy(weightKg = it.weightKg ?: p.startWeightKg) }
    }

    fun setRamadan(on: Boolean) = set { s -> s.profile?.let { s.copy(profile = it.copy(ramadan = on)) } ?: s }

    fun checkIn(energy: Energy, time: TimeBudget) = updateDay { it.copy(energy = energy, time = time) }

    fun clearCheckIn() = updateDay { it.copy(energy = null, time = null) }

    fun toggleMission(id: String) = updateDay { d ->
        d.copy(done = if (id in d.done) d.done - id else d.done + id)
    }

    fun addMeal(name: String, kcal: Int, protein: Int, source: MealSource) = updateDay { d ->
        d.copy(
            meals = d.meals + MealEntry(uid(), name, kcal, protein, System.currentTimeMillis(), source),
            done = if ("eat" in d.done) d.done else d.done + "eat",
        )
    }

    fun removeMeal(id: String) = updateDay { d -> d.copy(meals = d.meals.filterNot { it.id == id }) }

    fun addWater(delta: Int) = updateDay { d -> d.copy(water = (d.water + delta).coerceIn(0, 20)) }

    fun logWorkout(routineId: String) {
        val r = routineById(routineId) ?: return
        updateDay { d ->
            d.copy(
                workouts = d.workouts + WorkoutEntry(uid(), r.id, r.title, r.minutes, System.currentTimeMillis()),
                done = if ("move" in d.done) d.done else d.done + "move",
            )
        }
    }

    fun logWeight(kg: Double) = updateDay { it.copy(weightKg = Math.round(kg * 10) / 10.0) }

    fun toggleFavorite(foodId: String) = set { s ->
        s.copy(favorites = if (foodId in s.favorites) s.favorites - foodId else s.favorites + foodId)
    }

    fun addIfThen(whenText: String, thenText: String) = set { s ->
        val p = s.profile ?: return@set s
        s.copy(profile = p.copy(ifThens = p.ifThens + IfThen(uid(), whenText, thenText)))
    }

    fun removeIfThen(id: String) = set { s ->
        val p = s.profile ?: return@set s
        s.copy(profile = p.copy(ifThens = p.ifThens.filterNot { it.id == id }))
    }

    fun pushChat(role: ChatRole, text: String, actions: List<CoachAction> = emptyList(), offline: Boolean = false, image: String? = null) = set { s ->
        s.copy(chat = (s.chat + ChatMessage(uid(), role, text, System.currentTimeMillis(), actions, offline = offline, image = image)).takeLast(80))
    }

    /** يحفظ صورة الوجبة مضغوطة (أطول ضلع ١٢٨٠) داخل ملفات التطبيق ويرجع مسارها وبياناتها. */
    fun saveMealPhoto(bytes: ByteArray): Pair<String, ByteArray>? {
        val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        if (opts.outWidth <= 0) return null
        var sample = 1
        while (maxOf(opts.outWidth, opts.outHeight) / (sample * 2) >= 1280) sample *= 2
        val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val out = java.io.ByteArrayOutputStream()
        bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
        val data = out.toByteArray()
        val dir = File(file.parentFile, "meals").apply { mkdirs() }
        val f = File(dir, "${uid()}.jpg")
        f.writeBytes(data)
        return f.absolutePath to data
    }

    fun markApplied(msgId: String, index: Int) = set { s ->
        s.copy(chat = s.chat.map { if (it.id == msgId) it.copy(applied = it.applied + index) else it })
    }

    fun clearChat() = set { it.copy(chat = emptyList()) }

    /** ينفّذ اقتراح المدرب بعد موافقة المستخدم ويرجع وصفاً قصيراً. */
    fun apply(a: CoachAction): String = when (a) {
        is CoachAction.LogMeal -> { addMeal(a.name, a.kcal, a.protein, MealSource.COACH); "سُجّل: ${a.name}" }
        is CoachAction.LogWater -> { addWater(a.cups); "+${a.cups} ماء" }
        is CoachAction.AddIfThen -> { addIfThen(a.whenText, a.thenText); "انحفظت الخطة" }
        is CoachAction.StartWorkout -> ""
    }

    fun reset() = set { AppState() }

    fun replaceAll(s: AppState) = set { s }

    companion object {
        fun today(): String = LocalDate.now().toString()
        fun uid(): String = UUID.randomUUID().toString().take(12)
    }
}

fun AppState.latestWeight(): Double? =
    days.values.sortedByDescending { it.date }.firstNotNullOfOrNull { it.weightKg } ?: profile?.startWeightKg

fun AppState.targets(): Targets? {
    val p = profile ?: return null
    val w = latestWeight() ?: p.startWeightKg
    return computeTargets(p, w, adaptiveTdee(p, days.values, AppStore.today()))
}

fun AppState.today(): DayLog = days[AppStore.today()] ?: DayLog(AppStore.today())
