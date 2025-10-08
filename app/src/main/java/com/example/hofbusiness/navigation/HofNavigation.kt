package com.example.hofbusiness.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.hofbusiness.presentation.screen.AddOrderScreen
import com.example.hofbusiness.presentation.screen.DashboardScreen
import com.example.hofbusiness.presentation.screen.KitchenPrepScreen
import com.example.hofbusiness.presentation.screen.OrdersMasterScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object AddOrder : Screen("add_order", "Add Order", Icons.Default.Add)
    object OrdersList : Screen("orders_list", "Orders", Icons.AutoMirrored.Default.List)
    object KitchenPrep : Screen("kitchen_prep", "Kitchen", Icons.Default.Kitchen)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HofNavigation() {
    val navController = rememberNavController()
    val screens = listOf(
        Screen.Dashboard,
        Screen.AddOrder,
        Screen.OrdersList,
        Screen.KitchenPrep
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen()
            }
            composable(Screen.AddOrder.route) {
                AddOrderScreen()
            }
            composable(Screen.OrdersList.route) {
                OrdersMasterScreen()
            }
            composable(Screen.KitchenPrep.route) {
                KitchenPrepScreen()
            }
        }
    }
}