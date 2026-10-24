package com.slooshfilm.app.ui.navigation

import androidx.annotation.DrawableRes
import com.slooshfilm.app.R

sealed class Screen(val route: String, val title: String, @DrawableRes val iconRes: Int? = null) {
    object Home : Screen("home", "Главная", R.drawable.ic_sf_house_fill)
    object Search : Screen("search", "Поиск", R.drawable.ic_sf_magnifyingglass)
    object Messenger : Screen("messenger", "Чаты", R.drawable.ic_sf_bubble_fill)
    object ContinueWatching : Screen("continue", "Продолжить", R.drawable.ic_sf_clock_arrow_circlepath)
    object Profile : Screen("profile", "Профиль", R.drawable.ic_sf_person_fill)

    object Details : Screen("details/{id}/{type}", "Подробнее") {
        fun createRoute(id: String, type: String = "movie") = "details/$id/$type"
    }

    object Catalog : Screen("catalog/{kind}/{id}/{mediaType}?name={name}", "Каталог") {
        fun createGenreRoute(genre: String, mediaType: String?): String =
            "catalog/genre/${android.net.Uri.encode(genre)}/${mediaType?.takeIf { it.isNotBlank() } ?: "all"}?name=${android.net.Uri.encode(genre)}"

        fun createStudioRoute(studioId: String, studioName: String): String =
            "catalog/studio/${android.net.Uri.encode(studioId)}/all?name=${android.net.Uri.encode(studioName)}"
    }

    object Player : Screen("player/{id}/{type}?season={season}&episode={episode}&voice={voice}&tid={tid}&iframe={iframe}", "Плеер") {
        fun createRoute(
            id: String,
            type: String = "movie",
            season: Int? = null,
            episode: Int? = null,
            voice: String? = null,
            translationId: String? = null,
            iframeUrl: String? = null
        ): String {
            val base = "player/$id/$type"
            val queryParams = mutableListOf<String>()
            if (season != null) queryParams.add("season=$season")
            if (episode != null) queryParams.add("episode=$episode")
            if (!voice.isNullOrBlank()) queryParams.add("voice=${java.net.URLEncoder.encode(voice, "UTF-8")}")
            if (!translationId.isNullOrBlank()) queryParams.add("tid=${java.net.URLEncoder.encode(translationId, "UTF-8")}")
            if (!iframeUrl.isNullOrBlank()) queryParams.add("iframe=${java.net.URLEncoder.encode(iframeUrl, "UTF-8")}")
            return if (queryParams.isNotEmpty()) "$base?${queryParams.joinToString("&")}" else base
        }
    }

    object Settings : Screen("settings", "Настройки")
    object SettingsAppearance : Screen("settings/appearance", "Внешний вид")
    object SettingsGeneral : Screen("settings/general", "Общие")
    object SettingsPlayer : Screen("settings/player", "Плеер")
    object SettingsAbout : Screen("settings/about", "О приложении")

    companion object {
        val bottomBarTabs: List<Screen> by lazy { listOf(Home, Messenger, ContinueWatching, Profile) }
        val mainScreens: List<Screen> by lazy { listOf(Home, Messenger, ContinueWatching, Profile, Search) }
    }
}
