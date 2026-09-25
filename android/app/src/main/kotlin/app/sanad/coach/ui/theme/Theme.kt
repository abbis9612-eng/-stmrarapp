package app.sanad.coach.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.sanad.coach.R

/** هوية سند: ليل نيلي + نسيج السدو + ذهب التمر. الجرأة في "الخيط"، والباقي هادئ. */
@Immutable
data class SanadColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val ink: Color,
    val inkSoft: Color,
    val line: Color,
    val night: Color,
    val night2: Color,
    val onNight: Color,
    val onNightSoft: Color,
    val sadu: Color,
    val sadu2: Color,
    val date: Color,
    val dateSoft: Color,
    val palm: Color,
    val palmSoft: Color,
    val sky: Color,
    val wool: Color,
    val isDark: Boolean,
)

val LightColors = SanadColors(
    bg = Color(0xFFF1F3F7), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFE8ECF3),
    ink = Color(0xFF14213D), inkSoft = Color(0xFF56607A), line = Color(0xFFD7DDE8),
    night = Color(0xFF14213D), night2 = Color(0xFF263A66), onNight = Color(0xFFFFFFFF), onNightSoft = Color(0xFFB7C1D9),
    sadu = Color(0xFFA83A2F), sadu2 = Color(0xFFD9674F), date = Color(0xFFD9962E), dateSoft = Color(0xFFF7E6C8),
    palm = Color(0xFF2C6E4F), palmSoft = Color(0xFFD7EBE1), sky = Color(0xFF3B6FD8), wool = Color(0xFFF4EFE6),
    isDark = false,
)

val DarkColors = SanadColors(
    bg = Color(0xFF0B1322), surface = Color(0xFF131E36), surface2 = Color(0xFF1B2946),
    ink = Color(0xFFE9EDF5), inkSoft = Color(0xFF9AA6C0), line = Color(0xFF26375C),
    night = Color(0xFF0A1120), night2 = Color(0xFF1B2B4F), onNight = Color(0xFFFFFFFF), onNightSoft = Color(0xFFB7C1D9),
    sadu = Color(0xFFD9674F), sadu2 = Color(0xFFF08C72), date = Color(0xFFF0B457), dateSoft = Color(0xFF3A2F1C),
    palm = Color(0xFF5FBF8F), palmSoft = Color(0xFF183A2C), sky = Color(0xFF7AA2FF), wool = Color(0xFFF4EFE6),
    isDark = true,
)

val LocalSanad = staticCompositionLocalOf { LightColors }

@OptIn(ExperimentalTextApi::class)
private fun alexandria(weight: Int) = Font(
    R.font.alexandria,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

@OptIn(ExperimentalTextApi::class)
val Display = FontFamily(alexandria(500), alexandria(700), alexandria(800))

val Body = FontFamily(
    Font(R.font.plex_regular, FontWeight.Normal),
    Font(R.font.plex_medium, FontWeight.Medium),
    Font(R.font.plex_semibold, FontWeight.SemiBold),
    Font(R.font.plex_bold, FontWeight.Bold),
)

/** مقاسات عربية: أسطر أعلى (١٫٧) وعناوين أكبر ~١٠٪، بدون تباعد حروف. */
object Type {
    val hero = TextStyle(fontFamily = Display, fontWeight = FontWeight(800), fontSize = 38.sp, lineHeight = 1.3.em)
    val h1 = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 1.35.em)
    val h2 = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 1.4.em)
    val h3 = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 1.45.em)
    val body = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 1.7.em)
    val bodyStrong = body.copy(fontWeight = FontWeight.SemiBold)
    val small = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 1.65.em)
    val label = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 1.5.em)
    val number = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 1.2.em)
}

@Composable
fun SanadTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val c = if (dark) DarkColors else LightColors
    val scheme = if (dark) darkColorScheme(
        primary = c.date, onPrimary = Color(0xFF1B1406), background = c.bg, surface = c.surface,
        onBackground = c.ink, onSurface = c.ink, secondary = c.sadu, outline = c.line,
    ) else lightColorScheme(
        primary = c.night, onPrimary = Color.White, background = c.bg, surface = c.surface,
        onBackground = c.ink, onSurface = c.ink, secondary = c.sadu, outline = c.line,
    )
    CompositionLocalProvider(LocalSanad provides c, LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

object Sanad {
    val colors: SanadColors @Composable get() = LocalSanad.current
}
