package com.example.hofbusiness.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hofbusiness.data.model.Order
import com.example.hofbusiness.data.model.OrderStatus
import com.example.hofbusiness.data.repository.OrderRepository
import com.example.hofbusiness.presentation.state.KitchenPrepUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class KitchenPrepViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(KitchenPrepUiState())
    val uiState: StateFlow<KitchenPrepUiState> = _uiState.asStateFlow()

    private val _selectedDate = MutableStateFlow(Date())
    val selectedDate: StateFlow<Date> = _selectedDate.asStateFlow()

    private val _prepItems = MutableStateFlow<List<PrepItem>>(emptyList())
    val prepItems: StateFlow<List<PrepItem>> = _prepItems.asStateFlow()

    init {
        loadPrepItems()
    }

    fun selectDate(date: Date) {
        _selectedDate.value = date
        loadPrepItems()
    }

    fun toggleItemPrepared(prepItem: PrepItem) {
        viewModelScope.launch {
            val updatedItems = _prepItems.value.map { item ->
                if (item.menuItemName == prepItem.menuItemName && item.size == prepItem.size) {
                    item.copy(isPrepared = !item.isPrepared)
                } else {
                    item
                }
            }
            _prepItems.value = updatedItems

            // Update preparation status in local storage or preferences
            // This could be saved to Firebase or local preferences
        }
    }

    fun markAllPrepared() {
        viewModelScope.launch {
            val updatedItems = _prepItems.value.map { it.copy(isPrepared = true) }
            _prepItems.value = updatedItems
        }
    }

    fun resetAllPreparation() {
        viewModelScope.launch {
            val updatedItems = _prepItems.value.map { it.copy(isPrepared = false) }
            _prepItems.value = updatedItems
        }
    }

    private fun loadPrepItems() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val calendar = Calendar.getInstance()
                calendar.time = _selectedDate.value
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.time

                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val endOfDay = calendar.time

                orderRepository.getOrdersByDateRange(startOfDay, endOfDay)
                    .catch { exception ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Failed to load prep items: ${exception.message}"
                            )
                        }
                    }
                    .collect { orders ->
                        val prepItems = aggregateOrderItems(orders)
                        _prepItems.value = prepItems
                        _uiState.update { it.copy(isLoading = false) }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error loading prep items: ${e.message}"
                    )
                }
            }
        }
    }

    private fun aggregateOrderItems(orders: List<Order>): List<PrepItem> {
        val itemMap = mutableMapOf<String, PrepItem>()

        orders.forEach { order ->
            // Only include orders that need preparation
            if (order.status != OrderStatus.CANCELLED && !order.orderPrepared) {
                order.items.forEach { orderItem ->
                    val key = "${orderItem.menuItemName}_${orderItem.size}"

                    if (itemMap.containsKey(key)) {
                        val existingItem = itemMap[key]!!
                        itemMap[key] = existingItem.copy(
                            totalQuantity = existingItem.totalQuantity + orderItem.quantity,
                            orderIds = existingItem.orderIds + order.id
                        )
                    } else {
                        itemMap[key] = PrepItem(
                            menuItemName = orderItem.menuItemName,
                            size = orderItem.size,
                            totalQuantity = orderItem.quantity,
                            unitPrice = orderItem.unitPrice,
                            orderIds = listOf(order.id),
                            isPrepared = false
                        )
                    }
                }
            }
        }

        return itemMap.values.sortedBy { it.menuItemName }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

data class PrepItem(
    val menuItemName: String,
    val size: String,
    val totalQuantity: Int,
    val unitPrice: Int,
    val orderIds: List<String>,
    val isPrepared: Boolean = false
) {
    val totalValue: Int get() = totalQuantity * unitPrice
}