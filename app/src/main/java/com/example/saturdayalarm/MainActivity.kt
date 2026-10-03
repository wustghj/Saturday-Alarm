package com.example.saturdayalarm

import android.Manifest
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.saturdayalarm.alarm.AlarmScheduler
import com.example.saturdayalarm.alarm.RingingService
import com.example.saturdayalarm.settings.AlarmSettings
import com.example.saturdayalarm.settings.SettingsRepository
import com.example.saturdayalarm.ui.AlarmHomeScreen
import com.example.saturdayalarm.ui.FirstLaunchPermissionDialog
import com.example.saturdayalarm.ui.HomeState
import com.example.saturdayalarm.ui.theme.SaturdayAlarmTheme
import java.time.ZonedDateTime

class MainActivity : ComponentActivity() {
    private lateinit var repository: SettingsRepository
    private var homeState by mutableStateOf(
        HomeState(
            settings = AlarmSettings(),
            exactAlarmAllowed = false,
            notificationsAllowed = false,
            fullScreenAllowed = false,
            now = ZonedDateTime.now(),
        ),
    )
    private var showFirstLaunchPermissionDialog by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = SettingsRepository(this)
        RingingService.ensureNotificationChannel(this)
        enableEdgeToEdge()
        refreshState()
        updateFirstLaunchPermissionState()

        setContent {
            SaturdayAlarmTheme {
                AlarmHomeScreen(
                    state = homeState,
                    onEnabledChange = { enabled -> updateSettings(repository.get().copy(enabled = enabled)) },
                    onVibrateChange = { vibrate -> updateSettings(repository.get().copy(vibrate = vibrate)) },
                    onTimeClick = ::showTimePicker,
                    onLabelChange = { label ->
                        updateSettings(repository.get().copy(label = label.take(80)))
                    },
                    onRequestExactAlarm = ::requestExactAlarmAccess,
                    onRequestNotifications = ::openNotificationSettings,
                    onRequestFullScreen = ::requestFullScreenAccess,
                )
                if (showFirstLaunchPermissionDialog) {
                    FirstLaunchPermissionDialog(
                        state = homeState,
                        onRequestExactAlarm = ::requestExactAlarmAccess,
                        onRequestNotifications = ::requestNotificationsAccess,
                        onRequestFullScreen = ::requestFullScreenAccess,
                        onComplete = ::completeFirstLaunchPermissionFlow,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::repository.isInitialized) {
            if (repository.get().enabled) AlarmScheduler.reschedule(this)
            refreshState()
            updateFirstLaunchPermissionState()
        }
    }

    @Deprecated("Deprecated in Android, kept for the platform permission callback")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATIONS) {
            if (repository.get().enabled) AlarmScheduler.reschedule(this)
            refreshState()
            updateFirstLaunchPermissionState()
        }
    }

    private fun refreshState() {
        val manager = getSystemService(AlarmManager::class.java)
        val notificationsAllowed = RingingService.notificationsAvailable(this)
        homeState = HomeState(
            settings = repository.get(),
            exactAlarmAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms(),
            notificationsAllowed = notificationsAllowed,
            fullScreenAllowed = Build.VERSION.SDK_INT < 34 ||
                getSystemService(android.app.NotificationManager::class.java).canUseFullScreenIntent(),
            now = ZonedDateTime.now(),
        )
    }

    private fun updateFirstLaunchPermissionState() {
        if (repository.hasCompletedFirstLaunchPermissionFlow()) {
            showFirstLaunchPermissionDialog = false
            return
        }

        val permissionsReady = homeState.exactAlarmAllowed &&
            homeState.notificationsAllowed &&
            homeState.fullScreenAllowed
        if (permissionsReady) {
            repository.markFirstLaunchPermissionFlowCompleted()
            showFirstLaunchPermissionDialog = false
        } else {
            showFirstLaunchPermissionDialog = true
        }
    }

    private fun completeFirstLaunchPermissionFlow() {
        repository.markFirstLaunchPermissionFlowCompleted()
        showFirstLaunchPermissionDialog = false
    }

    private fun updateSettings(settings: AlarmSettings) {
        repository.save(settings)
        if (!settings.enabled) stopService(Intent(this, RingingService::class.java))
        AlarmScheduler.reschedule(this)
        refreshState()
        if (settings.enabled) requestNotificationPermissionIfNeeded()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
        }
    }

    private fun requestNotificationsAccess() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermissionIfNeeded()
        } else {
            openNotificationSettings()
        }
    }

    private fun requestExactAlarmAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    .setData(Uri.parse("package:$packageName")),
            )
        }
    }

    private fun requestFullScreenAccess() {
        if (Build.VERSION.SDK_INT >= 34) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                    .setData(Uri.parse("package:$packageName")),
            )
        }
    }

    private fun openNotificationSettings() {
        startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
        )
    }

    private fun showTimePicker() {
        val current = repository.get()
        TimePickerDialog(
            this,
            { _, hour, minute -> updateSettings(repository.get().copy(hour = hour, minute = minute)) },
            current.hour,
            current.minute,
            true,
        ).show()
    }

    companion object {
        private const val REQUEST_NOTIFICATIONS = 91
    }
}
