package com.example.saturdayalarm.settings

import android.content.Context

class SettingsRepository(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun get(): AlarmSettings = AlarmSettings(
        enabled = preferences.getBoolean(KEY_ENABLED, false),
        hour = preferences.getInt(KEY_HOUR, AlarmSettings.DEFAULT_HOUR).coerceIn(0, 23),
        minute = preferences.getInt(KEY_MINUTE, AlarmSettings.DEFAULT_MINUTE).coerceIn(0, 59),
        vibrate = preferences.getBoolean(KEY_VIBRATE, true),
        label = preferences.getString(KEY_LABEL, AlarmSettings.DEFAULT_LABEL)
            ?.take(MAX_LABEL_LENGTH) ?: AlarmSettings.DEFAULT_LABEL,
    )

    fun save(settings: AlarmSettings) {
        preferences.edit()
            .putBoolean(KEY_ENABLED, settings.enabled)
            .putInt(KEY_HOUR, settings.hour)
            .putInt(KEY_MINUTE, settings.minute)
            .putBoolean(KEY_VIBRATE, settings.vibrate)
            .putString(KEY_LABEL, settings.label.take(MAX_LABEL_LENGTH))
            .apply()
    }

    fun hasCompletedFirstLaunchPermissionFlow(): Boolean =
        preferences.getBoolean(KEY_FIRST_LAUNCH_PERMISSION_FLOW_COMPLETED, false)

    fun markFirstLaunchPermissionFlowCompleted() {
        preferences.edit()
            .putBoolean(KEY_FIRST_LAUNCH_PERMISSION_FLOW_COMPLETED, true)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "alarm_settings"
        const val KEY_ENABLED = "enabled"
        const val KEY_HOUR = "hour"
        const val KEY_MINUTE = "minute"
        const val KEY_VIBRATE = "vibrate"
        const val KEY_LABEL = "label"
        const val KEY_FIRST_LAUNCH_PERMISSION_FLOW_COMPLETED = "first_launch_permission_flow_completed"
        const val MAX_LABEL_LENGTH = 80
    }
}
