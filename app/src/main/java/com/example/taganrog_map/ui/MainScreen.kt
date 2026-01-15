package com.example.taganrog_map.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.taganrog_map.R

sealed class Screen(val route: String, val titleRes: Int, val icon: ImageVector) {
    object Map : Screen("map", R.string.nav_map, Icons.Default.LocationOn)
    object Feed : Screen("feed", R.string.nav_feed, Icons.Default.List)
    object Profile : Screen("profile", R.string.nav_profile, Icons.Default.Person)
    object Create : Screen("create", R.string.create_initiative, Icons.Default.Add)
    object Detail : Screen("detail/{initiativeId}", R.string.app_name, Icons.Default.LocationOn) {
        fun createRoute(initiativeId: String) = "detail/$initiativeId"
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    var mapRefreshKey by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.Create.route) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Создать инициативу")
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        bottomBar = {
            val isDetail = currentDestination?.route?.startsWith(Screen.Detail.route.substringBefore("/")) == true
            val isCreate = currentDestination?.route == Screen.Create.route
            if (!isDetail && !isCreate) {
                NavigationBar {
                    val screens = listOf(Screen.Map, Screen.Feed, Screen.Profile)
                    
                    screens.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(stringResource(screen.titleRes)) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Map.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Map.route) {
                MapScreen(
                    refreshKey = mapRefreshKey,
                    onInitiativeClick = { initiativeId ->
                        navController.navigate(Screen.Detail.createRoute(initiativeId))
                    }
                )
            }
            
            composable(Screen.Feed.route) {
                FeedScreen(
                    onInitiativeClick = { initiativeId ->
                        navController.navigate(Screen.Detail.createRoute(initiativeId))
                    }
                )
            }
            
            composable(Screen.Profile.route) {
                ProfileScreen()
            }

            composable(Screen.Create.route) {
                CreateInitiativeScreen(
                    onBackClick = { navController.popBackStack() },
                    onCreated = {
                        mapRefreshKey += 1
                        navController.popBackStack()
                    }
                )
            }
            
            composable(
                route = Screen.Detail.route,
                arguments = listOf(navArgument("initiativeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val initiativeId = backStackEntry.arguments?.getString("initiativeId") ?: ""
                DetailScreen(
                    initiativeId = initiativeId,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
