package com.example.hofbusiness.data.repository

import android.util.Log
import com.example.hofbusiness.data.firebase.FirebaseConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.hofbusiness.data.model.Order
import com.example.hofbusiness.data.model.OrderStatus
import com.google.firebase.Timestamp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class OrderRepositoryImpl @Inject constructor(
    firestore: FirebaseFirestore
) : OrderRepository {

    private val ordersCollection = firestore.collection("orders")

    override suspend fun createOrder(order: Order): Result<String> {
        return try {
            val orderId = order.id.ifEmpty { generateOrderId() }
            val orderWithId = order.copy(id = orderId)
            ordersCollection.document(orderId).set(orderWithId).await()
            Result.success(orderId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateOrder(order: Order): Result<Unit> {
        return try {
            ordersCollection.document(order.id).set(order).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getOrderById(orderId: String): Result<Order?> {
        return try {
            val document = ordersCollection.document(orderId).get().await()
            val order = document.toObject(Order::class.java)
            Result.success(order)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAllOrders(): Flow<List<Order>> = callbackFlow {
        val listener = ordersCollection
            .orderBy("orderDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val orders = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Order::class.java)
                } ?: emptyList()

                trySend(orders)
            }

        awaitClose { listener.remove() }
    }

    override fun getOrdersByDateRange(startDate: Date, endDate: Date): Flow<List<Order>> = callbackFlow {
        val listener = ordersCollection
            .whereGreaterThanOrEqualTo("orderDate", startDate)
            .whereLessThanOrEqualTo("orderDate", endDate)
            .orderBy("orderDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val orders = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Order::class.java)
                } ?: emptyList()

                trySend(orders)
            }

        awaitClose { listener.remove() }
    }

    override fun getOrdersByStatus(status: OrderStatus): Flow<List<Order>> = callbackFlow {
        val listener = ordersCollection
            .whereEqualTo("status", status.name)
            .orderBy("orderDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val orders = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Order::class.java)
                } ?: emptyList()

                trySend(orders)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun deleteOrder(orderId: String): Result<Unit> {
        return try {
            ordersCollection.document(orderId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateOrderId(): String {
        val dateFormat = SimpleDateFormat("ddMMyy", Locale.getDefault())
        val dateString = dateFormat.format(Date())

        // Get today's start and tomorrow's start (to define range)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = Timestamp(calendar.time)

        val calendarTomorrow = Calendar.getInstance().apply {
            time = calendar.time
            add(Calendar.DAY_OF_YEAR, 1)
        }

        Log.d("todayStart",todayStart.toString())

        val tomorrowStart = Timestamp(calendarTomorrow.time)

        Log.d("tomorrowStart",tomorrowStart.toString())
        // Query today's orders based on Timestamp range
        val todayOrders = ordersCollection
            .whereGreaterThanOrEqualTo("orderDate", todayStart)
            .whereLessThan("orderDate", tomorrowStart)
            .get()
            .await()

        val sequenceNumber = String.format("%03d", todayOrders.size() + 1)
        return "HOF$dateString$sequenceNumber"
    }

    // Example of using the new Firebase config in a repository method
    fun syncOfflineData() {
        try {
            // Enable network to sync
            FirebaseConfig.setupOfflineSupport()

            // Wait for pending writes
            FirebaseConfig.waitForPendingWrites()

            // Your sync logic here

        } catch (e: Exception) {
            // Handle sync errors
            Log.e("OrderRepository", "Sync failed: ${e.message}")
        }
    }
}