package com.example.hofbusiness.data.repository

import com.example.hofbusiness.data.model.Order
import com.example.hofbusiness.data.model.OrderStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface OrderRepository {
    suspend fun createOrder(order: Order): Result<String>
    suspend fun updateOrder(order: Order): Result<Unit>
    suspend fun getOrderById(orderId: String): Result<Order?>
    fun getAllOrders(): Flow<List<Order>>
    fun getOrdersByDateRange(startDate: Date, endDate: Date): Flow<List<Order>>
    fun getOrdersByStatus(status: OrderStatus): Flow<List<Order>>
    suspend fun deleteOrder(orderId: String): Result<Unit>
    suspend fun generateOrderId(): String
}