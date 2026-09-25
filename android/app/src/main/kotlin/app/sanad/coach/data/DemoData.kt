package app.sanad.coach.data

import app.sanad.core.Activity
import app.sanad.core.AppState
import app.sanad.core.Barrier
import app.sanad.core.DayLog
import app.sanad.core.Energy
import app.sanad.core.IfThen
import app.sanad.core.MealEntry
import app.sanad.core.MealSource
import app.sanad.core.Pace
import app.sanad.core.Profile
import app.sanad.core.Sex
import app.sanad.core.TimeBudget
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.math.sin

/** بيانات تجريبية واقعية (٣ أسابيع) للقطات الشاشة في نسخة المطوّر فقط. */
fun demoState(today: LocalDate = LocalDate.now()): AppState {
    val skip = setOf(4, 11, 12)
    val days = (20 downTo 0).mapNotNull { back ->
        val idx = 20 - back
        if (idx in skip) return@mapNotNull null
        val date = today.minusDays(back.toLong()).toString()
        val isToday = back == 0
        date to DayLog(
            date = date,
            energy = if (isToday) null else Energy.entries[idx % 3],
            time = if (isToday) null else TimeBudget.entries[idx % 3],
            meals = if (isToday) listOf(MealEntry("t1", "بيض مسلوق", 155, 13, 0, MealSource.DB)) else listOf(
                MealEntry("a$idx", "بيض مسلوق", 155, 13, 0, MealSource.DB),
                MealEntry("b$idx", "كبسة دجاج", 650, 38, 0, MealSource.COACH),
                MealEntry("c$idx", "زبادي يوناني", 100, 17, 0, MealSource.DB),
                MealEntry("d$idx", "شيش طاووق", 330, 42, 0, MealSource.DB),
                MealEntry("e$idx", "شاي كرك", 150, 3, 0, MealSource.DB),
            ),
            water = if (isToday) 3 else 6,
            done = if (isToday) emptyList() else listOf("move", "eat"),
            weightKg = if (idx % 2 == 0 || idx > 17) ((96 - idx * 0.09 + sin(idx * 1.7) * 0.6) * 10).roundToInt() / 10.0 else null,
        )
    }.toMap()
    return AppState(
        profile = Profile(
            name = "أبو فهد", sex = Sex.M, age = 38, heightCm = 174.0, startWeightKg = 96.0, goalWeightKg = 84.0,
            activity = Activity.SEDENTARY, pace = Pace.STEADY, why = "أتحرك بخفة مع عيالي",
            barriers = listOf(Barrier.TIME, Barrier.NIGHT, Barrier.SOCIAL),
            ifThens = listOf(
                IfThen("1", "إذا جاني جوع بعد الساعة ٩", "أشرب شاي أو ماء، وإذا استمر آكل زبادي يوناني"),
                IfThen("2", "إذا عندي عزيمة", "آكل بروتين خفيف قبلها وآخذ صحن واحد"),
            ),
            createdAt = today.minusDays(20).toString(),
        ),
        days = days,
        favorites = listOf("greek-yogurt", "eggs-2"),
    )
}
