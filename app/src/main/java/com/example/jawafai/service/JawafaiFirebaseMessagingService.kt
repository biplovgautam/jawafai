package com.example.jawafai.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bumptech.glide.Glide
import com.example.jawafai.R
import com.example.jawafai.view.dashboard.DashboardActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class JawafaiFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        // Always create notification channel before showing notification (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "jawafai_chat_messages"
            val channelName = "Chat Messages"
            val channelDesc = "Notifications for chat messages"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDesc
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        // Check if message contains a data payload.
        remoteMessage.data.let { data ->
            val senderId = data["senderId"] ?: return
            val senderName = data["senderName"] ?: "New Message"
            val message = data["message"] ?: ""
            val chatId = data["chatId"] ?: ""
            val senderImageUrl = data["senderImageUrl"]

            // Check if user is in chat detail with sender (SharedPreferences flag)
            val prefs = applicationContext.getSharedPreferences("JawafaiPrefs", Context.MODE_PRIVATE)
            val currentChatUserId = prefs.getString("currentChatUserId", null)
            if (currentChatUserId == senderId) {
                // User is already chatting with sender, don't show notification
                return
            }

            // Show notification
            CoroutineScope(Dispatchers.IO).launch {
                val largeIcon: Bitmap? = senderImageUrl?.let {
                    try {
                        Glide.with(applicationContext)
                            .asBitmap()
                            .load(it)
                            .submit()
                            .get()
                    } catch (e: Exception) {
                        null
                    }
                }
                showNotification(senderId, senderName, message, chatId, largeIcon)
            }
        }
    }

    private fun showNotification(senderId: String, senderName: String, message: String, chatId: String, largeIcon: Bitmap?) {
        val intent = Intent(this, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigateToChat", true)
            putExtra("chatId", chatId)
            putExtra("otherUserId", senderId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, senderId.hashCode(), intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val channelId = "jawafai_chat_messages"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(senderName)
            .setContentText(if (message.length > 40) message.take(40) + "..." else message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        largeIcon?.let { notificationBuilder.setLargeIcon(it) }

        with(NotificationManagerCompat.from(this)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        this@JawafaiFirebaseMessagingService,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    notify(senderId.hashCode(), notificationBuilder.build())
                } else {
                    Log.w("JawafaiFCM", "Notification permission not granted, cannot show notification.")
                }
            } else {
                // On Android 12 and below, always show notification
                notify(senderId.hashCode(), notificationBuilder.build())
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("JawafaiFCM", "Refreshed token: $token")
        // Save the token to the user's database entry
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val usersRef = FirebaseDatabase.getInstance("https://jawafai-d2c23-default-rtdb.firebaseio.com/").getReference("users")
            usersRef.child(user.uid).child("fcmToken").setValue(token)
        }
    }
}
