package pl.wsei.pam.lab06

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import pl.wsei.pam.lab01.R

class NotificationHandler(private val context: Context) {
    private val notificationManager =
        context.getSystemService(NotificationManager::class.java)

    fun showSimpleNotification() {
        val notification = NotificationCompat.Builder(context, channelID)
            .setContentTitle("Proste powiadomienie")
            .setContentText("Tekst powiadomienia")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(notificationID, notification)
    }
}
