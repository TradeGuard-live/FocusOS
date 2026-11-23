package com.naman.focusos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.naman.focusos.data.preferences.UserPreferencesManager
import com.naman.focusos.service.FocusTimerService
import com.naman.focusos.ui.components.FocusScreen
import com.naman.focusos.ui.components.FuturisticBottomBar
import com.naman.focusos.ui.components.SplashScreen
import com.naman.focusos.ui.screens.analytics.AnalyticsScreen
import com.naman.focusos.ui.screens.analytics.AnalyticsViewModel
import com.naman.focusos.ui.screens.calendar.CalendarScreen
import com.naman.focusos.ui.screens.calendar.CalendarViewModel
import com.naman.focusos.ui.screens.focus.FocusViewModel
import com.naman.focusos.ui.screens.focus.FocusWorkScreen
import com.naman.focusos.ui.screens.routines.RoutinesScreen
import com.naman.focusos.ui.screens.routines.RoutinesViewModel
import com.naman.focusos.ui.screens.settings.SettingsScreen
import com.naman.focusos.ui.screens.settings.SettingsViewModel
import com.naman.focusos.ui.screens.today.TodayScreen
import com.naman.focusos.ui.screens.today.TodayViewModel
import com.naman.focusos.ui.theme.FocusOSTheme
import com.naman.focusos.ui.theme.FocusTheme

class MainActivity : ComponentActivity() {

    private val navigateToFocusFlow = androidx.compose.runtime.mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (intent?.getBooleanExtra(FocusTimerService.EXTRA_NAVIGATE_TO_FOCUS, false) == true) {
            navigateToFocusFlow.value = true
        }

        setContent {
            val prefsManager = remember { UserPreferencesManager(applicationContext) }
            val settings by prefsManager.userSettingsFlow.collectAsState(initial = com.naman.focusos.data.preferences.UserSettings())

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
                var showSplash by remember {
                    mutableStateOf(!FocusOSApplication.hasPlayedSplash)
                }

                Crossfade(
                    targetState = showSplash,
                    animationSpec = tween(350),
                    label = "splash_transition"
                ) { isSplash ->
                    if (isSplash) {
                        SplashScreen(
                            onAnimationComplete = {
                                FocusOSApplication.hasPlayedSplash = true
                                showSplash = false
                            }
                        )
                    } else {
                        FocusOSApp(initialNavigateToFocus = navigateToFocusFlow.value)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(FocusTimerService.EXTRA_NAVIGATE_TO_FOCUS, false)) {
            navigateToFocusFlow.value = true
        }
    }
}

@Composable
fun FocusOSApp(initialNavigateToFocus: Boolean = false) {
    var currentScreen by remember {
        mutableStateOf(if (initialNavigateToFocus) FocusScreen.FOCUS else FocusScreen.TODAY)
    }

    LaunchedEffect(initialNavigateToFocus) {
        if (initialNavigateToFocus) {
            currentScreen = FocusScreen.FOCUS
        }
    }

    val todayViewModel: TodayViewModel = viewModel()
    val calendarViewModel: CalendarViewModel = viewModel()
    val focusViewModel: FocusViewModel = viewModel()
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
                    FocusScreen.FOCUS -> FocusWorkScreen(viewModel = focusViewModel)
                    FocusScreen.ROUTINES -> RoutinesScreen(viewModel = routinesViewModel)
                    FocusScreen.ANALYTICS -> AnalyticsScreen(viewModel = analyticsViewModel)
                    FocusScreen.SETTINGS -> SettingsScreen(viewModel = settingsViewModel)
                }
            }
        }
    }
}
