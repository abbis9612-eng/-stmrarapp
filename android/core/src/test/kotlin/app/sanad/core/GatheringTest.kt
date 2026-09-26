package app.sanad.core

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GatheringTest {
    private val p = Profile(
        name = "علي", sex = Sex.M, age = 35, heightCm = 175.0, startWeightKg = 100.0, goalWeightKg = 85.0,
        activity = Activity.SEDENTARY, pace = Pace.STEADY, createdAt = "2026-09-01",
        ifThens = listOf(IfThen("b", "إذا عندي عزيمة", "صحن واحد")),
    )
    private val t = computeTargets(p, 100.0)

    @Test fun budgetReservesProteinSnackWhenLowProtein() {
        val d = DayLog("2026-09-26", meals = listOf(MealEntry("m", "صمون", 500, 10, 1, MealSource.QUICK)))
        val g = gatheringPlan(t, d)
        val expected = (((t.kcal - 500 - 150) / 50.0).let { kotlin.math.round(it) } * 50).toInt()
        assertEquals(expected, g.budget)
        assertTrue(g.before.first().contains("بروتين"))
        assertTrue(g.budget % 50 == 0)
    }

    @Test fun noSnackWhenProteinCovered() {
        val d = DayLog("2026-09-26", meals = listOf(MealEntry("m", "دجاج", 400, 80, 1, MealSource.QUICK)))
        assertTrue(gatheringPlan(t, d).before.none { it.contains("زبادي") })
    }

    @Test fun overBudgetNeverNegativeAndNoGuilt() {
        val d = DayLog("2026-09-26", meals = listOf(MealEntry("m", "كل شي", 5000, 80, 1, MealSource.QUICK)))
        val g = gatheringPlan(t, d)
        assertEquals(0, g.budget)
        assertTrue("عادي" in g.note)
    }

    @Test fun gatheringFlagRaisesRadarAndPicksUserPlan() {
        val s = AppState(profile = p, days = mapOf("2026-09-26" to DayLog("2026-09-26", gathering = true, done = listOf("eat")), "2026-09-25" to DayLog("2026-09-25", done = listOf("eat"))))
        val r = lapseRisk(s, t, LocalDateTime.of(2026, 9, 26, 19, 0))
        assertTrue(r.signals.any { it.id == "social" })
        assertEquals("b", r.plan?.id)
    }

    @Test fun gatheringReminderBeforeEvening() {
        val s = AppState(profile = p, days = mapOf("2026-09-26" to DayLog("2026-09-26", energy = Energy.MID, gathering = true, meals = listOf(MealEntry("m", "x", 300, 20, 5, MealSource.QUICK)))))
        val r = nextReminder(s, t, LocalDateTime.of(2026, 9, 26, 15, 0))
        assertEquals("gathering", r?.id)
    }

    @Test fun offlineCoachGivesPlan() {
        val r = offlineReply("عندي عزيمة الليلة", AppState(profile = p), t, "2026-09-26")
        assertTrue("قبضتك" in r.text)
    }
}
