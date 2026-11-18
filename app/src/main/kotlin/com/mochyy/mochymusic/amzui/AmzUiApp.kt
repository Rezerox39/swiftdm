package com.mochyy.mochymusic.amzui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.mochyy.mochymusic.amzui.data.AmzPrefs
import com.mochyy.mochymusic.amzui.data.FakeAmzDownloadsRepository
import com.mochyy.mochymusic.amzui.nav.AmzNavHost
import com.mochyy.mochymusic.amzui.theme.AmzTheme
import com.mochyy.mochymusic.utils.rememberPreference

/** Canvas UI entry point. The engine stays behind the repository seam. */
@Composable
fun AmzUiApp(
    initialLink: String? = null,
    onLinkConsumed: () -> Unit = {},
) {
    val repository = androidx.compose.runtime.remember { FakeAmzDownloadsRepository() }
    DisposableEffect(repository) {
        onDispose { repository.dispose() }
    }
    val viewModel: AmzUiViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AmzUiViewModel(repository) as T
            }
        },
    )
    val profileName by rememberPreference(AmzPrefs.ProfileName, "")
    val profilePhoto by rememberPreference(AmzPrefs.ProfilePhotoUri, "")
    val onboardingDone by rememberPreference(AmzPrefs.OnboardingDone, false)
    val navController = rememberNavController()

    AmzTheme {
        AmzNavHost(
            navController = navController,
            viewModel = viewModel,
            profileName = profileName,
            profilePhoto = profilePhoto.takeIf { it.isNotBlank() },
            onboardingDone = onboardingDone,
            initialLink = initialLink,
            onLinkConsumed = onLinkConsumed,
        )
    }
}
