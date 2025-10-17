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
)