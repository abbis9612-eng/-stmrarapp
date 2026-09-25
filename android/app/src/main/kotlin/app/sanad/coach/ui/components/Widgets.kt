package app.sanad.coach.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.sanad.coach.ui.theme.Sanad
import app.sanad.coach.ui.theme.Type

/* ---------------- أيقونات مرسومة خصيصاً (نفس لغة الويب) ---------------- */

enum class Ico(val d: String, val filled: Boolean = false) {
    TODAY("M4 11a8 8 0 0 1 16 0v8a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1z M9 20v-5h6v5"),
    EAT("M7 3v8 M4 3v5a3 3 0 0 0 6 0V3 M7 11v10 M17 21V3c-2 1-3 4-3 7v3h3"),
    COACH("M4 6a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2h-6l-5 4v-4H6a2 2 0 0 1-2-2z M8.5 10.5h.01 M12 10.5h.01 M15.5 10.5h.01"),
    MOVE("M13 4.5a1.5 1.5 0 1 0 0 .01 M7 21l3-6 3 2v4 M10 15l1-5 4 3 3-1 M6 11l3-2 3 1"),
    PROGRESS("M4 19h16 M6 15l4-4 3 3 5-6"),
    CHECK("M5 12.5l4.5 4.5L19 7.5"),
    PLUS("M12 5v14 M5 12h14"),
    MINUS("M5 12h14"),
    SEND("M4 12l16-8-6 16-2.5-6.5z"),
    CLOSE("M6 6l12 12 M18 6L6 18"),
    PLAY("M8 5v14l11-7z", true),
    PAUSE("M8 5v14 M16 5v14"),
    STAR("M12 4l2.4 5 5.6.6-4.2 3.8 1.2 5.6L12 16.2 7 19l1.2-5.6L4 9.6 9.6 9z"),
    TRASH("M5 7h14 M10 11v6 M14 11v6 M6 7l1 13h10l1-13 M9 7V4h6v3"),
    BACK("M9 6l6 6-6 6"),
    NEXT("M15 6l-6 6 6 6"),
    SPARK("M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8z", true),
    FLAME("M12 21c-4 0-7-3-7-7 0-4 3-6 4-9 1 2 2 3 3 3 0-2 1-4 2-5 2 3 5 6 5 11 0 4-3 7-7 7z"),
    DROP("M12 3c3 4 6 7.5 6 11a6 6 0 0 1-12 0c0-3.5 3-7 6-11z"),
    SCALE("M5 4h14l-1.5 16h-11z M9 10a3 3 0 0 1 6 0 M12 10l1.5-2"),
    SETTINGS("M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8z M12 2v3 M12 19v3 M2 12h3 M19 12h3 M4.9 4.9l2.1 2.1 M17 17l2.1 2.1 M4.9 19.1l2.1-2.1 M17 7l2.1-2.1"),
    MIC("M12 3a3 3 0 0 1 3 3v6a3 3 0 0 1-6 0V6a3 3 0 0 1 3-3z M5 11a7 7 0 0 0 14 0 M12 18v3"),
    CAMERA("M4 8h3l2-3h6l2 3h3v11H4z M12 11a3 3 0 1 0 0 6 3 3 0 0 0 0-6z"),
}

private val iconCache = HashMap<Ico, ImageVector>()

fun Ico.vector(): ImageVector = iconCache.getOrPut(this) {
    val b = ImageVector.Builder(name.lowercase(), 24.dp, 24.dp, 24f, 24f)
    for ((i, part) in d.split(" M").withIndex()) {
        val nodes = PathParser().parsePathString(if (i == 0) part else "M$part").toNodes()
        b.addPath(
            nodes,
            fill = if (filled) SolidColor(Color.Black) else null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    b.build()
}

@Composable
fun SIcon(icon: Ico, modifier: Modifier = Modifier, size: Dp = 24.dp, tint: Color = Sanad.colors.ink, description: String? = null) {
    Icon(icon.vector(), description, modifier.size(size), tint = tint)
}

/* ---------------- ضغط بلمسة: تصغير نابض + اهتزاز خفيف ---------------- */

@Composable
fun Modifier.press(onClick: () -> Unit, role: Role = Role.Button, haptic: Boolean = true, enabled: Boolean = true): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.96f else 1f, spring(dampingRatio = 0.5f, stiffness = 600f), label = "press")
    val h = LocalHapticFeedback.current
    return this
        .scale(s)
        .clickable(source, indication = null, enabled = enabled, role = role) {
            if (haptic) h.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
}

/* ---------------- بطاقات وأزرار ---------------- */

@Composable
fun SCard(modifier: Modifier = Modifier, color: Color = Sanad.colors.surface, shape: Shape = RoundedCornerShape(22.dp), pad: Dp = 18.dp, content: @Composable ColumnScope.() -> Unit) {
    val c = Sanad.colors
    Column(
        modifier
            .shadow(if (c.isDark) 0.dp else 10.dp, shape, ambientColor = c.night.copy(alpha = 0.10f), spotColor = c.night.copy(alpha = 0.14f))
            .clip(shape)
            .background(color)
            .then(if (c.isDark) Modifier.border(1.dp, c.line, shape) else Modifier)
            .padding(pad),
        content = content,
    )
}

/** بطاقة ليلية فاخرة بتدرّج نيلي — للبطاقات البطلة (الخيط، الخطة). */
@Composable
fun NightCard(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(26.dp), pad: Dp = 18.dp, content: @Composable ColumnScope.() -> Unit) {
    val c = Sanad.colors
    Column(
        modifier
            .shadow(if (c.isDark) 0.dp else 16.dp, shape, spotColor = c.night.copy(alpha = 0.4f))
            .clip(shape)
            .background(Brush.linearGradient(listOf(c.night2, c.night)))
            .then(if (c.isDark) Modifier.border(1.dp, c.line, shape) else Modifier)
            .padding(pad),
        content = content,
    )
}

enum class BtnStyle { PRIMARY, SOFT, GHOST, GOLD }

@Composable
fun SButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: BtnStyle = BtnStyle.PRIMARY,
    enabled: Boolean = true,
    icon: Ico? = null,
    small: Boolean = false,
) {
    val c = Sanad.colors
    val (bg, fg) = when (style) {
        BtnStyle.PRIMARY -> if (c.isDark) c.date to Color(0xFF1B1406) else c.night to Color.White
        BtnStyle.GOLD -> c.date to Color(0xFF1B1406)
        BtnStyle.SOFT -> c.surface2 to c.ink
        BtnStyle.GHOST -> Color.Transparent to c.ink
    }
    val alpha by animateFloatAsState(if (enabled) 1f else 0.4f, tween(200), label = "btn-alpha")
    Row(
        modifier
            .heightIn(min = if (small) 42.dp else 54.dp)
            .clip(CircleShape)
            .background(bg.copy(alpha = bg.alpha * alpha))
            .then(if (style == BtnStyle.GHOST) Modifier.border(BorderStroke(1.5.dp, c.line), CircleShape) else Modifier)
            .press(onClick, enabled = enabled)
            .padding(horizontal = if (small) 16.dp else 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            SIcon(icon, size = if (small) 18.dp else 20.dp, tint = fg.copy(alpha = alpha))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = (if (small) Type.label else Type.h3).copy(color = fg.copy(alpha = alpha)), textAlign = TextAlign.Center)
    }
}

@Composable
fun SChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Sanad.colors
    val bg = if (selected) (if (c.isDark) c.date else c.night) else c.surface
    val fg = if (selected) (if (c.isDark) Color(0xFF1B1406) else Color.White) else c.ink
    Box(
        modifier
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.5.dp, if (selected) bg else c.line, CircleShape)
            .press(onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = Type.label.copy(color = fg)) }
}

@Composable
fun Badge(text: String, modifier: Modifier = Modifier, gold: Boolean = false) {
    val c = Sanad.colors
    Box(
        modifier
            .clip(CircleShape)
            .background(if (gold) c.dateSoft else c.palmSoft)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) { Text(text, style = Type.label.copy(color = if (gold) (if (c.isDark) c.date else Color(0xFF8A5A12)) else c.palm)) }
}

/** شريط تقدّم متحرك (سعرات/بروتين). */
@Composable
fun Meter(progress: Float, modifier: Modifier = Modifier, color: Color = Sanad.colors.night, height: Dp = 12.dp) {
    val c = Sanad.colors
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), spring(stiffness = 120f), label = "meter")
    Box(modifier.fillMaxWidth().height(height).clip(CircleShape).background(c.surface2)) {
        Box(Modifier.fillMaxWidth(p).height(height).clip(CircleShape).background(color))
    }
}

/** بطارية الطاقة: تمتلئ بخلايا ذهبية حسب المستوى. */
@Composable
fun Battery(level: Int, modifier: Modifier = Modifier, color: Color = Sanad.colors.ink) {
    val c = Sanad.colors
    Column(modifier.clearAndSetSemantics { contentDescription = "مستوى $level من ٣" }, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.width(14.dp).height(5.dp).clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)).background(color))
        Column(
            Modifier.width(34.dp).height(54.dp).border(2.5.dp, color, RoundedCornerShape(9.dp)).padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            for (k in 3 downTo 1) {
                val on = k <= level
                val a by animateFloatAsState(if (on) 1f else 0f, tween(300 + k * 80), label = "cell$k")
                Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(3.dp)).background(if (on) c.date.copy(alpha = 0.35f + 0.65f * a) else c.surface2))
            }
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = Type.h3.copy(color = Sanad.colors.inkSoft), modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    val c = Sanad.colors
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(c.surface2).padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = Type.number.copy(fontSize = Type.h2.fontSize, color = c.ink))
        Text(label, style = Type.label.copy(color = c.inkSoft), textAlign = TextAlign.Center)
    }
}

@Composable
fun Note(text: String, modifier: Modifier = Modifier, alert: Boolean = false) {
    val c = Sanad.colors
    Text(
        text,
        style = Type.small.copy(color = c.ink),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (alert) c.sadu.copy(alpha = 0.14f) else c.dateSoft)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    )
}
