package com.slooshfilm.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.slooshfilm.app.data.repository.HdRezkaRepository
import com.slooshfilm.app.data.repository.WatchHistoryRepository
import com.slooshfilm.app.ui.actordetails.ActorDetailsViewModel
import com.slooshfilm.app.ui.bookmarks.BookmarksViewModel
import com.slooshfilm.app.ui.comments.CommentsViewModel
import com.slooshfilm.app.ui.home.HomeViewModel
import com.slooshfilm.app.ui.moviedetails.MovieDetailsViewModel
import com.slooshfilm.app.ui.player.PlayerViewModel
import com.slooshfilm.app.ui.profile.ProfileViewModel
import com.slooshfilm.app.ui.search.SearchViewModel
import com.slooshfilm.app.ui.watchlist.WatchlistViewModel

class AppViewModelFactory(
    private val hdRezkaRepository: HdRezkaRepository? = null,
    private val watchHistoryRepository: WatchHistoryRepository? = null,
    private val actorLink: String? = null,
    private val postId: String? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(hdRezkaRepository!!, watchHistoryRepository!!) as T
            modelClass.isAssignableFrom(SearchViewModel::class.java) -> SearchViewModel(hdRezkaRepository!!) as T
            modelClass.isAssignableFrom(MovieDetailsViewModel::class.java) -> MovieDetailsViewModel(hdRezkaRepository!!) as T
            modelClass.isAssignableFrom(WatchlistViewModel::class.java) -> WatchlistViewModel(hdRezkaRepository!!) as T
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> ProfileViewModel(hdRezkaRepository!!) as T
            modelClass.isAssignableFrom(BookmarksViewModel::class.java) -> BookmarksViewModel(hdRezkaRepository!!) as T
            modelClass.isAssignableFrom(PlayerViewModel::class.java) -> PlayerViewModel(hdRezkaRepository!!) as T
            modelClass.isAssignableFrom(ActorDetailsViewModel::class.java) -> {
                if (actorLink != null) {
                    ActorDetailsViewModel(hdRezkaRepository!!, actorLink) as T
                } else {
                    throw IllegalArgumentException("ActorLink required for ActorDetailsViewModel")
                }
            }
            modelClass.isAssignableFrom(CommentsViewModel::class.java) -> {
                if (postId != null) {
                    CommentsViewModel(hdRezkaRepository!!, postId) as T
                } else {
                    throw IllegalArgumentException("PostId required for CommentsViewModel")
                }
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
