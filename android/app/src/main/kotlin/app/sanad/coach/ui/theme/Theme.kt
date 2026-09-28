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
 * ألوان النص كلها تتجاوز تباين 4.5:1 على الأبيض.
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
    /** هوية «تحرّك»: ليموني الأيقونة وزيتوني السهم الخلفي. للعلامة والبداية فقط، ليس للنص */
    val brand: Color = Color(0xFFD4F25C),
    val brandOlive: Color = Color(0xFF7E9A2E),
    /** بطاقة «الخطوة التالية»: أقوى كتلة في الشاشة */
    val hero: Color = Color(0xFF15231C),
    val onHero: Color = Color(0xFFFFFFFF),
    val heroSoft: Color = Color(0xFFB7C0B2),
    val heroAccent: Color = Color(0xFFD4F25C),
    /** زر داخل بطاقة الخطوة */
    val heroBtn: Color = Color(0xFFD4F25C),
    val onHeroBtn: Color = Color(0xFF15231C),
    /** الأزرار الرئيسية */
    val act: Color = Color(0xFFD4F25C),
    val onAct: Color = Color(0xFF15231C),
    /** العنصر المختار (الطاقة، اليوم في شريط الأسبوع) */
    val sel: Color = Color(0xFFD4F25C),
    val onSel: Color = Color(0xFF15231C),
    val dark: Boolean = false,
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
    val isDark: Boolean get() = dark
}

/** «ضوء»: الرئيسي. فاتح، بطاقة الخطوة سوداء، والليموني للفعل والإنجاز. */
val SanadLight = SanadColors(
    bg = Color(0xFFF4F6EF),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFEEF1E8),
    ink = Color(0xFF15231C),
    inkSoft = Color(0xFF55604F),
    faint = Color(0xFF6E7869),
    line = Color(0xFFE2E6DA),
    primary = Color(0xFF15231C),
    primaryTint = Color(0xFFEEF6D2),
    onPrimary = Color(0xFFFFFFFF),
    amber = Color(0xFFD9902C),
    amberTint = Color(0xFFF7ECD6),
    saffron = Color(0xFFA36410),
    oasis = Color(0xFF5C7A1C),
    sky = Color(0xFF276C9C),
    rose = Color(0xFFB23A3A),
    violet = Color(0xFF6B4FA0),
    violetTint = Color(0xFFEEE8F6),
    note = Color(0xFFFBF6E4),
    noteLine = Color(0xFFEDE3C2),
    nightSky = Color(0xFF15231C),
    nightSky2 = Color(0xFF24362B),
    moonLight = Color(0xFFD4F25C),
    moonDark = Color(0xFFE3E8E4),
)

/** «ليل»: داكن كامل، والليموني يلمع على الأسود. */
val SanadNight = SanadColors(
    bg = Color(0xFF0D110E),
    surface = Color(0xFF171C18),
    surface2 = Color(0xFF20261F),
    ink = Color(0xFFF2F5EC),
    inkSoft = Color(0xFFA3AD9F),
    faint = Color(0xFF7F897B),
    line = Color(0xFF2A312A),
    primary = Color(0xFFD4F25C),
    primaryTint = Color(0xFF2B361E),
    onPrimary = Color(0xFF15231C),
    amber = Color(0xFFE5A23A),
    amberTint = Color(0xFF3A3118),
    saffron = Color(0xFFE8B45C),
    oasis = Color(0xFFA7C74C),
    sky = Color(0xFF6AB0E0),
    rose = Color(0xFFE07A6A),
    violet = Color(0xFFA58BE0),
    violetTint = Color(0xFF2A2438),
    note = Color(0xFF1C201A),
    noteLine = Color(0xFF2F3327),
    nightSky = Color(0xFF1B231C),
    nightSky2 = Color(0xFF28322A),
    moonLight = Color(0xFFD4F25C),
    moonDark = Color(0xFF2A322A),
    hero = Color(0xFFD4F25C),
    onHero = Color(0xFF15231C),
    heroSoft = Color(0xFF44511E),
    heroAccent = Color(0xFF3D4A18),
    heroBtn = Color(0xFF15231C),
    onHeroBtn = Color(0xFFD4F25C),
    dark = true,
)

/** «طاقة»: بطاقة الخطوة ليمونية، والأزرار والمختار بالأسود. */
val SanadEnergy = SanadLight.copy(
    bg = Color(0xFFF6F8EC),
    surface2 = Color(0xFFF0F3E4),
    line = Color(0xFFE3E7D6),
    hero = Color(0xFFD4F25C),
    onHero = Color(0xFF15231C),
    heroSoft = Color(0xFF4C5A24),
    heroAccent = Color(0xFF3D4A18),
    heroBtn = Color(0xFF15231C),
    onHeroBtn = Color(0xFFD4F25C),
    act = Color(0xFF15231C),
    onAct = Color(0xFFD4F25C),
    sel = Color(0xFF15231C),
    onSel = Color(0xFFD4F25C),
)

/** «نقي»: أبيض وأسود بسيط، والليموني للتقدّم فقط. */
val SanadClean = SanadLight.copy(
    bg = Color(0xFFF6F7F8),
    surface2 = Color(0xFFF1F2F4),
    ink = Color(0xFF111418),
    inkSoft = Color(0xFF5E646C),
    faint = Color(0xFF747A83),
    line = Color(0xFFE6E8EB),
    primary = Color(0xFF111418),
    primaryTint = Color(0xFFEEF0F2),
    note = Color(0xFFFFFFFF),
    noteLine = Color(0xFFE6E8EB),
    hero = Color(0xFF111418),
    heroSoft = Color(0xFFA9AFB6),
    heroBtn = Color(0xFFFFFFFF),
    onHeroBtn = Color(0xFF111418),
    act = Color(0xFF111418),
    onAct = Color(0xFFFFFFFF),
    sel = Color(0xFF111418),
    onSel = Color(0xFFFFFFFF),
)

/** النماذج المتاحة في «المظهر». الأول هو الافتراضي. */
enum class Palette(val label: String, val note: String, val colors: SanadColors) {
    LIGHT("ضوء", "فاتح بلمسات الأيقونة", SanadLight),
    NIGHT("ليل", "داكن كامل", SanadNight),
    ENERGY("طاقة", "بطاقة ليمونية وأزرار سوداء", SanadEnergy),
    CLEAN("نقي", "أبيض وأسود بسيط", SanadClean),
}

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
fun SanadTheme(palette: Palette = Palette.LIGHT, content: @Composable () -> Unit) {
    val c = palette.colors
    val scheme = if (c.dark) androidx.compose.material3.darkColorScheme(
        primary = c.primary, onPrimary = c.onPrimary, background = c.bg, surface = c.surface,
        onBackground = c.ink, onSurface = c.ink, secondary = c.oasis, outline = c.line,
    ) else lightColorScheme(
        primary = c.primary, onPrimary = c.onPrimary, background = c.bg, surface = c.surface,
        onBackground = c.ink, onSurface = c.ink, secondary = c.oasis, outline = c.line,
    )
    // أيقونات شريط الحالة والتنقل تتبع النموذج (فاتحة على الداكن)
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) androidx.compose.runtime.SideEffect {
        val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !c.dark
            isAppearanceLightNavigationBars = !c.dark
        }
        window.decorView.setBackgroundColor(android.graphics.Color.argb(255, (c.bg.red * 255).toInt(), (c.bg.green * 255).toInt(), (c.bg.blue * 255).toInt()))
    }
    CompositionLocalProvider(LocalSanad provides c, LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

object Sanad {
    val colors: SanadColors @Composable get() = LocalSanad.current
}
