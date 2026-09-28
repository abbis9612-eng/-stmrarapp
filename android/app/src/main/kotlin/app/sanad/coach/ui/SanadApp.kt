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
import app.sanad.coach.ui.components.MoveMark
import app.sanad.coach.ui.components.LocalConfetti
import app.sanad.coach.ui.components.SButton
import app.sanad.coach.ui.components.SIcon
import app.sanad.coach.ui.components.Wordmark
import app.sanad.coach.ui.components.BrandMark
import app.sanad.coach.ui.components.burstFrom
import app.sanad.coach.ui.components.press
import app.sanad.coach.ui.components.rememberBurstPoint
import app.sanad.coach.ui.screens.CoachScreen
import app.sanad.coach.ui.screens.CoachSettingsScreen
import app.sanad.coach.ui.screens.EatScreen
import app.sanad.coach.ui.screens.ExerciseScreen
import app.sanad.coach.ui.screens.MoveScreen
import app.sanad.coach.ui.screens.OnboardingScreen
import app.sanad.coach.ui.screens.PacerScreen
import app.sanad.coach.ui.screens.PlateScreen
import app.sanad.coach.ui.screens.PlayerScreen
import app.sanad.coach.ui.screens.ProgressScreen
import app.sanad.coach.ui.screens.TodayScreen
import app.sanad.coach.ui.theme.Sanad
import app.sanad.coach.ui.theme.Type
import app.sanad.core.ar
import app.sanad.core.skyOf
import app.sanad.core.AppState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.heightIn
import androidx.activity.compose.BackHandler
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
    const val PACER = "pacer"
    const val PLATE = "plate"
    fun player(id: String) = "player/$id"
    fun exercise(id: String) = "exercise/$id"
    fun coach(q: String? = null, camera: Boolean = false): String {
        val args = listOfNotNull(q?.let { "q=${android.net.Uri.encode(it)}" }, if (camera) "camera=true" else null)
        return if (args.isEmpty()) COACH else "$COACH?${args.joinToString("&")}"
    }
}

private data class Tab(val route: String, val label: String, val icon: Ico)

/** شريط التنقل: أربع صفحات، وبالنص زر التسجيل (أكثر فعل يتكرر). المدرب من أيقونة «اليوم». */
private val LEFT_TABS = listOf(Tab(Routes.TODAY, "اليوم", Ico.SUN), Tab(Routes.EAT, "الأكل", Ico.EAT))
private val RIGHT_TABS = listOf(Tab(Routes.MOVE, "حركة", Ico.DUMBBELL), Tab(Routes.PROGRESS, "تقدّمي", Ico.TREND))

/** احتفال "يومك اكتمل" فوق كل شي، يُطلب من أي شاشة. */
class Celebration { var streak by mutableStateOf<Int?>(null) }

val LocalCelebration = staticCompositionLocalOf { Celebration() }

@Composable
fun SanadApp(store: AppStore, coach: CoachSettings, startRoute: String? = null, skipIntro: Boolean = false, onboardDemo: Boolean = false) {
    val state by store.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    var introDone by rememberSaveable { mutableStateOf(skipIntro) }
    var logOpen by rememberSaveable { mutableStateOf(false) }
    val start = remember { if (state.profile == null) Routes.START else Routes.TODAY }
    val confetti = remember { ConfettiState() }
    val celebration = remember { Celebration() }

    LaunchedEffect(startRoute) {
        if (startRoute != null && state.profile != null) nav.navigate(startRoute) { launchSingleTop = true }
    }

    CompositionLocalProvider(LocalConfetti provides confetti, LocalCelebration provides celebration) {
        val c = Sanad.colors
        Box(Modifier.fillMaxSize().background(c.bg)) {
            val entry by nav.currentBackStackEntryAsState()
            val route = entry?.destination?.route?.substringBefore("?")
            val showBar = route in setOf(Routes.TODAY, Routes.EAT, Routes.MOVE, Routes.PROGRESS)
            val out = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

            NavHost(
                navController = nav,
                startDestination = start,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { fadeIn(tween(350, easing = out)) + slideInHorizontally(tween(450, easing = out)) { -it / 16 } },
                exitTransition = { fadeOut(tween(180)) },
                popEnterTransition = { fadeIn(tween(300, easing = out)) },
                popExitTransition = { fadeOut(tween(180)) + slideOutVertically(tween(260)) { it / 24 } },
            ) {
                composable(Routes.START) {
                    OnboardingScreen(store, demo = onboardDemo) { nav.navigate(Routes.TODAY) { popUpTo(Routes.START) { inclusive = true } } }
                }
                composable(Routes.TODAY) { TodayScreen(store, state, nav) }
                composable(Routes.EAT) { EatScreen(store, state, nav) }
                composable(
                    "${Routes.COACH}?q={q}&camera={camera}",
                    arguments = listOf(
                        navArgument("q") { type = NavType.StringType; nullable = true; defaultValue = null },
                        navArgument("camera") { type = NavType.BoolType; defaultValue = false },
                    ),
                ) { e -> CoachScreen(store, coach, state, nav, initialQuestion = e.arguments?.getString("q"), openCamera = e.arguments?.getBoolean("camera") == true) }
                composable(Routes.COACH_SETTINGS) { CoachSettingsScreen(coach, nav) }
                composable(Routes.PACER) { PacerScreen(store, nav) }
                composable(Routes.PLATE) { PlateScreen(store, nav) }
                composable(Routes.MOVE) { MoveScreen(state, nav) }
                composable(Routes.PROGRESS) { ProgressScreen(store, state, nav) }
                composable(Routes.PLAYER, arguments = listOf(navArgument("routineId") { type = NavType.StringType })) { e ->
                    PlayerScreen(e.arguments?.getString("routineId").orEmpty(), store, nav)
                }
                composable(Routes.EXERCISE, arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
                    ExerciseScreen(e.arguments?.getString("id").orEmpty(), nav)
                }
            }

            // خلفية ورقية تحت شريط الحالة حتى ما يتداخل المحتوى مع الساعة
            if (route != null && !route.startsWith("player") && route != Routes.PACER) {
                Box(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(c.bg))
            }

            AnimatedVisibility(
                visible = showBar,
                enter = slideInVertically(tween(350, easing = out)) { it } + fadeIn(),
                exit = slideOutVertically(tween(250)) { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) { TabBar(nav, route) { logOpen = true } }

            if (logOpen) LogSheet(
                onClose = { logOpen = false },
                onGo = { r -> logOpen = false; nav.navigate(r) { launchSingleTop = true } },
            )

            celebration.streak?.let { n -> DayComplete(n, state) { celebration.streak = null } }
            ConfettiLayer(confetti)
            if (!introDone) Intro { introDone = true }
        }
    }
}

@Composable
private fun TabBar(nav: NavHostController, route: String?, onLog: () -> Unit) {
    val c = Sanad.colors
    fun go(r: String) = nav.navigate(r) {
        popUpTo(Routes.TODAY) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
    Column(Modifier.fillMaxWidth().background(c.surface)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().height(72.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LEFT_TABS.forEach { t -> TabItem(t, route == t.route, Modifier.weight(1f)) { go(t.route) } }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(58.dp).clip(RoundedCornerShape(19.dp)).background(c.brand)
                        .press(onLog).semantics { contentDescription = "سجّل أكلاً أو وزناً أو حركة" },
                    contentAlignment = Alignment.Center,
                ) { SIcon(Ico.PLUS, size = 26.dp, tint = androidx.compose.ui.graphics.Color(0xFF15231C)) }
            }
            RIGHT_TABS.forEach { t -> TabItem(t, route == t.route, Modifier.weight(1f)) { go(t.route) } }
        }
    }
}

@Composable
private fun TabItem(t: Tab, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = Sanad.colors
    val tint = if (active) c.primary else c.inkSoft
    val lift by animateFloatAsState(if (active) 1.06f else 1f, spring(dampingRatio = 0.55f), label = "lift")
    Column(
        modifier.fillMaxHeight().press({ if (!active) onClick() }, role = Role.Tab).semantics { selected = active },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SIcon(t.icon, size = 24.dp, tint = tint, modifier = Modifier.graphicsLayer { scaleX = lift; scaleY = lift })
        Spacer(Modifier.height(2.dp))
        Text(t.label, style = Type.label.copy(color = tint, fontSize = 12.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium))
    }
}

/** قائمة التسجيل من الزر الأوسط: أسرع طرق التسجيل بمكان واحد. */
@Composable
private fun LogSheet(onClose: () -> Unit, onGo: (String) -> Unit) {
    val c = Sanad.colors
    BackHandler(onBack = onClose)
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 500f)) }
    Box(
        Modifier.fillMaxSize().background(c.ink.copy(alpha = 0.35f * appear.value))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClose),
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer { translationY = (1f - appear.value) * 400.dp.toPx() }
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(c.surface)
                .clickable(remember { MutableInteractionSource() }, indication = null) {}
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(width = 40.dp, height = 4.dp).clip(CircleShape).background(c.line))
            Spacer(Modifier.height(8.dp))
            Text("سجّل", style = Type.h2.copy(color = c.ink))
            LogRow(Ico.CAMERA, "صوّر صحنك", "تحرّك يحسب السعرات بالصحن والرغيف") { onGo(Routes.coach(camera = true)) }
            LogRow(Ico.WRITE, "اكتب ما أكلت", "جملة واحدة تكفي: «تغدّيت كبسة وسلطة»") { onGo(Routes.COACH) }
            LogRow(Ico.REPEAT, "وجبة معتادة", "وجباتك المتكررة بضغطة واحدة") { onGo(Routes.EAT) }
            LogRow(Ico.SCALE, "سجّل وزنك", "مرة بالأسبوع تكفي") { onGo(Routes.PROGRESS) }
            LogRow(Ico.MOVE, "تحركت؟", "اختر حركة اليوم وسجّلها") { onGo(Routes.MOVE) }
        }
    }
}

@Composable
private fun LogRow(icon: Ico, title: String, hint: String, onClick: () -> Unit) {
    val c = Sanad.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(14.dp)).press(onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(c.primaryTint), contentAlignment = Alignment.Center) {
            SIcon(icon, size = 23.dp, tint = c.primary)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.h3.copy(color = c.ink))
            Text(hint, style = Type.small.copy(color = c.inkSoft))
        }
        SIcon(Ico.NEXT, size = 20.dp, tint = c.faint)
    }
}

/** الافتتاحية: السهمان يدخلان على الليموني، ثم يظهر اسم «تحرّك». */
@Composable
private fun Intro(onDone: () -> Unit) {
    val c = Sanad.colors
    val grow = remember { Animatable(0.05f) }
    val word = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        launch { grow.animateTo(1f, tween(1700, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f))) }
        launch { delay(500); word.animateTo(1f, tween(900)) }
        delay(2500)
        fade.animateTo(0f, tween(450))
        onDone()
    }
    Box(
        Modifier.fillMaxSize().alpha(fade.value).background(c.brand)
            .clickable(remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BrandMark(150.dp, progress = grow.value, tile = false)
            Spacer(Modifier.height(10.dp))
            Wordmark(64.sp, Modifier.alpha(word.value))
            Text(
                "تحرّك… وجسمك يشكرك",
                style = Type.hand.copy(fontSize = 24.sp, color = c.ink.copy(alpha = 0.75f)),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(word.value),
            )
        }
    }
}

/** "اكتمل يومك": حلقة الأيقونة تتقدّم يوماً، والسلسلة تنقلب للرقم الجديد. */
@Composable
private fun DayComplete(streak: Int, state: AppState, onClose: () -> Unit) {
    val c = Sanad.colors
    val confetti = LocalConfetti.current
    val point = rememberBurstPoint()
    val appear = remember { Animatable(0f) }
    val flip = remember { Animatable(0f) }
    var kick by remember { mutableStateOf(0) }
    val sky = skyOf(state.days, AppStore.today(), state.profile?.createdAt)
    LaunchedEffect(Unit) {
        appear.animateTo(1f, tween(400))
        kick++
        confetti.burst(point.center, 120, 1.1f)
        flip.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 180f))
    }
    Box(
        Modifier.fillMaxSize().alpha(appear.value).background(c.bg.copy(alpha = 0.97f))
            .clickable(remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MoveMark(130.dp, Modifier.burstFrom(point), progress = sky.phase, kick = kick)
            Spacer(Modifier.height(6.dp))
            Text("اكتمل يومك", style = Type.h1.copy(color = c.ink))
            Text(
                ar(streak),
                style = Type.hero.copy(fontSize = 64.sp, color = c.primary),
                modifier = Modifier.graphicsLayer {
                    rotationX = 90f * (1f - flip.value); alpha = flip.value.coerceIn(0f, 1f)
                    scaleX = 0.6f + 0.4f * flip.value; scaleY = 0.6f + 0.4f * flip.value
                },
            )
            Text("يوم في السلسلة، ودائرتك تقدّمت يوماً", style = Type.body.copy(color = c.inkSoft))
            Text("هذه الأيام الصغيرة هي التي تُنزل الوزن فعلاً. نراك غداً.", style = Type.body.copy(color = c.inkSoft), textAlign = TextAlign.Center)
            SButton("تمام", onClose, Modifier.width(220.dp))
        }
    }
}

/** مسافة سفلية ثابتة للصفحات فوق شريط التنقل. */
val BottomBarSpace = 112.dp
