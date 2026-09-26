package app.sanad.coach.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import app.sanad.core.StepCursor
import app.sanad.core.advanceSteps

/** يقرأ عدّاد خطوات الجهاز (بدون إنترنت ولا حساب) ويحدّث خطوات اليوم. */
class StepTracker(private val context: Context, private val store: AppStore) : SensorEventListener {
    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val prefs = context.getSharedPreferences("steps", Context.MODE_PRIVATE)
    private var listening = false
    private var written = -1

    val available: Boolean get() = sensor != null

    fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < 29 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    fun start() {
        val s = sensor ?: return
        if (listening || !hasPermission()) return
        listening = sm.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stop() {
        if (listening) sm.unregisterListener(this)
        listening = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        val reading = event.values.firstOrNull()?.toLong() ?: return
        val cur = prefs.getString("date", null)?.let { StepCursor(it, prefs.getLong("last", 0), prefs.getInt("today", 0)) }
        val next = advanceSteps(cur, reading, AppStore.today())
        prefs.edit().putString("date", next.date).putLong("last", next.last).putInt("today", next.today).apply()
        // نكتب للحالة كل ~٢٠ خطوة حتى ما نحفظ الملف مع كل خطوة
        if (written < 0 || next.today < written || next.today - written >= 20) {
            written = next.today
            store.setSteps(next.today)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
