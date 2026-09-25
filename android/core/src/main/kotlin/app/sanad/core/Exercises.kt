package app.sanad.core

/**
 * مكتبة التمارين + رسوم متحركة بإطارات مفتاحية.
 * كل وضعية = ١٢ مفصل في صندوق ١٠٠×١٠٠ (الأرض y=٩٤)، منظر جانبي.
 * N = الطرف القريب، F = البعيد (يُرسم خلف الجسم).
 */
enum class Joint { HEAD, NECK, MID, HIP, ELBOW_N, HAND_N, ELBOW_F, HAND_F, KNEE_N, FOOT_N, KNEE_F, FOOT_F }

class Pose(val xy: FloatArray) {
    init { require(xy.size == Joint.entries.size * 2) }
    fun x(j: Joint) = xy[j.ordinal * 2]
    fun y(j: Joint) = xy[j.ordinal * 2 + 1]

    fun lerp(other: Pose, t: Float) = Pose(FloatArray(xy.size) { xy[it] + (other.xy[it] - xy[it]) * t })

    /** نسخة معدّلة لبعض المفاصل */
    fun with(vararg changes: Pair<Joint, Pair<Number, Number>>): Pose {
        val a = xy.copyOf()
        for ((j, p) in changes) {
            a[j.ordinal * 2] = p.first.toFloat(); a[j.ordinal * 2 + 1] = p.second.toFloat()
        }
        return Pose(a)
    }
}

private fun pose(vararg pts: Pair<Number, Number>): Pose {
    require(pts.size == 12)
    return Pose(FloatArray(24) { i -> (if (i % 2 == 0) pts[i / 2].first else pts[i / 2].second).toFloat() })
}

enum class Prop { NONE, CHAIR, WALL, MAT }

enum class Muscle(val label: String) {
    CHEST("الصدر"), BACK("الظهر"), SHOULDERS("الأكتاف"), ARMS("الذراعين"), CORE("البطن والجذع"),
    GLUTES("المؤخرة"), QUADS("الفخذ الأمامي"), HAMSTRINGS("الفخذ الخلفي"), CALVES("السمانة"),
    HEART("القلب والتنفس"), MOBILITY("المرونة"),
}

data class Exercise(
    val id: String,
    val name: String,
    val cue: String,
    val muscles: List<Muscle>,
    val mistakes: List<String>,
    val easier: String?,
    val harder: String?,
    val jointFriendly: Boolean,
    val prop: Prop,
    val frames: List<Pose>,
    /** مدة الانتقال بين إطارين بالملّي ثانية */
    val tempoMs: Int,
)

/* -------------------- وضعيات أساسية -------------------- */

private val STAND = pose(
    50 to 13, 50 to 24, 50 to 38, 50 to 52,
    52 to 38, 53 to 52, 48 to 38, 47 to 52,
    51 to 73, 52 to 94, 49 to 73, 48 to 94,
)

private val SQUAT_LOW = pose(
    58 to 34, 55 to 43, 47 to 55, 36 to 66,
    68 to 44, 80 to 43, 66 to 45, 78 to 45,
    57 to 72, 52 to 94, 56 to 73, 50 to 94,
)

private val STAND_ARMS_FWD = STAND.with(Joint.ELBOW_N to (62 to 30), Joint.HAND_N to (74 to 30), Joint.ELBOW_F to (61 to 31), Joint.HAND_F to (73 to 31))

private val SEATED = pose(
    46 to 24, 46 to 35, 46 to 49, 46 to 63,
    48 to 49, 56 to 60, 45 to 49, 54 to 61,
    66 to 63, 66 to 94, 65 to 64, 65 to 94,
)

private val PUSH_TOP = pose(
    83 to 56, 75 to 60, 61 to 63, 46 to 66,
    75 to 74, 75 to 88, 74 to 74, 74 to 88,
    29 to 74, 12 to 82, 28 to 75, 11 to 83,
)
private val PUSH_LOW = pose(
    84 to 71, 76 to 75, 62 to 77, 46 to 79,
    66 to 72, 75 to 88, 65 to 73, 74 to 88,
    29 to 83, 12 to 86, 28 to 84, 11 to 87,
)

private val WALL_TOP = pose(
    66 to 22, 62 to 31, 56 to 43, 50 to 55,
    72 to 36, 85 to 34, 71 to 37, 85 to 36,
    46 to 74, 42 to 94, 45 to 74, 40 to 94,
)
private val WALL_LOW = pose(
    73 to 20, 69 to 29, 61 to 41, 53 to 54,
    70 to 41, 85 to 34, 69 to 42, 85 to 36,
    47 to 74, 42 to 94, 46 to 74, 40 to 94,
)

private val LUNGE_LOW = pose(
    50 to 30, 50 to 41, 50 to 54, 49 to 66,
    52 to 54, 53 to 66, 48 to 54, 47 to 66,
    68 to 70, 66 to 94, 40 to 86, 22 to 92,
)

private val PLANK = pose(
    80 to 62, 72 to 66, 58 to 69, 44 to 72,
    70 to 86, 84 to 87, 69 to 86, 83 to 88,
    28 to 78, 12 to 85, 27 to 79, 11 to 86,
)

private val BRIDGE_DOWN = pose(
    88 to 86, 78 to 86, 64 to 87, 50 to 87,
    68 to 91, 58 to 92, 67 to 91, 57 to 92,
    36 to 70, 28 to 93, 35 to 71, 27 to 93,
)
private val BRIDGE_UP = BRIDGE_DOWN.with(Joint.HIP to (50 to 72), Joint.MID to (64 to 79), Joint.KNEE_N to (33 to 66), Joint.KNEE_F to (32 to 67))

private val MARCH_A = STAND.with(
    Joint.KNEE_N to (60 to 60), Joint.FOOT_N to (58 to 80),
    Joint.ELBOW_N to (47 to 38), Joint.HAND_N to (42 to 48), Joint.ELBOW_F to (55 to 37), Joint.HAND_F to (62 to 44),
)
private val MARCH_B = STAND.with(
    Joint.KNEE_F to (58 to 61), Joint.FOOT_F to (56 to 80),
    Joint.ELBOW_N to (55 to 37), Joint.HAND_N to (62 to 44), Joint.ELBOW_F to (47 to 38), Joint.HAND_F to (42 to 48),
)

private val WALK_A = STAND.with(
    Joint.KNEE_N to (56 to 72), Joint.FOOT_N to (62 to 94), Joint.KNEE_F to (46 to 74), Joint.FOOT_F to (38 to 92),
    Joint.ELBOW_N to (47 to 38), Joint.HAND_N to (43 to 50), Joint.ELBOW_F to (54 to 38), Joint.HAND_F to (59 to 49),
)
private val WALK_B = STAND.with(
    Joint.KNEE_F to (56 to 72), Joint.FOOT_F to (62 to 94), Joint.KNEE_N to (46 to 74), Joint.FOOT_N to (38 to 92),
    Joint.ELBOW_F to (47 to 38), Joint.HAND_F to (43 to 50), Joint.ELBOW_N to (54 to 38), Joint.HAND_N to (59 to 49),
)

private val GUARD = STAND.with(
    Joint.HIP to (49 to 54), Joint.KNEE_N to (53 to 74), Joint.KNEE_F to (47 to 74),
    Joint.ELBOW_N to (57 to 34), Joint.HAND_N to (59 to 26), Joint.ELBOW_F to (56 to 35), Joint.HAND_F to (58 to 27),
)
private val PUNCH_N = GUARD.with(Joint.ELBOW_N to (64 to 31), Joint.HAND_N to (78 to 30))
private val PUNCH_F = GUARD.with(Joint.ELBOW_F to (63 to 32), Joint.HAND_F to (77 to 31))

private val COW = pose(
    80 to 58, 72 to 66, 56 to 72, 38 to 70,
    72 to 78, 72 to 92, 71 to 78, 71 to 92,
    38 to 92, 18 to 93, 37 to 92, 17 to 93,
)
private val CAT = COW.with(Joint.MID to (56 to 60), Joint.HEAD to (79 to 74), Joint.NECK to (72 to 68))

private val CHILD = pose(
    78 to 88, 70 to 84, 56 to 80, 40 to 82,
    82 to 88, 94 to 90, 81 to 89, 93 to 91,
    56 to 92, 30 to 93, 55 to 93, 29 to 94,
)

private val HINGE = STAND.with(
    Joint.HEAD to (77 to 33), Joint.NECK to (68 to 38), Joint.MID to (56 to 45), Joint.HIP to (45 to 52),
    Joint.ELBOW_N to (69 to 50), Joint.HAND_N to (69 to 62), Joint.ELBOW_F to (68 to 51), Joint.HAND_F to (68 to 63),
)

private val ARMS_UP = STAND.with(Joint.ELBOW_N to (53 to 14), Joint.HAND_N to (55 to 3), Joint.ELBOW_F to (48 to 14), Joint.HAND_F to (46 to 3))

private val SEATED_KNEE = SEATED.with(Joint.KNEE_N to (64 to 52), Joint.FOOT_N to (70 to 74))
private val SEATED_SHRUG = SEATED.with(Joint.NECK to (46 to 33), Joint.ELBOW_N to (44 to 45), Joint.HAND_N to (50 to 58), Joint.ELBOW_F to (43 to 45), Joint.HAND_F to (48 to 59))

private val STEP_SIDE = STAND.with(Joint.KNEE_N to (56 to 73), Joint.FOOT_N to (60 to 94), Joint.ELBOW_N to (58 to 36), Joint.HAND_N to (66 to 40))

/* -------------------- التمارين -------------------- */

val EXERCISES: List<Exercise> = listOf(
    Exercise(
        "squat", "سكوات", "القدمين بعرض الكتفين، الورك للخلف كأنك بتجلس، صدرك مرفوع.",
        listOf(Muscle.QUADS, Muscle.GLUTES, Muscle.CORE),
        listOf("الركب تدخل للداخل", "الكعب يرتفع عن الأرض", "انحناء الظهر"),
        "chair-squat", "lunge", true, Prop.NONE, listOf(STAND_ARMS_FWD, SQUAT_LOW), 1300,
    ),
    Exercise(
        "chair-squat", "سكوات للكرسي", "انزل ببطء حتى تلمس الكرسي بخفة، ثم قم بقوة.",
        listOf(Muscle.QUADS, Muscle.GLUTES),
        listOf("تطيح على الكرسي بثقلك", "تستند على يديك"),
        "sit-stand", "squat", true, Prop.CHAIR, listOf(STAND_ARMS_FWD, SQUAT_LOW), 1500,
    ),
    Exercise(
        "sit-stand", "وقوف وجلوس", "من الكرسي، قم بدون ما تستند إن قدرت، واجلس ببطء.",
        listOf(Muscle.QUADS, Muscle.GLUTES),
        listOf("الاندفاع للأمام بسرعة"),
        null, "chair-squat", true, Prop.CHAIR, listOf(SEATED, STAND_ARMS_FWD), 1600,
    ),
    Exercise(
        "pushup", "ضغط", "جسمك خط مستقيم من الرأس للكعب، انزل بصدرك قريب من الأرض.",
        listOf(Muscle.CHEST, Muscle.ARMS, Muscle.CORE),
        listOf("الورك نازل أو مرفوع", "الكوع مفتوح للجنب بزاوية ٩٠°"),
        "wall-pushup", null, true, Prop.MAT, listOf(PUSH_TOP, PUSH_LOW), 1300,
    ),
    Exercise(
        "wall-pushup", "ضغط على الجدار", "يداك على الجدار بعرض الكتفين، انزل بصدرك للجدار.",
        listOf(Muscle.CHEST, Muscle.ARMS),
        listOf("الرقبة تنزل قبل الصدر"),
        null, "pushup", true, Prop.WALL, listOf(WALL_TOP, WALL_LOW), 1300,
    ),
    Exercise(
        "lunge", "طعنة خلفية", "خطوة للخلف، الركبة الخلفية قرب الأرض، وارجع.",
        listOf(Muscle.QUADS, Muscle.GLUTES, Muscle.HAMSTRINGS),
        listOf("الركبة الأمامية تتعدى الأصابع كثير", "الجذع يطيح للأمام"),
        "squat", null, false, Prop.NONE, listOf(STAND, LUNGE_LOW), 1400,
    ),
    Exercise(
        "plank", "بلانك", "على ساعديك، خط مستقيم من الرأس للكعب، شد بطنك وتنفس.",
        listOf(Muscle.CORE, Muscle.SHOULDERS),
        listOf("الورك نازل", "حبس النفس"),
        null, null, true, Prop.MAT, listOf(PLANK, PLANK.with(Joint.HIP to (44 to 71), Joint.MID to (58 to 68))), 1800,
    ),
    Exercise(
        "glute-bridge", "جسر الورك", "على ظهرك، ارفع الورك واعصر المؤخرة ثانيتين فوق.",
        listOf(Muscle.GLUTES, Muscle.HAMSTRINGS, Muscle.CORE),
        listOf("تقوّس أسفل الظهر", "الدفع من الأصابع بدل الكعب"),
        null, null, true, Prop.MAT, listOf(BRIDGE_DOWN, BRIDGE_UP), 1400,
    ),
    Exercise(
        "march", "مشي بمكانك", "ارفع ركبك بالتناوب وحرّك ذراعيك.",
        listOf(Muscle.HEART, Muscle.QUADS),
        listOf("الانحناء للخلف"),
        null, null, true, Prop.NONE, listOf(MARCH_A, STAND, MARCH_B, STAND), 380,
    ),
    Exercise(
        "walk", "مشي", "إيقاع تقدر تتكلم فيه لكن ما تقدر تغني.",
        listOf(Muscle.HEART, Muscle.CALVES),
        emptyList(),
        null, null, true, Prop.NONE, listOf(WALK_A, WALK_B), 520,
    ),
    Exercise(
        "step-touch", "خطوة جانبية مع لمس", "يمين ويسار بإيقاع ثابت، ذراعك تتحرك معك.",
        listOf(Muscle.HEART),
        emptyList(),
        null, "march", true, Prop.NONE, listOf(STAND, STEP_SIDE), 480,
    ),
    Exercise(
        "punches", "لكمات هوائية", "بطنك مشدود، بدّل اليدين بسرعة.",
        listOf(Muscle.HEART, Muscle.SHOULDERS, Muscle.CORE),
        listOf("قفل الكوع بعنف"),
        null, null, true, Prop.NONE, listOf(PUNCH_N, GUARD, PUNCH_F, GUARD), 260,
    ),
    Exercise(
        "seated-knee", "رفع الركبة جالساً", "ظهرك مستقيم، ارفع ركبة وحدة بالتناوب.",
        listOf(Muscle.CORE, Muscle.QUADS),
        listOf("الاستناد للخلف"),
        null, "march", true, Prop.CHAIR, listOf(SEATED, SEATED_KNEE), 700,
    ),
    Exercise(
        "shoulder-roll", "دوران الكتفين", "ارفع كتفيك للأذن وارجعها للخلف ببطء.",
        listOf(Muscle.SHOULDERS, Muscle.MOBILITY),
        emptyList(),
        null, null, true, Prop.CHAIR, listOf(SEATED, SEATED_SHRUG), 900,
    ),
    Exercise(
        "cat-cow", "القط والبقرة", "على اليدين والركب، قوّس ظهرك ثم اخفضه مع النفس.",
        listOf(Muscle.MOBILITY, Muscle.BACK),
        emptyList(),
        null, null, true, Prop.MAT, listOf(COW, CAT), 1800,
    ),
    Exercise(
        "child-pose", "وضعية الطفل", "ارخِ جسمك للخلف ومد ذراعيك للأمام.",
        listOf(Muscle.MOBILITY, Muscle.BACK),
        emptyList(),
        null, null, true, Prop.MAT, listOf(CHILD, CHILD.with(Joint.HAND_N to (96 to 90), Joint.HAND_F to (95 to 91))), 2200,
    ),
    Exercise(
        "hamstring-stretch", "إطالة الفخذ الخلفي", "مِل للأمام بظهر مستقيم حتى تحس بشد لطيف.",
        listOf(Muscle.HAMSTRINGS, Muscle.MOBILITY),
        listOf("تقويس الظهر للوصول أبعد"),
        null, null, true, Prop.NONE, listOf(STAND, HINGE), 2000,
    ),
    Exercise(
        "breathe", "تنفّس ٤-٦", "شهيق ٤ ثوانٍ مع رفع الذراعين، زفير ٦ ثوانٍ وأنت تنزلهم.",
        listOf(Muscle.MOBILITY),
        emptyList(),
        null, null, true, Prop.NONE, listOf(STAND, ARMS_UP), 4000,
    ),
)

fun exerciseById(id: String): Exercise? = EXERCISES.firstOrNull { it.id == id }

/* -------------------- الجلسات -------------------- */

data class Move(val exerciseId: String?, val seconds: Int, val cue: String? = null) {
    val isRest: Boolean get() = exerciseId == null
}

data class Routine(
    val id: String,
    val title: String,
    val minutes: Int,
    val energy: Int,
    val tag: String,
    val why: String,
    val moves: List<Move>,
) {
    val totalSeconds: Int get() = moves.sumOf { it.seconds }
    val exercises: List<Exercise> get() = moves.mapNotNull { m -> m.exerciseId?.let(::exerciseById) }.distinctBy { it.id }
}

private fun rest(s: Int) = Move(null, s)

val ROUTINES: List<Routine> = listOf(
    Routine(
        "reset-2", "إعادة شحن", 2, 1, "وأنت جالس",
        "لأيام التعب: تحريك الدورة الدموية بدون إجهاد. يحسب لك يوم كامل في الخيط.",
        listOf(Move("shoulder-roll", 30), Move("seated-knee", 40), Move("sit-stand", 50)),
    ),
    Routine(
        "wake-2", "صحصحة دقيقتين", 2, 2, "بين الاجتماعات",
        "دفعة قصيرة ترفع النبض وتكسر الجلوس الطويل.",
        listOf(Move("chair-squat", 40), Move("wall-pushup", 40), Move("march", 40)),
    ),
    Routine(
        "night-5", "قبل النوم ٥", 5, 1, "يهدّي الجوع",
        "حركة لطيفة وتنفّس تقلل رغبة الأكل الليلي وتحسّن النوم.",
        listOf(Move("breathe", 60), Move("cat-cow", 60), Move("child-pose", 60), Move("glute-bridge", 60, "ارفع وانزل مع النفس."), Move("breathe", 60, "وخلاص — المطبخ مسكّر.")),
    ),
    Routine(
        "low-impact-10", "١٠ دقائق بدون قفز", 10, 2, "مناسب للركب",
        "حرق وقوة بدون ضغط على المفاصل — مثالي مع الوزن الزائد.",
        listOf(
            Move("march", 60, "إحماء: تنفّس براحة."), Move("chair-squat", 45), rest(15), Move("wall-pushup", 45), rest(15),
            Move("step-touch", 60), Move("glute-bridge", 45), rest(15), Move("punches", 45), Move("chair-squat", 45, "الجولة الثانية — أبطأ وأدق."),
            rest(15), Move("wall-pushup", 45), rest(15), Move("glute-bridge", 45), Move("march", 60, "تهدئة."), Move("hamstring-stretch", 45),
        ),
    ),
    Routine(
        "strength-10", "قوة ١٠ دقائق", 10, 3, "يحمي العضل",
        "تمارين المقاومة أثناء النزول تحافظ على العضل وتمنع هبوط الحرق.",
        listOf(
            Move("squat", 45), rest(15), Move("pushup", 40), rest(20), Move("lunge", 45), rest(15), Move("plank", 30), rest(20),
            Move("squat", 45, "الجولة الثانية."), rest(15), Move("pushup", 40, "ركّز على النزول البطيء."), rest(20), Move("lunge", 45), rest(15),
            Move("glute-bridge", 45), Move("plank", 30, "آخر تحدي!"), Move("hamstring-stretch", 60),
        ),
    ),
    Routine(
        "strength-20", "قوة ومشي ٢٠", 20, 3, "الجلسة الكاملة",
        "مقاومة + كارديو معتدل: أفضل تركيبة لحرق الدهون مع حماية العضل.",
        listOf(Move("march", 120, "إحماء.")) +
            (1..3).flatMap {
                listOf(Move("squat", 45), rest(15), Move("pushup", 40), rest(20), Move("lunge", 45), rest(15), Move("plank", 30), rest(30))
            } +
            listOf(Move("walk", 300, "حافظ على إيقاع تقدر تتكلم فيه."), Move("hamstring-stretch", 60)),
    ),
    Routine(
        "walk-20", "مشي ٢٠ دقيقة", 20, 2, "بعد الأكل أفضل",
        "المشي بعد الوجبة يخفّض ارتفاع السكر ويرفع حرقك اليومي بدون إرهاق.",
        listOf(Move("walk", 180, "إحماء بإيقاع هادئ."), Move("walk", 840, "أسرع شوي: تتكلم لكن ما تغني."), Move("walk", 180, "تهدئة.")),
    ),
)

fun routineById(id: String): Routine? = ROUTINES.firstOrNull { it.id == id }
