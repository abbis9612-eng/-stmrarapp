package app.sanad.core

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * خطة العزايم: ما نهرب من المناسبات، نروح لها بخطة.
 * المبادئ: لا تجويع قبلها (يزيد الأكل بعدين)، بروتين خفيف قبلها،
 * صحن واحد بتقسيم واضح، أكل على مهل، وباچر يوم عادي بلا تعويض.
 */
data class GatheringPlan(
    /** السعرات المتبقية تقريباً للعزيمة بعد وجبة البروتين الخفيفة */
    val budget: Int,
    val before: List<String>,
    val plate: List<String>,
    val after: List<String>,
    val note: String,
)

private const val PRE_SNACK = 150

fun gatheringPlan(t: Targets, d: DayLog?): GatheringPlan {
    val eaten = d?.intake ?: 0
    val needSnack = (d?.protein ?: 0) < t.protein / 3
    val budget = max(0, t.kcal - eaten - if (needSnack) PRE_SNACK else 0)
    // نقرّب لأقرب ٥٠ حتى يبان تقدير مو رقم مخبري
    val rounded = ((budget / 50.0).roundToInt() * 50)
    val before = buildList {
        if (needSnack) add("قبلها بساعة: بروتين خفيف (زبادي أو بيضتان) حتى لا تذهب وأنت جائع جداً")
        add("اشرب كوبين من الماء قبل أن تخرج")
        add("لا تقطع الأكل طوال اليوم؛ التجويع يدفعك إلى الإفراط ليلاً")
    }
    val plate = listOf(
        "صحن واحد فقط، بلا إعادة",
        "نصفه سلطة وخضار، وربعه لحم أو دجاج أو سمك (بقدر كفّين)، وأرز بقدر قبضتك",
        "الأرز أو الخبز: واحد فقط، لا الاثنان",
        "محاشي أو ورق عنب؟ ٥–٦ قطع مع زبادي بدل الأرز",
        "كُل على مهل: لقمة، ثم ضع الملعقة، وتحدّث",
    )
    val after = listOf(
        "حلويات؟ قطعة صغيرة واحدة مع كوب شاي بلا سكر",
        "إن أصرّ أحد: «الحمد لله شبعت، سلمت يداك» وابتسم",
        "غداً يوم عادي: لا تعويض ولا تجويع",
    )
    return GatheringPlan(
        rounded, before, plate, after,
        if (rounded >= 500) "ميزانيتك للمناسبة نحو ${ar(rounded)} سعرة، وهي تكفي صحناً متوازناً وقطعة حلوى. هذا عادي تماماً."
        else "حصتك اليوم قاربت على الانتهاء، فاجعل نصف الصحن سلطة ومشاوي، والأرز ملعقتين. من العادي أن تزيد قليلاً؛ المهم أن تعود غداً.",
    )
}
