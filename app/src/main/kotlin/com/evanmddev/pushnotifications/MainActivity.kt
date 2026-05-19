package com.evanmddev.pushnotifications

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Simple programmatic layout
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(64, 64, 64, 64)
        }

        val status = TextView(this).apply {
            text = "Service status: checking…"
            textSize = 16f
        }

        val startBtn = Button(this).apply {
            text = "Start Listener"
            setOnClickListener { startService(); status.text = "Service status: running on port 3212" }
        }

        val stopBtn = Button(this).apply {
            text = "Stop Listener"
            setOnClickListener { stopService(); status.text = "Service status: stopped" }
        }

        val infoText = TextView(this).apply {
            text = "\nSend a notification:\ncurl -d \"Hello!\" http://<tailscale-ip>:3212/notify"
            textSize = 13f
        }

        layout.addView(status)
        layout.addView(startBtn)
        layout.addView(stopBtn)
        layout.addView(infoText)
        setContentView(layout)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        // Auto-start on open
        startService()
        status.text = "Service status: running on port 3212"
    }

    private fun startService() {
        val intent = Intent(this, NotifyService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopService() {
        val intent = Intent(this, NotifyService::class.java)
        stopService(intent)
    }
}