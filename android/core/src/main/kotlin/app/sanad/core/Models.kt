package app.sanad.core

import kotlinx.serialization.Serializable

@Serializable
enum class Sex { M, F }

@Serializable
enum class Activity(val factor: Double, val baseSteps: Int) {
    SEDENTARY(1.2, 6000),
    LIGHT(1.375, 7000),
    MODERATE(1.55, 8000),
    ACTIVE(1.725, 9000),
}

/** نسبة النزول الأسبوعي من وزن الجسم. الحد الآمن ~١٪. */
@Serializable
enum class Pace(val weeklyRate: Double) {
    GENTLE(0.0035),
    STEADY(0.006),
    BRISK(0.009),
}

/** ١ منخفضة، ٢ متوسطة، ٣ عالية */
@Serializable
enum class Energy(val level: Int) { LOW(1), MID(2), HIGH(3) }

/** الدقائق المتاحة للحركة اليوم */
@Serializable
enum class TimeBudget(val minutes: Int) { TWO(2), TEN(10), TWENTY(20) }

@Serializable
enum class Barrier { TIME, ENERGY, NIGHT, SOCIAL, STRESS, SWEETS }

@Serializable
enum class SafetyFlag { UNDER18, PREGNANT, EATING_DISORDER, DIABETES_MEDS, GLP1, LOW_BMI, HEART }

@Serializable
data class IfThen(val id: String, val whenText: String, val thenText: String)

@Serializable
data class Profile(
    val name: String,
    val sex: Sex,
    val age: Int,
    val heightCm: Double,
    val startWeightKg: Double,
    val goalWeightKg: Double,
    val activity: Activity,
    val pace: Pace,
    val why: String = "",
    val barriers: List<Barrier> = emptyList(),
    val ifThens: List<IfThen> = emptyList(),
    val ramadan: Boolean = false,
    val flags: List<SafetyFlag> = emptyList(),
    /** yyyy-MM-dd */
    val createdAt: String,
)

@Serializable
enum class MealSource { DB, COACH, QUICK, PHOTO }

@Serializable
data class MealEntry(
    val id: String,
    val name: String,
    val kcal: Int,
    val protein: Int,
    val at: Long,
    val source: MealSource,
)

@Serializable
data class WorkoutEntry(val id: String, val routineId: String, val name: String, val minutes: Int, val at: Long)

@Serializable
data class DayLog(
    /** yyyy-MM-dd */
    val date: String,
    val energy: Energy? = null,
    val time: TimeBudget? = null,
    val meals: List<MealEntry> = emptyList(),
    val workouts: List<WorkoutEntry> = emptyList(),
    val water: Int = 0,
    val steps: Int = 0,
    val done: List<String> = emptyList(),
    val weightKg: Double? = null,
) {
    val intake: Int get() = meals.sumOf { it.kcal }
    val protein: Int get() = meals.sumOf { it.protein }
}

@Serializable
enum class ChatRole { USER, COACH }

@Serializable
sealed class CoachAction {
    @Serializable
    data class LogMeal(val name: String, val kcal: Int, val protein: Int) : CoachAction()

    @Serializable
    data class LogWater(val cups: Int) : CoachAction()

    @Serializable
    data class StartWorkout(val routineId: String) : CoachAction()

    @Serializable
    data class AddIfThen(val whenText: String, val thenText: String) : CoachAction()
}

@Serializable
data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val text: String,
    val at: Long,
    val actions: List<CoachAction> = emptyList(),
    val applied: List<Int> = emptyList(),
    val offline: Boolean = false,
)

@Serializable
data class AppState(
    val version: Int = 1,
    val profile: Profile? = null,
    val days: Map<String, DayLog> = emptyMap(),
    val chat: List<ChatMessage> = emptyList(),
    val favorites: List<String> = emptyList(),
)
