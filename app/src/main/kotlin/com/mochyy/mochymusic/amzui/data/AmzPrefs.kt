package com.mochyy.mochymusic.amzui.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object AmzPrefs {
    val OnboardingDone = booleanPreferencesKey("amz_ui_onboarding_done")
    val ProfileName = stringPreferencesKey("amz_ui_profile_name")
    val ProfilePhotoUri = stringPreferencesKey("amz_ui_profile_photo_uri")
    val AutoCapture = booleanPreferencesKey("amz_ui_auto_capture")
    val PureBlack = booleanPreferencesKey("amz_ui_pure_black")
    val Motion = booleanPreferencesKey("amz_ui_motion")
}
