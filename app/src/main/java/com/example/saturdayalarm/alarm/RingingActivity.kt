package com.example.saturdayalarm.alarm

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.example.saturdayalarm.R
import com.example.saturdayalarm.settings.SettingsRepository

class RingingActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val label = SettingsRepository(this).get().label
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(32, 32, 32, 32)
            addView(TextView(this@RingingActivity).apply {
                text = label
                textSize = 32f
                gravity = android.view.Gravity.CENTER
            })
            addView(Button(this@RingingActivity).apply {
                text = getString(R.string.stop_alarm)
                setOnClickListener { sendAction(RingingService.ACTION_STOP) }
            })
            addView(Button(this@RingingActivity).apply {
                text = getString(R.string.snooze_alarm)
                setOnClickListener { sendAction(RingingService.ACTION_SNOOZE) }
            })
        }
        setContentView(content)
    }

    private fun sendAction(action: String) {
        startService(Intent(this, RingingService::class.java).setAction(action))
        finish()
    }
}
