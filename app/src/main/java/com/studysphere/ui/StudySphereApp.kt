package com.studysphere.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.studysphere.ui.screens.SubjectsScreen
import com.studysphere.ui.screens.assignments.AssignmentsScreen
import com.studysphere.ui.screens.attendance.AttendanceDetailScreen
import com.studysphere.ui.screens.attendance.AttendanceScreen
import com.studysphere.ui.screens.dashboard.DashboardScreen
import com.studysphere.ui.theme.LocalDarkTheme
import com.studysphere.viewmodel.MainViewModel

data class BottomNavItem(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem("Home",        Screen.Dashboard.route,   Icons.Rounded.Home,            Icons.Rounded.HomeWork),
    BottomNavItem("Attendance",  Screen.Attendance.route,  Icons.Rounded.HowToReg,        Icons.Rounded.AppRegistration),
    BottomNavItem("Assignments", Screen.Assignments.route, Icons.Rounded.Assignment,       Icons.Rounded.Assignment),
    BottomNavItem("Subjects",    Screen.Subjects.route,    Icons.Rounded.LibraryBooks,    Icons.Rounded.LibraryBooks),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudySphereApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isDark = LocalDarkTheme.current

    // Top bar titles
    val currentRoute = currentDestination?.route ?: ""
    val topBarTitle = when {
        currentRoute == Screen.Dashboard.route  -> "Home"
        currentRoute == Screen.Attendance.route -> "Attendance"
        currentRoute.startsWith("attendance_detail/") -> "Subject Details"
        currentRoute == Screen.Assignments.route -> "Assignments"
        currentRoute == Screen.Subjects.route    -> "Subjects"
        else -> "StudySphere"
    }

    val showBottomBar = bottomNavItems.any { it.route == currentRoute }
    val showBackButton = currentRoute.startsWith("attendance_detail/")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = topBarTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Rounded.ArrowBackIos, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    // Theme toggle
                    IconButton(onClick = { viewModel.toggleTheme() }) {
                        Icon(
                            imageVector = if (isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                            contentDescription = if (isDark) "Switch to light mode" else "Switch to dark mode"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter   = slideInVertically { it } + fadeIn(),
                exit    = slideOutVertically { it } + fadeOut()
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            selected = selected,
                            onClick  = {
                                if (selected) return@NavigationBarItem

                                if (item.route == Screen.Dashboard.route) {
                                    val poppedToDashboard = navController.popBackStack(
                                        Screen.Dashboard.route,
                                        inclusive = false
                                    )
                                    if (!poppedToDashboard) {
                                        navController.navigate(Screen.Dashboard.route) {
                                            launchSingleTop = true
                                        }
                                    }
                                } else {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState    = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor   = MaterialTheme.colorScheme.primary,
                                selectedTextColor   = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor      = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Dashboard.route,
            modifier         = Modifier.padding(innerPadding),
            enterTransition  = { fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) },
            exitTransition   = { fadeOut(animationSpec = androidx.compose.animation.core.tween(200)) }
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel              = viewModel,
                    onNavigateToAttendance = { navController.navigate(Screen.Attendance.route) },
                    onNavigateToAssignments = { navController.navigate(Screen.Assignments.route) },
                    onNavigateToSubjectDetail = { subjectId ->
                        navController.navigate(Screen.AttendanceDetail.createRoute(subjectId))
                    },
                    onNavigateToSubjects   = { navController.navigate(Screen.Subjects.route) }
                )
            }
            composable(Screen.Attendance.route) {
                AttendanceScreen(
                    viewModel            = viewModel,
                    onSubjectDetail      = { subjectId ->
                        navController.navigate(Screen.AttendanceDetail.createRoute(subjectId))
                    },
                    onNavigateToSubjects = { navController.navigate(Screen.Subjects.route) }
                )
            }
            composable(Screen.AttendanceDetail.route) { backStackEntry ->
                val subjectId = backStackEntry.arguments?.getString("subjectId")?.toLongOrNull() ?: 0L
                AttendanceDetailScreen(
                    subjectId  = subjectId,
                    viewModel  = viewModel,
                    onBack     = { navController.popBackStack() }
                )
            }
            composable(Screen.Assignments.route) {
                AssignmentsScreen(viewModel = viewModel)
            }
            composable(Screen.Subjects.route) {
                SubjectsScreen(viewModel = viewModel)
            }
        }
    }
}
