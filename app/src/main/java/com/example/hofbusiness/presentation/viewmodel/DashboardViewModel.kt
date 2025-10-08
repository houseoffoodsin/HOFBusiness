package com.example.hofbusiness.presentation.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hofbusiness.data.model.DailyAnalytics
import com.example.hofbusiness.data.model.DashboardData
import com.example.hofbusiness.data.model.Order
import com.example.hofbusiness.data.model.OrderStatus
import com.example.hofbusiness.data.repository.AnalyticsRepository
import com.example.hofbusiness.data.repository.CustomerRepository
import com.example.hofbusiness.data.repository.OrderRepository
import com.example.hofbusiness.presentation.state.DashboardUiState
import com.example.hofbusiness.data.service.ExportService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val analyticsRepository: AnalyticsRepository,
    private val customerRepository: CustomerRepository,
    private val exportService: ExportService
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun loadDashboardData() {
        viewModelScope.launch {
            try {
                _uiState.value = DashboardUiState.Loading

                // Load all dashboard data
                val orders = orderRepository.getAllOrders().first()
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                // Filter today's orders
                val todayOrders = orders.filter {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it.orderDate) == todayDate
                }

                // Calculate dashboard metrics
                val dashboardData = DashboardData(
                    totalOrders = orders.size,
                    todayOrders = todayOrders.size,
                    totalRevenue = orders.sumOf { it.totalAmount },
                    todayRevenue = todayOrders.sumOf { it.totalAmount },
                    pendingOrders = orders.count { it.status == OrderStatus.PENDING },
                    completedOrders = orders.count { it.status == OrderStatus.COMPLETED },
                    cancelledOrders = orders.count { it.status == OrderStatus.CANCELLED },
                    averageOrderValue = if (orders.isNotEmpty()) orders.sumOf { it.totalAmount } / orders.size else 0,
                    recentOrders = orders.sortedByDescending { it.orderDate }.take(5),
                    topItems = getTopItems(orders)
                )

                _uiState.value = DashboardUiState.Success(dashboardData)
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun exportData(
        exportType: ExportType,
        dateRange: DateRange,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val filePath = when (exportType) {
                    ExportType.ORDERS -> {
                        val orders = getOrdersForDateRange(dateRange)
                        exportService.exportOrdersToExcel(orders)
                    }
                    ExportType.ANALYTICS -> {
                        val analytics = getAnalyticsForDateRange(dateRange)
                        exportService.exportAnalyticsToExcel(analytics)
                    }
                    ExportType.INVENTORY -> {
                        exportService.exportInventoryToExcel()
                    }
                    ExportType.CUSTOMERS -> {
                        val customers = customerRepository.getAllCustomers().first()
                        exportService.exportCustomersToExcel(customers)
                    }
                }
                onSuccess(filePath)
            } catch (e: Exception) {
                onError(e.message ?: "Export failed")
            }
        }
    }

    private suspend fun getOrdersForDateRange(dateRange: DateRange): List<Order> {
        val allOrders = orderRepository.getAllOrders().first()
        val calendar = Calendar.getInstance()

        return when (dateRange) {
            DateRange.TODAY -> {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                allOrders.filter {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it.orderDate) == today
                }
            }
            DateRange.LAST_7_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val weekAgo = calendar.time
                allOrders.filter { it.orderDate >= weekAgo }
            }
            DateRange.LAST_30_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                val monthAgo = calendar.time
                allOrders.filter { it.orderDate >= monthAgo }
            }
            DateRange.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val monthStart = calendar.time
                allOrders.filter { it.orderDate >= monthStart }
            }
            DateRange.LAST_MONTH -> {
                // Set to first day of last month
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val lastMonthStart = calendar.time

                // Set to last day of last month
                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                val lastMonthEnd = calendar.time

                allOrders.filter { it.orderDate >= lastMonthStart && it.orderDate <= lastMonthEnd }
            }
            DateRange.CUSTOM -> {
                // For now, return last 7 days. You can implement custom date picker later
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val weekAgo = calendar.time
                allOrders.filter { it.orderDate >= weekAgo }
            }
        }
    }

    private suspend fun getAnalyticsForDateRange(dateRange: DateRange): List<DailyAnalytics> {
        val orders = getOrdersForDateRange(dateRange)

        // Group orders by date and create DailyAnalytics
        val analyticsByDate = orders
            .groupBy {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it.orderDate)
            }
            .map { (dateString, ordersForDate) ->
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString) ?: Date()

                // Calculate top selling item for this date
                val topItem = ordersForDate
                    .flatMap { it.items }
                    .groupBy { it.menuItemName }
                    .mapValues { (_, items) -> items.sumOf { it.quantity } }
                    .maxByOrNull { it.value }

                DailyAnalytics(
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
                    newCustomers = 0, // You can implement customer tracking later
                    returningCustomers = 0 // You can implement customer tracking later
                )
            }
            .sortedBy { it.date }

        return analyticsByDate
    }

    private fun getTopItems(orders: List<Order>): List<Pair<String, Int>> {
        return orders
            .flatMap { it.items }
            .groupBy { it.menuItemName }
            .mapValues { (_, items) -> items.sumOf { it.quantity } }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
    }
}

// Export Types and Date Ranges
enum class ExportType(val displayName: String) {
    ORDERS("Orders Report"),
    ANALYTICS("Analytics Report"),
    INVENTORY("Inventory Report"),
    CUSTOMERS("Customers Report")
}

enum class DateRange(val displayName: String) {
    TODAY("Today"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    CUSTOM("Custom Range")
}