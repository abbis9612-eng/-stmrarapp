package app.sanad.coach.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.sanad.coach.data.AppStore
import app.sanad.coach.ui.components.Ico
import app.sanad.coach.ui.components.SIcon
import app.sanad.coach.ui.components.SaduBand
import app.sanad.coach.ui.components.SaduLogo
import app.sanad.coach.ui.components.press
import app.sanad.coach.ui.screens.CoachScreen
import app.sanad.coach.ui.screens.EatScreen
import app.sanad.coach.ui.screens.ExerciseScreen
import app.sanad.coach.ui.screens.MoveScreen
import app.sanad.coach.ui.screens.OnboardingScreen
import app.sanad.coach.ui.screens.PlayerScreen
import app.sanad.coach.ui.screens.ProgressScreen
import app.sanad.coach.ui.screens.TodayScreen
import app.sanad.coach.ui.theme.Sanad
import app.sanad.coach.ui.theme.Type

object Routes {
    const val START = "start"
    const val TODAY = "today"
    const val EAT = "eat"
    const val COACH = "coach"
    const val MOVE = "move"
    const val PROGRESS = "progress"
    const val PLAYER = "player/{routineId}"
    const val EXERCISE = "exercise/{id}"
    fun player(id: String) = "player/$id"
    fun exercise(id: String) = "exercise/$id"
    fun coach(q: String? = null) = if (q == null) COACH else "$COACH?q=${android.net.Uri.encode(q)}"
}

private data class Tab(val route: String, val label: String, val icon: Ico)

private val TABS = listOf(
    Tab(Routes.TODAY, "اليوم", Ico.TODAY),
    Tab(Routes.EAT, "الأكل", Ico.EAT),
    Tab(Routes.COACH, "سند", Ico.COACH),
    Tab(Routes.MOVE, "الحركة", Ico.MOVE),
    Tab(Routes.PROGRESS, "التقدّم", Ico.PROGRESS),
)

@Composable
fun SanadApp(store: AppStore, startRoute: String? = null, skipIntro: Boolean = false) {
    val state by store.state.collectAsStateWithLifecycle()
    val c = Sanad.colors
    val nav = rememberNavController()
    var introDone by rememberSaveable { mutableStateOf(skipIntro) }
    val start = remember { if (state.profile == null) Routes.START else Routes.TODAY }

    LaunchedEffect(startRoute) {
        if (startRoute != null && state.profile != null) nav.navigate(startRoute) { launchSingleTop = true }
    }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        val entry by nav.currentBackStackEntryAsState()
        val route = entry?.destination?.route?.substringBefore("?")
        val showBar = route in setOf(Routes.TODAY, Routes.EAT, Routes.COACH, Routes.MOVE, Routes.PROGRESS)

        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(tween(260)) + slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it / 24 } },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(160)) + slideOutVertically(tween(260)) { it / 24 } },
        ) {
            composable(Routes.START) {
                OnboardingScreen(store) { nav.navigate(Routes.TODAY) { popUpTo(Routes.START) { inclusive = true } } }
            }
            composable(Routes.TODAY) { TodayScreen(store, state, nav) }
            composable(Routes.EAT) { EatScreen(store, state, nav) }
            composable(
                "${Routes.COACH}?q={q}",
                arguments = listOf(navArgument("q") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) { e -> CoachScreen(store, state, nav, initialQuestion = e.arguments?.getString("q")) }
            composable(Routes.MOVE) { MoveScreen(state, nav) }
            composable(Routes.PROGRESS) { ProgressScreen(store, state, nav) }
            composable(Routes.PLAYER, arguments = listOf(navArgument("routineId") { type = NavType.StringType })) { e ->
                PlayerScreen(e.arguments?.getString("routineId").orEmpty(), store, nav)
            }
            composable(Routes.EXERCISE, arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
                ExerciseScreen(e.arguments?.getString("id").orEmpty(), nav)
            }
        }

        AnimatedVisibility(
            visible = showBar,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) { BottomBar(nav, route) }

        if (!introDone) Intro { introDone = true }
    }
}

@Composable
private fun BottomBar(nav: NavHostController, route: String?) {
    val c = Sanad.colors
    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .shadow(18.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), spotColor = c.night.copy(alpha = 0.25f))
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(c.surface)
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Bottom,
        ) {
            TABS.forEach { t ->
                val active = route == t.route
                val go = {
                    if (!active) nav.navigate(t.route) {
                        popUpTo(Routes.TODAY) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                Column(
                    Modifier
                        .weight(1f)
                        .press(go, role = Role.Tab)
                        .semantics { selected = active }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (t.route == Routes.COACH) {
                        Box(
                            Modifier
                                .offset(y = (-18).dp)
                                .size(60.dp)
                                .shadow(12.dp, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 30.dp, bottomEnd = 30.dp), spotColor = c.night)
                                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 30.dp, bottomEnd = 30.dp))
                                .background(if (c.isDark) c.date else c.night),
                            contentAlignment = Alignment.Center,
                        ) { SIcon(Ico.COACH, size = 28.dp, tint = if (c.isDark) Color(0xFF1B1406) else Color.White) }
                        Text(t.label, style = Type.label.copy(color = if (active) c.ink else c.inkSoft), modifier = Modifier.offset(y = (-12).dp))
                    } else {
                        SIcon(t.icon, tint = if (active) c.ink else c.inkSoft)
                        Text(t.label, style = Type.label.copy(color = if (active) c.ink else c.inkSoft))
                        Box(Modifier.size(5.dp).clip(CircleShape).background(if (active) c.sadu else Color.Transparent))
                    }
                }
            }
        }
    }
}

/** افتتاحية: الشعار ينسج نفسه ثم يختفي. */
@Composable
private fun Intro(onDone: () -> Unit) {
    val c = Sanad.colors
    val weave = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        weave.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
        fade.animateTo(0f, tween(380))
        onDone()
    }
    Box(Modifier.fillMaxSize().alpha(fade.value).background(c.night), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SaduLogo(size = 132.dp, progress = weave.value)
            Spacer(Modifier.height(18.dp))
            Text("سَنَد", style = Type.hero.copy(color = Color.White), modifier = Modifier.alpha(((weave.value - 0.4f) / 0.6f).coerceIn(0f, 1f)))
            Text("خطوة صغيرة كل يوم", style = Type.body.copy(color = c.onNightSoft), modifier = Modifier.alpha(((weave.value - 0.6f) / 0.4f).coerceIn(0f, 1f)))
        }
        SaduBand(Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp).fillMaxWidth(0.5f).alpha(weave.value), height = 10.dp)
    }
}

/** مسافة سفلية ثابتة للصفحات فوق شريط التنقل. */
val BottomBarSpace = 120.dp
