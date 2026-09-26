package app.sanad.core

/**
 * وضع أدوية GLP-1: الدواء يقلل الشهية، فالخطر مو الأكل الزايد — الخطر خسارة العضل
 * وقلة البروتين والماي. الأولوية: بروتين أول لقمة، قوة ٢–٣ مرات بالأسبوع، ماي وألياف.
 * سند ما يعدّل الجرعة ولا يوصي بدواء؛ هذا قرار الطبيب.
 */
fun glp1Missions(energy: Energy, time: TimeBudget, t: Targets): List<Mission> {
    val perMeal = roundTo(t.protein / 4.0, 5)
    val move = when {
        energy == Energy.LOW || time == TimeBudget.TWO -> Mission(
            "move", MissionKind.MOVE, "دقيقتين حركة",
            "حتى بيوم التعب، حركة صغيرة تذكّر عضلك إنه مطلوب.", "reset-2",
        )
        time == TimeBudget.TEN -> Mission(
            "move", MissionKind.MOVE, "١٠ دقائق قوة",
            "مع الإبر، القوة هي اللي تخلّي النزول من الدهون مو من العضل.", "strength-10",
        )
        else -> Mission(
            "move", MissionKind.MOVE, "٢٠ دقيقة قوة كاملة",
            "أهم شي تسويه هالأسبوع لعضلك. ٢–٣ مرات بالأسبوع تكفي.", "strength-20",
        )
    }
    val eat = Mission(
        "eat", MissionKind.EAT, "البروتين أول لقمة",
        "شهيتك قليلة، فابدأ كل وجبة بالبروتين (~${ar(perMeal)} غ): بيض، لبن، دجاج، سمك. هدفك ${ar(t.protein)} غ.",
    )
    val restore = Mission(
        "restore", MissionKind.RESTORE, "${ar(t.water)} أكواب ماي اليوم",
        "الدواء يقلل العطش ويسبب إمساك أحياناً. الماي والخضار يساعدون.",
    )
    return listOf(move, eat, restore)
}

val GLP1_TIPS: List<String> = listOf(
    "البروتين أول لقمة بكل وجبة — لأن الشبع يجي بسرعة",
    "وجبات صغيرة، ووقّف عند أول إحساس بالشبع",
    "المقلي والدسم يزيدون الغثيان؛ خفّفهم خصوصاً بأيام الجرعة",
    "قوة ٢–٣ مرات بالأسبوع تحمي عضلك",
    "ماي وألياف كل يوم ضد الإمساك",
    "قيء مستمر، ألم بطن شديد، أو دوخة؟ راجع طبيبك فوراً",
    "الجرعة وتوقيتها قرار الطبيب، مو قرار التطبيق",
)
