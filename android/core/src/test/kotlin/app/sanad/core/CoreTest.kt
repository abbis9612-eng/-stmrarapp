package app.sanad.core

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoreTest {
    private val profile = Profile(
        name = "سارة", sex = Sex.F, age = 34, heightCm = 162.0, startWeightKg = 88.0, goalWeightKg = 70.0,
        activity = Activity.SEDENTARY, pace = Pace.STEADY, barriers = listOf(Barrier.TIME, Barrier.NIGHT), createdAt = "2026-09-01",
    )

    private fun day(date: String, meals: Int = 0, weight: Double? = null, done: List<String> = emptyList()) = DayLog(
        date = date,
        meals = if (meals > 0) listOf(MealEntry("m$date", "x", meals, 90, 0, MealSource.QUICK)) else emptyList(),
        weightKg = weight,
        done = done,
    )

    @Test fun mifflinMatchesReference() {
        assertEquals(1561.5, bmr(Sex.F, 88.0, 162.0, 34), 0.01)
        assertEquals(1930.0, bmr(Sex.M, 100.0, 180.0, 40), 0.01)
    }

    @Test fun proteinUsesAdjustedWeight() {
        val ref = referenceWeight(88.0, 162.0)
        assertTrue(ref < 88)
        val p = proteinTarget(88.0, 162.0)
        assertTrue(p >= 1.2 * ref - 5 && p <= 1.6 * ref + 5)
    }

    @Test fun targetsRespectFloorAndRate() {
        val t = computeTargets(profile.copy(heightCm = 150.0, age = 60, pace = Pace.BRISK, startWeightKg = 62.0), 62.0)
        assertTrue(t.kcal >= 1200)
        assertTrue(t.floorApplied)
        assertTrue(computeTargets(profile.copy(pace = Pace.BRISK), 88.0).weeklyLossKg / 88 < 0.01)
    }

    @Test fun trendSmoothsSpikesAndWeightsGaps() {
        val t = trendWeights(listOf("2026-09-01" to 88.0, "2026-09-02" to 87.8, "2026-09-03" to 89.5))
        assertTrue(t[2].trend < 88.3)
        val short = trendWeights(listOf("2026-09-01" to 90.0, "2026-09-02" to 85.0))
        val long = trendWeights(listOf("2026-09-01" to 90.0, "2026-09-15" to 85.0))
        assertTrue(long[1].trend < short[1].trend)
    }

    @Test fun adaptiveLearnsLowerTdeeOnFlatWeight() {
        val days = (0 until 21).map { i -> day(addDays("2026-09-01", i.toLong()), meals = 1700, weight = 88.0 + if (i % 2 == 1) 0.2 else -0.2) }
        val r = adaptiveTdee(profile, days, "2026-09-21")
        assertNotNull(r)
        assertTrue(r.tdee < 1860)
        assertTrue(r.confidence > 0.5)
        assertNull(adaptiveTdee(profile, listOf(day("2026-09-01", weight = 88.0)), "2026-09-02"))
    }

    @Test fun threadSurvivesOneMissBreaksOnTwo() {
        val c = { d: String -> d to day(d, done = listOf("move")) }
        assertEquals(3, computeThread(mapOf(c("2026-09-01"), c("2026-09-02"), c("2026-09-04")), "2026-09-04").length)
        val broken = computeThread(mapOf(c("2026-09-01"), c("2026-09-02"), c("2026-09-05")), "2026-09-05")
        assertEquals(1, broken.length)
        assertEquals(2, broken.best)
        val rescue = computeThread(mapOf(c("2026-09-01")), "2026-09-03")
        assertTrue(rescue.rescueToday)
    }

    @Test fun weaveCellsMarkHeldAndBroken() {
        val c = { d: String -> d to day(d, done = listOf("eat")) }
        val cells = weaveCells(mapOf(c("2026-09-01"), c("2026-09-03"), c("2026-09-06")), "2026-09-06", 6, "2026-09-01")
        assertEquals(listOf(WeaveCell.WOVEN, WeaveCell.HELD, WeaveCell.WOVEN, WeaveCell.BROKEN, WeaveCell.BROKEN, WeaveCell.WOVEN), cells)
    }

    @Test fun missionsAdaptAndReferenceRealRoutines() {
        val t = computeTargets(profile, 88.0)
        assertEquals("reset-2", dayMissions(Energy.LOW, TimeBudget.TWENTY, t, profile)[0].routineId)
        assertEquals("strength-20", dayMissions(Energy.HIGH, TimeBudget.TWENTY, t, profile)[0].routineId)
        assertContains(dayMissions(Energy.MID, TimeBudget.TEN, t, profile)[2].title, "المطبخ")
        for (e in Energy.entries) for (tm in TimeBudget.entries) for (m in dayMissions(e, tm, t, profile))
            m.routineId?.let { assertNotNull(routineById(it), it) }
    }

    @Test fun foodSearchAndParsing() {
        assertEquals("kabsa-chicken", searchFoods("كبسه").first().id)
        val ids = parseMealText("تغديت كبسة دجاج ولبن ونص صحن سلطة").map { it.food.id }
        assertContains(ids, "kabsa-chicken")
        assertContains(ids, "laban")
        assertContains(parseMealText("فطرت بيضتين وشاي").map { it.food.id }, "eggs-2")
        assertTrue(FOODS.size >= 60)
        assertEquals(FOODS.size, FOODS.map { it.id }.toSet().size)
    }

    @Test fun safetyScreening() {
        assertTrue(assessSafety(16, 80.0, 165.0, emptyList()).block)
        assertTrue(assessSafety(30, 80.0, 165.0, listOf(SafetyFlag.PREGNANT)).block)
        val glp = assessSafety(40, 100.0, 170.0, listOf(SafetyFlag.GLP1))
        assertFalse(glp.block)
        assertEquals(1, glp.notes.size)
    }

    @Test fun routinesAreConsistent() {
        for (r in ROUTINES) {
            val mins = r.totalSeconds / 60.0
            assertTrue(abs(mins - r.minutes) <= maxOf(1.0, r.minutes * 0.25), "${r.id} lasts $mins min")
            for (m in r.moves) m.exerciseId?.let { assertNotNull(exerciseById(it), "${r.id} → $it") }
        }
    }

    @Test fun exercisesAreWellFormedAndAnatomicallySane() {
        assertEquals(EXERCISES.size, EXERCISES.map { it.id }.toSet().size)
        for (e in EXERCISES) {
            assertTrue(e.frames.size >= 2, e.id)
            e.easier?.let { assertNotNull(exerciseById(it), "${e.id} easier $it") }
            e.harder?.let { assertNotNull(exerciseById(it), "${e.id} harder $it") }
            for (f in e.frames) {
                for (v in f.xy) assertTrue(v in 0f..100f, "${e.id} out of box")
                // أطوال الأطراف لازم تبقى معقولة (ما يتمدد الجسم)
                val thigh = hypot(f.x(Joint.HIP) - f.x(Joint.KNEE_N), f.y(Joint.HIP) - f.y(Joint.KNEE_N))
                val torso = hypot(f.x(Joint.NECK) - f.x(Joint.HIP), f.y(Joint.NECK) - f.y(Joint.HIP))
                assertTrue(thigh in 12f..30f, "${e.id} thigh=$thigh")
                assertTrue(torso in 20f..36f, "${e.id} torso=$torso")
            }
        }
    }

    @Test fun poseInterpolation() {
        val a = EXERCISES.first().frames[0]
        val b = EXERCISES.first().frames[1]
        val mid = a.lerp(b, 0.5f)
        assertEquals((a.x(Joint.HIP) + b.x(Joint.HIP)) / 2, mid.x(Joint.HIP), 0.001f)
    }

    @Test fun offlineCoachLogsGulfMeals() {
        val t = computeTargets(profile, 88.0)
        val r = offlineReply("فطرت بيضتين وشاي كرك", AppState(profile = profile), t, "2026-09-10")
        val meals = r.actions.filterIsInstance<CoachAction.LogMeal>()
        assertEquals(305, meals.sumOf { it.kcal })
        assertContains(r.text, "٣٠٥")
        val tired = offlineReply("اليوم تعبان مرة", AppState(profile = profile), t, "2026-09-10")
        assertEquals(CoachAction.StartWorkout("reset-2"), tired.actions.single())
    }

    @Test fun arabicNumbers() {
        assertEquals("١٬٥٠٠", ar(1500))
        assertEquals("٨٧٫٥", ar(87.5))
        assertEquals("٠", ar(0))
        assertEquals(87.5, parseNum("٨٧٫٥"))
        assertEquals(92.0, parseNum("92"))
    }
}
