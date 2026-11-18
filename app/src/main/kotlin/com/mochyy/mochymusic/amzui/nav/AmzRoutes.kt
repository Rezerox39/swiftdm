package com.mochyy.mochymusic.amzui.nav

object AmzRoutes {
    const val Splash = "amz_splash"
    const val Import = "amz_import"
    const val Setup = "amz_setup"
    const val Home = "amz_home"
    const val Library = "amz_library"
    const val Browser = "amz_browser"
    const val Profile = "amz_profile"
    const val Stats = "amz_stats"
    const val Settings = "amz_settings"
    const val Details = "amz_details/{downloadId}"

    const val ArgDownloadId = "downloadId"

    fun details(id: String) = "amz_details/$id"
}
