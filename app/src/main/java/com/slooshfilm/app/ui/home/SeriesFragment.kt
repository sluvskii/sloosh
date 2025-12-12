package com.slooshfilm.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.slooshfilm.app.R
import com.slooshfilm.app.SlooshApplication
import com.slooshfilm.app.data.LocalStorage
import com.slooshfilm.app.databinding.FragmentSeriesBinding
import com.slooshfilm.app.ui.adapters.MovieAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SeriesFragment : Fragment() {

    private var _binding: FragmentSeriesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by activityViewModels { (requireActivity().application as SlooshApplication).viewModelFactory }
    private lateinit var movieAdapter: MovieAdapter
    private lateinit var localStorage: LocalStorage

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSeriesBinding.inflate(inflater, container, false)
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
                    return if (movieAdapter.getItemViewType(position) == 1) columns else 1
                }
            }
        }

        binding.seriesRecyclerView.setHasFixedSize(true)
        binding.seriesRecyclerView.layoutManager = layoutManager
        binding.seriesRecyclerView.adapter = movieAdapter
        binding.seriesRecyclerView.itemAnimator = null

        binding.seriesRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                (parentFragment as? HomeFragment)?.handleScroll(dy, recyclerView.canScrollVertically(-1))

                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                val totalItemCount = layoutManager.itemCount
                val canLoadMore = viewModel.viewState.value.canLoadMoreSeries

                if (canLoadMore && totalItemCount > 0 && lastVisibleItemPosition >= totalItemCount - 7) {
                    viewModel.loadMore()
                }
            }
        })
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.viewState.collectLatest { state ->
                movieAdapter.submitList(state.seriesList)
                binding.seriesRecyclerView.updatePadding(top = state.topInset, bottom = state.bottomInset)
            }
        }
    }

    fun scrollToTop() {
        binding.seriesRecyclerView.smoothScrollToPosition(0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
