package app.sanad.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Glp1Test {
    private val base = Profile(
        name = "س", sex = Sex.F, age = 42, heightCm = 160.0, startWeightKg = 95.0, goalWeightKg = 75.0,
        activity = Activity.SEDENTARY, pace = Pace.STEADY, createdAt = "2026-09-01",
    )

    @Test fun moreProteinAndWater() {
        val off = computeTargets(base, 95.0)
        val on = computeTargets(base.copy(glp1 = true), 95.0)
        assertTrue(on.protein > off.protein)
        assertEquals(10, on.water)
        assertEquals(off.kcal, on.kcal)
    }

    @Test fun missionsPrioritiseStrengthAndProtein() {
        val t = computeTargets(base.copy(glp1 = true), 95.0)
        val m = dayMissions(Energy.MID, TimeBudget.TEN, t, base.copy(glp1 = true))
        assertEquals("strength-10", m.first { it.id == "move" }.routineId)
        assertTrue("أول لقمة" in m.first { it.id == "eat" }.title)
    }

    @Test fun tipsIncludeDoctorSafety() {
        assertTrue(GLP1_TIPS.any { "طبيب" in it })
    }
}
