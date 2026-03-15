package pl.kindergate.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import pl.kindergate.feature.apppicker.AppPickerScreen
import pl.kindergate.feature.dashboard.DashboardScreen
import pl.kindergate.feature.onboarding.OnboardingScreen
import pl.kindergate.feature.onboarding.OnboardingViewModel

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Dashboard : Screen("dashboard")
    data object AppPicker : Screen("app_picker")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val onboardingVm: OnboardingViewModel = hiltViewModel()
    val isOnboardingDone by onboardingVm.isOnboardingComplete.collectAsStateWithLifecycle(initialValue = false)

    val startDestination = if (isOnboardingDone) Screen.Dashboard.route else Screen.Onboarding.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onEditApps = { navController.navigate(Screen.AppPicker.route) }
            )
        }

        composable(Screen.AppPicker.route) {
            AppPickerScreen(
                onDone = { navController.popBackStack() }
            )
        }
    }
}
