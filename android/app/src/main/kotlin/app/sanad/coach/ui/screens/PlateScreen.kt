package app.sanad.coach.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import app.sanad.coach.data.AppStore
import app.sanad.coach.ui.components.BtnStyle
import app.sanad.coach.ui.components.Eyebrow
import app.sanad.coach.ui.components.Ico
import app.sanad.coach.ui.components.SButton
import app.sanad.coach.ui.components.SIcon
import app.sanad.coach.ui.components.glass
import app.sanad.coach.ui.components.press
import app.sanad.coach.ui.theme.Sanad
import app.sanad.coach.ui.theme.Type
import app.sanad.core.PlateInput
import app.sanad.core.ar
import app.sanad.core.plateScore

/** صحن سند: تحدد الأرباع بعينك، والصحن يترسم والدرجة تطلع مع نصيحة وحدة. */
@Composable
fun PlateScreen(store: AppStore, nav: NavHostController) {
    val c = Sanad.colors
    var veg by rememberSaveable { mutableIntStateOf(1) }
    var protein by rememberSaveable { mutableIntStateOf(1) }
    var carbs by rememberSaveable { mutableIntStateOf(2) }
    var fried by rememberSaveable { mutableStateOf(false) }
    var drink by rememberSaveable { mutableStateOf(false) }
    var saved by rememberSaveable { mutableStateOf(false) }
    val r = plateScore(PlateInput(veg, protein, carbs, fried, drink))
    val shown by animateFloatAsState(r.score.toFloat(), tween(700), label = "score")
    val tint = when {
        r.score >= 85 -> c.oasis
        r.score >= 65 -> c.sky
        r.score >= 40 -> c.saffron
        else -> c.rose
    }

    Page {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(c.glass2).press({ nav.popBackStack() }),
                    contentAlignment = Alignment.Center,
                ) { SIcon(Ico.BACK, size = 20.dp, description = "رجوع") }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Eyebrow("قيّم صحنك بعينك")
                    Text("صحن سند", style = Type.h1.copy(color = c.ink))
                }
            }
        }
        item {
            Column(
                Modifier.fillMaxWidth().glass(RoundedCornerShape(30.dp), tint).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PlateDrawing(veg, protein, carbs, Modifier.size(210.dp).semantics {
                    contentDescription = "خضار ${veg} ربع، بروتين ${protein} ربع، نشويات ${carbs} ربع"
                })
                Text("${ar(shown.toInt())}", style = Type.number.copy(fontSize = 44.sp, color = tint))
                Text(r.grade, style = Type.h2.copy(color = c.ink))
            }
        }
        item {
            Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(24.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                QuarterRow("خضار وسلطة", veg, 2, c.oasis) { veg = it; saved = false }
                QuarterRow("بروتين", protein, 2, c.rose) { protein = it; saved = false }
                QuarterRow("تمن / خبز / صمون", carbs, 3, c.saffron) { carbs = it; saved = false }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Toggle("مقلي", fried, Modifier.weight(1f)) { fried = it; saved = false }
                    Toggle("عصير / غازي", drink, Modifier.weight(1f)) { drink = it; saved = false }
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(24.dp), c.oasis).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("خطوتك الجاية", style = Type.label.copy(color = c.oasis))
                Text(r.tips.first(), style = Type.body.copy(color = c.ink, fontWeight = FontWeight.Medium))
                r.tips.drop(1).forEach { Text("• $it", style = Type.small.copy(color = c.inkSoft)) }
            }
        }
        item {
            if (!saved) SButton("سجّل الصحن", { store.logPlate(r.score); saved = true }, Modifier.fillMaxWidth(), style = BtnStyle.GOLD, icon = Ico.CHECK)
            else Text("انسجل. كل صحن مرتب خطوة.", style = Type.body.copy(color = c.oasis))
        }
    }
}

/** الصحن كدائرة بأرباع ملونة: خضار ثم بروتين ثم نشويات، والباقي فاضي. */
@Composable
private fun PlateDrawing(veg: Int, protein: Int, carbs: Int, modifier: Modifier) {
    val c = Sanad.colors
    val v by animateFloatAsState(veg * 90f, tween(500), label = "v")
    val p by animateFloatAsState(protein * 90f, tween(500), label = "p")
    val k by animateFloatAsState(carbs * 90f, tween(500), label = "k")
    Canvas(modifier) {
        val rim = 10.dp.toPx()
        drawCircle(Color.White.copy(alpha = 0.10f))
        drawCircle(Color.White.copy(alpha = 0.18f), style = Stroke(rim / 3))
        val inset = rim
        val tl = Offset(inset, inset)
        val sz = Size(size.width - inset * 2, size.height - inset * 2)
        var start = -90f
        listOf(v to c.oasis, p to c.rose, k to c.saffron).forEach { (sweep, col) ->
            val s = sweep.coerceAtMost(360f - (start + 90f))
            if (s > 0f) drawArc(col.copy(alpha = 0.85f), start, s, true, tl, sz)
            start += sweep
        }
        // خطوط الأرباع
        val cx = size.width / 2
        val cy = size.height / 2
        drawLine(c.bg.copy(alpha = 0.6f), Offset(cx, inset), Offset(cx, size.height - inset), 3.dp.toPx())
        drawLine(c.bg.copy(alpha = 0.6f), Offset(inset, cy), Offset(size.width - inset, cy), 3.dp.toPx())
    }
}

@Composable
private fun QuarterRow(label: String, value: Int, max: Int, color: Color, onPick: (Int) -> Unit) {
    val c = Sanad.colors
    val names = listOf("ماكو", "ربع", "نص", "ثلاث أرباع")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(8.dp))
            Text(label, style = Type.h3.copy(color = c.ink))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (0..max).forEach { n ->
                val sel = n == value
                Box(
                    Modifier.weight(1f).height(38.dp).clip(CircleShape)
                        .background(if (sel) color.copy(alpha = 0.9f) else c.glassTop)
                        .border(1.dp, if (sel) Color.Transparent else c.line, CircleShape)
                        .press({ onPick(n) }),
                    contentAlignment = Alignment.Center,
                ) { Text(names[n], style = Type.small.copy(color = if (sel) c.onGold else c.ink, fontWeight = FontWeight.Medium)) }
            }
        }
    }
}

@Composable
private fun Toggle(label: String, on: Boolean, modifier: Modifier, onChange: (Boolean) -> Unit) {
    val c = Sanad.colors
    Box(
        modifier.height(40.dp).clip(CircleShape)
            .background(if (on) c.rose.copy(alpha = 0.25f) else c.glassTop)
            .border(1.dp, if (on) c.rose else c.line, CircleShape)
            .press({ onChange(!on) }),
        contentAlignment = Alignment.Center,
    ) { Text((if (on) "✓ " else "") + label, style = Type.small.copy(color = c.ink)) }
}
