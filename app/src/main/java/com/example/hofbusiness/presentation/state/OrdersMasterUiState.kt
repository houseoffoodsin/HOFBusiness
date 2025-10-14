package com.example.hofbusiness.presentation.state

import com.example.hofbusiness.data.model.OrderStatus
import java.util.Date

data class OrdersMasterUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val selectedStatus: OrderStatus? = null,
    val startDate: Date? = null,
    val endDate: Date? = null,
    val showFilters: Boolean = false
)