package com.example.hofbusiness.data.model

import java.util.Date

data class Customer(
    val id: String = "",
    val name: String = "",
    val mobileNumber: String = "",
    val address: String = "",
    val region: String = "",
    val firstOrderDate: Date = Date(),
    val totalOrders: Int = 0,
    val lastOrderDate: Date = Date()
)