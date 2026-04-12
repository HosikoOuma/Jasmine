package com.nkds.hosikoouma.jasmine

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews

class PlayerWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val intent = Intent(context, PlaybackService::class.java).apply {
            action = "ACTION_WIDGET_UPDATE_REQUEST"
        }
        context.startService(intent)
    }

    companion object {
        fun updateWidget(
            context: Context,
            title: String,
            artist: String,
            isPlaying: Boolean,
            albumArt: Bitmap?,
            backgroundColor: Int? = null
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val views = RemoteViews(context.packageName, R.layout.player_widget)

            views.setTextViewText(R.id.widget_title, title)
            views.setTextViewText(R.id.widget_artist, artist)

            // Установка обложки
            if (albumArt != null) {
                views.setImageViewBitmap(R.id.widget_album_art, albumArt)
            } else {
                views.setImageViewResource(R.id.widget_album_art, R.drawable.ison_vec)
            }

            // Установка иконки Play/Pause
            views.setImageViewResource(
                R.id.widget_play_pause,
                if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
            )

            // Динамический цвет фона через подложку (сохраняем скругления)
            backgroundColor?.let {
                views.setInt(R.id.widget_background_view, "setColorFilter", it)
            } ?: run {
                views.setInt(R.id.widget_background_view, "setColorFilter", android.graphics.Color.TRANSPARENT)
            }

            setupPendingIntents(context, views)

            val componentName = ComponentName(context, PlayerWidget::class.java)
            appWidgetManager.updateAppWidget(componentName, views)
        }

        private fun setupPendingIntents(context: Context, views: RemoteViews) {
            val playPauseIntent = Intent(context, PlaybackService::class.java).apply { action = "ACTION_WIDGET_PLAY_PAUSE" }
            val nextIntent = Intent(context, PlaybackService::class.java).apply { action = "ACTION_WIDGET_NEXT" }
            val prevIntent = Intent(context, PlaybackService::class.java).apply { action = "ACTION_WIDGET_PREV" }
            
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("OPEN_PLAYER", true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

            views.setOnClickPendingIntent(R.id.widget_play_pause, PendingIntent.getService(context, 1, playPauseIntent, flags))
            views.setOnClickPendingIntent(R.id.widget_next, PendingIntent.getService(context, 2, nextIntent, flags))
            views.setOnClickPendingIntent(R.id.widget_prev, PendingIntent.getService(context, 3, prevIntent, flags))
            
            val openAppPI = PendingIntent.getActivity(context, 0, mainIntent, flags)
            views.setOnClickPendingIntent(R.id.widget_album_art, openAppPI)
            views.setOnClickPendingIntent(R.id.text_container, openAppPI)
            views.setOnClickPendingIntent(R.id.widget_root, openAppPI)
        }
    }
}
