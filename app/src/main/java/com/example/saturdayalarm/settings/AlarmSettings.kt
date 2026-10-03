package com.example.saturdayalarm.settings

data class AlarmSettings(
    val enabled: Boolean = false,
    val hour: Int = DEFAULT_HOUR,
    val minute: Int = DEFAULT_MINUTE,
    val vibrate: Boolean = true,
    val label: String = DEFAULT_LABEL,
) {
    init {
        require(hour in 0..23)
        require(minute in 0..59)
    }

    companion object {
        const val DEFAULT_HOUR = 7
        const val DEFAULT_MINUTE = 30
        const val DEFAULT_LABEL = "月末周六加班"
    }
}
