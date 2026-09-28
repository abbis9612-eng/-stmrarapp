package app.sanad.core

/**
 * وضع أدوية GLP-1: الدواء يقلل الشهية، فالخطر مو الأكل الزايد — الخطر خسارة العضل
 * وقلة البروتين والماي. الأولوية: بروتين أول لقمة، قوة 2–3 مرات بالأسبوع، ماي وألياف.
 * سند ما يعدّل الجرعة ولا يوصي بدواء؛ هذا قرار الطبيب.
 */
fun glp1Missions(energy: Energy, time: TimeBudget, t: Targets): List<Mission> {
    val perMeal = roundTo(t.protein / 4.0, 5)
    val move = when {
        energy == Energy.LOW || time == TimeBudget.TWO -> Mission(
            "move", MissionKind.MOVE, "دقيقتان من الحركة",
            "حتى في يوم التعب، حركة صغيرة تذكّر عضلاتك بأنها مطلوبة.", "reset-2",
        )
        time == TimeBudget.TEN -> Mission(
            "move", MissionKind.MOVE, "10 دقائق قوة",
            "مع الإبر، القوة هي ما يجعل النزول من الدهون لا من العضل.", "strength-10",
        )
        else -> Mission(
            "move", MissionKind.MOVE, "20 دقيقة قوة كاملة",
            "أهم ما تفعله لعضلاتك هذا الأسبوع. 2–3 مرات في الأسبوع تكفي.", "strength-20",
        )
    }
    val eat = Mission(
        "eat", MissionKind.EAT, "البروتين أول لقمة",
        "شهيتك قليلة، فابدأ كل وجبة بالبروتين (~${ar(perMeal)} غ): بيض، زبادي، دجاج، سمك. هدفك ${ar(t.protein)} غ.",
    )
    val restore = Mission(
        "restore", MissionKind.RESTORE, "${ar(t.water)} أكواب ماء اليوم",
        "الدواء يقلل العطش ويسبب الإمساك أحياناً. الماء والخضار يساعدان.",
    )
    return listOf(move, eat, restore)
}

val GLP1_TIPS: List<String> = listOf(
    "البروتين أول لقمة في كل وجبة، لأن الشبع يأتي بسرعة",
    "وجبات صغيرة، وتوقّف عند أول إحساس بالشبع",
    "المقلي والدسم يزيدان الغثيان؛ خفّفهما خصوصاً في أيام الجرعة",
    "تمارين القوة 2–3 مرات في الأسبوع تحمي عضلاتك",
    "ماء وألياف كل يوم ضد الإمساك",
    "قيء مستمر، ألم بطن شديد، أو دوخة؟ راجع طبيبك فوراً",
    "الجرعة وتوقيتها قرار الطبيب، لا قرار التطبيق",
)
