package com.slooshfilm.app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.slooshfilm.app.R
import com.slooshfilm.app.SlooshApplication
import com.slooshfilm.app.data.LocalStorage
import com.slooshfilm.app.databinding.FragmentBookmarkTabBinding
import com.slooshfilm.app.ui.adapters.MovieAdapter
import com.slooshfilm.app.ui.bookmarks.BookmarksViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BookmarkTabFragment : Fragment() {

    private var _binding: FragmentBookmarkTabBinding? = null
    private val binding get() = _binding!!

    private val bookmarksViewModel: BookmarksViewModel by activityViewModels { (requireActivity().application as SlooshApplication).viewModelFactory }

    private lateinit var movieAdapter: MovieAdapter
    private lateinit var localStorage: LocalStorage
    private val bookmarkId by lazy { arguments?.getString(ARG_BOOKMARK_ID)!! }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookmarkTabBinding.inflate(inflater, container, false)
        localStorage = LocalStorage(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        val columns = localStorage.getGridColumns()
        movieAdapter = MovieAdapter(columns) { movieUrl ->
            val bundle = Bundle().apply { putString("movie_url", movieUrl) }
            findNavController().navigate(R.id.action_global_to_movie_details, bundle)
        }
        binding.filmsRecyclerView.apply {
            layoutManager = GridLayoutManager(context, columns)
            adapter = movieAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    (parentFragment as? ScrollableFragment)?.onScroll(dy)
                }
            })
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                bookmarksViewModel.viewState.collectLatest { state ->
                    val isLoading = state.loadingBookmarkId == bookmarkId
                    val films = state.filmsByBookmarkId[bookmarkId] ?: emptyList()

                    if (_binding != null) { // Проверка, что View еще существует
                        binding.progressBar.isVisible = isLoading
                        binding.filmsRecyclerView.isVisible = !isLoading && films.isNotEmpty()
                        binding.emptyBookmarkText.isVisible = !isLoading && films.isEmpty()
                        movieAdapter.submitList(films)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_BOOKMARK_ID = "bookmark_id"

        fun newInstance(bookmarkId: String): BookmarkTabFragment {
            return BookmarkTabFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_BOOKMARK_ID, bookmarkId)
                }
            }
        }
    }
}