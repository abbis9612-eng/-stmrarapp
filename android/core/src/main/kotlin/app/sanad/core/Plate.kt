package app.sanad.core

/**
 * صحن سند: تقييم سريع للصحن بالأرباع (نموذج "طبق الصحة": نصف خضار، ربع بروتين، ربع نشويات).
 * ما يحتاج وزن ولا سعرات — عينك تكفي، والنصيحة وحدة وواضحة.
 */
data class PlateInput(
    /** أرباع الخضار 0..2 */
    val veg: Int,
    /** أرباع البروتين 0..2 */
    val protein: Int,
    /** أرباع النشويات (تمن/خبز/صمون) 0..3 */
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
        if (pro == 0) add("أضف بروتيناً بقدر كفّك: دجاج، لحم، سمك، بيض، أو عدس")
        if (veg < 2) add("اجعل الخضار والسلطة نصف الصحن، فهي تُشبع بسعرات قليلة")
        if (carb >= 2) add("قلّل الأرز أو الخبز إلى قبضة واحدة")
        if (p.fried) add("المشوي أو المطبوخ بدل المقلي يوفّر أكثر من 200 سعرة")
        if (p.sweetDrink) add("ماء أو لبن بدل العصير أو المشروب الغازي")
    }
    val grade = when {
        score >= 85 -> "صحن تحرّك"
        score >= 65 -> "صحن جيد"
        score >= 40 -> "قريب، ينقصه تعديل واحد"
        else -> "نبدأ بخطوة واحدة"
    }
    return PlateScore(score, grade, tips.ifEmpty { listOf("صحن متوازن. كُله على مهل.") })
}
