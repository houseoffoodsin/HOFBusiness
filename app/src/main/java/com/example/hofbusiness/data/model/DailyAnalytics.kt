package com.example.hofbusiness.data.model

import java.util.Date

data class DailyAnalytics(
    val id: String = "",
    val date: Date = Date(),
    val totalOrders: Int = 0,
    val totalRevenue: Int = 0,
    val averageOrderValue: Int = 0,
    val topSellingItem: String = "",
    val topSellingItemQuantity: Int = 0,
    val pendingOrders: Int = 0,
    val completedOrders: Int = 0,
    val cancelledOrders: Int = 0,
    val newCustomers: Int = 0,
    val returningCustomers: Int = 0
) {
    // Helper function to get completion rate
    fun getCompletionRate(): Float {
        return if (totalOrders > 0) {
            (completedOrders.toFloat() / totalOrders.toFloat()) * 100
        } else 0f
    }

    // Helper function to get cancellation rate
    fun getCancellationRate(): Float {
        return if (totalOrders > 0) {
            (cancelledOrders.toFloat() / totalOrders.toFloat()) * 100
        } else 0f
    }

    // Helper function to format revenue
    fun getFormattedRevenue(): String {
        return "₹$totalRevenue"
    }

    // Helper function to format average order value
    fun getFormattedAverageOrderValue(): String {
        return "₹$averageOrderValue"
    }
}