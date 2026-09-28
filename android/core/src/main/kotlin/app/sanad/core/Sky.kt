package app.sanad.core

/**
 * مسيرة المستخدم: رمز التقدّم (حلقة حول أيقونة «تحرّك»).
 * كل يوم مُسجّل = يوم تتقدّم فيه الحلقة. كل 30 يوماً = شهر مكتمل يُحفظ.
 * كل أسبوع كامل بلا يومين فارغين متتاليين = أسبوع كامل (stars).
 * أسماء الحقول قديمة (nights/fullMoons/stars) وتبقى حتى لا تتغيّر البيانات والاختبارات.
 */
data class Sky(
    /** أيام الجولة الحالية (0–29) */
    val nights: Int,
    val fullMoons: Int,
    val stars: Int,
    val daysWithSanad: Int,
) {
    /** امتلاء الحلقة من 0 إلى 1: يوم واحد = 1/30، وتكتمل يوم يكتمل الشهر. */
    val phase: Float
        get() = when {
            nights == 0 && fullMoons > 0 -> 1f
            else -> nights / NIGHTS_PER_MOON.toFloat()
        }
}

const val NIGHTS_PER_MOON = 30

fun skyOf(days: Map<String, DayLog>, today: String, firstDay: String?): Sky {
    val first = firstDay ?: days.keys.minOrNull() ?: today
    if (first > today) return Sky(0, 0, 0, 0)
    val span = (daysBetween(first, today) + 1).toInt()
    val counted = (0 until span).map { isCounted(days[addDays(first, it.toLong())]) }
    val total = counted.count { it }
    // النجوم تُحسب على الأسابيع المكتملة فقط، حتى ما تنطفئ نجمة الأسبوع الحالي قبل نهايته
    val stars = (0 until span / 7).count { w ->
        val week = counted.subList(w * 7, w * 7 + 7)
        week.any { it } && (0 until 6).none { !week[it] && !week[it + 1] }
    }
    return Sky(total % NIGHTS_PER_MOON, total / NIGHTS_PER_MOON, stars, span)
}

/** حالة يوم في شريط الأسبوع أعلى «اليوم». */
enum class DayMark { ON_PLAN, OVER, OCCASION, EMPTY, TODAY, BEFORE }

/** «فوقها شوية» تبدأ بعد 100 سعرة زيادة؛ الفرق الأصغر ضمن الخطة. */
const val OVER_MARGIN = 100

fun dayMarks(days: Map<String, DayLog>, today: String, kcalTarget: Int, firstDay: String?, count: Int = 7): List<DayMark> =
    (0 until count).map { i ->
        val date = addDays(today, (i - count + 1).toLong())
        val d = days[date]
        when {
            firstDay != null && date < firstDay -> DayMark.BEFORE
            d?.gathering == true && isCounted(d) -> DayMark.OCCASION
            date == today -> DayMark.TODAY
            !isCounted(d) -> DayMark.EMPTY
            d!!.intake > kcalTarget + OVER_MARGIN -> DayMark.OVER
            else -> DayMark.ON_PLAN
        }
    }

/**
 * رصيد المناسبات: ما يبقى تحت خطتك في الأيام الماضية يُجمع لعزومة قادمة، بلا ذنب.
 * حتى لا يكافئ الجوع: كل يوم يضيف 150 كحد أقصى، والمجموع 500. يبدأ العدّ بعد آخر مناسبة.
 */
data class OccasionBank(val saved: Int, val cap: Int = BANK_CAP)

const val BANK_CAP = 500
const val BANK_PER_DAY = 150

fun occasionBank(days: Map<String, DayLog>, today: String, kcalTarget: Int): OccasionBank {
    var saved = 0
    for (back in 6 downTo 1) {
        val d = days[addDays(today, -back.toLong())] ?: continue
        if (d.gathering) { saved = 0; continue }
        if (d.meals.isEmpty()) continue
        saved += (kcalTarget - d.intake).coerceIn(0, BANK_PER_DAY)
    }
    return OccasionBank(saved.coerceAtMost(BANK_CAP))
}

/** تغيّر الوزن الاتجاهي خلال فترة: جدول 7/14/30 يوم/منذ البداية في «تقدّمي». */
data class WeightChange(val label: String, val days: Int?, val kg: Double?)

fun weightChanges(trend: List<TrendPoint>): List<WeightChange> {
    val last = trend.lastOrNull()
    fun change(days: Int?): Double? {
        if (last == null || trend.size < 2) return null
        val from = if (days == null) trend.first() else {
            val cutoff = addDays(last.date, -days.toLong())
            // أقدم قراءة ضمن الفترة؛ إذا ما عندنا قراءة قبلها بأسبوع ما نعرض رقم مضلل
            val p = trend.firstOrNull { it.date >= cutoff } ?: return null
            if (p === last || daysBetween(p.date, last.date) < days / 2) return null
            p
        }
        return Math.round((last.trend - from.trend) * 10) / 10.0
    }
    return listOf(
        WeightChange("7 أيام", 7, change(7)),
        WeightChange("14 يوماً", 14, change(14)),
        WeightChange("30 يوماً", 30, change(30)),
        WeightChange("منذ البداية", null, change(null)),
    )
}
