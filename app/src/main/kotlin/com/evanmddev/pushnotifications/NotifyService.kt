package com.evanmddev.pushnotifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import fi.iki.elonen.NanoHTTPD

class NotifyService : Service() {

    private var server: NotifyServer? = null

    companion object {
        const val CHANNEL_SERVICE = "service_channel"
        const val CHANNEL_ALERTS  = "alert_channel"
        const val SERVICE_NOTIF_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createChannels()
        startForeground(SERVICE_NOTIF_ID, buildServiceNotification())
        server = NotifyServer(3212) { message -> showAlert(message) }
        server?.start()
    }

    override fun onDestroy() {
        server?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java)

        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_SERVICE, "Service", NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, "Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Incoming push notifications"
            }
        )
    }

    private fun buildServiceNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_SERVICE)
            .setContentTitle("Push Notifications")
            .setContentText("Listening on port 3212…")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun showAlert(message: String) {
        val nm = getSystemService(NotificationManager::class.java)
        val notif = NotificationCompat.Builder(this, CHANNEL_ALERTS)
            .setContentTitle("Notification")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(System.currentTimeMillis().toInt(), notif)
    }
}

class NotifyServer(port: Int, private val onMessage: (String) -> Unit) : NanoHTTPD(port) {
    override fun serve(session: IHTTPSession): Response {
        if (session.method == Method.POST && session.uri == "/notify") {
            val contentLength = session.headers["content-length"]?.toIntOrNull() ?: 0
            val buffer = ByteArray(contentLength)
            session.inputStream.read(buffer, 0, contentLength)
            val message = String(buffer).trim()
            if (message.isNotEmpty()) onMessage(message)
            return newFixedLengthResponse("ok\n")
        }
        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "not found\n")
    }
}