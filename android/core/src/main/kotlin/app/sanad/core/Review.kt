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

/** يوم أكل "مسجّل فعلاً" — أقل من ٦٠٠ سعرة غالباً تسجيل ناقص. */
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
        pacing == Pacing.ON_TRACK && change != null -> "اتجاهك نزل ${ar(abs(change))} كغ هالأسبوع — بالضبط على الخطة."
        active == 7 -> "حضرت ٧ أيام من ٧. هذي الاستمرارية اللي تغيّر الجسم."
        workouts >= 2 -> "سوّيت ${ar(workouts)} تمارين (${ar(minutes)} دقيقة) — عضلاتك محمية وأنت تنزل."
        proteinDays >= 4 -> "وصلت هدف البروتين ${ar(proteinDays)} أيام. هذا يحمي عضلك ويهدّي الجوع."
        active >= 4 -> "حضرت ${ar(active)} أيام من ٧ — ما خلّيت الأسبوع يروح."
        active >= 1 -> "رجعت وفتحت سند ${ar(active)} ${if (active == 1) "يوم" else "أيام"}. الرجوع نفسه مهارة."
        else -> "ما في أسبوع ضايع؛ نبدأ من اليوم بخطوة صغيرة."
    }

    val focusText = when (focus) {
        Focus.SHOW_UP -> "هدف الأسبوع الجاي: افتح سند وسوّ أصغر مهمة ٥ أيام — حتى لو دقيقتين حركة. لا تفوّت يومين ورا بعض."
        Focus.LOG_FOOD -> "هدف الأسبوع الجاي: سجّل أكلك ٤ أيام، ولو بجملة للمدرب. بدون تسجيل ما نقدر نعدّل خطتك صح."
        Focus.EAT_MORE -> "نزولك أسرع من ١٪ بالأسبوع. زيد أكلك ١٥٠–٢٠٠ سعرة (بروتين أفضل) — النزول السريع يأكل من العضل ويرجّع الوزن."
        Focus.PROTEIN -> "هدف الأسبوع الجاي: ${ar((t.protein / 3.0).roundToInt())} غ بروتين بكل وجبة رئيسية — بيض، زبادي يوناني، دجاج، تونة."
        Focus.STRENGTH -> "هدف الأسبوع الجاي: تمرينين قوة (١٠ دقائق تكفي). القوة مرتين بالأسبوع تحمي العضل والحرق."
        Focus.KEEP -> when (pacing) {
            Pacing.SLOW, Pacing.UP -> "التزامك ممتاز والميزان بطيء — طبيعي. أسبوع ثاني بنفس الالتزام وسند يعدّل هدفك من بياناتك."
            else -> "نفس الإيقاع بالضبط. ما تحتاج تغيّر شي — الثبات هو الخطة."
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
