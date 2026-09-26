package app.sanad.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.roundToInt

/*
 * "سند الحارس": ميزات تمنع الانقطاع قبل ما يصير.
 * - رادار الزلّة: خطر اللحظة من إشارات يعرفها التطبيق (JITAI مبني على قواعد).
 * - طقس الميزان: تفسير قفزة الوزن حتى ما يترك الشخص الوزن بعد زيادة.
 * - الرجوع: استقبال بلا لوم بعد الغياب.
 * - زلّيت: خطة رجوع فورية تكسر تفكير "الكل أو لا شيء".
 * - جدول التنبيهات: متى نتواصل وبأي كلام.
 */

/* ---------------- رادار الزلّة ---------------- */

enum class RiskLevel { LOW, MID, HIGH }

data class RiskSignal(val id: String, val weight: Int, val text: String)

data class Risk(
    val score: Int,
    val level: RiskLevel,
    val signals: List<RiskSignal>,
    /** خطة المستخدم نفسه المناسبة للحظة، إن وجدت */
    val plan: IfThen?,
    /** أداة سريعة مقترحة (معرّف جلسة حركة/تهدئة) */
    val toolRoutineId: String,
    val headline: String,
)

private val SOCIAL_WORDS = Regex("(عزيمه|عزومه|وليمه|مناسبه|عرس|ضيوف|مطعم|سفره|طلعه)")
private val HEAVY_WORDS = Regex("(رز|كبسه|مندي|برياني|شاورما|بيتزا|برگر|برجر|مقلي|كنافه|بقلاوه|حلو|قيمه|تشريب|دولمه|باچه|پاچه)")

/**
 * درجة خطر من ٠ إلى ١٠٠. القواعد مستوحاة من أدبيات JITAI والزلّات الغذائية:
 * الليل، نهاية الأسبوع، التعب، قلة النوم، الغياب، زيادة الوزن، الأكل العالي مبكراً، والمناسبات.
 */
fun lapseRisk(state: AppState, t: Targets, now: LocalDateTime): Risk {
    val p = state.profile
    val today = now.toLocalDate().toString()
    val d = state.days[today]
    val h = now.hour
    val sig = mutableListOf<RiskSignal>()

    if (h >= 21 || h < 2) sig += RiskSignal("night", 25, "آخر الليل")
    else if (h >= 17) sig += RiskSignal("evening", 10, "المسا")
    if (now.dayOfWeek == DayOfWeek.THURSDAY || now.dayOfWeek == DayOfWeek.FRIDAY) sig += RiskSignal("weekend", 10, "نهاية الأسبوع")
    if (d?.energy == Energy.LOW) sig += RiskSignal("tired", 20, "طاقتك تحت")
    d?.sleepHours?.let { if (it < 6.5) sig += RiskSignal("sleep", 20, "نومك قليل (${ar(it)} ساعة)") }
    val yesterday = addDays(today, -1)
    if (state.days.isNotEmpty() && !isCounted(state.days[yesterday]) && (p?.createdAt ?: today) <= yesterday) {
        sig += RiskSignal("missed", 15, "أمس فات")
    }
    val tr = trendWeights(weightPoints(state.days.values.filter { it.date <= today }))
    val weekAgo = tr.lastOrNull { daysBetween(it.date, today) >= 6 }
    val last = tr.lastOrNull()
    if (weekAgo != null && last != null && last.trend - weekAgo.trend > 0.3) sig += RiskSignal("gain", 15, "الاتجاه طالع هالأسبوع")
    if (d != null && h < 18 && d.intake > t.kcal * 0.85) sig += RiskSignal("early", 15, "أكلت أغلب حصتك قبل المسا")
    val recentChat = state.chat.takeLast(12).filter { it.role == ChatRole.USER && it.at > 0 }
    if (d?.gathering == true) sig += RiskSignal("social", 20, "عندك عزيمة اليوم")
    else if (recentChat.any { SOCIAL_WORDS.containsMatchIn(normalizeArabic(it.text)) }) sig += RiskSignal("social", 15, "عندك مناسبة")
    if (p != null && Barrier.NIGHT in p.barriers && h >= 21) sig += RiskSignal("night-barrier", 10, "جوع الليل من عوائقك")
    if (p != null && Barrier.STRESS in p.barriers && d?.energy == Energy.LOW) sig += RiskSignal("stress", 5, "التعب يفتح باب الأكل العاطفي")

    val score = sig.sumOf { it.weight }.coerceIn(0, 100)
    val level = when { score >= 55 -> RiskLevel.HIGH; score >= 30 -> RiskLevel.MID; else -> RiskLevel.LOW }
    val ids = sig.map { it.id }.toSet()

    // نختار خطة "إذا… فأنا…" من خطط المستخدم نفسه حسب نوع اللحظة
    val plans = p?.ifThens.orEmpty()
    fun find(vararg words: String) = plans.firstOrNull { r -> words.any { w -> w in normalizeArabic(r.whenText) } }
    val plan = when {
        "social" in ids -> find("عزيمه", "عزومه", "مناسبه")
        "night" in ids || "night-barrier" in ids -> find("الليل", "٩", "بالليل")
        "tired" in ids || "sleep" in ids -> find("تعبان", "صحيت")
        else -> null
    } ?: plans.firstOrNull()

    val tool = when {
        "night" in ids -> "night-5"
        "tired" in ids || "sleep" in ids -> "reset-2"
        else -> "wake-2"
    }
    val headline = when (level) {
        RiskLevel.HIGH -> "لحظة حساسة: ${sig.sortedByDescending { it.weight }.take(2).joinToString(" + ") { it.text }}"
        RiskLevel.MID -> "انتبه لنفسك شوي: ${sig.maxByOrNull { it.weight }?.text ?: ""}"
        RiskLevel.LOW -> "يومك هادي"
    }
    return Risk(score, level, sig, plan, tool, headline)
}

/* ---------------- طقس الميزان ---------------- */

data class WeighIn(
    val todayKg: Double,
    val previousKg: Double?,
    val rawDelta: Double?,
    val trendKg: Double,
    val trendDelta7: Double?,
    val causes: List<String>,
    val headline: String,
    val body: String,
    /** نبرة: قفزة، نزول، ثبات */
    val kind: Kind,
) {
    enum class Kind { JUMP, DROP, STEADY, FIRST }
}

/**
 * يفسّر وزن اليوم مثل نشرة جوية: الرقم اليومي يتأثر بالماء والملح والكربوهيدرات والنوم والدورة،
 * والحكم الحقيقي للاتجاه. هدفه ما يترك الشخص الميزان بعد قفزة.
 */
fun weighInWeather(state: AppState, today: String, t: Targets?): WeighIn? {
    val d = state.days[today] ?: return null
    val kg = d.weightKg ?: return null
    val prevDay = state.days.values.filter { it.date < today && it.weightKg != null }.maxByOrNull { it.date }
    val prev = prevDay?.weightKg
    val raw = prev?.let { ((kg - it) * 10).roundToInt() / 10.0 }
    val tr = trendWeights(weightPoints(state.days.values.filter { it.date <= today }))
    val trendNow = tr.lastOrNull()?.trend ?: kg
    val weekAgo = tr.lastOrNull { daysBetween(it.date, today) >= 7 }
    val trend7 = weekAgo?.let { ((trendNow - it.trend) * 100).roundToInt() / 100.0 }

    val causes = mutableListOf<String>()
    val y = state.days[addDays(today, -1)]
    if (y != null) {
        if (y.meals.any { HEAVY_WORDS.containsMatchIn(normalizeArabic(it.name)) }) causes += "أكل أمس فيه رز أو مالح: كل غرام كربوهيدرات يمسك ٣–٤ غرام ماي"
        if (t != null && y.intake > t.kcal * 1.15) causes += "أمس أكلت أكثر من هدفك؛ جزء كبير من القفزة أكل بالمعدة وماي"
        if (y.water in 1..3) causes += "شربت ماي قليل أمس؛ الجسم يمسك ماي أكثر"
    }
    d.sleepHours?.let { if (it < 6.5) causes += "نومك قليل؛ هرمونات التوتر تمسك ماي" }
    if (state.profile?.sex == Sex.F) causes += "إذا قريبة الدورة، طبيعي يزيد ١–٣ كغ ماي ويرجع"
    if (prevDay != null && daysBetween(prevDay.date, today) >= 7) causes += "صار لك أسبوع ما وزنت؛ الفرق فيه ماي وأكل مو بس دهون"

    val kind = when {
        raw == null -> WeighIn.Kind.FIRST
        raw >= 0.5 -> WeighIn.Kind.JUMP
        raw <= -0.5 -> WeighIn.Kind.DROP
        else -> WeighIn.Kind.STEADY
    }
    val trendLine = when {
        trend7 == null -> "خلّينا نجمع قراءات أكثر حتى يبان الاتجاه."
        trend7 <= -0.1 -> "الاتجاه الحقيقي نازل ${ar(abs(trend7))} كغ بالأسبوع — هذا المهم."
        trend7 >= 0.3 -> "الاتجاه طالع ${ar(trend7)} كغ؛ نعدّل شي صغير، مو نوقف."
        else -> "الاتجاه ثابت تقريباً؛ طبيعي بفترات، كمّل."
    }
    val (headline, body) = when (kind) {
        WeighIn.Kind.FIRST -> "أول قراءة انحفظت" to "من هسه نرسم خطك الاتجاهي. وزّن الصبح بعد الحمام وقبل الأكل، ٣ مرات بالأسبوع تكفي."
        WeighIn.Kind.JUMP -> "قفزة ${ar(raw!!)} كغ… غالباً ماي مو دهن" to ((if (causes.isNotEmpty()) causes.take(2).joinToString("\n") + "\n" else "زيادة كيلو دهون تحتاج ~٧٧٠٠ سعرة زيادة، وهذا ما يصير بيوم.\n") + trendLine)
        WeighIn.Kind.DROP -> "نزلت ${ar(abs(raw!!))} كغ عن آخر مرة" to "حلو، بس ما نفرح بالرقم اليومي ولا نزعل منه. $trendLine"
        WeighIn.Kind.STEADY -> "ثابت تقريباً" to trendLine
    }
    return WeighIn(kg, prev, raw, trendNow, trend7, causes, headline, body, kind)
}

/* ---------------- الرجوع بعد الغياب ---------------- */

data class Welcome(val daysAway: Int, val headline: String, val body: String, val mission: String)

/** لو آخر يوم مسجّل قبل يومين أو أكثر: استقبال بدون لوم ومهمة وحدة صغيرة. */
fun welcomeBack(state: AppState, today: String): Welcome? {
    val lastActive = state.days.values.filter { it.date < today && isCounted(it) }.maxByOrNull { it.date } ?: return null
    if (isCounted(state.days[today])) return null
    val away = daysBetween(lastActive.date, today).toInt() - 1
    if (away < 2) return null
    val name = state.profile?.name.orEmpty()
    val headline = if (name.isNotBlank()) "هلا بيك ${name}، اشتقنا" else "هلا بيك، اشتقنا"
    val body = when {
        away <= 4 -> "غبت ${ar(away)} أيام، وهذا يصير ويا الكل. ما نرجع من الصفر؛ عاداتك بعدها موجودة، بس نشغّلها."
        away <= 14 -> "صار لك ${ar(away)} يوم. الدراسات تكول الغياب القصير ما يمسح اللي بنيته. اليوم بس خطوة وحدة."
        else -> "رجوعك بعد ${ar(away)} يوم هو أصعب خطوة، وسويتها. نبدأ خفيف ونعدّل خطتك على وضعك الحالي."
    }
    return Welcome(away, headline, body, "سجّل وجبة وحدة أو سوّ دقيقتين حركة — وبس")
}

/* ---------------- زلّيت ---------------- */

enum class LapseKind(val label: String) {
    OVEREAT("أكلت هواية"),
    NIGHT("أكلت بالليل"),
    SWEETS("حلويات"),
    SOCIAL("عزيمة وخربت"),
    SKIPPED("ما سويت شي اليوم"),
}

data class Recovery(val title: String, val steps: List<String>, val reframe: String)

/**
 * خطة الرجوع بعد الزلّة. المبدأ: لا تعويض بالحرمان (يولّد دورة حرمان/إفراط)،
 * الوجبة الجاية عادية، وخطوة صغيرة الحين تكسر "خلاص خربت".
 */
fun lapseRecovery(kind: LapseKind, t: Targets): Recovery {
    val proteinMeal = ((t.protein / 3.0) / 5).roundToInt() * 5
    val reframe = "زلّة وحدة ما تسوي انتكاسة. اللي يفرق هو الخطوة الجاية، مو اللي صار."
    return when (kind) {
        LapseKind.OVEREAT -> Recovery(
            "صار، وعادي",
            listOf(
                "لا تعوّض بالجوع بكرة — هذا يرجعك لنفس الدائرة.",
                "الوجبة الجاية عادية: تبدأ ببروتين (~${ar(proteinMeal)} غ) وخضار.",
                "هسه: كوب ماي و١٠ دقايق مشي خفيف إذا تكدر.",
            ),
            reframe,
        )
        LapseKind.NIGHT -> Recovery(
            "أكل الليل له سبب",
            listOf(
                "اسأل نفسك: جوع لو تعب لو ملل؟ الجواب يحدد الحل.",
                "بكرة: عشا فيه بروتين أكثر، والمطبخ يسكّر بساعة ثابتة.",
                "إذا صار الجوع بالليل: ماي أو شاي أول، وبعدها زبادي إذا بعدك جوعان.",
            ),
            reframe,
        )
        LapseKind.SWEETS -> Recovery(
            "ما في أكل ممنوع",
            listOf(
                "سجّلها بدون تأنيب — التسجيل الصادق أهم من المثالية.",
                "المرة الجاية: قطعة صغيرة بعد وجبة فيها بروتين، مو على جوع.",
                "خلي الحلو برّه البيت، واطلبه بالمناسبات بس.",
            ),
            reframe,
        )
        LapseKind.SOCIAL -> Recovery(
            "العزايم جزء من حياتنا",
            listOf(
                "ليلة وحدة ما تخرب أسابيع. بكرة يوم عادي كامل.",
                "المرة الجاية: بروتين خفيف قبلها بساعتين، وصحن واحد بدون إعادة.",
                "جملة جاهزة للإحراج: «والله شبعت، الله يديمها نعمة».",
            ),
            reframe,
        )
        LapseKind.SKIPPED -> Recovery(
            "اليوم ما خلص بعد",
            listOf(
                "أصغر شي يحسب: دقيقتين حركة أو تسجيل وجبة وحدة.",
                "قاعدة سند: يوم فائت عادي، بس لا تخليه يومين.",
            ),
            reframe,
        )
    }
}

/* ---------------- جدول التنبيهات ---------------- */

data class Reminder(val at: LocalDateTime, val id: String, val title: String, val body: String)

/**
 * التنبيه الجاي فقط (نجدول واحد بكل مرة ونعيد الحساب بعده).
 * لطيف ومبني على اللحظة: الصبح سؤال طاقة، الظهر تسجيل، المسا رادار، الليل نوم، والغائب ترحيب.
 * بدون تنبيهات بين ١١ الليل و٨ الصبح.
 */
fun nextReminder(state: AppState, t: Targets, now: LocalDateTime): Reminder? {
    val p = state.profile ?: return null
    fun at(date: LocalDate, h: Int, m: Int = 0) = date.atTime(h, m)
    val candidates = mutableListOf<Reminder>()
    for (offset in 0L..1L) {
        val date = now.toLocalDate().plusDays(offset)
        val key = date.toString()
        val d = state.days[key]
        val welcome = welcomeBack(state, key)
        if (welcome != null) {
            candidates += Reminder(at(date, 19, 30), "welcome", welcome.headline, "${welcome.mission}.")
        }
        if (d?.energy == null) candidates += Reminder(at(date, 9, 0), "morning", "صباح الخير ${p.name}", "شلون طاقتك اليوم؟ لمسة وحدة وخطتك تتفصّل على قدّك.")
        if (d == null || d.meals.none { it.at > 0 }) candidates += Reminder(at(date, 14, 30), "lunch", "شنو تغديت؟", "قول لسند بجملة أو صوّر صحنك — ثواني بس.")
        if (d?.gathering == true) {
            candidates += Reminder(at(date, 17, 30), "gathering", "قبل العزيمة بشوي", "بروتين خفيف هسه وكوبين ماي. هناك: صحن واحد، وتمن بقدّ قبضتك.")
        }
        val risk = lapseRisk(state, t, at(date, 20, 45))
        if (risk.level != RiskLevel.LOW) {
            val plan = risk.plan?.let { "${it.whenText} ← ${it.thenText}" } ?: "جرّب ٥ دقايق تهدئة قبل ما تفتح الثلاجة."
            candidates += Reminder(at(date, 20, 45), "radar", "لحظة حساسة جاية", plan)
        }
        // النوم: بس لما يكون اليوم متعب أو النوم قليل (ما نزعج كل ليلة)
        if (d?.energy == Energy.LOW || (d?.sleepHours ?: 7.0) < 6.5) {
            candidates += Reminder(at(date, 22, 30), "sleep", "النوم جزء من الخطة", "نوم أبكر بنص ساعة يقلل الأكل بكرة تقريباً ٢٧٠ سعرة. تصبح على خير.")
        }
    }
    return candidates.filter { it.at.isAfter(now) && it.at.hour in 8..22 }.minByOrNull { it.at }
}
