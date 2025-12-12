package com.slooshfilm.tv.ui.search

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.ObjectAdapter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.slooshfilm.app.SlooshApplication
import com.slooshfilm.app.data.model.Movie
import com.slooshfilm.app.ui.search.SearchState
import com.slooshfilm.app.ui.search.SearchViewModel
import com.slooshfilm.tv.R
import com.slooshfilm.tv.ui.presenters.CardPresenter

class SearchFragment : SearchSupportFragment(), SearchSupportFragment.SearchResultProvider {

    private val viewModel: SearchViewModel by viewModels { (requireActivity().application as SlooshApplication).viewModelFactory }
    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
    private val cardPresenter = CardPresenter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSearchResultProvider(this)
        setOnItemViewClickedListener(ItemViewClickedListener())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.searchState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SearchState.Content -> {
                    displayResults(state.results)
                }
                else -> {
                    // Handle other states like loading, error, etc.
                    rowsAdapter.clear()
                }
            }
        }
    }

    private fun displayResults(results: List<Movie>) {
        rowsAdapter.clear()
        val header = HeaderItem("Search Results")
        val listRowAdapter = ArrayObjectAdapter(cardPresenter)
        listRowAdapter.addAll(0, results)
        rowsAdapter.add(ListRow(header, listRowAdapter))
    }

    override fun onQueryTextChange(newQuery: String?): Boolean {
        viewModel.onSearchQueryChanged(newQuery.orEmpty())
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        viewModel.onSearchConfirmed(query.orEmpty())
        return true
    }

    override fun getResultsAdapter(): ObjectAdapter {
        return rowsAdapter
    }

    private inner class ItemViewClickedListener : OnItemViewClickedListener {
        override fun onItemClicked(
            itemViewHolder: Presenter.ViewHolder?,
            item: Any?,
            rowViewHolder: RowPresenter.ViewHolder?,
            row: Row?
        ) {
            if (item is Movie) {
                val bundle = bundleOf("movie_url" to item.url)
                findNavController().navigate(R.id.action_search_to_movie_details, bundle)
            }
        }
    }
}