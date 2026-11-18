package com.mochyy.mochymusic.amzui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mochyy.mochymusic.amzui.AmzUiViewModel
import com.mochyy.mochymusic.amzui.screens.BrowserScreen
import com.mochyy.mochymusic.amzui.screens.DetailsScreen
import com.mochyy.mochymusic.amzui.screens.HomeScreen
import com.mochyy.mochymusic.amzui.screens.ImportScreen
import com.mochyy.mochymusic.amzui.screens.LibraryScreen
import com.mochyy.mochymusic.amzui.screens.ProfileScreen
import com.mochyy.mochymusic.amzui.screens.SettingsScreen
import com.mochyy.mochymusic.amzui.screens.SetupScreen
import com.mochyy.mochymusic.amzui.screens.SplashScreen
import com.mochyy.mochymusic.amzui.screens.StatsScreen

@Composable
fun AmzNavHost(
    navController: NavHostController,
    viewModel: AmzUiViewModel,
    profileName: String,
    profilePhoto: String?,
    onboardingDone: Boolean,
    initialLink: String?,
    onLinkConsumed: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = AmzRoutes.Splash,
    ) {
        composable(AmzRoutes.Splash) {
            SplashScreen(
                onFinished = {
                    navController.navigate(if (onboardingDone) AmzRoutes.Home else AmzRoutes.Import) {
                        popUpTo(AmzRoutes.Splash) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(AmzRoutes.Import) {
            ImportScreen(
                onContinue = {
                    navController.navigate(AmzRoutes.Setup) {
                        popUpTo(AmzRoutes.Import) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(AmzRoutes.Setup) {
            SetupScreen(
                onFinished = {
                    navController.navigate(AmzRoutes.Home) {
                        popUpTo(AmzRoutes.Setup) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(AmzRoutes.Home) {
            HomeScreen(
                viewModel = viewModel,
                profileName = profileName,
                profilePhoto = profilePhoto,
                onAvatarClick = { navController.amzTab(AmzRoutes.Profile) },
                onOpenDetails = { navController.navigate(AmzRoutes.details(it)) },
                onOpenStats = { navController.navigate(AmzRoutes.Stats) },
                onTab = { navController.amzTab(tabRoute(it)) },
            )
        }
        composable(
            route = AmzRoutes.Details,
            arguments = listOf(
                navArgument(AmzRoutes.ArgDownloadId) { type = NavType.StringType },
            ),
        ) { entry ->
            DetailsScreen(
                viewModel = viewModel,
                id = entry.arguments?.getString(AmzRoutes.ArgDownloadId).orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }
        composable(AmzRoutes.Library) {
            LibraryScreen(
                viewModel = viewModel,
                profileName = profileName,
                profilePhoto = profilePhoto,
                onAvatarClick = { navController.amzTab(AmzRoutes.Profile) },
                onAdd = { navController.amzTab(AmzRoutes.Browser) },
                onOpenDetails = { navController.navigate(AmzRoutes.details(it)) },
                onTab = { navController.amzTab(tabRoute(it)) },
            )
        }
        composable(AmzRoutes.Browser) {
            BrowserScreen(
                viewModel = viewModel,
                profileName = profileName,
                profilePhoto = profilePhoto,
                initialLink = initialLink,
                onLinkConsumed = onLinkConsumed,
                onAvatarClick = { navController.amzTab(AmzRoutes.Profile) },
                onBack = { navController.amzTab(AmzRoutes.Home) },
                onTab = { navController.amzTab(tabRoute(it)) },
            )
        }
        composable(AmzRoutes.Profile) {
            ProfileScreen(
                viewModel = viewModel,
                profileName = profileName,
                profilePhoto = profilePhoto,
                onAvatarClick = {},
                onOpenStats = { navController.navigate(AmzRoutes.Stats) },
                onOpenSettings = { navController.navigate(AmzRoutes.Settings) },
                onImport = { navController.navigate(AmzRoutes.Import) },
                onTab = { navController.amzTab(tabRoute(it)) },
            )
        }
        composable(AmzRoutes.Stats) {
            StatsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(AmzRoutes.Settings) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}

private fun tabRoute(index: Int) = when (index) {
    0 -> AmzRoutes.Home
    1 -> AmzRoutes.Browser
    2 -> AmzRoutes.Library
    3 -> AmzRoutes.Profile
    else -> AmzRoutes.Home
}

private fun NavController.amzTab(route: String) {
    navigate(route) {
        popUpTo(AmzRoutes.Home) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
