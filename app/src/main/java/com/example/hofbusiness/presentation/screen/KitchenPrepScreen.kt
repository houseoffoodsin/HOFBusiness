package com.example.hofbusiness.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hofbusiness.presentation.viewmodel.KitchenPrepViewModel
import com.example.hofbusiness.presentation.viewmodel.PrepItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitchenPrepScreen(
    viewModel: KitchenPrepViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val prepItems by viewModel.prepItems.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

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
        // Header
        Text(
            text = "Kitchen Prep List",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Date Selection and Actions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Date Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Prep Date: ${dateFormat.format(selectedDate)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )

                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = "Select Date",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.markAllPrepared() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mark All")
                    }

                    OutlinedButton(
                        onClick = { viewModel.resetAllPreparation() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset All")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Summary Card
        if (prepItems.isNotEmpty()) {
            PrepSummaryCard(prepItems = prepItems)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Loading Indicator
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Prep Items List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (prepItems.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No items to prepare for this date",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    // Group items by preparation status
                    val (unprepared, prepared) = prepItems.partition { !it.isPrepared }

                    if (unprepared.isNotEmpty()) {
                        item {
                            Text(
                                text = "Pending Preparation (${unprepared.size})",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        items(unprepared) { prepItem ->
                            PrepItemCard(
                                prepItem = prepItem,
                                onTogglePrepared = { viewModel.toggleItemPrepared(prepItem) }
                            )
                        }
                    }

                    if (prepared.isNotEmpty()) {
                        item {
                            Text(
                                text = "Completed (${prepared.size})",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        items(prepared) { prepItem ->
                            PrepItemCard(
                                prepItem = prepItem,
                                onTogglePrepared = { viewModel.toggleItemPrepared(prepItem) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { date ->
                viewModel.selectDate(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
fun PrepSummaryCard(prepItems: List<PrepItem>) {
    val totalItems = prepItems.size
    val completedItems = prepItems.count { it.isPrepared }
    val totalQuantity = prepItems.sumOf { it.totalQuantity }
    val completedQuantity = prepItems.filter { it.isPrepared }.sumOf { it.totalQuantity }
    val progress = if (totalItems > 0) completedItems.toFloat() / totalItems else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Preparation Summary",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = ProgressIndicatorDefaults.linearTrackColor,
            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Items: $completedItems/$totalItems",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Quantity: $completedQuantity/$totalQuantity",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
fun PrepItemCard(
    prepItem: PrepItem,
    onTogglePrepared: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (prepItem.isPrepared) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = prepItem.menuItemName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (prepItem.isPrepared) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                Text(
                    text = "${prepItem.size} × ${prepItem.totalQuantity}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Text(
                    text = "Orders: ${prepItem.orderIds.joinToString(", ")}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "₹${prepItem.totalValue}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Checkbox(
                    checked = prepItem.isPrepared,
                    onCheckedChange = { onTogglePrepared() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(Date(millis))
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}