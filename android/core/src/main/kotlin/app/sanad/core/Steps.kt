package app.sanad.core

/**
 * عدّاد الخطوات يعطي رقم تراكمي من آخر تشغيل للجهاز؛ نحوّله لخطوات اليوم
 * بطريقة الفروق: كل قراءة نضيف الفرق عن السابقة، ونبدأ من صفر بيوم جديد،
 * وإذا الرقم نزل (إعادة تشغيل) نعتبر القراءة نفسها هي الفرق.
 */
data class StepCursor(val date: String, val last: Long, val today: Int)

fun advanceSteps(cur: StepCursor?, reading: Long, date: String): StepCursor {
    if (cur == null) return StepCursor(date, reading, 0)
    val delta = if (reading >= cur.last) reading - cur.last else reading
    val base = if (cur.date == date) cur.today else 0
    return StepCursor(date, reading, (base + delta).coerceAtMost(100_000L).toInt())
}

/** ~٧٠٠٠ خطوة باليوم مرتبطة بانخفاض واضح بالوفيات والأمراض (مراجعات ٢٠٢٣–٢٠٢٥). */
fun stepsNote(steps: Int, target: Int): String = when {
    steps >= target -> "وصلت هدفك. كل خطوة زيادة مكسب، مو واجب."
    steps >= target * 0.6 -> "باقي ${ar(target - steps)} خطوة — مشية ١٥ دقيقة بعد الأكل تكفي."
    steps > 0 -> "مشية ١٠ دقايق بعد الوجبة تنزّل سكر الدم وتقرّبك للهدف."
    else -> "امشِ وتلفونك بجيبك، والعدّاد يحسب لحاله."
}
