package app.sanad.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PacerTest {
    private val cues = pacerCues()

    @Test fun startsWithBiteEndsAtTwentyMinutes() {
        assertEquals(CueKind.BITE, cues.first().kind)
        assertEquals(0, cues.first().atSec)
        assertEquals(CueKind.END, cues.last().kind)
        assertEquals(PACER_TOTAL_SEC, cues.last().atSec)
    }

    @Test fun oneFullnessCheckAtHalf() {
        val checks = cues.filter { it.kind == CueKind.CHECK }
        assertEquals(1, checks.size)
        assertEquals(PACER_TOTAL_SEC / 2, checks[0].atSec)
        assertEquals(CueKind.CHECK, cueAt(cues, 610).kind)
    }

    @Test fun cueAtPicksLatestReached() {
        assertEquals(CueKind.BITE, cueAt(cues, 10).kind)
        assertEquals(CueKind.CHEW, cueAt(cues, 45).kind)
        assertTrue(cues.zipWithNext().all { (a, b) -> a.atSec <= b.atSec })
    }

    @Test fun fullAdviceSuggestsStoppingWithoutGuilt() {
        assertTrue("توقف" in fullnessAdvice(4, 600))
        assertTrue("عادي" in fullnessAdvice(1, 600))
    }
}
