package com.QuQ.yomucards

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.work.WorkManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var notificationSwitch: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)
        notificationSwitch = findViewById(R.id.notification_switch)

        notificationSwitch.isChecked = sharedPreferences.getBoolean("notifications_enabled", true)

        if (notificationSwitch.isChecked) {
            startService(Intent(this, NotificationWorker::class.java))
            val colorStateList = ContextCompat.getColorStateList(this, R.color.red2)
            notificationSwitch.trackTintList = colorStateList
        } else {
            WorkManager.getInstance(this).cancelAllWorkByTag("daily_notification")
            val colorStateList = ContextCompat.getColorStateList(this, R.color.black)
            notificationSwitch.trackTintList = colorStateList
        }

        notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("notifications_enabled", isChecked).apply()
            if (isChecked) {
                startService(Intent(this, NotificationWorker::class.java))
                val colorStateList = ContextCompat.getColorStateList(this, R.color.red2)
                notificationSwitch.trackTintList = colorStateList
            } else {
                WorkManager.getInstance(this).cancelAllWorkByTag("daily_notification")
                val colorStateList = ContextCompat.getColorStateList(this, R.color.black)
                notificationSwitch.trackTintList = colorStateList
            }
        }

        val exitButton = findViewById<ImageButton>(R.id.btnExitSettings)
        exitButton.setOnClickListener{
            finish()
        }
    }
}
