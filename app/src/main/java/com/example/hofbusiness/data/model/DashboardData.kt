package com.example.hofbusiness.data.model

data class DashboardData(
    val totalOrders: Int = 0,
    val todayOrders: Int = 0,
    val totalRevenue: Int = 0,
    val pendingOrders: Int = 0,
    val recentOrders: List<Order> = emptyList(),
    val topItems: List<Pair<String, Int>> = emptyList(),
    val completedOrders: Int = 0,
    val cancelledOrders: Int = 0,
    val averageOrderValue: Int = 0,
    val todayRevenue: Int = 0
) {
    // Helper functions for formatting
    fun getFormattedTotalRevenue(): String = "₹$totalRevenue"
    fun getFormattedTodayRevenue(): String = "₹$todayRevenue"
    fun getFormattedAverageOrderValue(): String = "₹$averageOrderValue"

    // Helper function to get completion rate
    fun getCompletionRate(): Float {
        return if (totalOrders > 0) {
            (completedOrders.toFloat() / totalOrders.toFloat()) * 100
        } else 0f
    }

    // Helper function to get today's growth
    fun getTodayGrowthPercentage(yesterdayOrders: Int): Float {
        return if (yesterdayOrders > 0) {
            ((todayOrders - yesterdayOrders).toFloat() / yesterdayOrders.toFloat()) * 100
        } else if (todayOrders > 0) 100f else 0f
    }
}