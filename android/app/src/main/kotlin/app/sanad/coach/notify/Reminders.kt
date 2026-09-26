package app.sanad.coach.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.sanad.coach.Graph
import app.sanad.coach.MainActivity
import app.sanad.coach.R
import app.sanad.coach.data.targets
import app.sanad.core.nextReminder
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * تنبيهات سند: تنبيه واحد مجدول بكل مرة (الجاي فقط)، ولمن يوصل نحسب اللي بعده.
 * المحتوى من [nextReminder]: صبح/غدا/رادار الزلّة/نوم/ترحيب بعد الغياب — بدون إزعاج بالليل.
 */
object Reminders {
    private const val CHANNEL = "sanad-coach"
    private const val REQ = 4201

    fun schedule(context: Context) {
        Graph.init(context)
        val state = Graph.store.state.value
        val t = state.targets() ?: return
        val next = nextReminder(state, t, LocalDateTime.now()) ?: return
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra("id", next.id).putExtra("title", next.title).putExtra("body", next.body)
        val pi = PendingIntent.getBroadcast(context, REQ, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val millis = next.at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        // غير دقيق عمداً (ما يحتاج صلاحية المنبهات الدقيقة) — فرق دقايق ما يضر
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
    }

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "رسائل المدرب", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "تذكير الطاقة والأكل، ورسائل اللحظات الحساسة"
                },
            )
        }
    }

    fun show(context: Context, id: String, title: String, body: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context, id.hashCode(),
            Intent(context, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra("from_reminder", id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notify)
            .setColor(0xFF34D7B8.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        try { NotificationManagerCompat.from(context).notify(id.hashCode(), n) } catch (_: SecurityException) { }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.show(
            context,
            intent.getStringExtra("id") ?: "coach",
            intent.getStringExtra("title") ?: "سند",
            intent.getStringExtra("body") ?: "",
        )
        Reminders.schedule(context)
    }
}

/** بعد إعادة تشغيل الجهاز أو تحديث التطبيق، نرجع نجدول التنبيه. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Reminders.schedule(context)
}
