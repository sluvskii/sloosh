package com.slooshfilm.app.ui.watchlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.slooshfilm.app.R
import com.slooshfilm.app.SlooshApplication
import com.slooshfilm.app.data.LocalStorage
import com.slooshfilm.app.databinding.FragmentWatchlistTabBinding
import com.slooshfilm.app.ui.adapters.MovieAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WatchlistTabFragment : Fragment() {

    private var _binding: FragmentWatchlistTabBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WatchlistViewModel by viewModels({
        requireParentFragment() // Share ViewModel with parent WatchlistFragment
    }) { (requireActivity().application as SlooshApplication).viewModelFactory }

    private lateinit var movieAdapter: MovieAdapter
    private lateinit var localStorage: LocalStorage
    private var filter: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            filter = it.getString(ARG_FILTER)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWatchlistTabBinding.inflate(inflater, container, false)
        localStorage = LocalStorage(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        view.post {
            setupRecyclerView()
            observeViewModel()
        }
    }

    private fun setupRecyclerView() {
        val columns = localStorage.getGridColumns()
        movieAdapter = MovieAdapter(columns) { movieUrl ->
            val bundle = bundleOf("movie_url" to movieUrl)
            findNavController().navigate(R.id.action_global_to_movie_details, bundle)
        }

        val layoutManager = GridLayoutManager(context, columns).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    if (position < movieAdapter.currentList.size) {
                        return if (movieAdapter.getItemViewType(position) == 1) columns else 1
                    }
                    return 1
                }
            }
        }

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = movieAdapter
        binding.recyclerView.itemAnimator = null

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                (parentFragment as? WatchlistFragment)?.handleScroll(dy)

                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                val totalItemCount = layoutManager.itemCount
                val canLoadMore = (viewModel.watchlistState.value as? WatchlistState.Content)?.let {
                    it.page < it.totalPages
                } ?: false

                if (canLoadMore && totalItemCount > 0 && lastVisibleItemPosition >= totalItemCount - 5) {
                    viewModel.loadNextPage()
                }
            }
        })
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.watchlistState.collectLatest { state ->
                if (state is WatchlistState.Content) {
                    val filteredList = when (filter) {
                        "Фильмы" -> state.movies.filter { it.type == "film" }
                        "Сериалы" -> state.movies.filter { it.type == "series" }
                        "Мультики" -> state.movies.filter { it.type == "cartoon" || it.type == "cartoon-series" }
                        "Аниме" -> state.movies.filter { it.type == "anime" || it.type == "anime-series" }
                        else -> state.movies
                    }
                    movieAdapter.submitList(filteredList)
                }
            }
        }

        // Get insets from parent and apply padding
        (parentFragment as? WatchlistFragment)?.let {
            viewLifecycleOwner.lifecycleScope.launch {
                it.insets.collectLatest { insets ->
                    binding.recyclerView.updatePadding(top = insets.first, bottom = insets.second)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_FILTER = "filter"

        fun newInstance(filter: String): WatchlistTabFragment {
            val fragment = WatchlistTabFragment()
            val args = Bundle()
            args.putString(ARG_FILTER, filter)
            fragment.arguments = args
            return fragment
        }
    }
}