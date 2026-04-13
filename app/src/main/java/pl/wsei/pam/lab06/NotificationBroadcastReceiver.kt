package pl.wsei.pam.lab06

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import pl.wsei.pam.lab01.R

class NotificationBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {

        val title = intent?.getStringExtra(titleExtra) ?: "Zadanie"
        val message = intent?.getStringExtra(messageExtra) ?: "Zbliża się termin!"

        val notification = NotificationCompat.Builder(context, channelID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationID, notification)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val nextTime = System.currentTimeMillis() + (4 * 60 * 60 * 1000L)

        val nextIntent = Intent(context, NotificationBroadcastReceiver::class.java).apply {
            putExtra(titleExtra, title)
            putExtra(messageExtra, message)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationID,
            nextIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextTime,
            pendingIntent
        )
    }
}