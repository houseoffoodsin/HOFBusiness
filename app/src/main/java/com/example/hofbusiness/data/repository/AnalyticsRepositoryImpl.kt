package com.example.hofbusiness.data.repository

import com.example.hofbusiness.data.model.DailyAnalytics
import com.example.hofbusiness.data.model.Order
import com.example.hofbusiness.data.model.OrderStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
    firestore: FirebaseFirestore
) : AnalyticsRepository {

    private val analyticsCollection = firestore.collection("analytics")

    override suspend fun saveDailyAnalytics(analytics: DailyAnalytics): Result<Unit> {
        return try {
            analyticsCollection.document(analytics.id).set(analytics).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getDailyAnalytics(date: Date): Flow<DailyAnalytics?> = callbackFlow {
        try {
            val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
            val documentId = "daily-$dateString"

            val listener = analyticsCollection.document(documentId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val analytics = snapshot?.toObject(DailyAnalytics::class.java)
                    trySend(analytics)
                }

            awaitClose { listener.remove() }
        } catch (e: Exception) {
            close(e)
        }
    }

    override fun getAnalyticsByDateRange(startDate: Date, endDate: Date): Flow<List<DailyAnalytics>> = callbackFlow {
        try {
            val listener = analyticsCollection
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val analyticsList = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(DailyAnalytics::class.java)
                    } ?: emptyList()

                    trySend(analyticsList)
                }

            awaitClose { listener.remove() }
        } catch (e: Exception) {
            close(e)
        }
    }

    override fun getAllAnalytics(): Flow<List<DailyAnalytics>> = callbackFlow {
        try {
            val listener = analyticsCollection
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val analyticsList = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(DailyAnalytics::class.java)
                    } ?: emptyList()

                    trySend(analyticsList)
                }

            awaitClose { listener.remove() }
        } catch (e: Exception) {
            close(e)
        }
    }

    override suspend fun generateAnalyticsFromOrders(
        orders: List<Order>,
        date: Date
    ): DailyAnalytics {
        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)

        if (orders.isEmpty()) {
            return DailyAnalytics(
                id = "daily-$dateString",
                date = date
            )
        }

        val totalRevenue = orders.sumOf { it.totalAmount }
        val totalOrders = orders.size
        val averageOrderValue = if (totalOrders > 0) totalRevenue / totalOrders else 0

        // Calculate item quantities
        val itemCounts = mutableMapOf<String, Int>()
        orders.forEach { order ->
            order.items.forEach { item ->
                itemCounts[item.menuItemName] = itemCounts.getOrDefault(item.menuItemName, 0) + item.quantity
            }
        }

        val topSellingItem = itemCounts.maxByOrNull { it.value }?.key ?: "N/A"
        val topSellingItemQuantity = itemCounts.maxByOrNull { it.value }?.value ?: 0

        // Calculate order statuses
        val pendingOrders = orders.count { it.status == OrderStatus.PENDING }
        val completedOrders = orders.count { it.status == OrderStatus.COMPLETED }
        val cancelledOrders = orders.count { it.status == OrderStatus.CANCELLED }

        // Calculate customer metrics
        val uniqueCustomers = orders.map { it.customerId }.distinct().size
        val previousOrders = orders.filter { it.orderDate < date }
        val returningCustomers = orders
            .map { it.customerId }
            .distinct()
            .count { customerId ->
                previousOrders.any { it.customerId == customerId }
            }
        val newCustomers = uniqueCustomers - returningCustomers

        return DailyAnalytics(
            id = "daily-$dateString",
            date = date,
            totalOrders = totalOrders,
            totalRevenue = totalRevenue,
            averageOrderValue = averageOrderValue,
            topSellingItem = topSellingItem,
            topSellingItemQuantity = topSellingItemQuantity,
            pendingOrders = pendingOrders,
            completedOrders = completedOrders,
            cancelledOrders = cancelledOrders,
            newCustomers = newCustomers,
            returningCustomers = returningCustomers
        )
    }
}