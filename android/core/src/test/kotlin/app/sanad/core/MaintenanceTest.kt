package app.sanad.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MaintenanceTest {
    private val p = Profile(
        name = "علي", sex = Sex.M, age = 35, heightCm = 175.0, startWeightKg = 100.0, goalWeightKg = 85.0,
        activity = Activity.SEDENTARY, pace = Pace.STEADY, createdAt = "2026-01-01",
    )
    private fun steady(kg: Double) = (0..20).associate { i ->
        val d = addDays("2026-09-26", -i.toLong()); d to DayLog(d, weightKg = kg)
    }

    @Test fun maintenanceRemovesDeficitAndAddsSteps() {
        val lose = computeTargets(p, 85.0)
        val keep = computeTargets(p.copy(maintainSince = "2026-09-01"), 85.0)
        assertEquals(keep.tdee, keep.kcal)
        assertTrue(keep.kcal > lose.kcal)
        assertEquals(lose.steps + 1000, keep.steps)
        assertEquals(0.0, keep.weeklyLossKg)
    }

    @Test fun reachedGoalUsesTrend() {
        assertTrue(reachedGoal(p, steady(84.8)))
        assertFalse(reachedGoal(p, steady(86.0)))
        assertFalse(reachedGoal(p.copy(maintainSince = "2026-09-01"), steady(84.0)))
    }

    @Test fun zones() {
        val m = p.copy(maintainSince = "2026-09-01")
        assertEquals(Zone.GREEN, maintenanceStatus(m, steady(85.5))?.zone)
        assertEquals(Zone.AMBER, maintenanceStatus(m, steady(86.5))?.zone)
        assertEquals(Zone.RED, maintenanceStatus(m, steady(87.6))?.zone)
        assertNull(maintenanceStatus(p, steady(85.0)))
    }
}
