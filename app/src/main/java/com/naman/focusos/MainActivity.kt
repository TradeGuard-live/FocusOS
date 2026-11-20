package com.naman.focusos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.naman.focusos.data.preferences.UserPreferencesManager
import com.naman.focusos.ui.components.FocusScreen
import com.naman.focusos.ui.components.FuturisticBottomBar
import com.naman.focusos.ui.screens.analytics.AnalyticsScreen
import com.naman.focusos.ui.screens.analytics.AnalyticsViewModel
import com.naman.focusos.ui.screens.calendar.CalendarScreen
import com.naman.focusos.ui.screens.calendar.CalendarViewModel
import com.naman.focusos.ui.screens.routines.RoutinesScreen
import com.naman.focusos.ui.screens.routines.RoutinesViewModel
import com.naman.focusos.ui.screens.settings.SettingsScreen
import com.naman.focusos.ui.screens.settings.SettingsViewModel
import com.naman.focusos.ui.screens.today.TodayScreen
import com.naman.focusos.ui.screens.today.TodayViewModel
import com.naman.focusos.ui.theme.FocusOSTheme
import com.naman.focusos.ui.theme.FocusTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val prefsManager = remember { UserPreferencesManager(applicationContext) }
            val settings by prefsManager.userSettingsFlow.collectAsState(initial = com.naman.focusos.data.preferences.UserSettings())

            val isDarkTheme = when (settings.themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            // Notification permission request for Android 13+
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ -> }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            FocusOSTheme(themeMode = settings.themeMode) {
                FocusOSApp()
            }
        }
    }
}

@Composable
fun FocusOSApp() {
    var currentScreen by remember { mutableStateOf(FocusScreen.TODAY) }

    val todayViewModel: TodayViewModel = viewModel()
    val calendarViewModel: CalendarViewModel = viewModel()
    val routinesViewModel: RoutinesViewModel = viewModel()
    val analyticsViewModel: AnalyticsViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    Scaffold(
        containerColor = FocusTheme.colors.background,
        bottomBar = {
            FuturisticBottomBar(
                currentScreen = currentScreen,
                onScreenSelected = { currentScreen = it }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(
                targetState = currentScreen,
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    FocusScreen.TODAY -> TodayScreen(viewModel = todayViewModel)
                    FocusScreen.CALENDAR -> CalendarScreen(viewModel = calendarViewModel)
                    FocusScreen.ROUTINES -> RoutinesScreen(viewModel = routinesViewModel)
                    FocusScreen.ANALYTICS -> AnalyticsScreen(viewModel = analyticsViewModel)
                    FocusScreen.SETTINGS -> SettingsScreen(viewModel = settingsViewModel)
                }
            }
        }
    }
}
