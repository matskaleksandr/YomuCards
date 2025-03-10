package com.QuQ.yomucards

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews

class MyWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val ACTION_CLICK = "CLICK_WIDGET"
        private const val PREF_NAME = "ClickCounterPrefs"
        private const val COUNT_KEY = "click_count"
        private const val DATA_KEY = "cached_data"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, frame = 1, cachedData = null)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_CLICK) {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val count = prefs.getInt(COUNT_KEY, 0) + 1
            prefs.edit().putInt(COUNT_KEY, count).apply()

            // Запрашиваем данные из БД только один раз
            val dbHelp = DatabaseWidgetHelper(context)
            val dbOutput = dbHelp.getRandomCharacter(context)

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = intent.component
            componentName?.let {
                val appWidgetIds = appWidgetManager.getAppWidgetIds(it)
                for (appWidgetId in appWidgetIds) {
                    updateWidget(context, appWidgetManager, appWidgetId, frame = 1, cachedData = dbOutput)

                    // Через 500 мс смена на второй кадр
                    Handler(Looper.getMainLooper()).postDelayed({
                        updateWidget(context, appWidgetManager, appWidgetId, frame = 2, cachedData = dbOutput)
                    }, 100)
                }
            }
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, frame: Int, cachedData: String?) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt(COUNT_KEY, 0)

        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        // Используем переданные данные, если они есть, иначе загружаем из БД
        val dbOutput = cachedData ?: DatabaseWidgetHelper(context).getRandomCharacter(context)

        views.setTextViewText(R.id.widget_text, "$count $dbOutput")

        val imageRes = if (frame == 1) R.drawable.cat2 else R.drawable.cat1
        views.setImageViewResource(R.id.widget_gif, imageRes)

        val intent = Intent(context, MyWidgetProvider::class.java).apply {
            action = ACTION_CLICK
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_gif, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
