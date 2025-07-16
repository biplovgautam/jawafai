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
    // Define the package names of apps we want to capture notifications from
    private val supportedApps = setOf(
        "com.instagram.android",        // Instagram
        "com.whatsapp",                 // WhatsApp
        "com.facebook.orca",            // Facebook Messenger
        "com.whatsapp.w4b"              // WhatsApp Business
    )

    override fun onListenerConnected() {
        super.onListenerConnected()
        android.util.Log.d("JawafaiNotifService", "NotificationListenerService connected!")
    }

    private fun isSupportedApp(packageName: String): Boolean {
        return supportedApps.contains(packageName)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification
        val extras = notification.extras
        val title = extras.getString("android.title") ?: "(No Title)"
        val text = extras.getCharSequence("android.text")?.toString() ?: "(No Text)"
        val packageName = sbn.packageName ?: "(Unknown Package)"
        val time = sbn.postTime

        android.util.Log.d("JawafaiNotifService", "Notification received: $packageName | $title | $text")

        // Avoid echoing Jawafai's own notifications and only process supported apps
        if (packageName != this.packageName && isSupportedApp(packageName)) {
            NotificationMemoryStore.addNotification(
                NotificationMemoryStore.ExternalNotification(
                    title = title,
                    text = text,
                    packageName = packageName,
                    time = time
                )
            )
            // Removed postJawafaiNotification call - only store in memory
        } else {
            android.util.Log.d("JawafaiNotifService", "Notification ignored - not from supported app: $packageName")
        }
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
