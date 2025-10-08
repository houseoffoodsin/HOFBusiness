package com.example.hofbusiness.di

import android.content.Context
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.example.hofbusiness.data.repository.CustomerRepository
import com.example.hofbusiness.data.repository.CustomerRepositoryImpl
import com.example.hofbusiness.data.repository.OrderRepository
import com.example.hofbusiness.data.repository.OrderRepositoryImpl
import com.example.hofbusiness.data.repository.AnalyticsRepository
import com.example.hofbusiness.data.repository.AnalyticsRepositoryImpl
import com.example.hofbusiness.data.service.ExportService
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance()

        // Updated Firestore settings using new API
        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                PersistentCacheSettings.newBuilder()
                    .setSizeBytes(100 * 1024 * 1024) // 100 MB cache size
                    .build()
            )
            .build()

        firestore.firestoreSettings = settings

        return firestore
    }

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase {
        val database = FirebaseDatabase.getInstance()
        // Enable offline persistence for Realtime Database
        database.setPersistenceEnabled(true)
        return database
    }

    @Provides
    @Singleton
    fun provideOrderRepository(
        firestore: FirebaseFirestore
    ): OrderRepository {
        return OrderRepositoryImpl(firestore)
    }

    @Provides
    @Singleton
    fun provideCustomerRepository(
        firestore: FirebaseFirestore
    ): CustomerRepository {
        return CustomerRepositoryImpl(firestore)
    }

    @Provides
    @Singleton
    fun provideAnalyticsRepository(
        firestore: FirebaseFirestore
    ): AnalyticsRepository {
        return AnalyticsRepositoryImpl(firestore)
    }

    @Provides
    @Singleton
    fun provideExportService(
        @ApplicationContext context: Context
    ): ExportService {
        return ExportService(context)
    }
}