package com.example.saturdayalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.saturdayalarm.settings.SettingsRepository
import java.time.LocalDate

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return

        val settings = SettingsRepository(context).get()
        if (!settings.enabled) return

        // Register next month's occurrence before starting playback so killing the playback
        // process cannot leave the recurring schedule without a future alarm.
        val firedDate = intent.getStringExtra(EXTRA_OCCURRENCE_DATE)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        AlarmScheduler.reschedule(context, afterFiringDate = firedDate)
        if (!RingingService.notificationsAvailable(context)) return

        val ringIntent = Intent(context, RingingService::class.java)
            .setAction(RingingService.ACTION_START)
            .putExtra(RingingService.EXTRA_LABEL, settings.label)
            .putExtra(RingingService.EXTRA_VIBRATE, settings.vibrate)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(ringIntent)
        } else {
            context.startService(ringIntent)
        }
    }

    companion object {
        const val ACTION_FIRE = "com.example.saturdayalarm.action.FIRE_MONTH_END_ALARM"
        const val EXTRA_OCCURRENCE_DATE = "occurrence_date"
    }
}
