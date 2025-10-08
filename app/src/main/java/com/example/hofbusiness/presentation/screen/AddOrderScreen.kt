package com.example.hofbusiness.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hofbusiness.data.model.DeliveryMode
import com.example.hofbusiness.data.model.PaymentMode
import com.example.hofbusiness.data.model.MenuItem
import com.example.hofbusiness.presentation.viewmodel.AddOrderViewModel
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import com.example.hofbusiness.data.model.OrderItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrderScreen(
    viewModel: AddOrderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val customerSuggestions by viewModel.customerSuggestions.collectAsState()
    var showAddItemDialog by remember { mutableStateOf(false) }

    // Show success dialog
    if (uiState.isOrderSubmitted) {
        AlertDialog(
            onDismissRequest = { viewModel.resetOrder() },
            title = { Text("Order Submitted Successfully!") },
            text = { Text("Order ID: ${uiState.generatedOrderId}") },
            confirmButton = {
                TextButton(onClick = { viewModel.resetOrder() }) {
                    Text("OK")
                }
            }
        )
    }

    // Show error dialog
    uiState.errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Add New Order",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Customer Details Card
                CustomerDetailsCard(
                    customerName = uiState.customerName,
                    mobileNumber = uiState.mobileNumber,
                    address = uiState.address,
                    customerSuggestions = customerSuggestions,
                    onCustomerNameChange = viewModel::updateCustomerName,
                    onMobileNumberChange = viewModel::updateMobileNumber,
                    onAddressChange = viewModel::updateAddress,
                    onCustomerSuggestionClick = viewModel::selectCustomerSuggestion
                )
            }

            item {
                // Delivery & Payment Card
                DeliveryPaymentCard(
                    deliveryMode = uiState.deliveryMode,
                    paymentMode = uiState.paymentMode,
                    onDeliveryModeChange = viewModel::updateDeliveryMode,
                    onPaymentModeChange = viewModel::updatePaymentMode
                )
            }

            item {
                // Order Items Card
                OrderItemsCard(
                    orderItems = uiState.orderItems,
                    totalAmount = uiState.totalAmount,
                    onAddItemClick = { showAddItemDialog = true },
                    onRemoveItem = viewModel::removeOrderItem
                )
            }

            item {
                // Submit Button
                Button(
                    onClick = { viewModel.submitOrder() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = "Submit Order",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    // Add Item Dialog
    if (showAddItemDialog) {
        AddItemDialog(
            menuItems = viewModel.getMenuItems(),
            onDismiss = { showAddItemDialog = false },
            onAddItem = { menuItem, size, quantity ->
                viewModel.addOrderItem(menuItem, size, quantity)
                showAddItemDialog = false
            }
        )
    }
}

@Composable
fun CustomerDetailsCard(
    customerName: String,
    mobileNumber: String,
    address: String,
    customerSuggestions: List<com.example.hofbusiness.data.model.Customer>,
    onCustomerNameChange: (String) -> Unit,
    onMobileNumberChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onCustomerSuggestionClick: (com.example.hofbusiness.data.model.Customer) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Customer Details",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Customer Name with Suggestions
            Column {
                OutlinedTextField(
                    value = customerName,
                    onValueChange = onCustomerNameChange,
                    label = { Text("Customer Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (customerSuggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column {
                            customerSuggestions.forEach { customer ->
                                Text(
                                    text = "${customer.name} - ${customer.mobileNumber}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onCustomerSuggestionClick(customer) }
                                        .padding(12.dp),
                                    fontSize = 14.sp
                                )
                                if (customer != customerSuggestions.last()) {
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = mobileNumber,
                onValueChange = onMobileNumberChange,
                label = { Text("Mobile Number") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = address,
                onValueChange = onAddressChange,
                label = { Text("Address") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryPaymentCard(
    deliveryMode: DeliveryMode,
    paymentMode: PaymentMode,
    onDeliveryModeChange: (DeliveryMode) -> Unit,
    onPaymentModeChange: (PaymentMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Delivery & Payment",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Delivery Mode Dropdown
            var deliveryExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = deliveryExpanded,
                onExpandedChange = { deliveryExpanded = !deliveryExpanded }
            ) {
                OutlinedTextField(
                    value = deliveryMode.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Delivery Mode") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deliveryExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                )
                ExposedDropdownMenu(
                    expanded = deliveryExpanded,
                    onDismissRequest = { deliveryExpanded = false }
                ) {
                    DeliveryMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.displayName) },
                            onClick = {
                                onDeliveryModeChange(mode)
                                deliveryExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Payment Mode Dropdown
            var paymentExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = paymentExpanded,
                onExpandedChange = { paymentExpanded = !paymentExpanded }
            ) {
                OutlinedTextField(
                    value = paymentMode.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Payment Mode") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                )
                ExposedDropdownMenu(
                    expanded = paymentExpanded,
                    onDismissRequest = { paymentExpanded = false }
                ) {
                    PaymentMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.displayName) },
                            onClick = {
                                onPaymentModeChange(mode)
                                paymentExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

// Add these components to your AddOrderScreen.kt file

@Composable
fun OrderItemsCard(
    orderItems: List<OrderItem>,
    totalAmount: Int,
    onAddItemClick: () -> Unit,
    onRemoveItem: (OrderItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order Items",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Button(
                    onClick = onAddItemClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Item")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Item", color = MaterialTheme.colorScheme.onSecondary)
                }
            }

            if (orderItems.isEmpty()) {
                Text(
                    text = "No items added yet",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                orderItems.forEach { item ->
                    OrderItemRow(
                        orderItem = item,
                        onRemove = { onRemoveItem(item) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "Total: ₹$totalAmount",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun OrderItemRow(
    orderItem: OrderItem,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = orderItem.menuItemName,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${orderItem.size} × ${orderItem.quantity}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        Text(
            text = "₹${orderItem.totalPrice}",
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        TextButton(onClick = onRemove) {
            Text("Remove", color = MaterialTheme.colorScheme.error)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemDialog(
    menuItems: List<MenuItem>,
    onDismiss: () -> Unit,
    onAddItem: (MenuItem, String, Int) -> Unit
) {
    var selectedMenuItem by remember { mutableStateOf<MenuItem?>(null) }
    var selectedSize by remember { mutableStateOf("250g") }
    var quantity by remember { mutableStateOf("1") }
    var menuExpanded by remember { mutableStateOf(false) }
    var sizeExpanded by remember { mutableStateOf(false) }

    val sizes = listOf("250g", "500g", "1000g")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Add Item",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Menu Item Dropdown
                ExposedDropdownMenuBox(
                    expanded = menuExpanded,
                    onExpandedChange = { menuExpanded = !menuExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedMenuItem?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Item") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    )
                    ExposedDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        menuItems.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.name) },
                                onClick = {
                                    selectedMenuItem = item
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Size Dropdown
                ExposedDropdownMenuBox(
                    expanded = sizeExpanded,
                    onExpandedChange = { sizeExpanded = !sizeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedSize,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Size") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sizeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    )
                    ExposedDropdownMenu(
                        expanded = sizeExpanded,
                        onDismissRequest = { sizeExpanded = false }
                    ) {
                        sizes.forEach { size ->
                            // Only show sizes that have a price > 0
                            val price = selectedMenuItem?.getPriceForSize(size) ?: 0
                            if (price > 0) {
                                DropdownMenuItem(
                                    text = { Text("$size - ₹$price") },
                                    onClick = {
                                        selectedSize = size
                                        sizeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quantity Input
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { newValue ->
                        // Only allow numeric input
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            quantity = newValue
                        }
                    },
                    label = { Text("Quantity") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // Price Display
                selectedMenuItem?.let { item ->
                    val price = item.getPriceForSize(selectedSize)

                    if (price > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Price: ₹$price per $selectedSize",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        val quantityInt = quantity.toIntOrNull() ?: 0
                        val totalPrice = price * quantityInt
                        Text(
                            text = "Total: ₹$totalPrice",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            selectedMenuItem?.let { item ->
                                val quantityInt = quantity.toIntOrNull()
                                if (quantityInt != null && quantityInt > 0) {
                                    onAddItem(item, selectedSize, quantityInt)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedMenuItem != null &&
                                quantity.toIntOrNull() != null &&
                                quantity.toIntOrNull()!! > 0 &&
                                selectedMenuItem!!.getPriceForSize(selectedSize) > 0
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}