package com.example.ecommerceproject

import android.app.Application
import android.util.Log

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("MyApplication", "Application onCreate called")
        DatabaseHelper.initCloudinary(this)
    }
}