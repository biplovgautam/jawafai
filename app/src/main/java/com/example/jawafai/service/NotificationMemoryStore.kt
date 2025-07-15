package com.example.jawafai.service

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

// Simple in-memory notification store
object NotificationMemoryStore {
    data class ExternalNotification(
        val title: String,
        val text: String,
        val packageName: String,
        val time: Long
    )

    val notifications: SnapshotStateList<ExternalNotification> = mutableStateListOf()

    fun addNotification(notification: ExternalNotification) {
        notifications.add(0, notification) // Add to top
    }

    fun clear() {
        notifications.clear()
    }
}

