package com.example.jellyfinoffline

import android.app.Application

class JellyfinOfflineApp : Application() {
    override fun onCreate() {
        super.onCreate()
        JellyfinClientManager.init(this)
    }
}
