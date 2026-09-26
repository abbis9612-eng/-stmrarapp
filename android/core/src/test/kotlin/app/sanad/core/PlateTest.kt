package app.sanad.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlateTest {
    @Test fun idealPlateIsFullScore() {
        val s = plateScore(PlateInput(veg = 2, protein = 1, carbs = 1))
        assertEquals(100, s.score)
        assertEquals("صحن سند", s.grade)
    }

    @Test fun riceHeavyNoVegGetsFocusedTips() {
        val s = plateScore(PlateInput(veg = 0, protein = 1, carbs = 3))
        assertTrue(s.score < 40)
        assertTrue(s.tips.any { "الخضار" in it })
        assertTrue(s.tips.any { "قبضة" in it })
    }

    @Test fun friedAndDrinkPenalties() {
        val base = plateScore(PlateInput(2, 1, 1)).score
        val worse = plateScore(PlateInput(2, 1, 1, fried = true, sweetDrink = true)).score
        assertEquals(base - 30, worse)
    }

    @Test fun neverOutOfRange() {
        assertEquals(0, plateScore(PlateInput(0, 0, 3, fried = true, sweetDrink = true)).score)
        assertTrue(plateScore(PlateInput(9, 9, -2)).score in 0..100)
    }
}
