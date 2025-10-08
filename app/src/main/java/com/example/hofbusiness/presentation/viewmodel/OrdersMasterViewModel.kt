package com.example.hofbusiness.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hofbusiness.data.model.Order
import com.example.hofbusiness.data.model.OrderStatus
import com.example.hofbusiness.data.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class OrdersMasterViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersMasterUiState())
    val uiState: StateFlow<OrdersMasterUiState> = _uiState.asStateFlow()

    private val _allOrders = MutableStateFlow<List<Order>>(emptyList())

    val filteredOrders: StateFlow<List<Order>> = combine(
        _allOrders,
        _uiState
    ) { orders, state ->
        filterOrders(orders, state)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadOrders()
    }

    private fun loadOrders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            orderRepository.getAllOrders()
                .catch { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to load orders: ${exception.message}"
                        )
                    }
                }
                .collect { orders ->
                    _allOrders.value = orders
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    fun updateOrderStatus(order: Order, field: OrderStatusField, value: Boolean) {
        viewModelScope.launch {
            val updatedOrder = when (field) {
                OrderStatusField.PAYMENT_RECEIVED -> order.copy(paymentReceived = value)
                OrderStatusField.ORDER_PREPARED -> order.copy(orderPrepared = value)
                OrderStatusField.DISPATCHED -> order.copy(dispatched = value)
                OrderStatusField.DELIVERED -> order.copy(delivered = value)
            }

            // Update overall status based on individual fields
            val newStatus = determineOrderStatus(updatedOrder)
            val finalOrder = updatedOrder.copy(status = newStatus)

            val result = orderRepository.updateOrder(finalOrder)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to update order: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    fun setDateFilter(startDate: Date?, endDate: Date?) {
        _uiState.update {
            it.copy(
                startDate = startDate,
                endDate = endDate
            )
        }
    }

    fun setStatusFilter(status: OrderStatus?) {
        _uiState.update { it.copy(selectedStatus = status) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleFiltersVisibility() {
        _uiState.update { it.copy(showFilters = !it.showFilters) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun filterOrders(orders: List<Order>, state: OrdersMasterUiState): List<Order> {
        return orders.filter { order ->
            // Search filter
            val matchesSearch = if (state.searchQuery.isBlank()) {
                true
            } else {
                order.customerName.contains(state.searchQuery, ignoreCase = true) ||
                        order.mobileNumber.contains(state.searchQuery) ||
                        order.id.contains(state.searchQuery, ignoreCase = true)
            }

            // Date filter
            val matchesDate = if (state.startDate != null && state.endDate != null) {
                order.orderDate >= state.startDate && order.orderDate <= state.endDate
            } else {
                true
            }

            // Status filter
            val matchesStatus = if (state.selectedStatus != null) {
                order.status == state.selectedStatus
            } else {
                true
            }

            matchesSearch && matchesDate && matchesStatus
        }
    }

    private fun determineOrderStatus(order: Order): OrderStatus {
        return when {
            order.delivered -> OrderStatus.DELIVERED
            order.dispatched -> OrderStatus.DISPATCHED
            order.orderPrepared -> OrderStatus.READY
            order.paymentReceived -> OrderStatus.CONFIRMED
            else -> OrderStatus.PENDING
        }
    }
}

data class OrdersMasterUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val selectedStatus: OrderStatus? = null,
    val startDate: Date? = null,
    val endDate: Date? = null,
    val showFilters: Boolean = false
)

enum class OrderStatusField {
    PAYMENT_RECEIVED,
    ORDER_PREPARED,
    DISPATCHED,
    DELIVERED
}