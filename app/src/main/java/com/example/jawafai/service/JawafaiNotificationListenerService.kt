package com.example.jawafai.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.content.Context
import com.example.jawafai.service.NotificationMemoryStore
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import android.provider.Settings
import android.util.Log

class JawafaiNotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        android.util.Log.d("JawafaiNotifService", "NotificationListenerService connected!")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification
        val extras = notification.extras
        val title = extras.getString("android.title") ?: "(No Title)"
        val text = extras.getCharSequence("android.text")?.toString() ?: "(No Text)"
        val packageName = sbn.packageName ?: "(Unknown Package)"
        val time = sbn.postTime

        android.util.Log.d("JawafaiNotifService", "Notification received: $packageName | $title | $text")

        // Avoid echoing Jawafai's own notifications
        if (packageName != this.packageName) {
            NotificationMemoryStore.addNotification(
                NotificationMemoryStore.ExternalNotification(
                    title = title,
                    text = text,
                    packageName = packageName,
                    time = time
                )
            )
            postJawafaiNotification(title, text, packageName)
        }
    }

    private fun postJawafaiNotification(title: String, text: String, originalPackage: String) {
        val channelId = "jawafai_read_channel"
        val channelName = "Jawafai Read Notifications"
        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("[Jawafai] $title")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSubText(originalPackage)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val packageName = sbn.packageName ?: "(Unknown Package)"
        android.util.Log.d("JawafaiNotifService", "Notification removed: $packageName")
    }

    companion object {
        const val NOTIFICATION_BROADCAST_ACTION = "com.example.jawafai.NOTIFICATION_LISTENER_EVENT"
        // Utility to check if notification access is enabled
        fun isNotificationAccessEnabled(context: Context): Boolean {
            val enabledListeners = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            return enabledListeners.contains(context.packageName)
        }
    }
}
