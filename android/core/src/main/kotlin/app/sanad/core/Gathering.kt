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
        if (needSnack) add("قبلها بساعة: بروتين خفيف (زبادي يوناني أو بيضتين) — تروح وأنت مو ميت جوع")
        add("اشرب كوبين ماي قبل ما تطلع")
        add("لا تقطع الأكل طول اليوم؛ التجويع يخلّيك تنفجر بالليل")
    }
    val plate = listOf(
        "صحن واحد بس، وبلا رجعة",
        "نصه سلطة وخضار، ربعه لحم أو دجاج أو سمك (بقدّ كفّين)، وتمن بقدّ قبضتك",
        "التمن أو الخبز — واحد بس، مو الاثنين",
        "دولمة؟ ٥–٦ قطع مع لبن بدل التمن",
        "كل على مهلك: لقمة، نزّل الملعقة، سولف",
    )
    val after = listOf(
        "حلو؟ قطعة صغيرة وحدة مع استكان چاي بلا شكر",
        "إذا أحد أصرّ: «الحمدلله شبعت، تسلم إيدك» وابتسم",
        "باچر يوم عادي: لا تعويض ولا تجويع",
    )
    return GatheringPlan(
        rounded, before, plate, after,
        if (rounded >= 500) "ميزانيتك للعزيمة تقريباً ${ar(rounded)} سعرة — تكفي صحن مرتب وقطعة حلو."
        else "حصتك اليوم قربت تخلص، فخلّ الصحن نصه سلطة ومشاوي، والتمن ملعقتين. عادي تزيد شوي؛ المهم ترجع باچر.",
    )
}
