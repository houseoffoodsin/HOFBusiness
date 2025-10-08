package com.example.hofbusiness.data.model

data class DashboardMetrics(
    val totalRevenue: Int = 0,
    val numberOfOrders: Int = 0,
    val averageOrderValue: Int = 0,
    val retentionRate: Float = 0f,
    val itemDistribution: List<ItemDistribution> = emptyList(),
    val mostBoughtItem: String = "N/A",
    val leastBoughtItem: String = "N/A",
    val mostBoughtRegion: String = "N/A",
    val frequentlyBoughtTogether: List<String> = emptyList(),
    val growthRate: Float = 0f,
    val topCustomers: List<String> = emptyList(),
    val peakHours: List<String> = emptyList(),
    val conversionRate: Float = 0f
) {
    fun getFormattedTotalRevenue(): String = "₹$totalRevenue"
    fun getFormattedAverageOrderValue(): String = "₹$averageOrderValue"
    fun getFormattedRetentionRate(): String = "${String.format("%.1f", retentionRate)}%"
    fun getFormattedGrowthRate(): String = "${String.format("%.1f", growthRate)}%"
    fun getFormattedConversionRate(): String = "${String.format("%.1f", conversionRate)}%"
}
