package com.example.saturdayalarm.alarm

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.saturdayalarm.R
import com.example.saturdayalarm.settings.SettingsRepository

class RingingService : Service() {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_STOP -> {
                stopRinging()
                START_NOT_STICKY
            }
            ACTION_SNOOZE -> {
                scheduleSnooze()
                stopRinging()
                START_NOT_STICKY
            }
            ACTION_START -> {
                val startIntent = intent ?: return START_NOT_STICKY
                if (!SettingsRepository(this).get().enabled || !notificationsAvailable(this)) {
                    stopSelf(startId)
                    return START_NOT_STICKY
                }
                startRinging(
                    label = startIntent.getStringExtra(EXTRA_LABEL)
                        ?: getString(R.string.default_alarm_label),
                    vibrate = startIntent.getBooleanExtra(EXTRA_VIBRATE, true),
                )
                START_REDELIVER_INTENT
            }
            else -> {
                stopSelf(startId)
                START_NOT_STICKY
            }
        }
    }

    override fun onDestroy() {
        releasePlayback()
        super.onDestroy()
    }

    private fun startRinging(label: String, vibrate: Boolean) {
        ensureNotificationChannel(this)
        val notification = buildNotification(label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        releasePlayback()
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        if (alarmUri == null) {
            stopRinging()
            return
        }
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(this@RingingService, alarmUri)
                isLooping = true
                setOnPreparedListener { it.start() }
                setOnErrorListener { _, _, _ ->
                    stopRinging()
                    true
                }
                prepareAsync()
            }
        } catch (_: Exception) {
            stopRinging()
            return
        }
        if (vibrate && SettingsRepository(this).get().vibrate) startVibration()
    }

    private fun buildNotification(label: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            REQUEST_RING_SCREEN,
            Intent(this, RingingActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(label)
            .setContentText(getString(R.string.alarm_is_ringing))
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(contentIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.stop_alarm),
                serviceAction(ACTION_STOP, REQUEST_STOP),
            )
            .addAction(
                android.R.drawable.ic_lock_idle_alarm,
                getString(R.string.snooze_alarm),
                serviceAction(ACTION_SNOOZE, REQUEST_SNOOZE),
            )

        val canUseFullScreen = Build.VERSION.SDK_INT < 34 ||
            getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        if (canUseFullScreen) builder.setFullScreenIntent(contentIntent, true)
        return builder.build()
    }

    private fun serviceAction(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this,
            requestCode,
            Intent(this, RingingService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 700, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun scheduleSnooze() {
        if (!SettingsRepository(this).get().enabled || !notificationsAvailable(this)) return
        val manager = getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) return

        val operation = PendingIntent.getBroadcast(
            this,
            SnoozeReceiver.REQUEST_CODE,
            Intent(this, SnoozeReceiver::class.java).setAction(ACTION_FIRE_SNOOZE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val showIntent = PendingIntent.getActivity(
            this,
            REQUEST_RING_SCREEN,
            Intent(this, RingingActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val triggerAt = System.currentTimeMillis() + SNOOZE_MILLIS
        try {
            manager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showIntent), operation)
        } catch (_: SecurityException) {
            operation.cancel()
        }
    }

    private fun stopRinging() {
        releasePlayback()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releasePlayback() {
        vibrator?.cancel()
        vibrator = null
        player?.let { activePlayer ->
            runCatching { if (activePlayer.isPlaying) activePlayer.stop() }
            runCatching { activePlayer.reset() }
            runCatching { activePlayer.release() }
        }
        player = null
    }

    companion object {
        const val CHANNEL_ID = "monthly_alarm"
        const val ACTION_START = "com.example.saturdayalarm.action.START_RINGING"
        const val ACTION_STOP = "com.example.saturdayalarm.action.STOP_RINGING"
        const val ACTION_SNOOZE = "com.example.saturdayalarm.action.SNOOZE_RINGING"
        const val EXTRA_LABEL = "label"
        const val EXTRA_VIBRATE = "vibrate"
        const val ACTION_FIRE_SNOOZE = "com.example.saturdayalarm.action.FIRE_SNOOZE"

        fun notificationsAvailable(context: Context): Boolean {
            val manager = context.getSystemService(NotificationManager::class.java)
            if (!manager.areNotificationsEnabled()) return false
            if (Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                manager.getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE
            ) return false
            return true
        }

        fun ensureNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.alarm_notification_channel),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.alarm_notification_channel_description)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        private const val NOTIFICATION_ID = 71
        private const val REQUEST_RING_SCREEN = 72
        private const val REQUEST_STOP = 73
        private const val REQUEST_SNOOZE = 74
        private const val SNOOZE_MILLIS = 10 * 60 * 1000L
    }
}
