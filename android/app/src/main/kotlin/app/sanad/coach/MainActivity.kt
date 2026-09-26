package app.sanad.coach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import app.sanad.coach.data.AppStore
import app.sanad.coach.data.CoachSettings
import app.sanad.coach.data.demoState
import app.sanad.coach.ui.SanadApp
import app.sanad.coach.ui.theme.SanadTheme
import app.sanad.core.Energy
import app.sanad.core.TimeBudget

object Graph {
    lateinit var store: AppStore
        private set
    lateinit var coach: CoachSettings
        private set
    lateinit var steps: app.sanad.coach.data.StepTracker
        private set

    fun init(context: android.content.Context) {
        if (!::store.isInitialized) store = AppStore(context.applicationContext)
        if (!::coach.isInitialized) coach = CoachSettings(context.applicationContext)
        if (!::steps.isInitialized) steps = app.sanad.coach.data.StepTracker(context.applicationContext, store)
    }
}

class MainActivity : ComponentActivity() {
    override fun onResume() {
        super.onResume()
        Graph.steps.start()
    }

    override fun onPause() {
        super.onPause()
        Graph.steps.stop()
    }

    override fun onStop() {
        super.onStop()
        app.sanad.coach.notify.Reminders.schedule(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // الهوية داكنة دائماً: أيقونات شريط الحالة فاتحة
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        Graph.init(this)

        // نسخة المطوّر فقط: بيانات تجريبية ومسار بدء للقطات الشاشة الآلية في CI
        var startRoute: String? = null
        var skipIntro = false
        if (BuildConfig.DEBUG) {
            intent?.let {
                if (it.getBooleanExtra("demo", false)) Graph.store.replaceAll(demoState())
                if (it.getBooleanExtra("fresh", false)) Graph.store.reset()
                if (it.hasExtra("ramadan")) Graph.store.setRamadan(it.getBooleanExtra("ramadan", false))
                it.getStringExtra("checkin")?.let { e -> Graph.store.checkIn(Energy.valueOf(e), TimeBudget.TWENTY) }
                if (it.getBooleanExtra("gathering", false)) Graph.store.setGathering(true)
                // حالات للتحقق باللقطات: رجوع بعد غياب، نوم قليل، وصول للهدف، شريك، GLP-1، تنبيه فوري
                if (it.getBooleanExtra("away", false)) Graph.store.replaceAll(demoState(java.time.LocalDate.now().minusDays(3)))
                it.getStringExtra("sleep")?.toDoubleOrNull()?.let { h -> Graph.store.setSleep(h) }
                if (it.getBooleanExtra("reached", false)) {
                    val s = demoState()
                    Graph.store.replaceAll(s.copy(profile = s.profile!!.copy(goalWeightKg = 97.0)))
                }
                if (it.getBooleanExtra("partner", false)) Graph.store.setPartner("حسن", false)
                if (it.getBooleanExtra("glp1", false)) Graph.store.setGlp1(true)
                if (it.getBooleanExtra("notifyNow", false)) {
                    val st = Graph.store.state.value
                    val t = st.profile?.let { p -> app.sanad.core.computeTargets(p, st.days.values.mapNotNull { d -> d.weightKg }.lastOrNull() ?: p.startWeightKg) }
                    val r = t?.let { tt -> app.sanad.core.nextReminder(st, tt, java.time.LocalDateTime.now().minusHours(12)) }
                    app.sanad.coach.notify.Reminders.ensureChannel(this)
                    app.sanad.coach.notify.Reminders.show(this, r?.id ?: "test", r?.title ?: "سند", r?.body ?: "تنبيه تجريبي")
                }
                startRoute = it.getStringExtra("route")
                skipIntro = it.getBooleanExtra("skipIntro", false)
            }
        }

        setContent {
            SanadTheme {
                SanadApp(Graph.store, Graph.coach, startRoute = startRoute, skipIntro = skipIntro || savedInstanceState != null)
            }
        }
    }
}
