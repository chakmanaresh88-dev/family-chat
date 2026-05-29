package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class FamilyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
            Log.d("FamilyApplication", "FirebaseApp initialized manually and successfully.")
        } catch (e: Exception) {
            Log.e("FamilyApplication", "Failed manual FirebaseApp initialization: ${e.message}", e)
        }
    }
}
