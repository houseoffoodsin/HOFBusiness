package com.example.hofbusiness.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.example.hofbusiness.data.model.DailyAnalytics
import com.example.hofbusiness.data.model.Order
import com.example.hofbusiness.data.model.OrderStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : AnalyticsRepository {

    override fun getTodayAnalytics(): Flow<DailyAnalytics> = callbackFlow {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val listener = firestore.collection("analytics")
            .document(today)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val analytics = if (snapshot != null && snapshot.exists()) {
                    snapshot.toObject<DailyAnalytics>() ?: createEmptyAnalytics(today)
                } else {
                    createEmptyAnalytics(today)
                }

                trySend(analytics)
            }

        awaitClose { listener.remove() }
    }

    override fun getAnalyticsForDateRange(startDate: Date, endDate: Date): Flow<List<DailyAnalytics>> = callbackFlow {
        val listener = firestore.collection("analytics")
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .orderBy("date")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val analyticsList = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject<DailyAnalytics>()
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(analyticsList)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun updateDailyAnalytics(analytics: DailyAnalytics) {
        try {
            firestore.collection("analytics")
                .document(analytics.id)
                .set(analytics)
                .await()
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun createAnalyticsFromOrders(orders: List<Order>, date: Date): DailyAnalytics {
        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
        val ordersForDate = orders.filter {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it.orderDate) == dateString
        }

        // Calculate top selling item
        val topItem = ordersForDate
            .flatMap { it.items }
            .groupBy { it.menuItemName }
            .mapValues { (_, items) -> items.sumOf { it.quantity } }
            .maxByOrNull { it.value }

        // Calculate customer metrics
        val uniqueCustomers = ordersForDate.map { it.mobileNumber }.distinct()

        val analytics = DailyAnalytics(
            id = dateString,
            date = date,
            totalOrders = ordersForDate.size,
            totalRevenue = ordersForDate.sumOf { it.totalAmount },
            averageOrderValue = if (ordersForDate.isNotEmpty())
                ordersForDate.sumOf { it.totalAmount } / ordersForDate.size else 0,
            topSellingItem = topItem?.key ?: "N/A",
            topSellingItemQuantity = topItem?.value ?: 0,
            pendingOrders = ordersForDate.count { it.status == OrderStatus.PENDING },
            completedOrders = ordersForDate.count { it.status == OrderStatus.COMPLETED },
            cancelledOrders = ordersForDate.count { it.status == OrderStatus.CANCELLED },
            newCustomers = uniqueCustomers.size, // Simplified - you can enhance this
            returningCustomers = 0 // You can implement logic to track returning customers
        )

        // Save to Firestore
        updateDailyAnalytics(analytics)

        return analytics
    }

    override suspend fun getAnalyticsByDate(date: Date): DailyAnalytics? {
        return try {
            val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
            val snapshot = firestore.collection("analytics")
                .document(dateString)
                .get()
                .await()

            if (snapshot.exists()) {
                snapshot.toObject<DailyAnalytics>()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getWeeklyAnalytics(): List<DailyAnalytics> {
        return try {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val weekAgo = calendar.time

            val snapshot = firestore.collection("analytics")
                .whereGreaterThanOrEqualTo("date", weekAgo)
                .whereLessThanOrEqualTo("date", Date())
                .orderBy("date")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject<DailyAnalytics>()
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getMonthlyAnalytics(): List<DailyAnalytics> {
        return try {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val monthStart = calendar.time

            val snapshot = firestore.collection("analytics")
                .whereGreaterThanOrEqualTo("date", monthStart)
                .whereLessThanOrEqualTo("date", Date())
                .orderBy("date")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject<DailyAnalytics>()
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getTopSellingItems(limit: Int): List<Pair<String, Int>> {
        return try {
            // Get all analytics for the current month
            val monthlyAnalytics = getMonthlyAnalytics()

            // Aggregate top selling items
            val itemCounts = mutableMapOf<String, Int>()

            monthlyAnalytics.forEach { analytics ->
                if (analytics.topSellingItem != "N/A") {
                    itemCounts[analytics.topSellingItem] =
                        itemCounts.getOrDefault(analytics.topSellingItem, 0) + analytics.topSellingItemQuantity
                }
            }

            itemCounts.toList()
                .sortedByDescending { it.second }
                .take(limit)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getRevenueAnalytics(days: Int): List<Pair<Date, Int>> {
        return try {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -days)
            val startDate = calendar.time

            val snapshot = firestore.collection("analytics")
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", Date())
                .orderBy("date")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    val analytics = doc.toObject<DailyAnalytics>()
                    analytics?.let { Pair(it.date, it.totalRevenue) }
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getOrderStatusAnalytics(): Map<OrderStatus, Int> {
        return try {
            val todayAnalytics = getAnalyticsByDate(Date())

            if (todayAnalytics != null) {
                mapOf(
                    OrderStatus.PENDING to todayAnalytics.pendingOrders,
                    OrderStatus.COMPLETED to todayAnalytics.completedOrders,
                    OrderStatus.CANCELLED to todayAnalytics.cancelledOrders
                )
            } else {
                mapOf(
                    OrderStatus.PENDING to 0,
                    OrderStatus.COMPLETED to 0,
                    OrderStatus.CANCELLED to 0
                )
            }
        } catch (e: Exception) {
            mapOf(
                OrderStatus.PENDING to 0,
                OrderStatus.COMPLETED to 0,
                OrderStatus.CANCELLED to 0
            )
        }
    }

    override suspend fun calculateGrowthRate(currentPeriod: List<DailyAnalytics>, previousPeriod: List<DailyAnalytics>): Float {
        val currentRevenue = currentPeriod.sumOf { it.totalRevenue }
        val previousRevenue = previousPeriod.sumOf { it.totalRevenue }

        return if (previousRevenue > 0) {
            ((currentRevenue - previousRevenue).toFloat() / previousRevenue.toFloat()) * 100
        } else if (currentRevenue > 0) {
            100f
        } else {
            0f
        }
    }

    override suspend fun deleteAnalytics(date: Date) {
        try {
            val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
            firestore.collection("analytics")
                .document(dateString)
                .delete()
                .await()
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun bulkUpdateAnalytics(analyticsList: List<DailyAnalytics>) {
        try {
            val batch = firestore.batch()

            analyticsList.forEach { analytics ->
                val docRef = firestore.collection("analytics").document(analytics.id)
                batch.set(docRef, analytics)
            }

            batch.commit().await()
        } catch (e: Exception) {
            throw e
        }
    }

    // Helper function to create empty analytics
    private fun createEmptyAnalytics(dateString: String): DailyAnalytics {
        val date = try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString) ?: Date()
        } catch (e: Exception) {
            Date()
        }

        return DailyAnalytics(
            id = dateString,
            date = date,
            totalOrders = 0,
            totalRevenue = 0,
            averageOrderValue = 0,
            topSellingItem = "N/A",
            topSellingItemQuantity = 0,
            pendingOrders = 0,
            completedOrders = 0,
            cancelledOrders = 0,
            newCustomers = 0,
            returningCustomers = 0
        )
    }
}