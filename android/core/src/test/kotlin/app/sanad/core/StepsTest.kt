package app.sanad.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StepsTest {
    @Test fun firstReadingStartsAtZero() {
        assertEquals(0, advanceSteps(null, 12_000, "2026-09-26").today)
    }

    @Test fun accumulatesWithinDay() {
        var c = advanceSteps(null, 1000, "2026-09-26")
        c = advanceSteps(c, 1500, "2026-09-26")
        c = advanceSteps(c, 4200, "2026-09-26")
        assertEquals(3200, c.today)
    }

    @Test fun newDayResets() {
        val c = advanceSteps(StepCursor("2026-09-25", 9000, 8000), 9300, "2026-09-26")
        assertEquals(300, c.today)
        assertEquals("2026-09-26", c.date)
    }

    @Test fun rebootDoesNotGoNegative() {
        val c = advanceSteps(StepCursor("2026-09-26", 50_000, 4000), 120, "2026-09-26")
        assertEquals(4120, c.today)
    }

    @Test fun notes() {
        assertTrue("وصلت" in stepsNote(7000, 7000))
        assertTrue("باقي" in stepsNote(5000, 7000))
    }
}
