package com.example.hofbusiness

import android.app.Application
import android.util.Log
import com.example.hofbusiness.data.firebase.FirebaseConfig
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HouseOfFoodsApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
            Log.d("Firebase", "Firebase initialized")
        }

        // Configure Firestore with modern settings
        try {
            FirebaseConfig.initializeFirestore()

        } catch (e: Exception) {
            Log.e("Firebase", "Failed to configure Firestore: ${e.message}")
            // Fallback to memory cache if persistent cache fails
            try {
                FirebaseConfig.initializeFirestoreWithMemoryCache()
                Log.d("Firebase", "Fallback to memory cache successful")
            } catch (fallbackException: Exception) {
                Log.e("Firebase", "Fallback configuration also failed: ${fallbackException.message}")
            }
        }

        // Setup offline support
        FirebaseConfig.setupOfflineSupport()
    }

    override fun onTerminate() {
        super.onTerminate()

        // Wait for pending writes before terminating
        FirebaseConfig.waitForPendingWrites()
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }
}