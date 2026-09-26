package app.sanad.core

import java.time.LocalDate
import kotlin.math.max

/** أقل نوم يوصى به للبالغين (٧ ساعات فأكثر). */
const val SLEEP_TARGET_H = 7.0

/**
 * بنك النوم: آخر ٧ ليالي. قلة النوم ترفع الجوع وتضعف الإرادة، وتمديد النوم
 * عند قليلي النوم قلّل الأكل ~٢٧٠ سعرة يومياً (Tasali وآخرون، JAMA IM 2022).
 */
data class SleepBank(
    /** ساعات كل ليلة من الأقدم للأحدث؛ null = ما انسجلت */
    val nights: List<Double?>,
    val logged: Int,
    val avg: Double,
    /** مجموع الساعات الناقصة عن ٧ */
    val debt: Double,
    val shortNights: Int,
    val headline: String,
    val tip: String,
)

fun sleepBank(days: Map<String, DayLog>, today: String): SleepBank? {
    val start = LocalDate.parse(today).minusDays(6)
    val nights = (0..6).map { days[start.plusDays(it.toLong()).toString()]?.sleepHours }
    val got = nights.filterNotNull()
    if (got.isEmpty()) return null
    val avg = got.average()
    val debt = got.sumOf { max(0.0, SLEEP_TARGET_H - it) }
    val short = got.count { it < 6.5 }
    val headline = when {
        debt < 0.5 -> "رصيد نومك ممتلي"
        debt < 3 -> "ناقصك شوية نوم (${ar(debt)} ساعة)"
        else -> "عليك دين نوم ${ar(debt)} ساعة"
    }
    val tip = when {
        short >= 2 -> "نوم أبكر بنص ساعة يقلل أكل بكرة تقريباً ٢٧٠ سعرة. الليلة: الشاشة بعيد ٣٠ دقيقة قبل النوم."
        debt >= 0.5 -> "ليلة وحدة زيادة ساعة تسدّ أغلب الدين. النوم جزء من الخطة، مو رفاهية."
        else -> "ثبّت وقت النوم والصحيان حتى بالعطلة، هذا يحمي شهيتك."
    }
    return SleepBank(nights, got.size, avg, debt, short, headline, tip)
}
