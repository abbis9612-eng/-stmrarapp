package app.sanad.core

import kotlin.math.roundToInt

/* ---------------- وضع رمضان ---------------- */

data class RamadanMeal(val id: String, val name: String, val kcal: Int, val protein: Int, val tip: String)

data class RamadanPlan(val meals: List<RamadanMeal>, val water: String, val move: String, val cautions: List<String>)

private fun r10(x: Double) = (x / 10).roundToInt() * 10
private fun r5(x: Double) = (x / 5).roundToInt() * 5

/**
 * نفس هدف اليوم موزّع على نافذة الأكل: فطور على مرحلتين، وجبة خفيفة بعد التراويح،
 * وسحور غني بالبروتين والألياف يطوّل الشبع. التمرين الأنسب بعد الفطور لا وقت الصيام.
 */
fun ramadanPlan(p: Profile, t: Targets): RamadanPlan {
    val meals = listOf(
        RamadanMeal(
            "iftar", "الفطور", r10(t.kcal * 0.45), r5(t.protein * 0.4),
            "ابدأ بتمرتين وماء وشوربة، صلِّ، وبعدها صحن واحد: نصه خضار، ربعه بروتين، ربعه نشويات.",
        ),
        RamadanMeal(
            "snack", "بعد التراويح", r10(t.kcal * 0.15), r5(t.protein * 0.2),
            "وجبة بروتين خفيفة: لبن أو زبادي أو حمص. الحلويات حصة صغيرة هنا مو فوق الفطور.",
        ),
        RamadanMeal(
            "suhoor", "السحور", r10(t.kcal * 0.4), r5(t.protein * 0.4),
            "بيض أو فول أو جبن مع شوفان أو خبز أسمر وخضار. أخّره لقبل الفجر، وخفّف المالح.",
        ),
    )
    val cups = t.water.coerceAtLeast(8)
    val between = cups - 4
    val water = "${ar(cups)} أكواب بين الفطور والسحور: كوبين عند الفطور، ${ar(between)} موزّعة (كوب كل ساعة تقريباً)، وكوبين بالسحور."
    val move = "أفضل وقت للتمرين بعد الفطور بساعة إلى ساعتين. قبل المغرب مشي خفيف بس، ولا تمارين قوية وأنت صايم."
    val cautions = buildList {
        if (SafetyFlag.DIABETES_MEDS in p.flags || SafetyFlag.GLP1 in p.flags)
            add("الصيام مع أدوية السكري أو إبر GLP-1 يحتاج خطة من طبيبك للجرعات والأوقات قبل رمضان.")
        if (SafetyFlag.PREGNANT in p.flags) add("الصيام أثناء الحمل أو الرضاعة قرار ترجعين فيه لطبيبتك.")
        if (SafetyFlag.HEART in p.flags) add("مع أمراض القلب أو الضغط، راجع طبيبك بخصوص الصيام ومواعيد الأدوية.")
        add("إذا دخت أو حسيت بخفقان أو عطش شديد، أفطر فوراً. صحتك أولاً.")
    }
    return RamadanPlan(meals, water, move, cautions)
}

/** مهمات اليوم بنسخة رمضان: الحركة بعد الفطور، البروتين بالسحور، والماء بالليل. */
internal fun ramadanMissions(energy: Energy, time: TimeBudget, t: Targets): List<Mission> {
    val suhoorProtein = r5(t.protein * 0.4)
    val move = when {
        energy == Energy.LOW || time == TimeBudget.TWO -> Mission(
            "move", MissionKind.MOVE, "دقيقتين تمدد بعد الفطور",
            "حركة هادية تحافظ على السلسلة بدون ما تتعب وأنت صايم.", "reset-2",
        )
        time == TimeBudget.TEN -> Mission(
            "move", MissionKind.MOVE,
            if (energy == Energy.HIGH) "١٠ دقائق قوة بعد الفطور بساعة" else "١٠ دقائق حركة بدون قفز بعد الفطور",
            "بعد ما ياخذ جسمك أكل وماء. القوة تحمي عضلك بالصيام.",
            if (energy == Energy.HIGH) "strength-10" else "low-impact-10",
        )
        else -> Mission(
            "move", MissionKind.MOVE,
            if (energy == Energy.HIGH) "٢٠ دقيقة قوة بعد التراويح" else "٢٠ دقيقة مشي بعد الفطور",
            if (energy == Energy.HIGH) "جلسة كاملة وأنت مفطر ومرتوي." else "المشي بعد الفطور يهدّي السكر ويساعد الهضم.",
            if (energy == Energy.HIGH) "strength-20" else "walk-20",
        )
    }
    val eat = Mission(
        "eat", MissionKind.EAT, "سحور فيه ${ar(suhoorProtein)} غ بروتين",
        "بيض، لبن، فول أو جبن مع شوفان أو خبز أسمر. هذا اللي يشبعك لآخر النهار.",
    )
    val restore = Mission(
        "restore", MissionKind.RESTORE, "${ar(t.water.coerceAtLeast(8))} أكواب بين الفطور والسحور",
        "كوب كل ساعة تقريباً، وقلّل المالح والمقلي عشان العطش بكرة.",
    )
    return listOf(move, eat, restore)
}
