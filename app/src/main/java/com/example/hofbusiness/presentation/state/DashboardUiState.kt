package com.example.hofbusiness.presentation.state

import com.example.hofbusiness.data.model.DashboardData

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(val data: DashboardData) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}