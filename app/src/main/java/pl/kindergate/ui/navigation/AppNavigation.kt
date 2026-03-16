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
import pl.kindergate.feature.children.ChildProfileScreen
import pl.kindergate.feature.dashboard.DashboardScreen
import pl.kindergate.feature.onboarding.OnboardingScreen
import pl.kindergate.feature.onboarding.OnboardingViewModel
import pl.kindergate.feature.settings.CategoryConfigScreen

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Dashboard : Screen("dashboard")
    data object AppPicker : Screen("app_picker")
    data object ChildProfile : Screen("child_profile")
    data object CategoryConfig : Screen("category_config")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
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
                onEditApps = { navController.navigate(Screen.AppPicker.route) },
                onEditChildProfile = { navController.navigate(Screen.ChildProfile.route) },
                onConfigureCategories = { navController.navigate(Screen.CategoryConfig.route) },
            )
        }

        composable(Screen.AppPicker.route) {
            AppPickerScreen(
                onDone = { navController.popBackStack() }
            )
        }

        composable(Screen.ChildProfile.route) {
            ChildProfileScreen(
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CategoryConfig.route) {
            CategoryConfigScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
