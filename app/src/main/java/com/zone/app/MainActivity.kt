package com.zone.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Zone.init(this)
        setContent {
            val dark = resolvedDark()
            ZoneTheme(dark) {
                ZoneApp()
            }
        }
    }
}

@Composable
fun ZoneApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val dark = resolvedDark()

    val view = LocalView.current
    SideEffect {
        val window = (view.context as android.app.Activity).window
        window.statusBarColor = if (dark) 0xFF000000.toInt() else 0xFFF5F5F7.toInt()
        window.navigationBarColor = if (dark) 0xFF000000.toInt() else 0xFFF5F5F7.toInt()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            ZoneBottomBar(
                current = route,
                onNavigate = { navController.navigateTop(it) }
            )
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") { HomeScreen { navController.navigateTop(it) } }
            composable("diary") { DiaryScreen() }
            composable("lists") { ListsScreen() }
            composable("habits") { HabitsScreen() }
            composable("notes") { NotesScreen() }
            composable("money") { MoneyScreen() }
            composable("passbook") { PassbookScreen() }
            composable("split") { SplitScreen() }
            composable("mileage") { MileageScreen() }
            composable("watchlist") { WatchlistScreen() }
            composable("links") { LinksScreen() }
            composable("subscriptions") { SubscriptionsScreen() }
            composable("settings") { SettingsScreen() }
            composable("admin") { AdminScreen() }
            composable("more") { MoreScreen { navController.navigateTop(it) } }
        }
    }
}

private val primaryRoutes = listOf("home", "diary", "lists", "money")
private val moreRoutes = listOf(
    "notes", "habits", "passbook", "split", "mileage", "watchlist",
    "links", "subscriptions", "settings", "admin"
)

private fun NavHostController.navigateTop(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun ZoneBottomBar(
    current: String?,
    onNavigate: (String) -> Unit
) {
    val items = listOf("home" to "Home", "diary" to "Diary", "lists" to "Lists", "money" to "Money", "more" to "More")
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        items.forEach { (route, label) ->
            val selected = when (route) {
                "more" -> current != null && current in moreRoutes
                else -> current == route
            }
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(route) },
                icon = { Icon(iconFor(route), contentDescription = label) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ZoneColors.blue,
                    selectedTextColor = ZoneColors.blue,
                    indicatorColor = ZoneColors.blue.copy(alpha = 0.10f),
                    unselectedIconColor = ZoneColors.secondary,
                    unselectedTextColor = ZoneColors.secondary
                )
            )
        }
    }
}