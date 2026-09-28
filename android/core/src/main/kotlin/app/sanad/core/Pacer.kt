package app.sanad.core

/**
 * مؤقت الأكل على مهل. الشبع يوصل للمخ متأخر (~20 دقيقة)، والأكل البطيء
 * يزيد الإحساس بالشبع ويقلل الكمية عند كثيرين. المؤقت يعطي إيقاع لقمات هادئ
 * وسؤال شبع بالنص — بدون حساب ولا حرمان.
 */
enum class CueKind { BITE, CHEW, SIP, TALK, CHECK, END }

data class PacerCue(val atSec: Int, val kind: CueKind, val text: String)

const val PACER_TOTAL_SEC = 20 * 60

private val ROTATION = listOf(
    CueKind.BITE to "لقمة صغيرة",
    CueKind.CHEW to "ضع الملعقة وامضغ على مهل",
    CueKind.SIP to "رشفة ماء",
    CueKind.TALK to "تحدّث قليلاً، ولا تستعجل",
)

fun pacerCues(totalSec: Int = PACER_TOTAL_SEC, every: Int = 40): List<PacerCue> {
    require(totalSec > 0 && every > 0)
    val half = totalSec / 2
    val cues = mutableListOf<PacerCue>()
    var i = 0
    var t = 0
    while (t < totalSec) {
        if (t != half) {
            val (k, text) = ROTATION[i % ROTATION.size]
            cues += PacerCue(t, k, text); i++
        }
        t += every
    }
    cues += PacerCue(half, CueKind.CHECK, "وقفة: كم شبعك؟")
    cues += PacerCue(totalSec, CueKind.END, "انتهت العشرون دقيقة")
    return cues.sortedBy { it.atSec }
}

/** الإشارة الحالية = آخر إشارة وقتها وصل. */
fun cueAt(cues: List<PacerCue>, elapsedSec: Int): PacerCue =
    cues.lastOrNull { it.atSec <= elapsedSec } ?: cues.first()

/** مقياس الشبع 1 (جوعان) → 5 (متروس). */
fun fullnessAdvice(level: Int, elapsedSec: Int): String = when {
    level >= 4 && elapsedSec < PACER_TOTAL_SEC -> "هذا هو الشبع الحقيقي. جرّب أن توقف هنا، واترك الباقي لوقت لاحق، فلن يضيع شيء."
    level >= 4 -> "ممتاز، وصلت إلى الشبع وأنت على مهل."
    level == 3 -> "مرتاح؟ أكمل بالهدوء نفسه، وقد تشبع قبل أن ينتهي الصحن."
    else -> "ما زلت جائعاً؟ هذا عادي. أكمل على مهل، وابدأ بالبروتين والخضار."
}
