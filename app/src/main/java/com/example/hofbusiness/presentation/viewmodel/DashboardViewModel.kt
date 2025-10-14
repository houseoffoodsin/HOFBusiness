package com.example.hofbusiness.presentation.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hofbusiness.data.model.DailyAnalytics
import com.example.hofbusiness.data.model.Order
import com.example.hofbusiness.data.repository.OrderRepository
import com.example.hofbusiness.data.service.ExportService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val exportService: ExportService
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _selectedPeriod = MutableStateFlow(TimePeriod.DAY)
    val selectedPeriod: StateFlow<TimePeriod> = _selectedPeriod.asStateFlow()

    private val _dashboardMetrics = MutableStateFlow(DashboardMetrics())
    val dashboardMetrics: StateFlow<DashboardMetrics> = _dashboardMetrics.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    private val _analytics = MutableStateFlow<List<DailyAnalytics>>(emptyList())

    init {
        loadDashboardData()
    }

    fun selectTimePeriod(period: TimePeriod) {
        _selectedPeriod.value = period
        loadDashboardData()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun exportDashboardReport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }

            try {
                val filePath = exportService.exportDashboardReport(
                    orders = _orders.value,
                    analytics = _analytics.value,
                    period = _selectedPeriod.value.displayName
                )

                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportMessage = "Dashboard report exported successfully to: $filePath"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        errorMessage = "Export failed: ${e.message}"
                    )
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun exportOrdersToExcel() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }

            try {
                val filePath = exportService.exportOrdersToExcel(_orders.value)
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportMessage = "Orders exported successfully to: $filePath"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        errorMessage = "Export failed: ${e.message}"
                    )
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun exportAnalyticsToExcel() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }

            try {
                val filePath = exportService.exportAnalyticsToExcel(_analytics.value)
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportMessage = "Analytics exported successfully to: $filePath"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        errorMessage = "Export failed: ${e.message}"
                    )
                }
            }
        }
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val (startDate, endDate) = getDateRange(_selectedPeriod.value)

                orderRepository.getOrdersByDateRange(startDate, endDate)
                    .catch { exception ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Failed to load dashboard data: ${exception.message}"
                            )
                        }
                    }
                    .collect { orders ->
                        _orders.value = orders
                        val metrics = calculateMetrics(orders)
                        _dashboardMetrics.value = metrics
                        _uiState.update { it.copy(isLoading = false) }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error loading dashboard: ${e.message}"
                    )
                }
            }
        }
    }

    private fun calculateMetrics(orders: List<Order>): DashboardMetrics {
        if (orders.isEmpty()) {
            return DashboardMetrics()
        }

        val totalRevenue = orders.sumOf { it.totalAmount }
        val totalOrders = orders.size
        val averageOrderValue = if (totalOrders > 0) totalRevenue / totalOrders else 0

        // Calculate retention rate (customers with more than one order)
        val customerOrderCounts = orders.groupBy { it.customerId }.mapValues { it.value.size }
        val repeatCustomers = customerOrderCounts.values.count { it > 1 }
        val retentionRate = if (customerOrderCounts.isNotEmpty()) {
            (repeatCustomers.toDouble() / customerOrderCounts.size * 100).toInt()
        } else 0

        // Most and least bought items
        val itemCounts = mutableMapOf<String, Int>()
        orders.forEach { order ->
            order.items.forEach { item ->
                itemCounts[item.menuItemName] = itemCounts.getOrDefault(item.menuItemName, 0) + item.quantity
            }
        }

        val mostBoughtItem = itemCounts.maxByOrNull { it.value }?.key ?: "N/A"
        val leastBoughtItem = itemCounts.minByOrNull { it.value }?.key ?: "N/A"

        // Most bought region
        val regionCounts = orders.groupBy { extractRegion(it.address) }
            .mapValues { it.value.size }
        val mostBoughtRegion = regionCounts.maxByOrNull { it.value }?.key ?: "N/A"

        // Frequently bought together
        val frequentlyBoughtTogether = calculateFrequentlyBoughtTogether(orders)

        // Item distribution for pie chart
        val itemDistribution = itemCounts.map {
            ItemDistribution(
                itemName = it.key,
                quantity = it.value,
                percentage = (it.value.toDouble() / itemCounts.values.sum() * 100).toFloat()
            )
        }.sortedByDescending { it.quantity }

        return DashboardMetrics(
            totalRevenue = totalRevenue,
            numberOfOrders = totalOrders,
            averageOrderValue = averageOrderValue,
            retentionRate = retentionRate,
            mostBoughtItem = mostBoughtItem,
            leastBoughtItem = leastBoughtItem,
            mostBoughtRegion = mostBoughtRegion,
            frequentlyBoughtTogether = frequentlyBoughtTogether,
            itemDistribution = itemDistribution
        )
    }

    private fun getDateRange(period: TimePeriod): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        val endDate = calendar.time

        when (period) {
            TimePeriod.DAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
            TimePeriod.WEEK -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
            }
            TimePeriod.MONTH -> {
                calendar.add(Calendar.MONTH, -1)
            }
        }

        return Pair(calendar.time, endDate)
    }

    private fun extractRegion(address: String): String {
        val parts = address.split(",")
        return if (parts.size >= 2) parts[parts.size - 2].trim() else "Unknown"
    }

    private fun calculateFrequentlyBoughtTogether(orders: List<Order>): List<String> {
        val itemPairs = mutableMapOf<String, Int>()

        orders.forEach { order ->
            val items = order.items.map { it.menuItemName }
            for (i in items.indices) {
                for (j in i + 1 until items.size) {
                    val pair = listOf(items[i], items[j]).sorted().joinToString(" + ")
                    itemPairs[pair] = itemPairs.getOrDefault(pair, 0) + 1
                }
            }
        }

        return itemPairs.toList()
            .sortedByDescending { it.second }
            .take(5)
            .map { "${it.first} (${it.second} times)" }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearExportMessage() {
        _uiState.update { it.copy(exportMessage = null) }
    }
}

data class DashboardUiState(
    val isLoading: Boolean = false,
    val isExporting: Boolean = false,
    val errorMessage: String? = null,
    val exportMessage: String? = null
)

data class DashboardMetrics(
    val totalRevenue: Int = 0,
    val numberOfOrders: Int = 0,
    val averageOrderValue: Int = 0,
    val retentionRate: Int = 0,
    val mostBoughtItem: String = "N/A",
    val leastBoughtItem: String = "N/A",
    val mostBoughtRegion: String = "N/A",
    val frequentlyBoughtTogether: List<String> = emptyList(),
    val itemDistribution: List<ItemDistribution> = emptyList()
)

data class ItemDistribution(
    val itemName: String,
    val quantity: Int,
    val percentage: Float
)

enum class TimePeriod(val displayName: String) {
    DAY("Today"),
    WEEK("Last 7 Days"),
    MONTH("Last 30 Days")
}