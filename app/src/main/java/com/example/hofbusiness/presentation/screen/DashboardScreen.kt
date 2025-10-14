package com.example.hofbusiness.presentation.screen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
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

@RequiresApi(Build.VERSION_CODES.R)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val dashboardMetrics by viewModel.dashboardMetrics.collectAsState()

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

    // Export Success Dialog
    uiState.exportMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.clearExportMessage() },
            title = { Text("Export Successful") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearExportMessage() }) {
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
        // Header with Export Options
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

            ExportOptionsMenu(
                onExportDashboard = { viewModel.exportDashboardReport() },
                onExportOrders = { viewModel.exportOrdersToExcel() },
                onExportAnalytics = { viewModel.exportAnalyticsToExcel() },
                isExporting = uiState.isExporting
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Time Period Selector
        TimePeriodSelector(
            selectedPeriod = selectedPeriod,
            onPeriodSelected = viewModel::selectTimePeriod
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) {
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
                    // Business Insights Section
                    BusinessInsightsSection(dashboardMetrics)
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun ExportOptionsMenu(
    onExportDashboard: () -> Unit,
    onExportOrders: () -> Unit,
    onExportAnalytics: () -> Unit,
    isExporting: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { expanded = true },
            enabled = !isExporting
        ) {
            if (isExporting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Default.Download,
                    contentDescription = "Export Options",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Export Dashboard (PDF)") },
                onClick = {
                    onExportDashboard()
                    expanded = false
                },
                leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("Export Orders (Excel)") },
                onClick = {
                    onExportOrders()
                    expanded = false
                },
                leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("Export Analytics (Excel)") },
                onClick = {
                    onExportAnalytics()
                    expanded = false
                },
                leadingIcon = { Icon(Icons.Default.Analytics, contentDescription = null) }
            )
        }
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
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                MetricCard(
                    title = "Number of Orders",
                    value = "${dashboardMetrics.numberOfOrders}",
                    icon = Icons.Default.ShoppingCart,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            item {
                MetricCard(
                    title = "Average Order Value",
                    value = "₹${dashboardMetrics.averageOrderValue}",
                    icon = Icons.Default.AttachMoney,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            item {
                MetricCard(
                    title = "Retention Rate",
                    value = "${dashboardMetrics.retentionRate}%",
                    icon = Icons.Default.Repeat,
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
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
                text = "Item-wise Distribution",
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
fun BusinessInsightsSection(dashboardMetrics: com.example.hofbusiness.presentation.viewmodel.DashboardMetrics) {
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
                value = dashboardMetrics.mostBoughtItem,
                icon = Icons.AutoMirrored.Filled.TrendingUp
            )

            Spacer(modifier = Modifier.height(8.dp))

            InsightItem(
                title = "Least Popular Item",
                value = dashboardMetrics.leastBoughtItem,
                icon = Icons.AutoMirrored.Filled.TrendingDown
            )

            Spacer(modifier = Modifier.height(8.dp))

            InsightItem(
                title = "Top Region",
                value = dashboardMetrics.mostBoughtRegion,
                icon = Icons.Default.LocationOn
            )

            if (dashboardMetrics.frequentlyBoughtTogether.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
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
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
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