package app.sanad.coach.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
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

/**
 * هوية سند: ورق فاتح، حبر، ولون صلب واحد للفعل. بلا توهج ولا تدرجات.
 * الرسومات بخط حبر ولون مزاح قليلاً عن الخط، وملاحظات سند بخط الرقعة.
 * ألوان النص كلها تتجاوز تباين ٤٫٥:١ على الأبيض.
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
    /** الفعل الأساسي: الأزرار والمختار والتنقل */
    val primary: Color,
    val primaryTint: Color,
    val onPrimary: Color,
    /** كهرماني للتعبئة (أشرطة، القمر، رسومات)؛ مو للنص */
    val amber: Color,
    val amberTint: Color,
    /** زعفران غامق — نص التقدّم والسلسلة */
    val saffron: Color,
    /** فيروزي غامق — الإنجاز والبروتين */
    val oasis: Color,
    /** أزرق — الماء */
    val sky: Color,
    val rose: Color,
    /** بنفسجي — المناسبات والنوم */
    val violet: Color,
    val violetTint: Color,
    /** ورقة ملاحظة سند */
    val note: Color,
    val noteLine: Color,
    /** سماء القمر في صفحة التقدّم */
    val nightSky: Color,
    val nightSky2: Color,
    val moonLight: Color,
    val moonDark: Color,
) {
    // أسماء قديمة تبقى تعمل في الشاشات الثانوية
    val ember: Color get() = primary
    val onGold: Color get() = onPrimary
    val glassTop: Color get() = surface
    val glassBottom: Color get() = surface
    val glass2: Color get() = surface2
    val night: Color get() = surface
    val night2: Color get() = surface2
    val onNight: Color get() = ink
    val onNightSoft: Color get() = inkSoft
    val sadu: Color get() = primary
    val sadu2: Color get() = saffron
    val date: Color get() = saffron
    val dateSoft: Color get() = amberTint
    val palm: Color get() = primary
    val palmSoft: Color get() = primaryTint
    val wool: Color get() = ink
    val isDark: Boolean get() = false
}

/** «فيروز ونخل»: أخضر عميق للفعل، كهرماني للتقدّم، فيروزي للإنجاز. */
val SanadLight = SanadColors(
    bg = Color(0xFFF3F6F2),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFEEF2EE),
    ink = Color(0xFF15231C),
    inkSoft = Color(0xFF4B5A52),
    faint = Color(0xFF6B776F),
    line = Color(0xFFDDE4DE),
    primary = Color(0xFF1F6B4E),
    primaryTint = Color(0xFFE4EFE8),
    onPrimary = Color(0xFFFFFFFF),
    amber = Color(0xFFD9902C),
    amberTint = Color(0xFFF7ECD6),
    saffron = Color(0xFFA36410),
    oasis = Color(0xFF0B7475),
    sky = Color(0xFF276C9C),
    rose = Color(0xFFB23A3A),
    violet = Color(0xFF6B4FA0),
    violetTint = Color(0xFFEEE8F6),
    note = Color(0xFFFFF8E4),
    noteLine = Color(0xFFEADFC2),
    nightSky = Color(0xFF1E2F46),
    nightSky2 = Color(0xFF2C4160),
    moonLight = Color(0xFFF2C66B),
    moonDark = Color(0xFFE3E8E4),
)

val LocalSanad = staticCompositionLocalOf { SanadLight }

val Body = FontFamily(
    Font(R.font.plex_regular, FontWeight.Normal),
    Font(R.font.plex_medium, FontWeight.Medium),
    Font(R.font.plex_semibold, FontWeight.SemiBold),
    Font(R.font.plex_bold, FontWeight.Bold),
)

/** خط الرقعة: خط اليد عند أغلب العرب، لملاحظات سند فقط. */
val Hand = FontFamily(
    Font(R.font.ruqaa_regular, FontWeight.Normal),
    Font(R.font.ruqaa_bold, FontWeight.Bold),
)

/** العناوين بنفس خط القراءة (أثقل)، والأرقام بعرض ثابت حتى ما «ترقص» وهي تتغيّر. */
val Display = Body

object Type {
    val hero = TextStyle(fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 44.sp, lineHeight = 1.2.em, fontFeatureSettings = "tnum")
    val h1 = TextStyle(fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 1.35.em)
    val h2 = TextStyle(fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 1.45.em)
    val h3 = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 1.5.em)
    val body = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 1.75.em)
    val bodyStrong = body.copy(fontWeight = FontWeight.SemiBold)
    val small = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 1.6.em)
    val label = TextStyle(fontFamily = Body, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 1.5.em)
    val number = TextStyle(fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 1.15.em, fontFeatureSettings = "tnum")
    /** ملاحظة بخط اليد من سند */
    val hand = TextStyle(fontFamily = Hand, fontWeight = FontWeight.Normal, fontSize = 21.sp, lineHeight = 1.7.em)
    val handTitle = TextStyle(fontFamily = Hand, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 1.5.em)
}

@Composable
fun SanadTheme(content: @Composable () -> Unit) {
    val c = SanadLight
    val scheme = lightColorScheme(
        primary = c.primary, onPrimary = c.onPrimary, background = c.bg, surface = c.surface,
        onBackground = c.ink, onSurface = c.ink, secondary = c.oasis, outline = c.line,
    )
    CompositionLocalProvider(LocalSanad provides c, LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

object Sanad {
    val colors: SanadColors @Composable get() = LocalSanad.current
}
