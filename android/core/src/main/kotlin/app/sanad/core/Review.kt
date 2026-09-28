package app.sanad.core

import kotlin.math.abs
import kotlin.math.roundToInt

/* ---------------- مراجعة الأسبوع ---------------- */

enum class Pacing { NO_DATA, TOO_FAST, ON_TRACK, SLOW, UP }

enum class Focus { SHOW_UP, LOG_FOOD, PROTEIN, STRENGTH, EAT_MORE, KEEP }

data class WeeklyReview(
    /** أول وآخر يوم في نافذة الأيام السبعة (آخرها اليوم) */
    val from: String,
    val to: String,
    val activeDays: Int,
    val foodDays: Int,
    val avgKcal: Int?,
    val avgProtein: Int?,
    val proteinDays: Int,
    val kcalDays: Int,
    val workouts: Int,
    val workoutMinutes: Int,
    /** تغيّر الوزن الاتجاهي خلال الأسبوع (سالب = نزول) */
    val trendChangeKg: Double?,
    val expectedChangeKg: Double,
    val pacing: Pacing,
    val focus: Focus,
    val win: String,
    val focusText: String,
)

/** يوم أكل "مسجّل فعلاً" — أقل من 600 سعرة غالباً تسجيل ناقص. */
private fun hasFood(d: DayLog) = d.intake >= 600

/**
 * مراجعة أسبوعية مثل جلسة المدرب: نجاح واحد يُحتفل به، وتركيز واحد فقط للأسبوع الجاي
 * (تغيير سلوك واحد في كل مرة أنجح من قائمة طويلة).
 */
fun weeklyReview(p: Profile, days: Map<String, DayLog>, t: Targets, today: String): WeeklyReview {
    val from = addDays(today, -6)
    val week = (0L..6L).map { addDays(from, it) }.mapNotNull { days[it] }
    val food = week.filter(::hasFood)
    val proteinDays = food.count { it.protein >= t.protein * 0.9 }
    val kcalDays = food.count { it.intake <= t.kcal * 1.1 }
    val workouts = week.sumOf { it.workouts.size }
    val minutes = week.sumOf { d -> d.workouts.sumOf { it.minutes } }
    val active = (0L..6L).count { isCounted(days[addDays(from, it)]) }

    // الاتجاه: من آخر قراءة قبل الأسبوع (أو أول قراءة فيه) إلى آخر قراءة فيه
    val trend = trendWeights(weightPoints(days.values.filter { it.date <= today }))
    val end = trend.lastOrNull { it.date >= from }
    val start = trend.lastOrNull { it.date < from } ?: trend.firstOrNull { it.date >= from }
    val change = if (end != null && start != null && end.date != start.date && daysBetween(start.date, end.date) >= 4) {
        val raw = (end.trend - start.trend) * 7.0 / daysBetween(start.date, end.date).coerceAtLeast(7)
        (raw * 100).roundToInt() / 100.0
    } else null
    val expected = -t.weeklyLossKg
    val weight = end?.trend ?: p.startWeightKg

    val pacing = when {
        change == null -> Pacing.NO_DATA
        change < -0.01 * weight -> Pacing.TOO_FAST
        change > 0.2 -> Pacing.UP
        change > expected * 0.4 -> Pacing.SLOW
        else -> Pacing.ON_TRACK
    }

    val focus = when {
        active < 4 -> Focus.SHOW_UP
        food.size < 4 -> Focus.LOG_FOOD
        pacing == Pacing.TOO_FAST -> Focus.EAT_MORE
        proteinDays < 4 -> Focus.PROTEIN
        workouts < 2 -> Focus.STRENGTH
        else -> Focus.KEEP
    }

    val win = when {
        pacing == Pacing.ON_TRACK && change != null -> "نزل اتجاهك ${ar(abs(change))} كغ هذا الأسبوع، على الخطة تماماً."
        active == 7 -> "حضرت 7 أيام من 7. هذه الاستمرارية هي ما يغيّر الجسم."
        workouts >= 2 -> "أدّيت ${ar(workouts)} تمارين (${ar(minutes)} دقيقة)، فعضلاتك محمية وأنت تنزل."
        proteinDays >= 4 -> "بلغت هدف البروتين ${ar(proteinDays)} أيام. هذا يحمي عضلاتك ويهدّئ الجوع."
        active >= 4 -> "حضرت ${ar(active)} أيام من 7، ولم تترك الأسبوع يضيع."
        active >= 1 -> "عدت إلى «تحرّك» ${ar(active)} ${if (active == 1) "يوماً" else "أيام"}. العودة نفسها مهارة."
        else -> "لا يوجد أسبوع ضائع؛ نبدأ اليوم بخطوة صغيرة."
    }

    val focusText = when (focus) {
        Focus.SHOW_UP -> "هدف الأسبوع القادم: افتح «تحرّك» وأنجز أصغر مهمة 5 أيام، ولو دقيقتين من الحركة. لا تفوّت يومين متتاليين."
        Focus.LOG_FOOD -> "هدف الأسبوع القادم: سجّل أكلك 4 أيام، ولو بجملة لسند. بلا تسجيل لا نستطيع تعديل خطتك بدقة."
        Focus.EAT_MORE -> "نزولك أسرع من 1% في الأسبوع. زِد أكلك 150–200 سعرة (والبروتين أفضل)، فالنزول السريع يأكل من العضل ويعيد الوزن."
        Focus.PROTEIN -> "هدف الأسبوع القادم: ${ar((t.protein / 3.0).roundToInt())} غ بروتين في كل وجبة رئيسية: بيض، زبادي، دجاج، تونة."
        Focus.STRENGTH -> "هدف الأسبوع القادم: تمرينا قوة (10 دقائق تكفي). القوة مرتين في الأسبوع تحمي العضل والحرق."
        Focus.KEEP -> when (pacing) {
            Pacing.SLOW, Pacing.UP -> "التزامك ممتاز والميزان بطيء، وهذا طبيعي. أسبوع آخر بالالتزام نفسه وسيعدّل سند هدفك من بياناتك."
            else -> "الإيقاع نفسه تماماً. لا تحتاج إلى تغيير شيء، فالثبات هو الخطة."
        }
    }

    return WeeklyReview(
        from = from, to = today,
        activeDays = active, foodDays = food.size,
        avgKcal = food.takeIf { it.isNotEmpty() }?.map { it.intake }?.average()?.roundToInt(),
        avgProtein = food.takeIf { it.isNotEmpty() }?.map { it.protein }?.average()?.roundToInt(),
        proteinDays = proteinDays, kcalDays = kcalDays,
        workouts = workouts, workoutMinutes = minutes,
        trendChangeKg = change, expectedChangeKg = expected,
        pacing = pacing, focus = focus, win = win, focusText = focusText,
    )
}
