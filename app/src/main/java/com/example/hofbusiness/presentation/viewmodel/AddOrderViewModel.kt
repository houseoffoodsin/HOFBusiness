package com.example.hofbusiness.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hofbusiness.data.model.*
import com.example.hofbusiness.data.repository.CustomerRepository
import com.example.hofbusiness.data.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AddOrderViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddOrderUiState())
    val uiState: StateFlow<AddOrderUiState> = _uiState.asStateFlow()

    private val _customerSuggestions = MutableStateFlow<List<Customer>>(emptyList())
    val customerSuggestions: StateFlow<List<Customer>> = _customerSuggestions.asStateFlow()

    private val menuItems = listOf(
        MenuItem("1", "Ragi Laddu", 650, 370, 185),
        MenuItem("2", "Bellam Sunnundalu", 680, 390, 195),
        MenuItem("3", "Nuvvu Laddu", 500, 300, 150),
        MenuItem("4", "Dry Fruit Laddu", 1100, 600, 300),
        MenuItem("5", "Boondi Laddu", 700, 390, 195),
        MenuItem("6", "Bobbatlu", 500, 300, 0),
        MenuItem("7", "Bellam Gavvalu", 550, 300, 150),
        MenuItem("8", "Kaju Mysorepak", 900, 500, 250),
        MenuItem("9", "Butter Murukulu", 550, 330, 165),
        MenuItem("10", "Janthikalu", 550, 330, 165),
        MenuItem("11", "Chekkalu", 500, 300, 150),
        MenuItem("12", "Hot Boondi", 450, 270, 135),
        MenuItem("13", "Mixture", 500, 300, 150),
        MenuItem("14", "Flax Seed Laddu", 850, 450, 225)
    )

    fun updateCustomerName(name: String) {
        _uiState.update { it.copy(customerName = name) }
        if (name.length >= 2) {
            searchCustomers(name)
        } else {
            _customerSuggestions.value = emptyList()
        }
    }

    fun updateMobileNumber(number: String) {
        _uiState.update { it.copy(mobileNumber = number) }
        if (number.length == 10) {
            searchCustomerByPhone(number)
        }
    }

    fun updateAddress(address: String) {
        _uiState.update { it.copy(address = address) }
    }

    fun updateDeliveryMode(mode: DeliveryMode) {
        _uiState.update { it.copy(deliveryMode = mode) }
    }

    fun updatePaymentMode(mode: PaymentMode) {
        _uiState.update { it.copy(paymentMode = mode) }
    }

    fun addOrderItem(menuItem: MenuItem, size: String, quantity: Int) {
        val unitPrice = menuItem.getPriceForSize(size)
        if (unitPrice > 0) {
            val orderItem = OrderItem(
                menuItemId = menuItem.id,
                menuItemName = menuItem.name,
                size = size,
                quantity = quantity,
                unitPrice = unitPrice,
                totalPrice = unitPrice * quantity
            )

            val currentItems = _uiState.value.orderItems.toMutableList()
            currentItems.add(orderItem)

            val totalAmount = currentItems.sumOf { it.totalPrice }

            _uiState.update {
                it.copy(
                    orderItems = currentItems,
                    totalAmount = totalAmount
                )
            }
        }
    }

    fun removeOrderItem(orderItem: OrderItem) {
        val currentItems = _uiState.value.orderItems.toMutableList()
        currentItems.remove(orderItem)

        val totalAmount = currentItems.sumOf { it.totalPrice }

        _uiState.update {
            it.copy(
                orderItems = currentItems,
                totalAmount = totalAmount
            )
        }
    }

    fun selectCustomerSuggestion(customer: Customer) {
        _uiState.update {
            it.copy(
                customerName = customer.name,
                mobileNumber = customer.mobileNumber,
                address = customer.address
            )
        }
        _customerSuggestions.value = emptyList()
    }

    fun submitOrder() {
        val currentState = _uiState.value

        if (!isValidOrder(currentState)) {
            _uiState.update { it.copy(errorMessage = "Please fill all required fields") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                // Create or update customer
                val customer = Customer(
                    id = currentState.mobileNumber,
                    name = currentState.customerName,
                    mobileNumber = currentState.mobileNumber,
                    address = currentState.address,
                    region = extractRegionFromAddress(currentState.address),
                    firstOrderDate = Date(),
                    totalOrders = 1,
                    lastOrderDate = Date()
                )

                customerRepository.createOrUpdateCustomer(customer)

                // Generate order ID and create order
                val orderId = orderRepository.generateOrderId()
                val order = Order(
                    id = orderId,
                    customerId = currentState.mobileNumber,
                    customerName = currentState.customerName,
                    mobileNumber = currentState.mobileNumber,
                    address = currentState.address,
                    deliveryMode = currentState.deliveryMode,
                    paymentMode = currentState.paymentMode,
                    items = currentState.orderItems,
                    totalAmount = currentState.totalAmount,
                    orderDate = Date(),
                    status = OrderStatus.PENDING
                )

                val result = orderRepository.createOrder(order)

                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isOrderSubmitted = true,
                            generatedOrderId = orderId
                        )
                    }

                    // Update customer order count
                    customerRepository.updateCustomerOrderCount(currentState.mobileNumber)
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to submit order: ${result.exceptionOrNull()?.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    fun resetOrder() {
        _uiState.value = AddOrderUiState()
        _customerSuggestions.value = emptyList()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun searchCustomers(query: String) {
        viewModelScope.launch {
            customerRepository.searchCustomersByName(query)
                .collect { customers ->
                    _customerSuggestions.value = customers.take(5)
                }
        }
    }

    private fun searchCustomerByPhone(phoneNumber: String) {
        viewModelScope.launch {
            val result = customerRepository.getCustomerByPhone(phoneNumber)
            result.getOrNull()?.let { customer ->
                _uiState.update {
                    it.copy(
                        customerName = customer.name,
                        address = customer.address
                    )
                }
            }
        }
    }

    private fun isValidOrder(state: AddOrderUiState): Boolean {
        return state.customerName.isNotBlank() &&
                state.mobileNumber.isNotBlank() &&
                state.address.isNotBlank() &&
                state.orderItems.isNotEmpty()
    }

    private fun extractRegionFromAddress(address: String): String {
        // Simple region extraction logic - can be enhanced
        val parts = address.split(",")
        return if (parts.size >= 2) parts[parts.size - 1].trim() else "Unknown"
    }

    fun getMenuItems(): List<MenuItem> = menuItems
}

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