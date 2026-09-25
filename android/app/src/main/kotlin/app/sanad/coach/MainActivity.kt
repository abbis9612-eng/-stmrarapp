package app.sanad.coach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

    fun init(activity: ComponentActivity) {
        if (!::store.isInitialized) store = AppStore(activity.applicationContext)
        if (!::coach.isInitialized) coach = CoachSettings(activity.applicationContext)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Graph.init(this)

        // نسخة المطوّر فقط: بيانات تجريبية ومسار بدء للقطات الشاشة الآلية في CI
        var startRoute: String? = null
        var skipIntro = false
        if (BuildConfig.DEBUG) {
            intent?.let {
                if (it.getBooleanExtra("demo", false)) Graph.store.replaceAll(demoState())
                if (it.getBooleanExtra("fresh", false)) Graph.store.reset()
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
