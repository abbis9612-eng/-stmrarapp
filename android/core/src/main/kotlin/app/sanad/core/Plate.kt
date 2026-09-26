package app.sanad.core

/**
 * صحن سند: تقييم سريع للصحن بالأرباع (نموذج "طبق الصحة": نص خضار، ربع بروتين، ربع نشويات).
 * ما يحتاج وزن ولا سعرات — عينك تكفي، والنصيحة وحدة وواضحة.
 */
data class PlateInput(
    /** أرباع الخضار ٠..٢ */
    val veg: Int,
    /** أرباع البروتين ٠..٢ */
    val protein: Int,
    /** أرباع النشويات (تمن/خبز/صمون) ٠..٣ */
    val carbs: Int,
    val fried: Boolean = false,
    val sweetDrink: Boolean = false,
)

data class PlateScore(val score: Int, val grade: String, val tips: List<String>)

fun plateScore(p: PlateInput): PlateScore {
    val veg = p.veg.coerceIn(0, 2)
    val pro = p.protein.coerceIn(0, 2)
    val carb = p.carbs.coerceIn(0, 3)
    var s = when (veg) { 2 -> 35; 1 -> 20; else -> 0 } +
        when (pro) { 1 -> 30; 2 -> 25; else -> 0 } +
        when (carb) { 0 -> 20; 1 -> 25; 2 -> 12; else -> 0 }
    if (veg == 2 && pro >= 1 && carb <= 1) s += 10
    if (p.fried) s -= 15
    if (p.sweetDrink) s -= 15
    val score = s.coerceIn(0, 100)
    val tips = buildList {
        if (pro == 0) add("زيد بروتين بقدّ كفّك: دجاج، لحم، سمك، بيض، أو عدس")
        if (veg < 2) add("خلّ الخضار والسلطة نص الصحن — تشبع بسعرات قليلة")
        if (carb >= 2) add("قلّل التمن أو الصمون لقبضة وحدة")
        if (p.fried) add("المشوي أو المطبوخ بدل المقلي يوفّر ٢٠٠+ سعرة")
        if (p.sweetDrink) add("ماي أو لبن بدل العصير أو الغازي")
    }
    val grade = when {
        score >= 85 -> "صحن سند"
        score >= 65 -> "صحن زين"
        score >= 40 -> "قريب، تعديل واحد"
        else -> "نبدأ بخطوة وحدة"
    }
    return PlateScore(score, grade, tips.ifEmpty { listOf("صحن مرتب. كله على مهلك.") })
}
