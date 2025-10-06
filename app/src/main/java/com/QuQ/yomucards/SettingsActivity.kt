package com.QuQ.yomucards

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class SettingsActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var notificationSwitch: SwitchCompat
    private lateinit var workManager: WorkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)
        notificationSwitch = findViewById(R.id.notification_switch)
        workManager = WorkManager.getInstance(this)

        // Устанавливаем начальное состояние из SharedPreferences
        notificationSwitch.isChecked = sharedPreferences.getBoolean("notifications_enabled", true)
        updateSwitchAppearance(notificationSwitch.isChecked)

        // Проверяем реальное состояние WorkManager
        checkNotificationStatus()

        notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("notifications_enabled", isChecked).apply()
            if (isChecked) {
                scheduleDailyNotification()
            } else {
                cancelDailyNotification()
            }
            updateSwitchAppearance(isChecked)
        }

        val exitButton = findViewById<ImageButton>(R.id.btnExitSettings)
        exitButton.setOnClickListener{
            finish()
        }
    }

    private fun checkNotificationStatus() {
        val future = workManager.getWorkInfosByTag("daily_notification")
        future.addListener({
            val workInfos = future.get()
            val hasScheduledWork = workInfos.any { workInfo ->
                workInfo.state == WorkInfo.State.ENQUEUED || workInfo.state == WorkInfo.State.RUNNING
            }

            // Синхронизируем switch с реальным состоянием
            if (notificationSwitch.isChecked != hasScheduledWork) {
                notificationSwitch.isChecked = hasScheduledWork
                updateSwitchAppearance(hasScheduledWork)
            }
        }, ContextCompat.getMainExecutor(this))
    }


    private fun scheduleDailyNotification() {
        val dailyWorkRequest = PeriodicWorkRequest.Builder(
            NotificationWorker::class.java,
            1, TimeUnit.DAYS
        )
            .setInitialDelay(1, TimeUnit.HOURS)
            .addTag("daily_notification")
            .build()

        workManager.enqueue(dailyWorkRequest)
        Log.d("Notifications", "Daily notification scheduled")
    }

    private fun cancelDailyNotification() {
        workManager.cancelAllWorkByTag("daily_notification")
        Log.d("Notifications", "Daily notification cancelled")
    }

    private fun updateSwitchAppearance(isEnabled: Boolean) {
        val colorRes = if (isEnabled) R.color.red2 else R.color.black
        val colorStateList = ContextCompat.getColorStateList(this, colorRes)
        notificationSwitch.trackTintList = colorStateList
    }
}
