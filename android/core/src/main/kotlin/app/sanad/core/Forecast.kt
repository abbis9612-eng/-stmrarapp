package app.sanad.core

import kotlin.math.ceil

/**
 * «شاشة صراحة»: متى تصل تقريباً، كمدى لا كتاريخ واحد.
 * السرعة العليا من خطتك، والدنيا ثلثها أبطأ لأن الأسابيع الحقيقية فيها تعب ومناسبات.
 */
data class Forecast(
    val startKg: Double,
    val goalKg: Double,
    val fastKgWeek: Double,
    val slowKgWeek: Double,
    val weeksFast: Int,
    val weeksSlow: Int,
    val earliest: String,
    val latest: String,
)

/** نسبة الأسبوع الواقعي من سرعة الخطة. */
const val REAL_WEEK_FACTOR = 0.65

fun forecast(startKg: Double, goalKg: Double, pace: Pace, today: String): Forecast? {
    val fast = pace.weeklyRate * startKg
    if (goalKg >= startKg || fast <= 0) return null
    val slow = fast * REAL_WEEK_FACTOR
    val diff = startKg - goalKg
    val wf = ceil(diff / fast).toInt()
    val ws = ceil(diff / slow).toInt()
    return Forecast(
        startKg, goalKg,
        Math.round(fast * 100) / 100.0, Math.round(slow * 100) / 100.0,
        wf, ws, addDays(today, wf * 7L), addDays(today, ws * 7L),
    )
}
