package app.sanad.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LessonsTest {
    @Test fun twelveWeeksOfSeven() {
        assertEquals(84, LESSONS.size)
        assertEquals(12, WEEK_THEMES.size)
        assertEquals(LESSONS.size, LESSONS.map { it.id }.toSet().size)
        assertTrue(LESSONS.all { it.title.isNotBlank() && it.body.isNotBlank() && it.action.isNotBlank() })
        assertEquals((1..12).toList(), LESSONS.map { it.week }.distinct())
    }

    @Test fun firstDayGetsFirstLesson() {
        assertEquals("w1d1", lessonForToday(emptyList(), "2026-09-26", "2026-09-26", false)?.id)
    }

    @Test fun oneNewLessonPerDayAtMost() {
        // اليوم الأول وقرا الدرس الأول: ما يفتح الثاني إلا باچر
        assertNull(lessonForToday(listOf("w1d1"), "2026-09-26", "2026-09-26", false))
        assertEquals("w1d2", lessonForToday(listOf("w1d1"), "2026-09-26", "2026-09-27", false)?.id)
    }

    @Test fun absenceResumesWhereLeft() {
        // غاب ١٠ أيام بعد درسين: يكمل الثالث، مو العاشر
        assertEquals("w1d3", lessonForToday(listOf("w1d1", "w1d2"), "2026-09-01", "2026-09-12", false)?.id)
    }

    @Test fun readTodayHidesCard() {
        assertNull(lessonForToday(emptyList(), "2026-09-01", "2026-09-12", true))
    }

    @Test fun finishedJourney() {
        assertNull(lessonForToday(LESSONS.map { it.id }, "2026-01-01", "2026-09-26", false))
    }
}
