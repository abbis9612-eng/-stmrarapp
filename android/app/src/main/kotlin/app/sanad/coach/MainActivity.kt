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

    fun init(context: android.content.Context) {
        if (!::store.isInitialized) store = AppStore(context.applicationContext)
        if (!::coach.isInitialized) coach = CoachSettings(context.applicationContext)
    }
}

class MainActivity : ComponentActivity() {
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
