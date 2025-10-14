package com.example.hofbusiness.data.service

import android.content.Context
import android.os.Build
import android.os.storage.StorageManager
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.hofbusiness.data.model.Customer
import com.example.hofbusiness.data.model.DailyAnalytics
import com.example.hofbusiness.data.model.MenuItem
import com.example.hofbusiness.data.model.Order
import com.example.hofbusiness.data.model.OrderStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportService @Inject constructor(
    private val context: Context
) {

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun exportOrdersToExcel(orders: List<Order>): String = withContext(Dispatchers.IO) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Orders Report")

        // Create header row
        val headerRow = sheet.createRow(0)
        val headers = arrayOf(
            "Order ID", "Customer Name", "Phone", "Order Date",
            "Status", "Delivery Mode", "Payment Mode", "Total Amount", "Items"
        )

        headers.forEachIndexed { index, header ->
            headerRow.createCell(index).setCellValue(header)
        }

        // Add data rows
        orders.forEachIndexed { rowIndex, order ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(order.id)
            row.createCell(1).setCellValue(order.customerName)
            row.createCell(2).setCellValue(order.mobileNumber)
            row.createCell(3).setCellValue(
                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(order.orderDate)
            )
            row.createCell(4).setCellValue(order.status.name)
            row.createCell(5).setCellValue(order.deliveryMode.displayName)
            row.createCell(6).setCellValue(order.paymentMode.displayName)
            row.createCell(7).setCellValue(order.totalAmount.toDouble())

            // Items as comma-separated string
            val itemsString = order.items.joinToString(", ") {
                "${it.menuItemName} (${it.size}) x${it.quantity}"
            }
            row.createCell(8).setCellValue(itemsString)
        }

        // Auto-size columns
//        headers.indices.forEach { sheet.autoSizeColumn(it) }

        // Save file
        val fileName = "orders_report_${System.currentTimeMillis()}.xlsx"
        val file = File(getExportsDirectory(), fileName)

        FileOutputStream(file).use { outputStream ->
            workbook.write(outputStream)
        }
        workbook.close()

        file.absolutePath
    }

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun exportAnalyticsToExcel(analytics: List<DailyAnalytics>): String = withContext(Dispatchers.IO) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Analytics Report")

        // Create header row
        val headerRow = sheet.createRow(0)
        val headers = arrayOf(
            "Date", "Total Orders", "Total Revenue", "Average Order Value",
            "Top Item", "Top Item Quantity", "Pending Orders", "Completed Orders"
        )

        headers.forEachIndexed { index, header ->
            headerRow.createCell(index).setCellValue(header)
        }

        // Add data rows
        analytics.forEachIndexed { rowIndex, data ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(data.date)
            )
            row.createCell(1).setCellValue(data.totalOrders.toDouble())
            row.createCell(2).setCellValue(data.totalRevenue.toDouble())
            row.createCell(3).setCellValue(data.averageOrderValue.toDouble())
            row.createCell(4).setCellValue(data.topSellingItem)
            row.createCell(5).setCellValue(data.topSellingItemQuantity.toDouble())
            row.createCell(6).setCellValue(data.pendingOrders.toDouble())
            row.createCell(7).setCellValue(data.completedOrders.toDouble())
        }

        // Auto-size columns
//        headers.indices.forEach { sheet.autoSizeColumn(it) }

        // Save file
        val fileName = "analytics_report_${System.currentTimeMillis()}.xlsx"
        val file = File(getExportsDirectory(), fileName)

        FileOutputStream(file).use { outputStream ->
            workbook.write(outputStream)
        }
        workbook.close()

        file.absolutePath
    }

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun exportInventoryToExcel(): String = withContext(Dispatchers.IO) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Inventory Report")

        // Create header row
        val headerRow = sheet.createRow(0)
        val headers = arrayOf(
            "Item Name", "Price 250g", "Price 500g", "Price 1000g", "Available"
        )

        headers.forEachIndexed { index, header ->
            headerRow.createCell(index).setCellValue(header)
        }

        // Sample inventory data - replace with actual data source
        val sampleItems = listOf(
            MenuItem("1", "Chicken Biryani", 1200, 600, 300, true),
            MenuItem("2", "Mutton Biryani", 1500, 800, 400, true),
            MenuItem("3", "Veg Biryani", 1000, 500, 250, true),
            MenuItem("4", "Chicken Dum Biryani", 1300, 650, 325, true),
            MenuItem("5", "Mutton Dum Biryani", 1600, 850, 425, true),
            MenuItem("6", "Prawn Biryani", 1400, 700, 350, true),
            MenuItem("7", "Fish Biryani", 1350, 675, 340, true),
            MenuItem("8", "Egg Biryani", 900, 450, 225, true)
        )

        sampleItems.forEachIndexed { rowIndex, item ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(item.name)
            row.createCell(1).setCellValue(item.price250g.toDouble())
            row.createCell(2).setCellValue(item.price500g.toDouble())
            row.createCell(3).setCellValue(item.price1000g.toDouble())
            row.createCell(4).setCellValue(if (item.isAvailable) "Yes" else "No")
        }

        // Auto-size columns
        headers.indices.forEach { sheet.autoSizeColumn(it) }

        // Save file
        val fileName = "inventory_report_${System.currentTimeMillis()}.xlsx"
        val file = File(getExportsDirectory(), fileName)

        FileOutputStream(file).use { outputStream ->
            workbook.write(outputStream)
        }
        workbook.close()

        file.absolutePath
    }

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun exportCustomersToExcel(customers: List<Customer>): String = withContext(Dispatchers.IO) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Customers Report")

        // Create header row
        val headerRow = sheet.createRow(0)
        val headers = arrayOf(
            "Customer ID", "Name", "Mobile Number", "Address", "Region",
            "First Order Date", "Last Order Date", "Total Orders"
        )

        headers.forEachIndexed { index, header ->
            headerRow.createCell(index).setCellValue(header)
        }

        // Add data rows using your existing Customer data class
        customers.forEachIndexed { rowIndex, customer ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(customer.id)
            row.createCell(1).setCellValue(customer.name)
            row.createCell(2).setCellValue(customer.mobileNumber)
            row.createCell(3).setCellValue(customer.address)
            row.createCell(4).setCellValue(customer.region)
            row.createCell(5).setCellValue(
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(customer.firstOrderDate)
            )
            row.createCell(6).setCellValue(
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(customer.lastOrderDate)
            )
            row.createCell(7).setCellValue(customer.totalOrders.toDouble())
        }

        // Auto-size columns
        headers.indices.forEach { sheet.autoSizeColumn(it) }

        // Save file
        val fileName = "customers_report_${System.currentTimeMillis()}.xlsx"
        val file = File(getExportsDirectory(), fileName)

        FileOutputStream(file).use { outputStream ->
            workbook.write(outputStream)
        }
        workbook.close()
        file.absolutePath
    }

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun exportOrdersToCSV(orders: List<Order>): String = withContext(Dispatchers.IO) {
        val fileName = "orders_report_${System.currentTimeMillis()}.csv"
        val file = File(getExportsDirectory(), fileName)

        file.bufferedWriter().use { writer ->
            // Write header
            writer.appendLine("Order ID,Customer Name,Phone,Order Date,Status,Delivery Mode,Payment Mode,Total Amount,Items")

            // Write data
            orders.forEach { order ->
                val itemsString = order.items.joinToString("; ") {
                    "${it.menuItemName} (${it.size}) x${it.quantity}"
                }

                writer.appendLine(
                    "${order.id},${order.customerName},${order.mobileNumber}," +
                            "${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(order.orderDate)}," +
                            "${order.status.name},${order.deliveryMode.displayName},${order.paymentMode.displayName}," +
                            "${order.totalAmount},\"${itemsString}\""
                )
            }
        }

        file.absolutePath
    }

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun exportDashboardReport(
        orders: List<Order>,
        analytics: List<DailyAnalytics>,
        period: String
    ): String = withContext(Dispatchers.IO) {
        val fileName = "dashboard_report_${System.currentTimeMillis()}.txt"
        val file = File(getExportsDirectory(), fileName)

        file.bufferedWriter().use { writer ->
            // Header with proper string repetition
            writer.appendLine("HOUSE OF FOODS - DASHBOARD REPORT")
            writer.appendLine("Period: $period")
            writer.appendLine("Generated: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}")
            writer.appendLine("=".repeat(50))
            writer.appendLine()

            // Summary Statistics
            writer.appendLine("SUMMARY STATISTICS")
            writer.appendLine("-".repeat(20))
            writer.appendLine("Total Orders: ${orders.size}")
            writer.appendLine("Total Revenue: ₹${orders.sumOf { it.totalAmount }}")
            writer.appendLine("Average Order Value: ₹${if (orders.isNotEmpty()) orders.sumOf { it.totalAmount } / orders.size else 0}")
            writer.appendLine("Pending Orders: ${orders.count { it.status == OrderStatus.PENDING }}")
            writer.appendLine("Completed Orders: ${orders.count { it.status == OrderStatus.COMPLETED }}")
            writer.appendLine("Cancelled Orders: ${orders.count { it.status == OrderStatus.CANCELLED }}")
            writer.appendLine()

            // Top Items
            writer.appendLine("TOP SELLING ITEMS")
            writer.appendLine("-".repeat(20))
            val topItems = orders
                .flatMap { it.items }
                .groupBy { it.menuItemName }
                .mapValues { (_, items) -> items.sumOf { it.quantity } }
                .toList()
                .sortedByDescending { it.second }
                .take(5)

            topItems.forEachIndexed { index, (item, quantity) ->
                writer.appendLine("${index + 1}. $item - $quantity orders")
            }
            writer.appendLine()

            // Recent Orders
            writer.appendLine("RECENT ORDERS")
            writer.appendLine("-".repeat(15))
            orders.sortedByDescending { it.orderDate }.take(10).forEach { order ->
                writer.appendLine(
                    "${SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(order.orderDate)} - " +
                            "${order.customerName} - ₹${order.totalAmount} - ${order.status.name}"
                )
            }
            writer.appendLine()

            // Daily Analytics (if available)
            if (analytics.isNotEmpty()) {
                writer.appendLine("DAILY ANALYTICS")
                writer.appendLine("-".repeat(16))
                analytics.forEach { daily ->
                    writer.appendLine(
                        "${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(daily.date)} - " +
                                "Orders: ${daily.totalOrders}, Revenue: ₹${daily.totalRevenue}"
                    )
                }
            }

            writer.appendLine()
            writer.appendLine("=".repeat(50))
            writer.appendLine("End of Report")
        }

        file.absolutePath
    }

    // Updated storage directory logic for Android 13+
    @RequiresApi(Build.VERSION_CODES.R)
    private fun getExportsDirectory(): File {
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val storageVolume = storageManager.storageVolumes[0]
        val directory = File(storageVolume.directory?.path + "/Download/HouseOfFoods")

        if (!directory.exists()) {
            val dirCreated = directory.mkdirs()
            Log.d("DirectoryCreation", "Directory created: $dirCreated")
        }

        return directory
    }
}