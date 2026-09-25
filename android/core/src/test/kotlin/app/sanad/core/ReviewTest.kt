package app.sanad.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReviewTest {
    private val p = Profile(
        name = "سارة", sex = Sex.F, age = 34, heightCm = 162.0, startWeightKg = 88.0, goalWeightKg = 70.0,
        activity = Activity.SEDENTARY, pace = Pace.STEADY, createdAt = "2026-09-01",
    )
    private val t = computeTargets(p, 88.0)
    private val today = "2026-09-21"

    private fun d(i: Long, kcal: Int = 0, protein: Int = 0, weight: Double? = null, workout: Int = 0) = addDays(today, -i).let { date ->
        DayLog(
            date = date,
            meals = if (kcal > 0) listOf(MealEntry("m$date", "x", kcal, protein, 0, MealSource.QUICK)) else emptyList(),
            workouts = if (workout > 0) listOf(WorkoutEntry("w$date", "strength-10", "قوة", workout, 0)) else emptyList(),
            weightKg = weight,
        )
    }

    private fun days(vararg xs: DayLog) = xs.associateBy { it.date }

    @Test fun emptyWeekAsksToShowUp() {
        val r = weeklyReview(p, emptyMap(), t, today)
        assertEquals(0, r.activeDays)
        assertEquals(Pacing.NO_DATA, r.pacing)
        assertEquals(Focus.SHOW_UP, r.focus)
        assertNull(r.avgKcal)
    }

    @Test fun windowIsSevenDaysEndingToday() {
        val r = weeklyReview(p, days(d(0, 1500, 100), d(6, 1500, 100), d(7, 1500, 100)), t, today)
        assertEquals("2026-09-15", r.from)
        assertEquals(2, r.activeDays)
        assertEquals(2, r.foodDays)
    }

    @Test fun onTrackWeekCelebratesTrend() {
        // نزول ٠٫٥ كغ على ١٤ يوم من القراءات ≈ على الخطة (~٠٫٥٣ كغ/أسبوع)
        val all = (0L..13L).map { i -> d(i, 1500, t.protein, weight = 88.0 - (13 - i) * 0.076, workout = if (i % 3 == 0L) 10 else 0) }
        val r = weeklyReview(p, days(*all.toTypedArray()), t, today)
        val change = assertNotNull(r.trendChangeKg)
        assertTrue(change < 0, "change=$change")
        assertEquals(7, r.activeDays)
        assertTrue(r.pacing == Pacing.ON_TRACK || r.pacing == Pacing.SLOW, "pacing=${r.pacing}")
        assertEquals(Focus.KEEP, r.focus)
    }

    @Test fun tooFastLossPrioritisesEatingMore() {
        val all = (0L..10L).map { i -> d(i, 1100, t.protein, weight = 88.0 - (10 - i) * 0.3, workout = 10) }
        val r = weeklyReview(p, days(*all.toTypedArray()), t, today)
        assertEquals(Pacing.TOO_FAST, r.pacing)
        assertEquals(Focus.EAT_MORE, r.focus)
    }

    @Test fun lowProteinThenStrengthAreTheSingleFocus() {
        val lowProtein = (0L..6L).map { d(it, 1500, 40) }
        assertEquals(Focus.PROTEIN, weeklyReview(p, days(*lowProtein.toTypedArray()), t, today).focus)
        val noWorkouts = (0L..6L).map { d(it, 1500, t.protein) }
        val r = weeklyReview(p, days(*noWorkouts.toTypedArray()), t, today)
        assertEquals(Focus.STRENGTH, r.focus)
        assertEquals(7, r.proteinDays)
        assertEquals(1500, r.avgKcal)
    }

    @Test fun sparseLoggingAsksForFood() {
        val xs = (0L..4L).map { if (it < 2) d(it, 1500, 100) else d(it, weight = 88.0) }
        assertEquals(Focus.LOG_FOOD, weeklyReview(p, days(*xs.toTypedArray()), t, today).focus)
    }

    @Test fun reviewTextUsesArabicDigits() {
        val xs = (0L..6L).map { d(it, 1500, t.protein, workout = 10) }
        val r = weeklyReview(p, days(*xs.toTypedArray()), t, today)
        assertTrue((r.win + r.focusText).none { it in '0'..'9' }, r.win + r.focusText)
    }

    @Test fun offlineCoachAnswersWeeklyQuestion() {
        val xs = (0L..6L).map { d(it, 1500, 40) }
        val state = AppState(profile = p, days = days(*xs.toTypedArray()))
        val reply = offlineReply("كيف كان أسبوعي؟", state, t, today)
        assertTrue(reply.text.startsWith("مراجعة آخر ٧ أيام"), reply.text)
        assertTrue("بروتين" in reply.text, reply.text)
    }
}
