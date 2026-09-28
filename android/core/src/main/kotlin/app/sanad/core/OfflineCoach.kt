package app.sanad.core

import kotlin.math.roundToInt

data class CoachReply(val text: String, val actions: List<CoachAction>)

/**
 * مدرب محلي يعمل بلا إنترنت أو بلا مفتاح API — نفس مبادئ المدرب الذكي
 * بقواعد بسيطة، حتى لا يبقى المستخدم بدون سند.
 */
fun offlineReply(text: String, state: AppState, t: Targets, today: String): CoachReply {
    val q = normalizeArabic(text)
    val day = state.days[today]
    val proteinIn = day?.protein ?: 0
    val left = t.kcal - (day?.intake ?: 0)

    // تعثّر: نرد قبل تحليل الأكل حتى لا يصير الرد حساب سعرات بارداً. نفهم اللهجات والفصحى معاً
    if (Regex("(زليت|تعثرت|خربت|خربتها|افسدت|افرطت|انفجرت|اكلت هوايه|اكلت كثير|فشلت|ما التزمت|لم التزم)").containsMatchIn(q)) {
        val kind = when {
            Regex("(الليل|بالليل|ليلا|سهر)").containsMatchIn(q) -> LapseKind.NIGHT
            Regex("(حلو|حلويات|شوكولا|كيك)").containsMatchIn(q) -> LapseKind.SWEETS
            Regex("(عزيمه|عزومه|وليمه|مناسبه|عرس|مطعم)").containsMatchIn(q) -> LapseKind.SOCIAL
            Regex("(ما سويت|ما تمرنت|ما التزمت|لم افعل|لم اتمرن|لم التزم)").containsMatchIn(q) -> LapseKind.SKIPPED
            else -> LapseKind.OVEREAT
        }
        val r = lapseRecovery(kind, t)
        return CoachReply("${r.title}. ${r.reframe}\n" + r.steps.joinToString("\n") { "• $it" }, emptyList())
    }

    if (Regex("(شربت|اشرب).*(ماء|ماي|مويه)").containsMatchIn(q)) {
        val n = if ("كوبين" in q) 2 else Regex("\\d+").find(q)?.value?.toIntOrNull() ?: 1
        return CoachReply("ممتاز 💧 سجّلتها. الماء قبل الوجبة يساعد على الشبع.", listOf(CoachAction.LogWater(n.coerceIn(1, 10))))
    }

    val items = parseMealText(text)
    if (items.isNotEmpty()) {
        val actions = items.map { (food, qty) ->
            CoachAction.LogMeal(if (qty == 1.0) food.name else "${food.name} ×${ar(qty)}", (food.kcal * qty).roundToInt(), (food.protein * qty).roundToInt())
        }
        val kcal = actions.sumOf { it.kcal }
        val prot = actions.sumOf { it.protein }
        val gap = t.protein - proteinIn - prot
        val remaining = left - kcal
        val tip = when {
            gap > 30 -> "بقي عليك نحو ${ar(gap)} غ بروتين؛ اجعل وجبتك القادمة تبدأ ببروتين (زبادي، بيض، تونة)."
            remaining < 0 -> "تجاوزت هدف اليوم قليلاً، ولا بأس أبداً. لا تعوّض بالحرمان؛ غداً نعود إلى الخطة، والليلة امشِ 10 دقائق بعد الأكل."
            else -> "يبقى لك نحو ${ar(remaining)} سعرة اليوم. 👌"
        }
        return CoachReply("حسبتها تقريبياً: ${ar(kcal)} سعرة و${ar(prot)} غ بروتين. $tip", actions)
    }

    if (Regex("(تعبان|تعبانه|متعب|مرهق|ما عندي طاقه|ليس عندي طاقه|كسلان|ما لي خلق|ماني قادر|لا اقدر)").containsMatchIn(q)) {
        return CoachReply(
            "أفهمك، والتعب ليس فشلاً. اليوم نريد أصغر خطوة تحفظ قمرك: دقيقتان من الحركة وأنت جالس، ثم أنت حر.",
            listOf(CoachAction.StartWorkout("reset-2")),
        )
    }
    if (Regex("(الليل|بالليل|قبل النوم|سهر).*(جوع|اكل|جوعان|اشتهي)|(جوع|جوعان|اشتهي).*(الليل|بالليل)").containsMatchIn(q)) {
        return CoachReply(
            "جوع الليل غالباً تعب أو ملل أكثر منه جوع حقيقي. جرّب 5 دقائق تهدئة، وإن بقيت جائعاً: زبادي أو شاي بلا سكر.",
            listOf(
                CoachAction.StartWorkout("night-5"),
                CoachAction.AddIfThen("إذا جعت بعد الساعة 9", "أشرب كوب ماء أو شاي، وإن استمر الجوع آكل زبادي"),
            ),
        )
    }
    if (Regex("(عزيمه|عزومه|وليمه|مناسبه|عرس|ضيوف|مطعم)").containsMatchIn(q)) {
        return CoachReply(
            gatheringPlan(t, day).let { g ->
                "المناسبات جزء من حياتنا، لا نهرب منها، بل نذهب إليها بخطة.\n" +
                    "قبل: ${g.before.first()}.\n" +
                    "هناك: ${g.plate[1]}.\n" +
                    "بعد: ${g.after[2]}.\n" + g.note
            },
            listOf(CoachAction.AddIfThen("إذا كانت عندي عزيمة", "آكل بروتيناً خفيفاً قبلها وآخذ صحناً واحداً: نصف خضار، ربع بروتين، ربع أرز")),
        )
    }
    if (Regex("(ما نزل|لم ينزل|لا ينزل|زاد وزني|وزني زاد|ثابت|ما تغير|لم يتغير|نفس الوزن)").containsMatchIn(q)) {
        val tr = trendWeights(weightPoints(state.days.values))
        val line = if (tr.size >= 2) " خط اتجاهك: ${ar(tr.first().trend)} ← ${ar(tr.last().trend)} كغ." else ""
        return CoachReply(
            "الميزان اليومي يتأثر بالماء والملح والنوم، وقد يتحرك كيلو في يوم. نحكم على الاتجاه الأسبوعي لا على القراءة.$line استمر أسبوعين بتسجيل صادق، وسيعدّل سند هدفك تلقائياً.",
            emptyList(),
        )
    }
    val profile = state.profile
    if (profile != null && (Regex("(رمضان|صيام|صايم|سحور)").containsMatchIn(q) || (profile.ramadan && Regex("(فطور|افطار)").containsMatchIn(q)))) {
        val plan = ramadanPlan(profile, t)
        val split = plan.meals.joinToString("\n") { "${it.name}: ${ar(it.kcal)} سعرة و${ar(it.protein)} غ بروتين" }
        val lead = if (profile.ramadan) "خطتك الرمضانية لليوم:" else "إن كنت صائماً، فهذا توزيع هدفك في رمضان (فعّل وضع رمضان من «تقدّمي»):"
        return CoachReply("$lead\n$split\n${plan.meals.first().tip}\n${plan.move}", emptyList())
    }
    if (profile != null && Regex("(اسبوعي|الاسبوع|مراجعه|تقييمي|كيف ماشي|كيف حالي)").containsMatchIn(q)) {
        val r = weeklyReview(profile, state.days, t, today)
        return CoachReply("مراجعة آخر 7 أيام: حضرت ${ar(r.activeDays)} من 7، وتمرّنت ${ar(r.workouts)} مرات.\n${r.win}\n${r.focusText}", emptyList())
    }
    if (Regex("(حلا|حلويات|الحلو|سكر|شوكولا|ابي حلو|اريد حلو)").containsMatchIn(q)) {
        return CoachReply("لا يوجد أكل ممنوع. خذ حصة صغيرة واستمتع بها بعد وجبة فيها بروتين، لا على جوع. وسجّلها بلا لوم.", emptyList())
    }
    val focus = if (proteinIn < t.protein / 2) "البروتين (أكلت ${ar(proteinIn)} من ${ar(t.protein)} غ)" else "حركة قصيرة بعد وجبتك القادمة"
    val energyNote = if (day?.energy == Energy.LOW) "بما أن طاقتك اليوم منخفضة، " else ""
    return CoachReply("${energyNote}لنركّز على شيء واحد: $focus. أخبرني بما أكلت وأحسبه لك، أو قل \"متعب\" وأعطيك أخف خطة.", emptyList())
}
