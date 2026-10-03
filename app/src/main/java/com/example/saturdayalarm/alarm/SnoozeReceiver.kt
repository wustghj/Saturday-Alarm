package com.example.saturdayalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Build
import com.example.saturdayalarm.settings.SettingsRepository

class SnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != RingingService.ACTION_FIRE_SNOOZE) return
        val settings = SettingsRepository(context).get()
        if (!settings.enabled || !RingingService.notificationsAvailable(context)) return

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
        fun cancel(context: Context) {
            val appContext = context.applicationContext
            val operation = PendingIntent.getBroadcast(
                appContext,
                REQUEST_CODE,
                Intent(appContext, SnoozeReceiver::class.java)
                    .setAction(RingingService.ACTION_FIRE_SNOOZE),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            ) ?: return
            appContext.getSystemService(AlarmManager::class.java).cancel(operation)
            operation.cancel()
        }

        const val REQUEST_CODE = 75
    }
}
