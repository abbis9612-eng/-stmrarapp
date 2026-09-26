package app.sanad.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SleepTest {
    private fun d(date: String, h: Double?) = date to DayLog(date, sleepHours = h)

    @Test fun noSleepLoggedNoBank() {
        assertNull(sleepBank(mapOf(d("2026-09-26", null)), "2026-09-26"))
    }

    @Test fun debtCountsOnlyShortNightsInLastWeek() {
        val days = mapOf(
            d("2026-09-18", 3.0), // خارج الأسبوع
            d("2026-09-20", 6.0),
            d("2026-09-22", 8.0),
            d("2026-09-25", 5.0),
            d("2026-09-26", 7.0),
        )
        val b = sleepBank(days, "2026-09-26")!!
        assertEquals(7, b.nights.size)
        assertEquals(4, b.logged)
        assertEquals(3.0, b.debt, 1e-9)
        assertEquals(2, b.shortNights)
        assertEquals(6.5, b.avg, 1e-9)
        assertTrue("٢٧٠" in b.tip)
        assertEquals(7.0, b.nights.last())
    }

    @Test fun fullBank() {
        val b = sleepBank(mapOf(d("2026-09-26", 8.0)), "2026-09-26")!!
        assertEquals("رصيد نومك ممتلي", b.headline)
    }
}
