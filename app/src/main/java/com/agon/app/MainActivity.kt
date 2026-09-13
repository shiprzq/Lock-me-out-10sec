package com.agon.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.agon.app.ui.screens.CongratsScreen
import com.agon.app.ui.screens.LockedScreen
import com.agon.app.ui.screens.ReadyScreen
import com.agon.app.ui.screens.StatsScreen
import com.agon.app.ui.theme.AgonAppTheme
import com.agon.app.viewmodel.KickViewModel

class MainActivity : ComponentActivity() {

    private val kickViewModel: KickViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AgonAppTheme {
                MainApp(
                    vm = kickViewModel,
                    onKickOut = { kickUserOut() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-evaluate lockout every time the app comes to foreground.
        // This is what enforces the 10-second ban across re-opens.
        kickViewModel.refresh(System.currentTimeMillis())
    }

    /**
     * THE KICK: slam the door — remove the app from screen AND recents.
     * State was already committed synchronously, so the ban survives instant death.
     */
    private fun kickUserOut() {
        try {
            finishAndRemoveTask()
        } catch (_: Exception) {
            // fallback
        }
        // Ensure the activity is gone even on devices where removeTask behaves oddly
        try {
            finish()
        } catch (_: Exception) { }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(vm: KickViewModel, onKickOut: () -> Unit) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    when (val s = uiState) {
        is KickViewModel.KickUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is KickViewModel.KickUiState.Locked -> {
            // No scaffold, no escape. Show the ban screen briefly, then boot again.
            BackHandler { onKickOut() }
            LockedScreen(
                secondsLeft = s.secondsLeft,
                progress = s.progress,
                taunt = vm.tauntFor(s.secondsLeft),
                kickNumber = s.kickNumber,
                totalKicks = s.totalKicks,
                onKickOutNow = onKickOut
            )
        }

        is KickViewModel.KickUiState.Congrats -> {
            CongratsScreen(
                kickNumber = s.kickNumber,
                totalKicks = s.totalKicks,
                rank = vm.rankFor(s.totalKicks),
                title = vm.congratsTitle(s.kickNumber),
                onKickAgain = {
                    vm.persistKickSync()
                    onKickOut()
                },
                onBackToButton = { vm.acknowledgeCongrats() }
            )
        }

        is KickViewModel.KickUiState.Ready -> {
            val navController = rememberNavController()
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "KICK OUT!",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                },
                bottomBar = { KickBottomNav(navController) }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = "button",
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable("button") {
                        ReadyScreen(
                            totalKicks = s.totalKicks,
                            rank = vm.rankFor(s.totalKicks),
                            lastKickTime = s.lastKickTime,
                            history = s.history,
                            onKickPressed = {
                                vm.persistKickSync()
                                onKickOut()
                            }
                        )
                    }
                    composable("hall") {
                        StatsScreen(
                            totalKicks = s.totalKicks,
                            rank = vm.rankFor(s.totalKicks),
                            history = s.history,
                            firstKickTime = s.firstKickTime,
                            lastKickTime = s.lastKickTime,
                            onReset = { vm.resetAll() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KickBottomNav(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.TouchApp,
                    contentDescription = "Button",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("The Button") },
            selected = currentRoute == "button",
            onClick = {
                navController.navigate("button") {
                    popUpTo("button") { inclusive = true }
                }
            }
        )
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = "Hall of exile",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Hall of Exile") },
            selected = currentRoute == "hall",
            onClick = {
                navController.navigate("hall") {
                    popUpTo("button")
                }
            }
        )
    }
}
