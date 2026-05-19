package com.example.coinflow.Helper

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.coinflow.Activity.LoginActivity
import com.example.coinflow.R

class PlanExpirationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = PrefsManager(context)

        // Перевіряємо, чи це попередження за 5 днів
        val isWarning = intent.getBooleanExtra("IS_WARNING", false)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "plan_expiration_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Підписка CoinFlow", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val loginIntent = Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, loginIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title: String
        val message: String
        val notificationId: Int

        if (isWarning) {
            // ЛОГІКА ПОПЕРЕДЖЕННЯ
            title = "CoinFlow: Увага!"
            message = "Термін дії вашої підписки сплине через 5 днів. Не забудьте подовжити її!"
            notificationId = 1002 // Окремий ID для повідомлення
        } else {
            // ЛОГІКА ЗАКІНЧЕННЯ
            prefs.setCurrentPlan("BRONZE")
            prefs.setPlanExpiration(0L)

            title = "CoinFlow: Підписка"
            message = "Ваш преміальний план скінчився! Будь ласка, оновіть підписку."
            notificationId = 1001
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.wallet) // Іконка гаманця
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}