package app.sanad.coach.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import app.sanad.coach.data.AppStore
import app.sanad.coach.data.targets
import app.sanad.coach.data.today
import app.sanad.coach.ui.Routes
import app.sanad.coach.ui.components.Badge
import app.sanad.coach.ui.components.Ico
import app.sanad.coach.ui.components.SButton
import app.sanad.coach.ui.components.SCard
import app.sanad.coach.ui.components.SChip
import app.sanad.coach.ui.components.SIcon
import app.sanad.coach.ui.components.glass
import app.sanad.coach.ui.components.SectionTitle
import app.sanad.coach.ui.components.press
import app.sanad.coach.ui.theme.Sanad
import app.sanad.coach.ui.theme.Type
import app.sanad.core.AppState
import app.sanad.core.FOODS
import app.sanad.core.usualMeals
import app.sanad.core.Food
import app.sanad.core.FoodCat
import app.sanad.core.MealSource
import app.sanad.core.ar
import app.sanad.core.parseNum
import app.sanad.core.searchFoods
import kotlin.math.roundToInt

@Composable
fun SField(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier, number: Boolean = false) {
    val c = Sanad.colors
    Box(
        modifier.heightIn(min = 54.dp).clip(RoundedCornerShape(16.dp)).background(c.surface).border(1.5.dp, c.line, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) Text(hint, style = Type.body.copy(color = c.inkSoft))
        BasicTextField(
            value, onChange, singleLine = true, textStyle = Type.body.copy(color = c.ink), cursorBrush = SolidColor(c.sadu),
            keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = hint },
        )
    }
}

@Composable
fun EatScreen(store: AppStore, state: AppState, nav: NavHostController) {
    val c = Sanad.colors
    val t = state.targets() ?: return
    val day = state.today()
    var q by rememberSaveable { mutableStateOf("") }
    var cat by rememberSaveable { mutableStateOf("IRAQI") }
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var kcal by rememberSaveable { mutableStateOf("") }
    var prot by rememberSaveable { mutableStateOf("") }

    val results = when {
        q.isNotBlank() -> searchFoods(q, 20)
        cat == "star" -> FOODS.filter { it.proteinStar }
        cat == "fav" -> FOODS.filter { it.id in state.favorites }
        else -> FOODS.filter { it.cat.name == cat }
    }

    Page {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(c.glass2).press({ nav.popBackStack() }),
                    contentAlignment = Alignment.Center,
                ) { SIcon(Ico.BACK, size = 20.dp, description = "رجوع") }
                Spacer(Modifier.width(12.dp))
                Text("الأكل", style = Type.h1.copy(color = c.ink), modifier = Modifier.weight(1f))
                Badge("${ar(day.intake)} / ${ar(t.kcal)} سعرة")
            }
        }
        item {
            SCard(color = c.saffron, pad = 14.dp) {
                Row(Modifier.press({ nav.navigate(Routes.COACH) }), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(c.date), contentAlignment = Alignment.Center) {
                        SIcon(Ico.COACH, tint = Color(0xFF1B1406))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("أسرع طريقة", style = Type.label.copy(color = c.date))
                        Text("اكتب لسند وش أكلت بجملة، ويحسبها لك.", style = Type.body.copy(color = c.ink))
                    }
                    SIcon(Ico.NEXT, tint = c.ink)
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp), c.oasis).press({ nav.navigate(Routes.PACER) }).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SIcon(Ico.PLAY, size = 18.dp, tint = c.oasis)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("كل على مهلك", style = Type.h3.copy(color = c.ink))
                    Text("مؤقت ٢٠ دقيقة يخلّي الشبع يوصل قبل ما تتروس", style = Type.small.copy(color = c.inkSoft))
                }
                SIcon(Ico.NEXT, tint = c.inkSoft)
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp), c.saffron).press({ nav.navigate(Routes.PLATE) }).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SIcon(Ico.EAT, size = 18.dp, tint = c.saffron)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("صحن سند", style = Type.h3.copy(color = c.ink))
                    Text("قيّم صحنك بالأرباع بدون وزن ولا حساب", style = Type.small.copy(color = c.inkSoft))
                }
                SIcon(Ico.NEXT, tint = c.inkSoft)
            }
        }
        val usual = usualMeals(state)
        if (usual.isNotEmpty() && q.isBlank()) item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("وجباتي المعتادة — ضغطة وحدة", style = Type.label.copy(color = c.inkSoft))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    usual.forEach { u ->
                        SChip("+ ${u.name} · ${ar(u.kcal)}", false, { store.addMeal(u.name, u.kcal, u.protein, MealSource.QUICK) })
                    }
                }
            }
        }
        item { SField(q, { q = it }, "ابحث: دولمة، تشريب، صمون، كباب…", Modifier.fillMaxWidth()) }
        if (q.isBlank()) item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SChip("غني بالبروتين", cat == "star", { cat = "star" })
                SChip("المفضلة", cat == "fav", { cat = "fav" })
                FoodCat.entries.forEach { fc -> SChip(fc.label, cat == fc.name, { cat = fc.name }) }
            }
        }
        if (results.isEmpty()) item {
            Text(
                if (q.isNotBlank()) "ما لقينا \"$q\". سجّلها يدوي تحت، أو اسأل سند يقدّرها." else "اضغط ☆ على أي أكلة تتكرر عندك عشان تلقاها هنا بضغطة.",
                style = Type.small.copy(color = c.inkSoft),
            )
        }
        results.forEach { f ->
            item(key = "f-${f.id}") {
                FoodRow(f, open == f.id, f.id in state.favorites, onToggle = { open = if (open == f.id) null else f.id }, onFav = { store.toggleFavorite(f.id) }) { qty ->
                    store.addMeal(if (qty == 1.0) f.name else "${f.name} ×${ar(qty)}", (f.kcal * qty).roundToInt(), (f.protein * qty).roundToInt(), MealSource.DB)
                    open = null
                }
            }
        }
        item {
            SCard {
                Text("إضافة يدوية", style = Type.h3.copy(color = c.ink))
                Spacer(Modifier.height(10.dp))
                SField(name, { name = it }, "اسم الأكلة", Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SField(kcal, { kcal = it }, "سعرات", Modifier.weight(1f), number = true)
                    SField(prot, { prot = it }, "بروتين غ", Modifier.weight(1f), number = true)
                }
                Spacer(Modifier.height(10.dp))
                val k = parseNum(kcal)
                SButton("سجّل", {
                    store.addMeal(name.trim(), k!!.roundToInt(), (parseNum(prot) ?: 0.0).roundToInt(), MealSource.QUICK)
                    name = ""; kcal = ""; prot = ""
                }, Modifier.fillMaxWidth(), enabled = name.isNotBlank() && k != null && k > 0)
            }
        }
        item { SectionTitle("سجل اليوم") }
        if (day.meals.isEmpty()) item {
            Text("ما سجلت شي اليوم. أول وجبة تسجلها تنجز مهمة الأكل وتحسب يوم بسلسلتك.", style = Type.small.copy(color = c.inkSoft))
        }
        day.meals.forEach { m ->
            item(key = "m-${m.id}") {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(m.name, style = Type.bodyStrong.copy(color = c.ink))
                        Text("${ar(m.kcal)} سعرة، ${ar(m.protein)} غ بروتين" + if (m.source == MealSource.COACH) "، قدّرها سند" else "", style = Type.label.copy(color = c.inkSoft))
                    }
                    Box(Modifier.size(48.dp).press({ store.removeMeal(m.id) }).semantics { contentDescription = "احذف ${m.name}" }, contentAlignment = Alignment.Center) {
                        SIcon(Ico.TRASH, size = 20.dp, tint = c.inkSoft)
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodRow(f: Food, open: Boolean, fav: Boolean, onToggle: () -> Unit, onFav: () -> Unit, onAdd: (Double) -> Unit) {
    val c = Sanad.colors
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.surface)) {
        Row(Modifier.press(onToggle).padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(f.name, style = Type.bodyStrong.copy(color = c.ink))
                Text("${f.portion}، ${ar(f.kcal)} سعرة، ${ar(f.protein)} غ بروتين", style = Type.label.copy(color = c.inkSoft))
            }
            Box(Modifier.size(48.dp).press(onFav).semantics { contentDescription = if (fav) "إزالة من المفضلة" else "أضف للمفضلة" }, contentAlignment = Alignment.Center) {
                SIcon(Ico.STAR, tint = if (fav) c.date else c.inkSoft)
            }
        }
        AnimatedVisibility(open, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
                Text("كم أكلت من الحصة؟", style = Type.label.copy(color = c.inkSoft))
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.5 to "نص", 1.0 to "حصة", 1.5 to "حصة ونص", 2.0 to "حصتين").forEach { (qty, l) ->
                        Box(Modifier.clip(CircleShape).background(c.surface2).press({ onAdd(qty) }).padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text("$l  ${ar((f.kcal * qty).roundToInt())}", style = Type.label.copy(color = c.ink))
                        }
                    }
                }
            }
        }
    }
}
