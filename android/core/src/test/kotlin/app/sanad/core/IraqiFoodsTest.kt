package app.sanad.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IraqiFoodsTest {
    private fun ids(text: String) = parseMealText(text).map { it.food.id }

    @Test fun parsesIraqiDishesWithLocalSpelling() {
        assertEquals(listOf("dolma", "lablabi"), ids("تغديت دولمة ولبلبي"))
        assertTrue("masgouf" in ids("أكلت مسكوف"))
        assertTrue("masgouf" in ids("أكلت مسگوف"))
        assertTrue("baqilla-dihin" in ids("فطرت باكلة بالدهن"))
        assertTrue("kahi-geymar" in ids("كاهي وقيمر"))
    }

    @Test fun localPortionsCount() {
        val r = parseMealText("صمونتين وكباب").first { it.food.id == "samoon" }
        assertEquals(2.0, r.qty)
    }

    @Test fun shortNamesNeedWholeWord() {
        assertTrue(ids("اتمنى انزل كيلو").isEmpty())
        assertEquals(listOf("timman"), ids("تمن"))
    }

    @Test fun searchFindsIraqiWithGaf() {
        assertEquals("kahi-geymar", searchFoods("گيمر").first().id)
        assertTrue(FOODS.map { it.id }.toSet().size == FOODS.size, "duplicate food ids")
    }

    @Test fun usualMealsAreRepeatsOnly() {
        fun m(id: String, name: String, kcal: Int, at: Long) = MealEntry(id, name, kcal, 20, at, MealSource.QUICK)
        val s = AppState(days = mapOf(
            "2026-09-20" to DayLog("2026-09-20", meals = listOf(m("1", "صمون", 270, 1), m("2", "دولمة", 500, 2))),
            "2026-09-21" to DayLog("2026-09-21", meals = listOf(m("3", "صمون", 270, 3), m("4", "صمون", 280, 4))),
        ))
        val u = usualMeals(s)
        assertEquals(1, u.size)
        assertEquals("صمون", u[0].name)
        assertEquals(280, u[0].kcal)
        assertEquals(3, u[0].times)
    }
}
