package app.sanad.core

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/** طاقة تقريبية لكل كغ من نسيج الجسم المفقود (خليط دهون وعضل). */
const val KCAL_PER_KG = 7700.0

fun calorieFloor(sex: Sex): Int = if (sex == Sex.F) 1200 else 1500

private fun roundTo(x: Double, step: Int): Int = ((x / step).roundToInt()) * step

fun bmi(weightKg: Double, heightCm: Double): Double {
    val h = heightCm / 100
    return weightKg / (h * h)
}

/** معادلة Mifflin-St Jeor — الأدق بين المعادلات الشائعة للبالغين. */
fun bmr(sex: Sex, weightKg: Double, heightCm: Double, age: Int): Double {
    val base = 10 * weightKg + 6.25 * heightCm - 5 * age
    return if (sex == Sex.M) base + 5 else base - 161
}

fun formulaTdee(p: Profile, weightKg: Double): Double = bmr(p.sex, weightKg, p.heightCm, p.age) * p.activity.factor

/** وزن مرجعي للبروتين: عند زيادة الوزن = الوزن عند BMI 25 + ربع الفائض. */
fun referenceWeight(weightKg: Double, heightCm: Double): Double {
    val h = heightCm / 100
    val at25 = 25 * h * h
    return if (weightKg <= at25) weightKg else at25 + 0.25 * (weightKg - at25)
}

/** ١٫٢–١٫٦ غ/كغ أثناء النزول للحفاظ على العضل؛ نستهدف ١٫٥ من الوزن المرجعي. */
fun proteinTarget(weightKg: Double, heightCm: Double): Int = roundTo(1.5 * referenceWeight(weightKg, heightCm), 5)

data class Targets(
    val tdee: Int,
    val kcal: Int,
    val protein: Int,
    val weeklyLossKg: Double,
    val steps: Int,
    val water: Int,
    val floorApplied: Boolean,
    val adaptive: AdaptiveResult? = null,
)

fun computeTargets(p: Profile, weightKg: Double, adaptive: AdaptiveResult? = null): Targets {
    val tdee = adaptive?.tdee?.toDouble() ?: formulaTdee(p, weightKg)
    val loss = p.pace.weeklyRate * weightKg
    val raw = tdee - loss * KCAL_PER_KG / 7
    val floor = calorieFloor(p.sex)
    return Targets(
        tdee = roundTo(tdee, 10),
        kcal = roundTo(max(raw, floor.toDouble()), 10),
        protein = proteinTarget(weightKg, p.heightCm),
        weeklyLossKg = (loss * 100).roundToInt() / 100.0,
        steps = p.activity.baseSteps,
        water = 8,
        floorApplied = raw < floor,
        adaptive = adaptive,
    )
}

/* ---------------- الوزن الاتجاهي ---------------- */

data class TrendPoint(val date: String, val kg: Double, val trend: Double)

fun daysBetween(a: String, b: String): Long = ChronoUnit.DAYS.between(LocalDate.parse(a), LocalDate.parse(b))

/** EMA بمعامل ٠٫١ يومياً؛ الفجوة الأطول تعطي القراءة الجديدة وزناً أكبر. */
fun trendWeights(points: List<Pair<String, Double>>, alpha: Double = 0.1): List<TrendPoint> {
    var trend: Double? = null
    var last: String? = null
    return points.sortedBy { it.first }.map { (date, kg) ->
        val t = trend
        trend = if (t == null || last == null) kg else {
            val gap = max(1L, daysBetween(last!!, date)).toInt()
            val a = 1 - (1 - alpha).pow(gap)
            t + a * (kg - t)
        }
        last = date
        TrendPoint(date, kg, (trend!! * 100).roundToInt() / 100.0)
    }
}

fun weightPoints(days: Collection<DayLog>): List<Pair<String, Double>> =
    days.mapNotNull { d -> d.weightKg?.let { d.date to it } }

/* ---------------- الحرق التكيّفي ---------------- */

data class AdaptiveResult(val tdee: Int, val confidence: Double, val loggedDays: Int, val windowDays: Long)

/**
 * نافذة حتى ٢١ يوماً: الحرق ≈ متوسط الأكل − (تغيّر الوزن الاتجاهي × ٧٧٠٠ ÷ الأيام)،
 * ممزوج مع المعادلة حسب كمية البيانات، ومحدود بـ ±٢٥٪.
 */
fun adaptiveTdee(p: Profile, days: Collection<DayLog>, today: String): AdaptiveResult? {
    val window = days.filter { daysBetween(it.date, today) in 0..20 }.sortedBy { it.date }
    val intakeDays = window.filter { it.intake >= 600 }
    val weights = weightPoints(window)
    if (intakeDays.size < 7 || weights.size < 3) return null
    val trend = trendWeights(weights)
    val span = daysBetween(trend.first().date, trend.last().date)
    if (span < 7) return null
    val avgIntake = intakeDays.map { it.intake }.average()
    val measured = avgIntake - (trend.last().trend - trend.first().trend) * KCAL_PER_KG / span
    val formula = formulaTdee(p, trend.last().trend)
    val confidence = min(1.0, (intakeDays.size / 14.0) * min(1.0, span / 14.0))
    val blended = formula + (measured - formula) * confidence
    val clamped = min(formula * 1.25, max(formula * 0.75, blended))
    return AdaptiveResult(roundTo(clamped, 10), (confidence * 100).roundToInt() / 100.0, intakeDays.size, span)
}

/* ---------------- سلسلة الاستمرار: لا تفوّت مرتين ---------------- */

fun isCounted(d: DayLog?): Boolean =
    d != null && (d.done.isNotEmpty() || d.meals.isNotEmpty() || d.workouts.isNotEmpty() || d.weightKg != null || d.lapses.isNotEmpty())

data class ThreadState(val length: Int, val rescueToday: Boolean, val best: Int)

fun addDays(date: String, n: Long): String = LocalDate.parse(date).plusDays(n).toString()

fun computeThread(days: Map<String, DayLog>, today: String): ThreadState {
    val first = days.keys.minOrNull() ?: return ThreadState(0, false, 0)
    var best = 0
    var run = 0
    var misses = 0
    var cursor = first
    while (cursor <= today) {
        if (isCounted(days[cursor])) {
            run++; misses = 0
        } else if (cursor != today) {
            misses++
            if (misses >= 2) run = 0
        }
        best = max(best, run)
        cursor = addDays(cursor, 1)
    }
    val yesterday = addDays(today, -1)
    val rescue = run > 0 && !isCounted(days[yesterday]) && !isCounted(days[today]) && first <= yesterday
    return ThreadState(run, rescue, best)
}

/** حالة كل يوم في نسيج السدو (من الأقدم للأحدث). */
enum class WeaveCell { WOVEN, HELD, BROKEN, TODAY, BEFORE }

fun weaveCells(days: Map<String, DayLog>, today: String, count: Int, firstDay: String?): List<WeaveCell> {
    val dates = (0 until count).map { addDays(today, (it - count + 1).toLong()) }
    val counted = dates.map { isCounted(days[it]) }
    return dates.mapIndexed { i, d ->
        when {
            firstDay != null && d < firstDay -> WeaveCell.BEFORE
            counted[i] -> WeaveCell.WOVEN
            d == today -> WeaveCell.TODAY
            else -> {
                val prevMissed = i > 0 && !counted[i - 1] && !(firstDay != null && dates[i - 1] < firstDay)
                val nextMissed = i < count - 1 && !counted[i + 1] && dates[i + 1] != today
                if (prevMissed || nextMissed) WeaveCell.BROKEN else WeaveCell.HELD
            }
        }
    }
}

/* ---------------- خطة اليوم ---------------- */

enum class MissionKind { MOVE, EAT, RESTORE }

data class Mission(val id: String, val kind: MissionKind, val title: String, val detail: String, val routineId: String? = null)

fun dayMissions(energy: Energy, time: TimeBudget, t: Targets, p: Profile): List<Mission> {
    if (p.ramadan) return ramadanMissions(energy, time, t)
    val proteinMeal = roundTo(t.protein / 3.0, 5)
    val move = when {
        energy == Energy.LOW || time == TimeBudget.TWO -> Mission(
            "move", MissionKind.MOVE, "دقيقتين حركة فقط",
            "٣ تمارين هادئة وأنت بمكانك. الهدف تحافظ على السلسلة، مو تتعب.",
            if (energy == Energy.LOW) "reset-2" else "wake-2",
        )
        time == TimeBudget.TEN -> Mission(
            "move", MissionKind.MOVE,
            if (energy == Energy.HIGH) "١٠ دقائق قوة للجسم كامل" else "١٠ دقائق حركة بدون قفز",
            "تمارين بوزن الجسم تحمي عضلاتك وأنت تنزل وزن.",
            if (energy == Energy.HIGH) "strength-10" else "low-impact-10",
        )
        else -> Mission(
            "move", MissionKind.MOVE,
            if (energy == Energy.HIGH) "٢٠ دقيقة قوة + مشي" else "٢٠ دقيقة مشي خفيف",
            if (energy == Energy.HIGH) "جلسة قوة كاملة؛ أهم استثمار للحفاظ على العضل." else "امشِ بإيقاع مريح. هدف خطواتك ${ar(t.steps)}.",
            if (energy == Energy.HIGH) "strength-20" else "walk-20",
        )
    }
    val eat = if (energy == Energy.LOW) Mission(
        "eat", MissionKind.EAT, "ابدأ وجبتك الجاية بالبروتين",
        "حوالي ${ar(proteinMeal)} غ (بيض، زبادي، دجاج، تونة). التعب يرفع الجوع — البروتين يهدّيه.",
    ) else Mission(
        "eat", MissionKind.EAT, "سجّل وجباتك — ولو بجملة",
        "قل لسند \"تغديت كبسة دجاج\" ويحسبها. هدفك ${ar(t.protein)} غ بروتين اليوم.",
    )
    val restore = when {
        energy == Energy.LOW -> Mission("restore", MissionKind.RESTORE, "نوم أبكر بنص ساعة", "قلة النوم ترفع هرمون الجوع وتضعف الإرادة. الليلة استثمار.")
        Barrier.NIGHT in p.barriers -> Mission("restore", MissionKind.RESTORE, "المطبخ يسكّر الساعة ٩", "بعدها شاي أو ماء فقط. خطتك لو جاك جوع الليل جاهزة عند المدرب.")
        else -> Mission("restore", MissionKind.RESTORE, "٨ أكواب ماء", "ابدأ بكوب قبل كل وجبة؛ يساعد على الشبع.")
    }
    return listOf(move, eat, restore)
}

val ENERGY_COPY = mapOf(
    Energy.LOW to ("طاقتي تحت" to "عادي. اليوم نحافظ على السلسلة بأصغر خطوة ممكنة."),
    Energy.MID to ("نص نص" to "يوم متوازن: خطوات ثابتة بدون ضغط."),
    Energy.HIGH to ("فل طاقة" to "استغلها! اليوم نبني عضل ونسبق الخطة."),
)
