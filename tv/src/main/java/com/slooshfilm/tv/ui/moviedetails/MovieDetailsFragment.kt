package com.slooshfilm.tv.ui.moviedetails

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.DetailsOverviewRow
import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.lifecycle.ViewModelProvider
import com.slooshfilm.app.SlooshApplication
import com.slooshfilm.app.data.model.MovieDetails
import com.slooshfilm.app.ui.moviedetails.MovieDetailsViewModel
import com.slooshfilm.tv.ui.presenters.DetailsDescriptionPresenter

class MovieDetailsFragment : DetailsSupportFragment() {

    private val viewModel: MovieDetailsViewModel by viewModels { (requireActivity().application as SlooshApplication).viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val movieUrl = arguments?.getString("movie_url") ?: return

        viewModel.loadMovieDetails(movieUrl)
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.viewState.observe(viewLifecycleOwner) { state ->
            if (state.isLoading) {
                // Show loading spinner
            } else if (state.error != null) {
                // Show error
            } else if (state.movieDetails != null) {
                val details = state.movieDetails
                val detailsOverviewRow = DetailsOverviewRow(details)

                // TODO: Set up the rest of the details overview row

                val presenter = FullWidthDetailsOverviewRowPresenter(DetailsDescriptionPresenter())

                val rowsAdapter = ArrayObjectAdapter(presenter)
                rowsAdapter.add(detailsOverviewRow)

                // Add other rows like recommendations, etc.

                adapter = rowsAdapter
            }
        }
    }
}