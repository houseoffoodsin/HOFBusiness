package com.example.hofbusiness.presentation.state

import com.example.hofbusiness.data.model.DeliveryMode
import com.example.hofbusiness.data.model.OrderItem
import com.example.hofbusiness.data.model.PaymentMode


data class AddOrderUiState(
    val customerName: String = "",
    val mobileNumber: String = "",
    val address: String = "",
    val deliveryMode: DeliveryMode = DeliveryMode.PICKUP,
    val paymentMode: PaymentMode = PaymentMode.CASH,
    val orderItems: List<OrderItem> = emptyList(),
    val totalAmount: Int = 0,
    val isLoading: Boolean = false,
    val isOrderSubmitted: Boolean = false,
    val generatedOrderId: String = "",
    val errorMessage: String? = null
)
