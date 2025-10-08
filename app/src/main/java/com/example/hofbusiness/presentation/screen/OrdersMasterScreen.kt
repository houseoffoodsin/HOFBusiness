package com.example.hofbusiness.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hofbusiness.data.model.Order
import com.example.hofbusiness.data.model.OrderStatus
import com.example.hofbusiness.presentation.viewmodel.OrdersMasterViewModel
import com.example.hofbusiness.presentation.viewmodel.OrderStatusField
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersMasterScreen(
    viewModel: OrdersMasterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val filteredOrders by viewModel.filteredOrders.collectAsState()

    // Error Dialog
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
        // Header with Filter Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Orders Master List",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            IconButton(onClick = { viewModel.toggleFiltersVisibility() }) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = "Filter",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::setSearchQuery,
            label = { Text("Search orders...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Filters Section
        if (uiState.showFilters) {
            Spacer(modifier = Modifier.height(12.dp))
            FiltersSection(
                selectedStatus = uiState.selectedStatus,
                startDate = uiState.startDate,
                endDate = uiState.endDate,
                onStatusChange = viewModel::setStatusFilter,
                onDateRangeChange = viewModel::setDateFilter
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Loading Indicator
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Orders List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredOrders) { order ->
                    OrderCard(
                        order = order,
                        onUpdateOrderStatus = { field, value ->
                            viewModel.updateOrderStatus(order, field, value)
                        }
                    )
                }

                if (filteredOrders.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No orders found",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersSection(
    selectedStatus: OrderStatus?,
    startDate: Date?,
    endDate: Date?,
    onStatusChange: (OrderStatus?) -> Unit,
    onDateRangeChange: (Date?, Date?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Filters",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Status Filter
            var statusExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = statusExpanded,
                onExpandedChange = { statusExpanded = !statusExpanded }
            ) {
                OutlinedTextField(
                    value = selectedStatus?.name ?: "All Statuses",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Status") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                )
                ExposedDropdownMenu(
                    expanded = statusExpanded,
                    onDismissRequest = { statusExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Statuses") },
                        onClick = {
                            onStatusChange(null)
                            statusExpanded = false
                        }
                    )
                    OrderStatus.entries.forEach { status ->
                        DropdownMenuItem(
                            text = { Text(status.name) },
                            onClick = {
                                onStatusChange(status)
                                statusExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Date Range Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val today = Date()
                        onDateRangeChange(today, today)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Today")
                }

                OutlinedButton(
                    onClick = {
                        val calendar = Calendar.getInstance()
                        calendar.add(Calendar.DAY_OF_YEAR, -7)
                        val weekAgo = calendar.time
                        onDateRangeChange(weekAgo, Date())
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Last 7 Days")
                }

                OutlinedButton(
                    onClick = {
                        onDateRangeChange(null, null)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear")
                }
            }
        }
    }
}

@Composable
fun OrderCard(
    order: Order,
    onUpdateOrderStatus: (OrderStatusField, Boolean) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Order Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Order #${order.id}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = dateFormat.format(order.orderDate),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                StatusChip(status = order.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Customer Details
            Text(
                text = order.customerName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = order.mobileNumber,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Text(
                text = order.address,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Delivery and Payment Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Delivery: ${order.deliveryMode.displayName}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Payment: ${order.paymentMode.displayName}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "₹${order.totalAmount}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Order Items
            Text(
                text = "Items:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            order.items.forEach { item ->
                Text(
                    text = "• ${item.menuItemName} (${item.size}) × ${item.quantity} = ₹${item.totalPrice}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status Checkboxes
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Checkbox(
                            checked = order.paymentReceived,
                            onCheckedChange = {
                                onUpdateOrderStatus(OrderStatusField.PAYMENT_RECEIVED, it)
                            }
                        )
                        Text("Payment Received", fontSize = 12.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Checkbox(
                            checked = order.orderPrepared,
                            onCheckedChange = {
                                onUpdateOrderStatus(OrderStatusField.ORDER_PREPARED, it)
                            }
                        )
                        Text("Order Prepared", fontSize = 12.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Checkbox(
                            checked = order.dispatched,
                            onCheckedChange = {
                                onUpdateOrderStatus(OrderStatusField.DISPATCHED, it)
                            }
                        )
                        Text("Dispatched", fontSize = 12.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Checkbox(
                            checked = order.delivered,
                            onCheckedChange = {
                                onUpdateOrderStatus(OrderStatusField.DELIVERED, it)
                            }
                        )
                        Text("Delivered", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: OrderStatus) {
    val (backgroundColor, textColor) = when (status) {
        OrderStatus.PENDING -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        OrderStatus.CONFIRMED -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        OrderStatus.PREPARING -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        OrderStatus.READY -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        OrderStatus.DISPATCHED -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        OrderStatus.DELIVERED -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
        OrderStatus.COMPLETED -> TODO()
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(4.dp)
    ) {
        Text(
            text = status.name,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}