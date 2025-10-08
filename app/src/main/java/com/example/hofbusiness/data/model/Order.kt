package com.example.hofbusiness.data.model

import java.util.Date

data class Order(
    val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val mobileNumber: String = "",
    val address: String = "",
    val deliveryMode: DeliveryMode = DeliveryMode.PICKUP,
    val paymentMode: PaymentMode = PaymentMode.CASH,
    val items: List<OrderItem> = emptyList(),
    val totalAmount: Int = 0,
    val orderDate: Date = Date(),
    val status: OrderStatus = OrderStatus.PENDING,
    val paymentReceived: Boolean = false,
    val orderPrepared: Boolean = false,
    val dispatched: Boolean = false,
    val delivered: Boolean = false
)

enum class DeliveryMode(val displayName: String) {
    PICKUP("Pickup"),
    DELIVERY("Delivery"),
    BIKE_TAXI("Bike Taxi"),
    SELF_DELIVERY("Self Delivery")
}

enum class PaymentMode(val displayName: String) {
    CASH("Cash"),
    UPI("UPI"),
    OTHER("Other")
}

enum class OrderStatus {
    PENDING,
    CONFIRMED,
    PREPARING,
    READY,
    DISPATCHED,
    DELIVERED,
    CANCELLED,
    COMPLETED
}