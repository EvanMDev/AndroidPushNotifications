package com.evanmddev.pushnotifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import fi.iki.elonen.NanoHTTPD
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

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
        server = NotifyServer(3212) { title, message -> showAlert(title, message) }
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

    private fun showAlert(title: String, message: String) {
        val nm = getSystemService(NotificationManager::class.java)
        val notif = NotificationCompat.Builder(this, CHANNEL_ALERTS)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(System.currentTimeMillis().toInt(), notif)
    }
}

class NotifyServer(port: Int, private val onMessage: (String, String) -> Unit) : NanoHTTPD(port) {
    override fun serve(session: IHTTPSession): Response {
        if (session.method == Method.POST && session.uri == "/notify") {
            val params = rawQuery
                .split('&')
                .mapNotNull { part ->
                    val idx = part.indexOf('=')
                    if (idx <= 0) return@mapNotNull null
                    val key = URLDecoder.decode(part.substring(0, idx), StandardCharsets.UTF_8)
                    val value = URLDecoder.decode(part.substring(idx + 1), StandardCharsets.UTF_8)
                    key to value
                }
                .toMap()

            val title = params["title"]?.trim().orEmpty()
            val message = params["message"]?.trim().orEmpty()
            if (title.isNotEmpty() || message.isNotEmpty()) {
                onMessage(title, message)
            }
            return newFixedLengthResponse("ok\n")
        }
        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "not found\n")
    }
}