package app.sanad.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SkyTest {
    private fun meal(kcal: Int) = MealEntry("m$kcal", "أكل", kcal, 10, 1, MealSource.QUICK)
    private fun logged(date: String, kcal: Int = 1400, gathering: Boolean = false) =
        date to DayLog(date, meals = listOf(meal(kcal)), gathering = gathering)

    private fun daysFrom(start: String, n: Int, skip: Set<Int> = emptySet()) =
        (0 until n).filter { it !in skip }.associate { logged(addDays(start, it.toLong())) }

    @Test fun firstNightShowsThinCrescent() {
        val s = skyOf(daysFrom("2026-09-26", 1), "2026-09-26", "2026-09-26")
        assertEquals(1, s.nights)
        assertEquals(0, s.fullMoons)
        assertTrue(s.phase in 0.05f..0.1f)
    }

    @Test fun thirtyLoggedDaysMakeAFullMoon() {
        val s = skyOf(daysFrom("2026-08-28", 30), "2026-09-26", "2026-08-28")
        assertEquals(1, s.fullMoons)
        assertEquals(0, s.nights)
        assertEquals(1f, s.phase)
    }

    @Test fun missedDaysDoNotGrowTheMoon() {
        val s = skyOf(daysFrom("2026-09-17", 10, skip = setOf(3, 4)), "2026-09-26", "2026-09-17")
        assertEquals(8, s.nights)
        assertEquals(10, s.daysWithSanad)
    }

    @Test fun starsNeedAFullWeekWithoutTwoEmptyDaysInARow() {
        // الأسبوع الأول فيه يوم فائت واحد (نجمة)، الثاني فيه يومين متتاليين (بلا نجمة)
        val d = daysFrom("2026-09-12", 14, skip = setOf(2, 9, 10))
        val s = skyOf(d, "2026-09-25", "2026-09-12")
        assertEquals(1, s.stars)
    }

    @Test fun weekMarksShowPlanOverOccasionAndEmpty() {
        val days = mapOf(
            logged("2026-09-20", 1400), logged("2026-09-21", 1700),
            logged("2026-09-22", 2200, gathering = true), logged("2026-09-26", 300),
        )
        val m = dayMarks(days, "2026-09-26", 1500, "2026-09-01")
        assertEquals(DayMark.ON_PLAN, m[0])
        assertEquals(DayMark.OVER, m[1])
        assertEquals(DayMark.OCCASION, m[2])
        assertEquals(DayMark.EMPTY, m[3])
        assertEquals(DayMark.TODAY, m[6])
        assertEquals(DayMark.BEFORE, dayMarks(days, "2026-09-26", 1500, "2026-09-24")[0])
    }

    @Test fun smallExcessStillCountsAsOnPlan() {
        val m = dayMarks(mapOf(logged("2026-09-25", 1590)), "2026-09-26", 1500, null)
        assertEquals(DayMark.ON_PLAN, m[5])
    }

    @Test fun occasionBankCapsPerDayAndTotal() {
        // كل يوم 500 تحت الخطة، لكن اليوم يضيف 150 بس، والمجموع ما يتجاوز 500
        val days = daysFrom("2026-09-20", 6).mapValues { (d, _) -> DayLog(d, meals = listOf(meal(1000))) }
        assertEquals(BANK_CAP, occasionBank(days, "2026-09-26", 1500).saved)
        val two = mapOf(logged("2026-09-24", 1400), logged("2026-09-25", 1200))
        assertEquals(100 + 150, occasionBank(two, "2026-09-26", 1500).saved)
    }

    @Test fun occasionBankResetsAfterAnOccasionAndIgnoresUnloggedDays() {
        val days = mapOf(
            logged("2026-09-21", 1000), logged("2026-09-23", 2600, gathering = true),
            logged("2026-09-25", 1450), "2026-09-24" to DayLog("2026-09-24"),
        )
        assertEquals(50, occasionBank(days, "2026-09-26", 1500).saved)
    }

    @Test fun overEatingNeverMakesTheBankNegative() {
        assertEquals(0, occasionBank(mapOf(logged("2026-09-25", 2400)), "2026-09-26", 1500).saved)
    }

    @Test fun weightChangesUseTrendAndHideShortWindows() {
        val t = (0..20).map { TrendPoint(addDays("2026-09-01", it.toLong()), 90.0 - it * 0.1, 90.0 - it * 0.1) }
        val ch = weightChanges(t)
        assertEquals(-0.7, ch[0].kg)
        assertEquals(-1.4, ch[1].kg)
        // 20 يوم بيانات فقط: الـ30 يوم تساوي من البداية
        assertEquals(-2.0, ch[2].kg)
        assertEquals(-2.0, ch[3].kg)
        val short = listOf(TrendPoint("2026-09-25", 80.0, 80.0), TrendPoint("2026-09-26", 79.8, 79.9))
        assertEquals(null, weightChanges(short)[2].kg)
        assertEquals(null, weightChanges(listOf(short[0]))[3].kg)
    }

    @Test fun dayCountsUseCorrectArabicPlural() {
        assertEquals("يوم واحد", arDays(1))
        assertEquals("يومان", arDays(2))
        assertEquals("8 أيام", arDays(8))
        assertEquals("21 يوماً", arDays(21))
        assertEquals("0 يوماً", arDays(0))
    }

    @Test fun forecastGivesARangeNotADate() {
        val f = forecast(91.0, 80.0, Pace.STEADY, "2026-09-27")!!
        assertEquals(0.55, f.fastKgWeek)
        assertEquals(0.35, f.slowKgWeek)
        assertEquals(21, f.weeksFast)
        assertEquals(31, f.weeksSlow)
        assertTrue(f.earliest < f.latest)
        assertEquals("2027-02-21", f.earliest)
        assertEquals(null, forecast(80.0, 85.0, Pace.STEADY, "2026-09-27"))
    }
}
