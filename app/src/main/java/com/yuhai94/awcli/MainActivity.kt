package com.yuhai94.awcli

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yuhai94.awcli.ui.CreateInstanceScreen
import com.yuhai94.awcli.ui.HomeScreen
import com.yuhai94.awcli.ui.InstanceDetailScreen
import com.yuhai94.awcli.ui.LogsScreen
import com.yuhai94.awcli.ui.Routes
import com.yuhai94.awcli.ui.SettingsScreen
import com.yuhai94.awcli.ui.theme.AwCliTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AwCliTheme {
                val navController = rememberNavController()
                val mainViewModel: MainViewModel = viewModel()
                val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(uiState.config, uiState.tunnelState) {
                    if (uiState.config == null) {
                        val currentRoute = navController.currentDestination?.route
                        if (currentRoute != Routes.Settings) {
                            navController.navigate(Routes.Settings) {
                                popUpTo(Routes.Home) { inclusive = true }
                            }
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = Routes.Home,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(Routes.Home) {
                            HomeScreen(
                                onCreateClick = { navController.navigate(Routes.Create) },
                                onSettingsClick = { navController.navigate(Routes.Settings) },
                                onInstanceClick = { uuid ->
                                    navController.navigate("${Routes.Detail}/$uuid")
                                }
                            )
                        }
                        composable(Routes.Create) {
                            CreateInstanceScreen(
                                onBack = { navController.popBackStack() },
                                onCreated = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = "${Routes.Detail}/{uuid}",
                            arguments = listOf(navArgument("uuid") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val uuid = backStackEntry.arguments?.getString("uuid").orEmpty()
                            InstanceDetailScreen(
                                uuid = uuid,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Routes.Settings) {
                            SettingsScreen(
                                onBack = { navController.popBackStack() },
                                onLogsClick = { navController.navigate(Routes.Logs) }
                            )
                        }
                        composable(Routes.Logs) {
                            LogsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }

                    val currentRoute = navController.currentDestination?.route
                    if (uiState.isConfigReady && !uiState.isConnected && currentRoute != Routes.Settings) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Text(
                                    text = "正在与服务器建立连接中...",
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        AppContainer.appForeground.value = true
    }

    override fun onStop() {
        super.onStop()
        AppContainer.appForeground.value = false
    }
}
