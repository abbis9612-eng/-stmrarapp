package app.sanad.core

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PartnerTest {
    private val p = Profile(
        name = "علي", sex = Sex.M, age = 35, heightCm = 175.0, startWeightKg = 100.0, goalWeightKg = 85.0,
        activity = Activity.SEDENTARY, pace = Pace.STEADY, createdAt = "2026-09-01",
    )
    private val t = computeTargets(p, 100.0)
    private val days = (0..13).associate { i ->
        val d = addDays("2026-09-26", -i.toLong())
        d to DayLog(d, meals = listOf(MealEntry("m$i", "x", 1800, 130, 1, MealSource.QUICK)), weightKg = 100.0 - (13 - i) * 0.1, done = listOf("eat", "move"))
    }
    private val r = weeklyReview(p, days, t, "2026-09-26")

    @Test fun weightHiddenByDefault() {
        val s = partnerReport(p, r, 14, showWeight = false)
        assertTrue("علي" in s)
        assertTrue("٧" in s)
        assertFalse("كغ" in s)
    }

    @Test fun weightShownWhenAllowed() {
        assertTrue("كغ" in partnerReport(p, r, 14, showWeight = true))
    }
}

class PartnerReminderTest {
    @Test fun fridayReminderWhenPartnerSet() {
        val p = Profile(
            name = "علي", sex = Sex.M, age = 35, heightCm = 175.0, startWeightKg = 100.0, goalWeightKg = 85.0,
            activity = Activity.SEDENTARY, pace = Pace.STEADY, createdAt = "2026-09-01", partner = "حسن",
        )
        val t = computeTargets(p, 100.0)
        // الجمعة ٢٠٢٦-١٠-٠٢، يوم مرتب (طاقة ووجبة) حتى ما تسبقه تنبيهات ثانية
        val s = AppState(profile = p, days = mapOf("2026-10-02" to DayLog("2026-10-02", energy = Energy.MID, meals = listOf(MealEntry("m", "x", 500, 40, 5, MealSource.QUICK)), done = listOf("eat"))))
        val r = nextReminder(s, t, java.time.LocalDateTime.of(2026, 10, 2, 15, 0))
        kotlin.test.assertEquals("partner", r?.id)
    }
}
