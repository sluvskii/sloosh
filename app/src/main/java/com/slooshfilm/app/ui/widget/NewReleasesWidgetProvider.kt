package com.slooshfilm.app.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.slooshfilm.app.MainActivity
import com.slooshfilm.app.R

class NewReleasesWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.new_releases_widget_layout)

        // Intent для запуска сервиса, который поставляет данные для списка
        val serviceIntent = Intent(context, NewReleasesWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME))
        }

        views.setRemoteAdapter(R.id.widget_list, serviceIntent)
        views.setEmptyView(R.id.widget_list, R.id.widget_empty_view)

        // Intent-шаблон для элементов списка
        val itemClickIntent = Intent(context, MainActivity::class.java) // Предполагаем, что клик открывает MainActivity
        val itemClickPendingIntent = PendingIntent.getActivity(context, 0, itemClickIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setPendingIntentTemplate(R.id.widget_list, itemClickPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
