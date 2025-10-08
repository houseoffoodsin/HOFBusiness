package com.example.hofbusiness.data.model

data class OrderItem(
    val menuItemId: String = "",
    val menuItemName: String = "",
    val size: String = "",
    val quantity: Int = 0,
    val unitPrice: Int = 0,
    val totalPrice: Int = 0
)