package com.example.hofbusiness.data.repository

import com.example.hofbusiness.data.model.DailyAnalytics
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface AnalyticsRepository {
    suspend fun saveDailyAnalytics(analytics: DailyAnalytics): Result<Unit>
    fun getDailyAnalytics(date: Date): Flow<DailyAnalytics?>
    fun getAnalyticsByDateRange(startDate: Date, endDate: Date): Flow<List<DailyAnalytics>>
    fun getAllAnalytics(): Flow<List<DailyAnalytics>>
    suspend fun generateAnalyticsFromOrders(
        orders: List<com.example.hofbusiness.data.model.Order>,
        date: Date
    ): DailyAnalytics
}