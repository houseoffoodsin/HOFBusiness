package com.example.hofbusiness.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.PersistentCacheSettings

object FirebaseConfig {

    fun initializeFirestore() {
        // Modern way to configure Firestore settings
        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                PersistentCacheSettings.newBuilder()
                    .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build()
            )
            .build()

        FirebaseFirestore.getInstance().firestoreSettings = settings
    }

    fun initializeFirestoreWithMemoryCache() {
        // Using memory cache instead of persistent cache
        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                MemoryCacheSettings.newBuilder()
                    .build()
            )
            .build()

        FirebaseFirestore.getInstance().firestoreSettings = settings
    }


    fun setupOfflineSupport() {
        // Enable network connectivity
        FirebaseFirestore.getInstance().enableNetwork()
            .addOnSuccessListener {
                println("Firestore network enabled successfully")
            }
            .addOnFailureListener { exception ->
                println("Failed to enable Firestore network: ${exception.message}")
            }
    }

    fun disableNetwork() {
        // Disable network for offline mode
        FirebaseFirestore.getInstance().disableNetwork()
            .addOnSuccessListener {
                println("Firestore network disabled successfully")
            }
            .addOnFailureListener { exception ->
                println("Failed to disable Firestore network: ${exception.message}")
            }
    }

    fun clearPersistentCache() {
        // Clear the persistent cache
        FirebaseFirestore.getInstance().clearPersistence()
            .addOnSuccessListener {
                println("Firestore cache cleared successfully")
            }
            .addOnFailureListener { exception ->
                println("Failed to clear Firestore cache: ${exception.message}")
            }
    }

    fun waitForPendingWrites() {
        // Wait for all pending writes to complete
        FirebaseFirestore.getInstance().waitForPendingWrites()
            .addOnSuccessListener {
                println("All pending writes completed")
            }
            .addOnFailureListener { exception ->
                println("Failed to complete pending writes: ${exception.message}")
            }
    }

    fun terminateFirestore() {
        // Terminate the Firestore instance
        FirebaseFirestore.getInstance().terminate()
            .addOnSuccessListener {
                println("Firestore terminated successfully")
            }
            .addOnFailureListener { exception ->
                println("Failed to terminate Firestore: ${exception.message}")
            }
    }
}