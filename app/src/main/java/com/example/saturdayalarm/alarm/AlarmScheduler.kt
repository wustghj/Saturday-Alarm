package com.example.saturdayalarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.saturdayalarm.MainActivity
import com.example.saturdayalarm.date.NextAlarmCalculator
import com.example.saturdayalarm.settings.AlarmSettings
import com.example.saturdayalarm.settings.SettingsRepository
import java.time.LocalTime
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime

object AlarmScheduler {
    private const val REQUEST_ALARM = 1201
    private const val REQUEST_SHOW = 1202

    enum class Result { SCHEDULED, DISABLED, EXACT_ALARM_ACCESS_REQUIRED, NOTIFICATIONS_REQUIRED }

    @Synchronized
    fun reschedule(context: Context, afterFiringDate: LocalDate? = null): Result {
        val appContext = context.applicationContext
        cancelMonthly(appContext)

        val settings = SettingsRepository(appContext).get()
        if (!settings.enabled) {
            SnoozeReceiver.cancel(appContext)
            return Result.DISABLED
        }
        val manager = appContext.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
            return Result.EXACT_ALARM_ACCESS_REQUIRED
        }
        if (!RingingService.notificationsAvailable(appContext)) {
            SnoozeReceiver.cancel(appContext)
            return Result.NOTIFICATIONS_REQUIRED
        }

        val zoneNow = ZonedDateTime.now()
        val next = NextAlarmCalculator.getNext(
            now = zoneNow,
            time = LocalTime.of(settings.hour, settings.minute),
            afterMonthExclusive = afterFiringDate?.let { YearMonth.from(it) },
        )
        val triggerAtMillis = next.toInstant().toEpochMilli()
        try {
            manager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAtMillis, showAlarmIntent(appContext)),
                alarmIntent(appContext, next.toLocalDate()),
            )
        } catch (_: SecurityException) {
            return Result.EXACT_ALARM_ACCESS_REQUIRED
        }
        return Result.SCHEDULED
    }

    fun cancel(context: Context) {
        val appContext = context.applicationContext
        cancelMonthly(appContext)
        SnoozeReceiver.cancel(appContext)
    }

    private fun cancelMonthly(context: Context) {
        val appContext = context.applicationContext
        val operation = PendingIntent.getBroadcast(
            appContext,
            REQUEST_ALARM,
            alarmBroadcast(appContext),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (operation != null) {
            appContext.getSystemService(AlarmManager::class.java).cancel(operation)
            operation.cancel()
        }
    }

    fun settings(context: Context): AlarmSettings = SettingsRepository(context).get()

    private fun alarmIntent(context: Context, occurrenceDate: LocalDate): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_ALARM,
        alarmBroadcast(context).putExtra(AlarmReceiver.EXTRA_OCCURRENCE_DATE, occurrenceDate.toString()),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun alarmBroadcast(context: Context): Intent =
        Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_FIRE)

    private fun showAlarmIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            REQUEST_SHOW,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
