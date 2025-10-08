package com.example.hofbusiness.data.repository

import com.example.hofbusiness.data.model.DailyAnalytics
import com.example.hofbusiness.data.model.Order
import com.example.hofbusiness.data.model.OrderStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface AnalyticsRepository {
    // Basic analytics operations
    fun getTodayAnalytics(): Flow<DailyAnalytics>
    fun getAnalyticsForDateRange(startDate: Date, endDate: Date): Flow<List<DailyAnalytics>>
    suspend fun updateDailyAnalytics(analytics: DailyAnalytics)

    // Analytics creation and retrieval
    suspend fun createAnalyticsFromOrders(orders: List<Order>, date: Date): DailyAnalytics
    suspend fun getAnalyticsByDate(date: Date): DailyAnalytics?

    // Period-based analytics
    suspend fun getWeeklyAnalytics(): List<DailyAnalytics>
    suspend fun getMonthlyAnalytics(): List<DailyAnalytics>

    // Specific analytics queries
    suspend fun getTopSellingItems(limit: Int = 10): List<Pair<String, Int>>
    suspend fun getRevenueAnalytics(days: Int = 30): List<Pair<Date, Int>>
    suspend fun getOrderStatusAnalytics(): Map<OrderStatus, Int>

    // Analytics calculations
    suspend fun calculateGrowthRate(currentPeriod: List<DailyAnalytics>, previousPeriod: List<DailyAnalytics>): Float

    // Maintenance operations
    suspend fun deleteAnalytics(date: Date)
    suspend fun bulkUpdateAnalytics(analyticsList: List<DailyAnalytics>)
}