package com.example.hofbusiness.presentation.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hofbusiness.presentation.viewmodel.DashboardViewModel
import com.example.hofbusiness.presentation.viewmodel.ItemDistribution
import com.example.hofbusiness.presentation.viewmodel.TimePeriod
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import java.io.File

@RequiresApi(Build.VERSION_CODES.R)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val dashboardMetrics by viewModel.dashboardMetrics.collectAsState()
    var showExportDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    val context = LocalContext.current
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
        // Header with Export Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Dashboard",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            IconButton(
                onClick = { showExportDialog = true },
                enabled = !isExporting
            ) {
                Icon(
                    Icons.Default.ArrowDownward,
                    contentDescription = "Export Report",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Time Period Selector
        TimePeriodSelector(
            selectedPeriod = selectedPeriod,
            onPeriodSelected = viewModel::selectTimePeriod
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.Loading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Key Metrics Cards
                    KeyMetricsSection(dashboardMetrics)
                }

                item {
                    // Item Distribution Pie Chart
                    if (dashboardMetrics.itemDistribution.isNotEmpty()) {
                        ItemDistributionChart(dashboardMetrics.itemDistribution)
                    }
                }

                item {
                    // Insights Section
                    InsightsSection(dashboardMetrics)
                }
            }
        }


        // Export Dialog
        if (showExportDialog) {
            ExportDialog(
                onDismiss = { showExportDialog = false },
                onExport = { exportType, dateRange ->
                    showExportDialog = false
                    isExporting = true

                    // Perform export
                    viewModel.exportData(
                        exportType = exportType,
                        dateRange = dateRange,
                        onSuccess = { filePath ->
                            isExporting = false
                            // Show success message
                            Toast.makeText(
                                context,
                                "Report exported to: $filePath",
                                Toast.LENGTH_LONG
                            ).show()

                            // Optionally open the file or share it
                            shareExportedFile(context, filePath)
                        },
                        onError = { error ->
                            isExporting = false
                            Toast.makeText(
                                context,
                                "Export failed: $error",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                }
            )
        }
    }
}

// Export Dialog Component
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onExport: (ExportType, DateRange) -> Unit
) {
    var selectedExportType by remember { mutableStateOf(ExportType.ORDERS) }
    var selectedDateRange by remember { mutableStateOf(DateRange.LAST_7_DAYS) }
    var exportTypeExpanded by remember { mutableStateOf(false) }
    var dateRangeExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Export Report",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Export Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = exportTypeExpanded,
                    onExpandedChange = { exportTypeExpanded = !exportTypeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedExportType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Export Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = exportTypeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    )
                    ExposedDropdownMenu(
                        expanded = exportTypeExpanded,
                        onDismissRequest = { exportTypeExpanded = false }
                    ) {
                        ExportType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                onClick = {
                                    selectedExportType = type
                                    exportTypeExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Date Range Dropdown
                ExposedDropdownMenuBox(
                    expanded = dateRangeExpanded,
                    onExpandedChange = { dateRangeExpanded = !dateRangeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedDateRange.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Date Range") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dateRangeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    )
                    ExposedDropdownMenu(
                        expanded = dateRangeExpanded,
                        onDismissRequest = { dateRangeExpanded = false }
                    ) {
                        DateRange.entries.forEach { range ->
                            DropdownMenuItem(
                                text = { Text(range.displayName) },
                                onClick = {
                                    selectedDateRange = range
                                    dateRangeExpanded = false
                                }
                            )
                        }
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
                        onClick = { onExport(selectedExportType, selectedDateRange) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Export")
                    }
                }
            }
        }
    }
}

// Export Types and Date Ranges
enum class ExportType(val displayName: String) {
    ORDERS("Orders Report"),
    ANALYTICS("Analytics Report"),
    INVENTORY("Inventory Report"),
    CUSTOMERS("Customers Report")
}

enum class DateRange(val displayName: String) {
    TODAY("Today"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    CUSTOM("Custom Range")
}

// Helper function to share exported file
fun shareExportedFile(context: Context, filePath: String) {
    try {
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share Report"))
    } catch (e: Exception) {
        Toast.makeText(context, "Could not share file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun TimePeriodSelector(
    selectedPeriod: TimePeriod,
    onPeriodSelected: (TimePeriod) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(TimePeriod.entries.toTypedArray()) { period ->
            FilterChip(
                onClick = { onPeriodSelected(period) },
                label = { Text(period.displayName) },
                selected = selectedPeriod == period,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
fun KeyMetricsSection(dashboardMetrics: com.example.hofbusiness.presentation.viewmodel.DashboardMetrics) {
    Column {
        Text(
            text = "Key Metrics",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MetricCard(
                    title = "Total Revenue",
                    value = "₹${dashboardMetrics.totalRevenue}",
                    icon = Icons.Default.Star,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                MetricCard(
                    title = "Number of Orders",
                    value = "${dashboardMetrics.numberOfOrders}",
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            item {
                MetricCard(
                    title = "Average Order Value",
                    value = "₹${dashboardMetrics.averageOrderValue}",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            item {
                MetricCard(
                    title = "Retention Rate",
                    value = "${dashboardMetrics.retentionRate}%",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    color: Color
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = color,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun ItemDistributionChart(itemDistribution: List<ItemDistribution>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Item Distribution",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Pie Chart
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PieChart(
                        data = itemDistribution.take(6), // Show top 6 items
                        modifier = Modifier.size(180.dp)
                    )
                }

                // Legend
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemDistribution.take(6).forEachIndexed { index, item ->
                        LegendItem(
                            color = getChartColor(index),
                            label = item.itemName,
                            percentage = item.percentage
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PieChart(
    data: List<ItemDistribution>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val total = data.sumOf { it.quantity.toDouble() }.toFloat()
        var startAngle = 0f

        data.forEachIndexed { index, item ->
            val sweepAngle = (item.quantity / total) * 360f
            val color = getChartColor(index)

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(0f, 0f),
                size = Size(size.width, size.height)
            )

            startAngle += sweepAngle
        }
    }
}

@Composable
fun LegendItem(
    color: Color,
    label: String,
    percentage: Float
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )

        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = "${percentage.toInt()}%",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun InsightsSection(dashboardMetrics: com.example.hofbusiness.presentation.viewmodel.DashboardMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Business Insights",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            InsightItem(
                title = "Most Popular Item",
                value = dashboardMetrics.mostBoughtItem
            )

            InsightItem(
                title = "Least Popular Item",
                value = dashboardMetrics.leastBoughtItem
            )

            InsightItem(
                title = "Top Region",
                value = dashboardMetrics.mostBoughtRegion
            )

            if (dashboardMetrics.frequentlyBoughtTogether.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Frequently Bought Together:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                dashboardMetrics.frequentlyBoughtTogether.forEach { pair ->
                    Text(
                        text = "• $pair",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InsightItem(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

fun getChartColor(index: Int): Color {
    val colors = listOf(
        Color(0xFF2196F3), // Blue
        Color(0xFF4CAF50), // Green
        Color(0xFFFF9800), // Orange
        Color(0xFF9C27B0), // Purple
        Color(0xFFF44336), // Red
        Color(0xFF00BCD4), // Cyan
        Color(0xFFFFEB3B), // Yellow
        Color(0xFF795548)  // Brown
    )
    return colors[index % colors.size]
}