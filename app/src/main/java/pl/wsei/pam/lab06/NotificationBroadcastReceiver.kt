package pl.wsei.pam.lab06

import android.app.*
import android.content.*
import androidx.core.app.NotificationCompat

class NotificationBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val title = intent?.getStringExtra(titleExtra) ?: "Zadanie"
        val message = intent?.getStringExtra(messageExtra) ?: "Zbliża się termin!"

        // 1. Wyświetl powiadomienie
        val notification = NotificationCompat.Builder(context, channelID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationID, notification)

        // 2. Zaplanuj powtórzenie za 4 godziny (logika Lab 8)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nextTime = System.currentTimeMillis() + (4 * 60 * 60 * 1000L)

        val nextIntent = Intent(context, NotificationBroadcastReceiver::class.java).apply {
            putExtra(titleExtra, title)
            putExtra(messageExtra, message)
        }

        val pendingIntent = PendingIntent.getBroadcast(context, notificationID, nextIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTime, pendingIntent)
    }
}