package app.sanad.coach.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.sanad.coach.data.AppStore
import app.sanad.coach.data.CoachSettings
import app.sanad.coach.data.today
import app.sanad.coach.ui.components.BtnStyle
import app.sanad.coach.ui.components.ConfettiLayer
import app.sanad.coach.ui.components.ConfettiState
import app.sanad.coach.ui.components.Ico
import app.sanad.coach.ui.components.LivingOrb
import app.sanad.coach.ui.components.LocalConfetti
import app.sanad.coach.ui.components.SButton
import app.sanad.coach.ui.components.SIcon
import app.sanad.coach.ui.components.Wordmark
import app.sanad.coach.ui.components.burstFrom
import app.sanad.coach.ui.components.press
import app.sanad.coach.ui.components.rememberBurstPoint
import app.sanad.coach.ui.screens.CoachScreen
import app.sanad.coach.ui.screens.CoachSettingsScreen
import app.sanad.coach.ui.screens.EatScreen
import app.sanad.coach.ui.screens.ExerciseScreen
import app.sanad.coach.ui.screens.MoveScreen
import app.sanad.coach.ui.screens.OnboardingScreen
import app.sanad.coach.ui.screens.PlayerScreen
import app.sanad.coach.ui.screens.ProgressScreen
import app.sanad.coach.ui.screens.TodayScreen
import app.sanad.coach.ui.theme.ProvideMood
import app.sanad.coach.ui.theme.Sanad
import app.sanad.coach.ui.theme.Type
import app.sanad.core.ar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object Routes {
    const val START = "start"
    const val TODAY = "today"
    const val EAT = "eat"
    const val COACH = "coach"
    const val MOVE = "move"
    const val PROGRESS = "progress"
    const val PLAYER = "player/{routineId}"
    const val EXERCISE = "exercise/{id}"
    const val COACH_SETTINGS = "coach-settings"
    fun player(id: String) = "player/$id"
    fun exercise(id: String) = "exercise/$id"
    fun coach(q: String? = null) = if (q == null) COACH else "$COACH?q=${android.net.Uri.encode(q)}"
}

private data class Tab(val route: String, val label: String, val icon: Ico?)

private val TABS = listOf(
    Tab(Routes.TODAY, "اليوم", Ico.SUN),
    Tab(Routes.COACH, "المدرب", null),
    Tab(Routes.MOVE, "تمارين", Ico.DUMBBELL),
    Tab(Routes.PROGRESS, "التقدّم", Ico.TREND),
)

/** احتفال "يومك اكتمل" فوق كل شي، يُطلب من أي شاشة. */
class Celebration { var streak by mutableStateOf<Int?>(null) }

val LocalCelebration = staticCompositionLocalOf { Celebration() }

@Composable
fun SanadApp(store: AppStore, coach: CoachSettings, startRoute: String? = null, skipIntro: Boolean = false) {
    val state by store.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    var introDone by rememberSaveable { mutableStateOf(skipIntro) }
    val start = remember { if (state.profile == null) Routes.START else Routes.TODAY }
    val confetti = remember { ConfettiState() }
    val celebration = remember { Celebration() }

    LaunchedEffect(startRoute) {
        if (startRoute != null && state.profile != null) nav.navigate(startRoute) { launchSingleTop = true }
    }

    ProvideMood(state.today().energy) {
        CompositionLocalProvider(LocalConfetti provides confetti, LocalCelebration provides celebration) {
            val c = Sanad.colors
            Box(Modifier.fillMaxSize().background(c.bg)) {
                AmbientLight()
                val entry by nav.currentBackStackEntryAsState()
                val route = entry?.destination?.route?.substringBefore("?")
                val showBar = route in setOf(Routes.TODAY, Routes.COACH, Routes.MOVE, Routes.PROGRESS)
                val out = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

                NavHost(
                    navController = nav,
                    startDestination = start,
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = { fadeIn(tween(450, easing = out)) + slideInHorizontally(tween(550, easing = out)) { -it / 12 } + scaleIn(tween(550, easing = out), initialScale = 0.985f) },
                    exitTransition = { fadeOut(tween(200)) },
                    popEnterTransition = { fadeIn(tween(350, easing = out)) },
                    popExitTransition = { fadeOut(tween(200)) + slideOutVertically(tween(300)) { it / 24 } },
                ) {
                    composable(Routes.START) {
                        OnboardingScreen(store) { nav.navigate(Routes.TODAY) { popUpTo(Routes.START) { inclusive = true } } }
                    }
                    composable(Routes.TODAY) { TodayScreen(store, state, nav) }
                    composable(Routes.EAT) { EatScreen(store, state, nav) }
                    composable(
                        "${Routes.COACH}?q={q}",
                        arguments = listOf(navArgument("q") { type = NavType.StringType; nullable = true; defaultValue = null }),
                    ) { e -> CoachScreen(store, coach, state, nav, initialQuestion = e.arguments?.getString("q")) }
                    composable(Routes.COACH_SETTINGS) { CoachSettingsScreen(coach, nav) }
                    composable(Routes.MOVE) { MoveScreen(state, nav) }
                    composable(Routes.PROGRESS) { ProgressScreen(store, state, nav) }
                    composable(Routes.PLAYER, arguments = listOf(navArgument("routineId") { type = NavType.StringType })) { e ->
                        PlayerScreen(e.arguments?.getString("routineId").orEmpty(), store, nav)
                    }
                    composable(Routes.EXERCISE, arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
                        ExerciseScreen(e.arguments?.getString("id").orEmpty(), nav)
                    }
                }

                // ظل ناعم تحت شريط الحالة حتى لا يتداخل المحتوى مع الساعة
                if (route != null && !route.startsWith("player")) {
                    Box(
                        Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars)
                            .background(Brush.verticalGradient(listOf(c.bg, c.bg.copy(alpha = 0.85f)))),
                    )
                }

                AnimatedVisibility(
                    visible = showBar,
                    enter = slideInVertically(tween(500, easing = out)) { it * 2 } + fadeIn(),
                    exit = slideOutVertically(tween(400)) { it * 2 } + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) { TabBar(nav, route) }

                celebration.streak?.let { n -> DayComplete(n) { celebration.streak = null } }
                ConfettiLayer(confetti)
                if (!introDone) Intro { introDone = true }
            }
        }
    }
}

/** إضاءة محيطة بألوان مزاج اليوم خلف كل الشاشات. */
@Composable
private fun AmbientLight() {
    val m = Sanad.mood
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        drawRect(Brush.radialGradient(listOf(m.a.copy(alpha = 0.26f), Color.Transparent), center = Offset(w * 0.85f, -h * 0.05f), radius = w * 0.95f))
        drawRect(Brush.radialGradient(listOf(m.c.copy(alpha = 0.13f), Color.Transparent), center = Offset(0f, h * 0.3f), radius = w * 0.75f))
        drawRect(Brush.radialGradient(listOf(m.b.copy(alpha = 0.12f), Color.Transparent), center = Offset(w * 0.5f, h * 1.12f), radius = w * 0.95f))
    }
}

@Composable
private fun TabBar(nav: NavHostController, route: String?) {
    val c = Sanad.colors
    val shape = RoundedCornerShape(28.dp)
    val index = TABS.indexOfFirst { it.route == route }.coerceAtLeast(0)
    BoxWithConstraints(
        Modifier
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
            .fillMaxWidth()
            .height(70.dp)
            .shadow(24.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .background(Color(0xE6141820), shape)
            .border(1.dp, c.line, shape)
            .padding(7.dp),
    ) {
        val slot = maxWidth / TABS.size
        val x by animateDpAsState(slot * index, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow), label = "tab-ind")
        Box(Modifier.offset(x = x).width(slot).fillMaxHeight().background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp)))
        Row(Modifier.fillMaxSize()) {
            TABS.forEach { t ->
                val active = route == t.route
                val tint = if (active) c.ink else c.faint
                val lift by animateFloatAsState(if (active) 1.08f else 1f, spring(dampingRatio = 0.5f), label = "lift")
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .press({
                            if (!active) nav.navigate(t.route) {
                                popUpTo(Routes.TODAY) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }, role = Role.Tab)
                        .semantics { selected = active },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(Modifier.graphicsLayer { scaleX = lift; scaleY = lift }, contentAlignment = Alignment.Center) {
                        if (t.icon == null) LivingOrb(22.dp, glow = true)
                        else SIcon(t.icon, size = 23.dp, tint = tint)
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(t.label, style = Type.label.copy(color = tint, fontSize = 11.sp))
                }
            }
        }
    }
}

/**
 * الافتتاحية: الكرة تولد فوق، الكلمة تنكتب من اليمين لليسار،
 * ثم تنزل الكرة وتصير نقطة النون.
 */
@Composable
private fun Intro(onDone: () -> Unit) {
    val c = Sanad.colors
    val density = LocalDensity.current
    val t = remember { Animatable(0f) }
    val write = remember { Animatable(0f) }
    val tag = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }
    var dot by remember { mutableStateOf(Offset.Zero) }
    var dotSize by remember { mutableStateOf(1f) }
    var width by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        launch { t.animateTo(1f, tween(2200, easing = CubicBezierEasing(0.6f, 0f, 0.25f, 1f))) }
        launch { delay(700); write.animateTo(1f, tween(1100, easing = CubicBezierEasing(0.6f, 0f, 0.2f, 1f))) }
        launch { delay(2000); tag.animateTo(1f, tween(800)) }
        delay(3100)
        fade.animateTo(0f, tween(600))
        onDone()
    }
    val orbPx = with(density) { 64.dp.toPx() }
    val k = t.value
    // من الأعلى: تكبر (٠–٣٠٪)، تستقر (٣٠–٥٥٪)، ثم تنزل للنقطة
    val startY = -with(density) { 150.dp.toPx() }
    val move = ((k - 0.55f) / 0.45f).coerceIn(0f, 1f)
    val scale = when {
        k < 0.3f -> 1.6f * (k / 0.3f)
        k < 0.55f -> 1.6f - 0.15f * ((k - 0.3f) / 0.25f)
        else -> 1.45f + (dotSize / orbPx - 1.45f) * move
    }
    val px = (width / 2) + (dot.x - width / 2) * move
    val py = startY + (dot.y - startY) * move

    Box(
        Modifier.fillMaxSize().alpha(fade.value).background(c.bg)
            .clickable(remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Box {
            Wordmark(
                96.sp, reveal = write.value, showDot = false,
                onDot = { center, size -> dot = center; dotSize = size },
                modifier = Modifier.onSizeChanged { width = it.width.toFloat() },
            )
            LivingOrb(
                64.dp,
                Modifier.align(AbsoluteAlignment.TopLeft).graphicsLayer {
                    translationX = px - orbPx / 2
                    translationY = py - orbPx / 2
                    scaleX = scale.coerceAtLeast(0.001f); scaleY = scale.coerceAtLeast(0.001f)
                },
            )
        }
        Text(
            "خطوة صغيرة كل يوم… وسند وياك",
            style = Type.body.copy(color = c.inkSoft),
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 140.dp).alpha(tag.value),
        )
    }
}

/** "يومك اكتمل": الكرة تنفجر فرحاً والسلسلة تنقلب للرقم الجديد. */
@Composable
private fun DayComplete(streak: Int, onClose: () -> Unit) {
    val c = Sanad.colors
    val confetti = LocalConfetti.current
    val point = rememberBurstPoint()
    val appear = remember { Animatable(0f) }
    val flip = remember { Animatable(0f) }
    var kick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, tween(500))
        kick++
        confetti.burst(point.center, 160, 1.3f)
        flip.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 180f))
        delay(400)
        confetti.burst(point.center + Offset(-260f, 180f), 60)
        confetti.burst(point.center + Offset(260f, 180f), 60)
    }
    Box(
        Modifier.fillMaxSize().alpha(appear.value).background(Color(0xD9080A0E))
            .clickable(remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            LivingOrb(130.dp, Modifier.burstFrom(point), kick = kick)
            Spacer(Modifier.height(6.dp))
            Text("يومك اكتمل", style = Type.h1.copy(fontSize = 34.sp, color = c.ink))
            Text(
                ar(streak),
                style = Type.hero.copy(fontSize = 64.sp, color = c.saffron),
                modifier = Modifier.graphicsLayer {
                    rotationX = 90f * (1f - flip.value); alpha = flip.value.coerceIn(0f, 1f)
                    scaleX = 0.6f + 0.4f * flip.value; scaleY = 0.6f + 0.4f * flip.value
                },
            )
            Text("يوم في السلسلة", style = Type.body.copy(color = c.inkSoft))
            Text("الأيام الصغيرة هذي هي اللي تنزّل الوزن فعلاً. نشوفك بكرة.", style = Type.body.copy(color = c.inkSoft), textAlign = TextAlign.Center)
            SButton("تمام", onClose, Modifier.width(220.dp), style = BtnStyle.GOLD)
        }
    }
}

/** مسافة سفلية ثابتة للصفحات فوق شريط التنقل العائم. */
val BottomBarSpace = 124.dp
