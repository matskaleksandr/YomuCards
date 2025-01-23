package com.QuQ.yomucards

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast

class MyWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val ACTION_CLICK = "CLICK_WIDGET"
        private const val PREF_NAME = "ClickCounterPrefs"
        private const val COUNT_KEY = "click_count"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_CLICK) {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val count = prefs.getInt(COUNT_KEY, 0) + 1  // Increment click counter
            prefs.edit().putInt(COUNT_KEY, count).apply()

            Toast.makeText(context, "Clicks: $count", Toast.LENGTH_SHORT).show()

            // Update all widgets
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = intent.component
            componentName?.let {
                val appWidgetIds = appWidgetManager.getAppWidgetIds(it)
                for (appWidgetId in appWidgetIds) {
                    updateWidget(context, appWidgetManager, appWidgetId)
                }
            }
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt(COUNT_KEY, 0)

        val views = RemoteViews(context.packageName, R.layout.widget_layout)
        views.setTextViewText(R.id.widget_text, "Clicks: $count")

        // Setup click listener for the button
        val intent = Intent(context, MyWidgetProvider::class.java).apply {
            action = ACTION_CLICK
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_button, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
