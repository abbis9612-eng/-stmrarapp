package app.sanad.core

/**
 * مؤقت الأكل على مهل. الشبع يوصل للمخ متأخر (~٢٠ دقيقة)، والأكل البطيء
 * يزيد الإحساس بالشبع ويقلل الكمية عند كثيرين. المؤقت يعطي إيقاع لقمات هادئ
 * وسؤال شبع بالنص — بدون حساب ولا حرمان.
 */
enum class CueKind { BITE, CHEW, SIP, TALK, CHECK, END }

data class PacerCue(val atSec: Int, val kind: CueKind, val text: String)

const val PACER_TOTAL_SEC = 20 * 60

private val ROTATION = listOf(
    CueKind.BITE to "لقمة صغيرة",
    CueKind.CHEW to "نزّل الملعقة وامضغ على مهلك",
    CueKind.SIP to "رشفة ماي",
    CueKind.TALK to "سولف، ولا تستعجل",
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
    cues += PacerCue(half, CueKind.CHECK, "وقفة: شبعان كم؟")
    cues += PacerCue(totalSec, CueKind.END, "خلصت العشرين دقيقة")
    return cues.sortedBy { it.atSec }
}

/** الإشارة الحالية = آخر إشارة وقتها وصل. */
fun cueAt(cues: List<PacerCue>, elapsedSec: Int): PacerCue =
    cues.lastOrNull { it.atSec <= elapsedSec } ?: cues.first()

/** مقياس الشبع ١ (جوعان) → ٥ (متروس). */
fun fullnessAdvice(level: Int, elapsedSec: Int): String = when {
    level >= 4 && elapsedSec < PACER_TOTAL_SEC -> "هذا الشبع الحقيقي. جرّب توقف هنا، وخلّ الباقي لبعدين — ماكو شي يضيع."
    level >= 4 -> "ممتاز، وصلت للشبع وأنت على مهلك."
    level == 3 -> "مرتاح؟ كمّل على نفس الهدوء، ويمكن تشبع قبل ما يخلص الصحن."
    else -> "بعدك جوعان — عادي. كمّل على مهلك، وخلّ البروتين والخضار أولاً."
}
