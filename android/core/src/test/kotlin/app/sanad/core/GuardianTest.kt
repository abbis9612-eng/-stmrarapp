package app.sanad.core

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GuardianTest {
    private val p = Profile(
        name = "علي", sex = Sex.M, age = 35, heightCm = 175.0, startWeightKg = 100.0, goalWeightKg = 85.0,
        activity = Activity.SEDENTARY, pace = Pace.STEADY, createdAt = "2026-09-01",
        barriers = listOf(Barrier.NIGHT),
        ifThens = listOf(IfThen("a", "إذا جاني جوع بعد الساعة ٩", "أشرب شاي أو ماء"), IfThen("b", "إذا عندي عزيمة", "صحن واحد")),
    )
    private val t = computeTargets(p, 100.0)
    private fun day(date: String, kcal: Int = 0, meal: String = "x", weight: Double? = null, energy: Energy? = null, sleep: Double? = null, done: List<String> = emptyList()) =
        DayLog(date, energy = energy, meals = if (kcal > 0) listOf(MealEntry("m$date", meal, kcal, 30, 1, MealSource.QUICK)) else emptyList(), weightKg = weight, sleepHours = sleep, done = done)

    @Test fun calmMorningIsLowRisk() {
        val s = AppState(profile = p, days = mapOf("2026-09-21" to day("2026-09-21", 500), "2026-09-22" to day("2026-09-22", 400)))
        val r = lapseRisk(s, t, LocalDateTime.of(2026, 9, 22, 10, 0)) // الثلاثاء
        assertEquals(RiskLevel.LOW, r.level)
    }

    @Test fun tiredNightAfterMissedDayIsHighAndPicksOwnNightPlan() {
        val s = AppState(profile = p, days = mapOf("2026-09-22" to day("2026-09-22", 900, energy = Energy.LOW, sleep = 5.5)))
        val r = lapseRisk(s, t, LocalDateTime.of(2026, 9, 24, 22, 0)) // خميس ليل
        assertEquals(RiskLevel.HIGH, r.level)
        assertTrue(r.signals.any { it.id == "night" } && r.signals.any { it.id == "weekend" })
        assertEquals("a", r.plan?.id)
        assertEquals("night-5", r.toolRoutineId)
        assertTrue(r.headline.startsWith("لحظة حساسة"))
    }

    @Test fun socialMentionRaisesRiskAndPicksGatheringPlan() {
        val s = AppState(
            profile = p,
            days = mapOf("2026-09-22" to day("2026-09-22", 500, energy = Energy.LOW)),
            chat = listOf(ChatMessage("c", ChatRole.USER, "عندي عزيمة الليلة", 5)),
        )
        val r = lapseRisk(s, t, LocalDateTime.of(2026, 9, 22, 18, 0))
        assertTrue(r.signals.any { it.id == "social" })
        assertEquals("b", r.plan?.id)
    }

    @Test fun weighInJumpAfterRiceIsExplainedAsWater() {
        val s = AppState(
            profile = p,
            days = mapOf(
                "2026-09-15" to day("2026-09-15", weight = 99.0),
                "2026-09-21" to day("2026-09-21", 2600, meal = "كبسة لحم", weight = 98.2),
                "2026-09-22" to day("2026-09-22", weight = 99.3),
            ),
        )
        val w = assertNotNull(weighInWeather(s, "2026-09-22", t))
        assertEquals(WeighIn.Kind.JUMP, w.kind)
        assertEquals(1.1, w.rawDelta)
        assertTrue(w.causes.any { "رز" in it }, w.causes.toString())
        assertTrue(w.headline.contains("ماي"))
        assertTrue((w.headline + w.body).none { it in '0'..'9' }, w.body)
    }

    @Test fun firstWeighInAndNoWeightCases() {
        val s = AppState(profile = p, days = mapOf("2026-09-22" to day("2026-09-22", weight = 100.0)))
        assertEquals(WeighIn.Kind.FIRST, weighInWeather(s, "2026-09-22", t)?.kind)
        assertNull(weighInWeather(s, "2026-09-23", t))
    }

    @Test fun welcomeBackOnlyAfterTwoMissedDays() {
        val s = AppState(profile = p, days = mapOf("2026-09-18" to day("2026-09-18", 500)))
        assertNull(welcomeBack(s, "2026-09-20")) // يوم واحد فائت
        val w = assertNotNull(welcomeBack(s, "2026-09-22"))
        assertEquals(3, w.daysAway)
        assertTrue("علي" in w.headline)
        // إذا سجّل اليوم، ما نرحّب
        assertNull(welcomeBack(s.copy(days = s.days + ("2026-09-22" to day("2026-09-22", 300))), "2026-09-22"))
    }

    @Test fun lapseRecoveryNeverPrescribesCompensation() {
        LapseKind.entries.forEach { k ->
            val r = lapseRecovery(k, t)
            assertTrue(r.steps.isNotEmpty())
            assertTrue(r.steps.none { "صيام" in it || "لا تاكل" in it }, "$k: ${r.steps}")
        }
        assertTrue(lapseRecovery(LapseKind.OVEREAT, t).steps.first().contains("لا تعوّض"))
    }

    @Test fun loggedLapseStillCountsForTheStreak() {
        assertTrue(isCounted(DayLog("2026-09-22", lapses = listOf("NIGHT"))))
    }

    @Test fun remindersRespectQuietHoursAndContext() {
        val s = AppState(profile = p, days = emptyMap())
        val morning = assertNotNull(nextReminder(s, t, LocalDateTime.of(2026, 9, 22, 7, 0)))
        assertEquals("morning", morning.id)
        assertEquals(9, morning.at.hour)
        // بعد ١١ الليل: التنبيه الجاي الصبح مو الحين
        val late = assertNotNull(nextReminder(s, t, LocalDateTime.of(2026, 9, 22, 23, 30)))
        assertTrue(late.at.hour in 8..22 && late.at.toLocalDate().toString() == "2026-09-23")
        // سجّل طاقة وأكل: ما نسأله الصبح ولا الغدا
        val busy = s.copy(days = mapOf("2026-09-22" to day("2026-09-22", 600, energy = Energy.MID)))
        val r = nextReminder(busy, t, LocalDateTime.of(2026, 9, 22, 10, 0))
        assertTrue(r == null || r.id !in setOf("morning", "lunch") || r.at.toLocalDate().toString() != "2026-09-22", r.toString())
    }

    @Test fun offlineCoachHandlesLapseWithoutShame() {
        val r = offlineReply("خربت بالليل أكلت هواية", AppState(profile = p), t, "2026-09-22")
        assertTrue(r.text.startsWith("أكل الليل له سبب"), r.text)
        assertTrue(r.actions.isEmpty())
    }
}
