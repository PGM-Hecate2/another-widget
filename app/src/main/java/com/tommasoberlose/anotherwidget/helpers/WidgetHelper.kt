package com.tommasoberlose.anotherwidget.helpers

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tommasoberlose.anotherwidget.db.EventRepository
import com.tommasoberlose.anotherwidget.global.Preferences

object WidgetHelper {
    class WidgetSizeProvider(
        private val context: Context,
        private val appWidgetManager: AppWidgetManager
    ) {

        fun getWidgetsSize(widgetId: Int): Pair<Int, Int> {
            val isPortrait = context.resources.configuration.orientation == ORIENTATION_PORTRAIT
            val width = getWidgetWidth(isPortrait, widgetId)
            val height = getWidgetHeight(isPortrait, widgetId)
            val widthInPx = context.dip(width)
            val heightInPx = context.dip(height)
            FirebaseCrashlytics.getInstance().setCustomKey("widthInPx", widthInPx)
            FirebaseCrashlytics.getInstance().setCustomKey("heightInPx", heightInPx)
            return widthInPx to heightInPx
        }

        private fun getWidgetWidth(isPortrait: Boolean, widgetId: Int): Int =
            if (isPortrait) {
                getWidgetSizeInDp(widgetId, AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            } else {
                getWidgetSizeInDp(widgetId, AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
            }

        private fun getWidgetHeight(isPortrait: Boolean, widgetId: Int): Int =
            if (isPortrait) {
                getWidgetSizeInDp(widgetId, AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
            } else {
                getWidgetSizeInDp(widgetId, AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            }

        private fun getWidgetSizeInDp(widgetId: Int, key: String): Int =
            appWidgetManager.getAppWidgetOptions(widgetId).getInt(key, 0)

        private fun Context.dip(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    }

    fun Pair<Int, Int>.reduceDimensionWithMaxWidth(width: Int): Pair<Int, Int> {
        return if (first < width) {
            this
        } else {
            val factor = width / first
            width to second * factor
        }
    }

    fun showSpecialWeather(context: Context): Boolean {
        return EventRepository(context).getEventsCount() == 0 && (
                (Preferences.showNextAlarm && AlarmHelper.getNextAlarm(context) != "") ||
                (MediaPlayerHelper.isSomeonePlaying(context))
            )
    }
}