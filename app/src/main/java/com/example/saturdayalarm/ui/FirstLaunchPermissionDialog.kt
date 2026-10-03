package com.example.saturdayalarm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.saturdayalarm.R

private enum class RequiredPermission {
    EXACT_ALARM,
    NOTIFICATIONS,
    FULL_SCREEN,
}

@Composable
fun FirstLaunchPermissionDialog(
    state: HomeState,
    onRequestExactAlarm: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestFullScreen: () -> Unit,
    onComplete: () -> Unit,
) {
    val missingPermissions = buildList {
        if (!state.exactAlarmAllowed) add(RequiredPermission.EXACT_ALARM)
        if (!state.notificationsAllowed) add(RequiredPermission.NOTIFICATIONS)
        if (!state.fullScreenAllowed) add(RequiredPermission.FULL_SCREEN)
    }
    val nextPermission = missingPermissions.firstOrNull()

    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.first_launch_permissions_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.first_launch_permissions_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
                missingPermissions.forEach { permission ->
                    Text(
                        text = stringResource(permission.descriptionRes()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (nextPermission) {
                        RequiredPermission.EXACT_ALARM -> onRequestExactAlarm()
                        RequiredPermission.NOTIFICATIONS -> onRequestNotifications()
                        RequiredPermission.FULL_SCREEN -> onRequestFullScreen()
                        null -> onComplete()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(
                        if (nextPermission == null) R.string.first_launch_permissions_done
                        else R.string.first_launch_permissions_continue,
                    ),
                )
            }
        },
    )
}

private fun RequiredPermission.descriptionRes(): Int = when (this) {
    RequiredPermission.EXACT_ALARM -> R.string.exact_alarm_permission_hint
    RequiredPermission.NOTIFICATIONS -> R.string.notification_permission_hint
    RequiredPermission.FULL_SCREEN -> R.string.full_screen_permission_hint
}
