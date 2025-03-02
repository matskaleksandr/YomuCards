package com.QuQ.yomucards

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class NotificationWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        sendNotification()
        return Result.success()
    }

    private fun sendNotification() {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "daily_notification",
                "Ежедневные уведомления",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Создайте уведомление
        val notification = NotificationCompat.Builder(applicationContext, "daily_notification")
            .setContentTitle("Пора учить японский!")
            .setContentText("Время проходить новые уроки")
            .setSmallIcon(R.drawable.yo)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // Покажите уведомление
        notificationManager.notify(1, notification)
    }
}
