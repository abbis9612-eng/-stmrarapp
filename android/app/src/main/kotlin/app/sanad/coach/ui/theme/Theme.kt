package app.sanad.coach.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.sanad.coach.R
import app.sanad.core.Energy

/**
 * هوية سند: ليل عميق، زجاج شفاف، ونور حي (الكرة) يتلوّن حسب طاقتك.
 * الجرأة في الكرة والحلقات؛ الباقي هادئ.
 */
@Immutable
data class SanadColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val ink: Color,
    val inkSoft: Color,
    val faint: Color,
    val line: Color,
    val glassTop: Color,
    val glassBottom: Color,
    val glass2: Color,
    /** زعفران — التقدّم والسلسلة */
    val saffron: Color,
    /** جمر — الطاقة والأزرار الأساسية */
    val ember: Color,
    /** واحة — الإنجاز والبروتين */
    val oasis: Color,
    /** سماء — الماء */
    val sky: Color,
    val rose: Color,
    /** نص فوق الأزرار الذهبية */
    val onGold: Color,
) {
    // أسماء قديمة تبقى تعمل في الشاشات الثانوية
    val night: Color get() = surface
    val night2: Color get() = surface2
    val onNight: Color get() = ink
    val onNightSoft: Color get() = inkSoft
    val sadu: Color get() = ember
    val sadu2: Color get() = saffron
    val date: Color get() = saffron
    val dateSoft: Color get() = saffron.copy(alpha = 0.14f)
    val palm: Color get() = oasis
    val palmSoft: Color get() = oasis.copy(alpha = 0.14f)
    val wool: Color get() = ink
    val isDark: Boolean get() = true
}

val SanadDark = SanadColors(
    bg = Color(0xFF0A0D12),
    surface = Color(0xFF141922),
    surface2 = Color(0xFF1C222C),
    ink = Color(0xFFF2F0EB),
    inkSoft = Color(0xFF98A1AF),
    faint = Color(0xFF5E6776),
    line = Color(0x14FFFFFF),
    glassTop = Color(0x13FFFFFF),
    glassBottom = Color(0x08FFFFFF),
    glass2 = Color(0x17FFFFFF),
    saffron = Color(0xFFFFB648),
    ember = Color(0xFFFF7A45),
    oasis = Color(0xFF34D7B8),
    sky = Color(0xFF5B8CFF),
    rose = Color(0xFFFF5C7A),
    onGold = Color(0xFF1A0F06),
)

/** ألوان الكرة الحيّة والإضاءة المحيطة لكل مستوى طاقة. */
@Immutable
data class Mood(val a: Color, val b: Color, val c: Color)

val MOODS = mapOf(
    Energy.LOW to Mood(Color(0xFF7C8BFF), Color(0xFF34D7B8), Color(0xFFA57BFF)),
    Energy.MID to Mood(Color(0xFF34D7B8), Color(0xFFFFB648), Color(0xFF5B8CFF)),
    Energy.HIGH to Mood(Color(0xFFFF7A45), Color(0xFFFFB648), Color(0xFFFF5C7A)),
)

fun moodFor(e: Energy?): Mood = MOODS.getValue(e ?: Energy.MID)

val LocalSanad = staticCompositionLocalOf { SanadDark }
val LocalMood = compositionLocalOf { moodFor(null) }

/** الخط الكوفي للهوية والأرقام الكبيرة. */
val Display = FontFamily(
    Font(R.font.reem_medium, FontWeight.Medium),
    Font(R.font.reem_semibold, FontWeight.SemiBold),
    Font(R.font.reem_bold, FontWeight.Bold),
)

val Body = FontFamily(
    Font(R.font.plex_regular, FontWeight.Normal),
    Font(R.font.plex_medium, FontWeight.Medium),
    Font(R.font.plex_semibold, FontWeight.SemiBold),
    Font(R.font.plex_bold, FontWeight.Bold),
)

/** سلّم الخطوط: الكوفي للعناوين الكبيرة والأرقام، وبلكس للقراءة. */
object Type {
    val hero = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 52.sp, lineHeight = 1.15.em)
    val h1 = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 1.3.em)
    val h2 = TextStyle(fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 1.45.em)
    val h3 = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 1.5.em)
    val body = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 1.75.em)
    val bodyStrong = body.copy(fontWeight = FontWeight.SemiBold)
    val small = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 1.6.em)
    val label = TextStyle(fontFamily = Body, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 1.5.em)
    val number = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 1.15.em)
}

@Composable
fun SanadTheme(content: @Composable () -> Unit) {
    val c = SanadDark
    val scheme = darkColorScheme(
        primary = c.saffron, onPrimary = c.onGold, background = c.bg, surface = c.surface,
        onBackground = c.ink, onSurface = c.ink, secondary = c.oasis, outline = c.line,
    )
    CompositionLocalProvider(LocalSanad provides c, LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

/** يلوّن الكرة والإضاءة المحيطة بطاقة اليوم، بانتقال ناعم عند التغيير. */
@Composable
fun ProvideMood(energy: Energy?, content: @Composable () -> Unit) {
    val target = moodFor(energy)
    val a by animateColorAsState(target.a, tween(1200), label = "mood-a")
    val b by animateColorAsState(target.b, tween(1200), label = "mood-b")
    val m by animateColorAsState(target.c, tween(1200), label = "mood-c")
    CompositionLocalProvider(LocalMood provides Mood(a, b, m), content = content)
}

object Sanad {
    val colors: SanadColors @Composable get() = LocalSanad.current
    val mood: Mood @Composable get() = LocalMood.current
}
