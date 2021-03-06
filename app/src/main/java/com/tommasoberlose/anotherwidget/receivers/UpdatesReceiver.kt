package com.tommasoberlose.anotherwidget.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.AlarmManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import com.tommasoberlose.anotherwidget.db.EventRepository
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.helpers.CalendarHelper
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import org.joda.time.Period
import java.util.*


class UpdatesReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            Actions.ACTION_CALENDAR_UPDATE -> CalendarHelper.updateEventList(context)

            "com.sec.android.widgetapp.APPWIDGET_RESIZE",
            Intent.ACTION_DATE_CHANGED,
            AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED,
            Actions.ACTION_TIME_UPDATE -> {
                MainWidget.updateWidget(context)
            }
        }
    }

    companion object {

        fun setUpdates(context: Context) {
            removeUpdates(context)


            val eventRepository = EventRepository(context)
            with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
                eventRepository.getEvents().forEach { event ->
                    val now = Calendar.getInstance().apply {
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val diff = Period(now.timeInMillis, event.startDate)
                    if (event.startDate > now.timeInMillis) {
                        // Update the widget every hour till the event
                        (0..diff.hours).forEach {
                            setExactAndAllowWhileIdle(
                                AlarmManager.RTC,
                                if (event.startDate - it * 1000 * 60 * 60 > 60 * 1000) event.startDate - it * 1000 * 60 * 60 else now.timeInMillis + 120000,
                                PendingIntent.getBroadcast(
                                    context,
                                    event.eventID.toInt() + it,
                                    Intent(context, UpdatesReceiver::class.java).apply {
                                        action = Actions.ACTION_TIME_UPDATE
                                    },
                                    0
                                )
                            )
                        }
                    }

                    // Update the widget one second after the event is finished
                    setExactAndAllowWhileIdle(
                        AlarmManager.RTC,
                        if (event.endDate > 60 *1000) event.endDate else now.timeInMillis + 120000,
                        PendingIntent.getBroadcast(context, 1, Intent(context, UpdatesReceiver::class.java).apply { action = Actions.ACTION_TIME_UPDATE }, 0)
                    )
                }
            }
        }

        fun removeUpdates(context: Context) {
            with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
                cancel(PendingIntent.getBroadcast(context, 1, Intent(context, UpdatesReceiver::class.java), 0))
                EventRepository(context).getEvents().forEach {
                    (0..24).forEach { hour ->
                        cancel(PendingIntent.getBroadcast(context, it.eventID.toInt() * hour, Intent(context, UpdatesReceiver::class.java), 0))
                    }
                }
            }
        }
    }
}
