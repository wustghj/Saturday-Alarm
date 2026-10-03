package com.example.saturdayalarm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saturdayalarm.R
import com.example.saturdayalarm.date.LastSaturdayCalculator
import com.example.saturdayalarm.date.NextAlarmCalculator
import com.example.saturdayalarm.settings.AlarmSettings
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class HomeState(
    val settings: AlarmSettings,
    val exactAlarmAllowed: Boolean,
    val notificationsAllowed: Boolean,
    val fullScreenAllowed: Boolean,
    val now: ZonedDateTime,
)

@Composable
fun AlarmHomeScreen(
    state: HomeState,
    onEnabledChange: (Boolean) -> Unit,
    onVibrateChange: (Boolean) -> Unit,
    onTimeClick: () -> Unit,
    onLabelChange: (String) -> Unit,
    onRequestExactAlarm: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestFullScreen: () -> Unit,
) {
    val settings = state.settings
    var showLabelDialog by remember { mutableStateOf(false) }
    var editedLabel by remember(settings.label) { mutableStateOf(settings.label) }
    val timeText = LocalTime.of(settings.hour, settings.minute)
        .format(DateTimeFormatter.ofPattern("HH:mm"))
    val switchDescription = stringResource(
        if (settings.enabled) R.string.alarm_switch_on_description else R.string.alarm_switch_off_description,
    )
    val upcoming = remember(settings.enabled, settings.hour, settings.minute, state.now) {
        if (!settings.enabled) emptyList()
        else upcomingDates(state.now, timeText)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
    ) { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets),
            contentPadding = PaddingValues(start = 20.dp, top = 14.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.home_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.enable_alarm),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = if (settings.enabled) stringResource(R.string.alarm_enabled_caption)
                                else stringResource(R.string.alarm_disabled_caption),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = settings.enabled,
                            onCheckedChange = onEnabledChange,
                            modifier = Modifier.semantics {
                                contentDescription = switchDescription
                            },
                        )
                    }
                }
            }
            item {
                NextAlarmCard(settings = settings, timeText = timeText, now = state.now)
            }
            if (settings.enabled) {
                item {
                    SectionHeader(
                        title = stringResource(R.string.future_alarms),
                        supporting = stringResource(R.string.future_alarms_caption),
                    )
                }
                items(upcoming) { entry ->
                    ScheduleRow(entry)
                }
            }
            item {
                SectionHeader(
                    title = stringResource(R.string.alarm_settings_title),
                    supporting = stringResource(R.string.settings_caption),
                )
            }
            item {
                SettingCard(
                    title = stringResource(R.string.time_setting),
                    value = timeText,
                    onClick = onTimeClick,
                )
            }
            item {
                Card(shape = RoundedCornerShape(22.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.vibration), style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (settings.vibrate) stringResource(R.string.setting_on)
                                else stringResource(R.string.setting_off),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = settings.vibrate, onCheckedChange = onVibrateChange)
                    }
                }
            }
            item {
                SettingCard(
                    title = stringResource(R.string.alarm_label_title),
                    value = settings.label,
                    onClick = { showLabelDialog = true },
                )
            }
            item {
                PermissionCard(
                    state = state,
                    onRequestExactAlarm = onRequestExactAlarm,
                    onRequestNotifications = onRequestNotifications,
                    onRequestFullScreen = onRequestFullScreen,
                )
            }
        }
    }

    if (showLabelDialog) {
        AlertDialog(
            onDismissRequest = { showLabelDialog = false },
            title = { Text(stringResource(R.string.alarm_label_title)) },
            text = {
                TextField(
                    value = editedLabel,
                    onValueChange = { editedLabel = it.take(80) },
                    singleLine = true,
                    label = { Text(stringResource(R.string.alarm_label_hint)) },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onLabelChange(editedLabel.trim().ifBlank { AlarmSettings.DEFAULT_LABEL })
                    showLabelDialog = false
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showLabelDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun NextAlarmCard(settings: AlarmSettings, timeText: String, now: ZonedDateTime) {
    val enabled = settings.enabled
    val next = remember(settings.enabled, settings.hour, settings.minute, now) {
        if (enabled) NextAlarmCalculator.getNext(
            now,
            LocalTime.of(settings.hour, settings.minute),
        ) else null
    }
    val date = next?.format(DateTimeFormatter.ofPattern("M月d日", Locale.CHINA))
    val weekday = next?.format(DateTimeFormatter.ofPattern("EEEE", Locale.CHINA))

    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    ),
                )
                .padding(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.next_alarm_heading),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                if (next == null) {
                    Text(
                        text = stringResource(R.string.alarm_disabled),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = stringResource(R.string.enable_to_schedule_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.next_alarm_month_date, next.year, date.orEmpty()),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoPill(weekday.orEmpty())
                        InfoPill(timeText)
                    }
                    Text(
                        text = settings.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoPill(value: String) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)) {
        Text(
            text = value,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SectionHeader(title: String, supporting: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private data class ScheduleEntry(val date: String, val weekday: String, val time: String)

private fun upcomingDates(now: ZonedDateTime, time: String): List<ScheduleEntry> {
    val next = NextAlarmCalculator.getNext(now, LocalTime.parse(time))
    return (0..2).map { offset ->
        val month = YearMonth.from(next).plusMonths(offset.toLong())
        val date = if (offset == 0) next.toLocalDate()
        else LastSaturdayCalculator.getLastSaturday(month.year, month.monthValue)
        ScheduleEntry(
            date = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA)),
            weekday = date.format(DateTimeFormatter.ofPattern("EEEE", Locale.CHINA)),
            time = time,
        )
    }
}

@Composable
private fun ScheduleRow(entry: ScheduleEntry) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                    Text("六", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Text(entry.date, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(entry.weekday, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(entry.time, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SettingCard(title: String, value: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(3.dp))
                Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", fontSize = 28.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PermissionCard(
    state: HomeState,
    onRequestExactAlarm: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestFullScreen: () -> Unit,
) {
    if (!state.settings.enabled) return
    val missing = listOf(
        Triple(state.exactAlarmAllowed, R.string.exact_alarm_permission_hint, onRequestExactAlarm),
        Triple(state.notificationsAllowed, R.string.notification_permission_hint, onRequestNotifications),
        Triple(state.fullScreenAllowed, R.string.full_screen_permission_hint, onRequestFullScreen),
    ).filterNot { it.first }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (missing.isEmpty()) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (missing.isEmpty()) stringResource(R.string.permissions_ready)
                else stringResource(R.string.permissions_required),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (missing.isNotEmpty()) {
                missing.forEach { (_, message, action) ->
                    Text(
                        text = stringResource(message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = action, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            when (message) {
                                R.string.exact_alarm_permission_hint -> stringResource(R.string.grant_exact_alarm)
                                R.string.notification_permission_hint -> stringResource(R.string.grant_notifications)
                                else -> stringResource(R.string.grant_full_screen)
                            },
                        )
                    }
                }
            }
        }
    }
}
