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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Canvas
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
            value, onChange, singleLine = true, textStyle = Type.body.copy(color = c.ink), cursorBrush = SolidColor(c.primary),
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
    var cat by rememberSaveable { mutableStateOf("MAIN") }
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
                Text("الأكل", style = Type.h1.copy(color = c.ink), modifier = Modifier.weight(1f))
                val left = t.kcal - day.intake
                Badge(if (left >= 0) "متبقٍ ${ar(left)} سعرة" else "فوق الخطة ${ar(-left)}")
            }
        }
        // أسرع طريقة: صورة الصحن، وتحرّك يحسبها بالصحن والرغيف
        item {
            Row(
                Modifier.fillMaxWidth().glass().padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("صوّر صحنك", style = Type.h1.copy(color = c.ink, fontSize = 25.sp))
                    Text("يعرف أكلاتنا العربية ويحسبها بالصحن والرغيف، لا بالغرامات.", style = Type.body.copy(color = c.inkSoft))
                    SButton("افتح الكاميرا", { nav.navigate(Routes.coach(camera = true)) }, small = true, icon = Ico.CAMERA)
                }
                Spacer(Modifier.width(10.dp))
                PlateViewfinder(Modifier.size(104.dp))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickTile(Ico.PENCIL, "اكتب جملة", Modifier.weight(1f)) { nav.navigate(Routes.COACH) }
                QuickTile(Ico.EAT, "صحن تحرّك", Modifier.weight(1f)) { nav.navigate(Routes.PLATE) }
                QuickTile(Ico.CLOCK, "كُل على مهل", Modifier.weight(1f)) { nav.navigate(Routes.PACER) }
            }
        }
        val usual = usualMeals(state)
        if (usual.isNotEmpty() && q.isBlank()) item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("وجباتك المعتادة — بضغطة واحدة", style = Type.h3.copy(color = c.ink))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    usual.forEach { u ->
                        SChip("+ ${u.name} · ${ar(u.kcal)}", false, { store.addMeal(u.name, u.kcal, u.protein, MealSource.QUICK) })
                    }
                }
            }
        }
        item { SField(q, { q = it }, "ابحث: كبسة، فول، شاورما، دولمة…", Modifier.fillMaxWidth()) }
        if (q.isBlank()) item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FoodCat.entries.forEach { fc -> SChip(fc.label, cat == fc.name, { cat = fc.name }) }
                SChip("غني بالبروتين", cat == "star", { cat = "star" })
                SChip("المفضلة", cat == "fav", { cat = "fav" })
            }
        }
        if (results.isEmpty()) item {
            Text(
                if (q.isNotBlank()) "لم نجد \"$q\". سجّلها يدوياً بالأسفل، أو اسأل المدرب ليقدّرها." else "اضغط ☆ على أي أكلة تتكرر عندك لتجدها هنا بضغطة.",
                style = Type.small.copy(color = c.inkSoft),
            )
        }
        // كل الأكلات في بطاقة واحدة، و«+» بجنب كل أكلة يسجّل حصة بضغطة
        if (results.isNotEmpty()) item(key = "foods-$cat-$q") {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(20.dp))) {
                results.forEachIndexed { i, f ->
                    if (i > 0) Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(1.dp).background(c.line))
                    FoodRow(f, open == f.id, f.id in state.favorites, onToggle = { open = if (open == f.id) null else f.id }, onFav = { store.toggleFavorite(f.id) }) { qty ->
                        store.addMeal(if (qty == 1.0) f.name else "${f.name} ×${ar(qty)}", (f.kcal * qty).roundToInt(), (f.protein * qty).roundToInt(), MealSource.DB)
                        open = null
                    }
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
            Text("لم تسجّل شيئاً اليوم. أول وجبة تسجّلها تُنجز مهمة الأكل وتُحسب يوماً في سلسلتك.", style = Type.small.copy(color = c.inkSoft))
        }
        day.meals.forEach { m ->
            item(key = "m-${m.id}") {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(m.name, style = Type.bodyStrong.copy(color = c.ink))
                        Text("${ar(m.kcal)} سعرة، ${ar(m.protein)} غ بروتين" + if (m.source == MealSource.COACH) "، قدّرها المدرب" else "", style = Type.label.copy(color = c.inkSoft))
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
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.press(onToggle).padding(start = 16.dp, end = 10.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(f.name, style = Type.bodyStrong.copy(color = c.ink))
                Text("${f.portion} · ${ar(f.kcal)} سعرة · ${ar(f.protein)} غ بروتين", style = Type.label.copy(color = c.inkSoft))
            }
            Box(Modifier.size(40.dp).press(onFav).semantics { contentDescription = if (fav) "إزالة من المفضلة" else "أضف للمفضلة" }, contentAlignment = Alignment.Center) {
                SIcon(Ico.STAR, size = 18.dp, tint = if (fav) c.amber else c.faint)
            }
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(c.surface2).press({ onAdd(1.0) })
                    .semantics { contentDescription = "سجّل حصة ${f.name}" },
                contentAlignment = Alignment.Center,
            ) { SIcon(Ico.PLUS, size = 20.dp) }
        }
        AnimatedVisibility(open, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
                Text("كم أكلت من الحصة؟", style = Type.label.copy(color = c.inkSoft))
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.5 to "نصف", 1.0 to "حصة", 1.5 to "حصة ونصف", 2.0 to "حصتان").forEach { (qty, l) ->
                        Box(Modifier.clip(RoundedCornerShape(12.dp)).background(c.primaryTint).press({ onAdd(qty) }).padding(horizontal = 14.dp, vertical = 12.dp)) {
                            Text("$l  ${ar((f.kcal * qty).roundToInt())}", style = Type.label.copy(color = c.ink))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickTile(icon: Ico, label: String, modifier: Modifier, onClick: () -> Unit) {
    val c = Sanad.colors
    Column(
        modifier.heightIn(min = 80.dp).glass(RoundedCornerShape(16.dp)).press(onClick).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        SIcon(icon, size = 24.dp)
        Text(label, style = Type.small.copy(color = c.ink, fontWeight = FontWeight.Medium))
    }
}

/** إطار الكاميرا حول صحن مرسوم بالحبر، واللون مزاح قليلاً عن الخط. */
@Composable
private fun PlateViewfinder(modifier: Modifier) {
    val c = Sanad.colors
    Canvas(modifier) {
        val s = size.minDimension / 112f
        fun o(x: Float, y: Float) = androidx.compose.ui.geometry.Offset(x * s, y * s)
        val sw = 3f * s
        // زوايا الإطار
        listOf(
            listOf(o(8f, 28f), o(8f, 8f), o(28f, 8f)), listOf(o(84f, 8f), o(104f, 8f), o(104f, 28f)),
            listOf(o(104f, 84f), o(104f, 104f), o(84f, 104f)), listOf(o(28f, 104f), o(8f, 104f), o(8f, 84f)),
        ).forEach { (a, b, d) ->
            drawLine(c.primary, a, b, sw, androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(c.primary, b, d, sw, androidx.compose.ui.graphics.StrokeCap.Round)
        }
        val shift = o(2.5f, 2.5f)
        drawCircle(c.amberTint, 30f * s, o(56f, 58f) + shift)
        drawCircle(Color(0xFF5FA877), 9f * s, o(48f, 54f) + shift)
        listOf(o(48f, 66f), o(58f, 68f), o(66f, 62f)).forEach { drawCircle(c.amber, 4f * s, it + shift) }
        val ink = androidx.compose.ui.graphics.drawscope.Stroke(1.8f * s)
        drawCircle(c.ink, 30f * s, o(56f, 58f), style = ink)
        drawCircle(c.ink, 9f * s, o(48f, 54f), style = ink)
        listOf(o(48f, 66f), o(58f, 68f), o(66f, 62f)).forEach { drawCircle(c.ink, 4f * s, it, style = androidx.compose.ui.graphics.drawscope.Stroke(1.4f * s)) }
    }
}
