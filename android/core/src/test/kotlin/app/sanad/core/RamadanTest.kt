package app.sanad.core

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RamadanTest {
    private val p = Profile(
        name = "أحمد", sex = Sex.M, age = 38, heightCm = 176.0, startWeightKg = 104.0, goalWeightKg = 85.0,
        activity = Activity.LIGHT, pace = Pace.STEADY, createdAt = "2026-09-01", ramadan = true,
    )
    private val t = computeTargets(p, 104.0)

    @Test fun planKeepsDailyTotals() {
        val plan = ramadanPlan(p, t)
        assertEquals(listOf("iftar", "snack", "suhoor"), plan.meals.map { it.id })
        assertTrue(abs(plan.meals.sumOf { it.kcal } - t.kcal) <= 20, "kcal ${plan.meals.sumOf { it.kcal }} vs ${t.kcal}")
        assertTrue(abs(plan.meals.sumOf { it.protein } - t.protein) <= 10, "protein ${plan.meals.sumOf { it.protein }} vs ${t.protein}")
        val suhoor = plan.meals.last()
        assertTrue(suhoor.protein >= plan.meals[1].protein, "suhoor carries more protein than the snack")
    }

    @Test fun medicationFlagsAddDoctorCaution() {
        assertEquals(1, ramadanPlan(p, t).cautions.size)
        val c = ramadanPlan(p.copy(flags = listOf(SafetyFlag.DIABETES_MEDS)), t).cautions
        assertTrue(c.any { "طبيب" in it }, c.toString())
        assertTrue(c.last().contains("أفطر"))
    }

    @Test fun missionsMoveTrainingAfterIftar() {
        val ms = dayMissions(Energy.HIGH, TimeBudget.TWENTY, t, p)
        assertEquals(listOf("move", "eat", "restore"), ms.map { it.id })
        assertTrue("التراويح" in ms[0].title || "الفطور" in ms[0].title, ms[0].title)
        assertTrue("سحور" in ms[1].title)
        val low = dayMissions(Energy.LOW, TimeBudget.TEN, t, p)
        assertEquals("reset-2", low[0].routineId)
        // خارج رمضان تبقى المهمات العادية
        assertTrue(dayMissions(Energy.HIGH, TimeBudget.TWENTY, t, p.copy(ramadan = false)).none { "سحور" in it.title })
    }

    @Test fun offlineCoachAnswersSuhoorQuestion() {
        val state = AppState(profile = p)
        val r = offlineReply("شنو آكل بالسحور؟", state, t, "2026-09-21")
        assertTrue(r.text.startsWith("خطتك الرمضانية"), r.text)
        val off = offlineReply("شنو آكل بالسحور؟", state.copy(profile = p.copy(ramadan = false)), t, "2026-09-21")
        assertTrue("فعّل وضع رمضان" in off.text, off.text)
        assertTrue((r.text).none { it in '0'..'9' }, r.text)
    }
}
