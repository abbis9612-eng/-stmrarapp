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

    if (Regex("(شربت|اشرب).*(ماء|ماي|مويه)").containsMatchIn(q)) {
        val n = if ("كوبين" in q) 2 else Regex("\\d+").find(q)?.value?.toIntOrNull() ?: 1
        return CoachReply("ممتاز 💧 سجّلها وكمّل. الماء قبل الوجبة يساعد على الشبع.", listOf(CoachAction.LogWater(n.coerceIn(1, 10))))
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
            gap > 30 -> "باقي عليك حوالي ${ar(gap)} غ بروتين؛ خل وجبتك الجاية تبدأ ببروتين (زبادي يوناني، بيض، تونة)."
            remaining < 0 -> "طلعت فوق هدف اليوم شوي — عادي جداً. لا تعوّض بالحرمان؛ بكرة نرجع للخطة، والليلة مشي ١٠ دقائق بعد الأكل."
            else -> "يبقى لك تقريباً ${ar(remaining)} سعرة اليوم. 👌"
        }
        return CoachReply("حسبتها تقريبياً: ${ar(kcal)} سعرة و${ar(prot)} غ بروتين. $tip", actions)
    }

    if (Regex("(تعبان|تعبانه|مرهق|ما عندي طاقه|كسلان|ما لي خلق|ماني قادر)").containsMatchIn(q)) {
        return CoachReply(
            "أفهمك، والتعب مو فشل. اليوم نبي أصغر خطوة تحافظ على السلسلة: دقيقتين حركة وأنت جالس، وبعدها أنت حر.",
            listOf(CoachAction.StartWorkout("reset-2")),
        )
    }
    if (Regex("(الليل|بالليل|قبل النوم|سهر).*(جوع|اكل|جوعان|اشتهي)|(جوع|جوعان|اشتهي).*(الليل|بالليل)").containsMatchIn(q)) {
        return CoachReply(
            "جوع الليل غالباً تعب أو ملل أكثر منه جوع حقيقي. جرّب ٥ دقائق تهدئة، وإذا بعدها جوعان: زبادي يوناني أو شاي بدون سكر.",
            listOf(
                CoachAction.StartWorkout("night-5"),
                CoachAction.AddIfThen("إذا جاني جوع بعد الساعة ٩", "أشرب كوب ماء أو شاي، وإذا استمر آكل زبادي يوناني"),
            ),
        )
    }
    if (Regex("(عزيمه|عزومه|وليمه|مناسبه|عرس|ضيوف|مطعم)").containsMatchIn(q)) {
        return CoachReply(
            "العزايم جزء من حياتنا، ما نبي نهرب منها. الخطة: صحن واحد، نصه سلطة ومشاوي، ربع رز، وتحلية صغيرة إذا تبي. وقبلها بساعتين وجبة بروتين خفيفة.",
            listOf(CoachAction.AddIfThen("إذا عندي عزيمة", "آكل بروتين خفيف قبلها وآخذ صحن واحد: نص خضار، ربع بروتين، ربع رز")),
        )
    }
    if (Regex("(ما نزل|زاد وزني|وزني زاد|ثابت|ما تغير|نفس الوزن)").containsMatchIn(q)) {
        val tr = trendWeights(weightPoints(state.days.values))
        val line = if (tr.size >= 2) " خط اتجاهك: ${ar(tr.first().trend)} ← ${ar(tr.last().trend)} كغ." else ""
        return CoachReply(
            "الميزان اليومي يتأثر بالماء والملح والنوم، ممكن يتحرك كيلو بيوم. نحكم على الاتجاه الأسبوعي مو القراءة.$line استمر أسبوعين بتسجيل صادق، وسند يعدّل هدفك تلقائياً.",
            emptyList(),
        )
    }
    val profile = state.profile
    if (profile != null && (Regex("(رمضان|صيام|صايم|سحور)").containsMatchIn(q) || (profile.ramadan && Regex("(فطور|افطار)").containsMatchIn(q)))) {
        val plan = ramadanPlan(profile, t)
        val split = plan.meals.joinToString("\n") { "${it.name}: ${ar(it.kcal)} سعرة و${ar(it.protein)} غ بروتين" }
        val lead = if (profile.ramadan) "خطتك الرمضانية لليوم:" else "إذا صايم، هذا توزيع هدفك على رمضان (فعّل وضع رمضان من صفحة التقدّم):"
        return CoachReply("$lead\n$split\n${plan.meals.first().tip}\n${plan.move}", emptyList())
    }
    if (profile != null && Regex("(اسبوعي|الاسبوع|مراجعه|تقييمي|كيف ماشي)").containsMatchIn(q)) {
        val r = weeklyReview(profile, state.days, t, today)
        return CoachReply("مراجعة آخر ٧ أيام: حضرت ${ar(r.activeDays)} من ٧، وتمرنت ${ar(r.workouts)} مرات.\n${r.win}\n${r.focusText}", emptyList())
    }
    if (Regex("(حلا|حلويات|سكر|شوكولا|ابي حلو)").containsMatchIn(q)) {
        return CoachReply("ما في أكل ممنوع. خذ حصة صغيرة وأنت مستمتع، بعد وجبة فيها بروتين — مو على جوع. وسجّلها بدون تأنيب.", emptyList())
    }
    val focus = if (proteinIn < t.protein / 2) "البروتين (أكلت ${ar(proteinIn)} من ${ar(t.protein)} غ)" else "حركة قصيرة بعد وجبتك الجاية"
    val energyNote = if (day?.energy == Energy.LOW) "بما إن طاقتك اليوم تحت، " else ""
    return CoachReply("${energyNote}خلنا نركّز على شي واحد: $focus. قل لي وش أكلت وأحسبه لك، أو قل \"تعبان\" وأعطيك أخف خطة.", emptyList())
}
