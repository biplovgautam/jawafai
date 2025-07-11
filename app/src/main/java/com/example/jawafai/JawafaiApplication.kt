package com.example.jawafai

import android.app.Application
import com.example.jawafai.managers.CloudinaryManager

class JawafaiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Cloudinary Manager
        CloudinaryManager.init(this)
    }
}
