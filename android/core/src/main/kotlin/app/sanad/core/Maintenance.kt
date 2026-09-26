package app.sanad.core

/**
 * وضع الحفاظ: النزول مهارة والحفاظ مهارة ثانية. اللي يحافظون بنجاح (سجل NWCR)
 * يوزّنون أسبوعياً، يتحركون أكثر، ويتصرفون بدري لما يزيد الوزن.
 * هنا "حد أمان" واضح: هدفك + ٢ كغ، وإذا تجاوزه الاتجاه نرجع أسبوعين نزول.
 */
enum class Zone { GREEN, AMBER, RED }

data class MaintenanceStatus(
    val trendKg: Double,
    val goalKg: Double,
    val guardrailKg: Double,
    val zone: Zone,
    val headline: String,
    val advice: String,
)

const val GUARDRAIL_KG = 2.0

fun latestTrend(days: Map<String, DayLog>): Double? = trendWeights(weightPoints(days.values)).lastOrNull()?.trend

/** وصل الهدف (بالاتجاه مو بقراءة يوم) وبعده بوضع النزول → نعرض عليه الحفاظ. */
fun reachedGoal(p: Profile, days: Map<String, DayLog>): Boolean =
    p.maintainSince == null && (latestTrend(days)?.let { it <= p.goalWeightKg } ?: false)

fun maintenanceStatus(p: Profile, days: Map<String, DayLog>): MaintenanceStatus? {
    if (p.maintainSince == null) return null
    val trend = latestTrend(days) ?: return null
    val goal = p.goalWeightKg
    val rail = goal + GUARDRAIL_KG
    val over = trend - goal
    val zone = when {
        over <= 1.0 -> Zone.GREEN
        over <= GUARDRAIL_KG -> Zone.AMBER
        else -> Zone.RED
    }
    val (headline, advice) = when (zone) {
        Zone.GREEN -> "محافظ على وزنك" to "استمر: ميزان مرة بالأسبوع، حركة يومية، وبروتين بكل وجبة. تقدر تزيد أكلك ١٠٠–٢٠٠ سعرة وتراقب."
        Zone.AMBER -> "الاتجاه طالع شوي" to "تصرّف بدري: رجّع التسجيل اليومي أسبوع، وزيد المشي. هذا مو فشل، هذا الحفاظ نفسه."
        Zone.RED -> "تجاوزت حد الأمان" to "رجّع وضع النزول أسبوعين بهدوء لحد ما ترجع تحت ${ar(rail)} كغ. بدون تجويع ولا ذنب."
    }
    return MaintenanceStatus(trend, goal, rail, zone, headline, advice)
}
