package com.slooshfilm.app.ui.comments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.slooshfilm.app.R
import com.slooshfilm.app.SlooshApplication
import com.slooshfilm.app.databinding.FragmentCommentsBinding
import com.slooshfilm.app.ui.AppViewModelFactory
import kotlinx.coroutines.launch

class CommentsFragment : Fragment() {

    private var _binding: FragmentCommentsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CommentsViewModel by viewModels {
        val postId = arguments?.getString("post_id")
        val application = requireActivity().application as SlooshApplication
        AppViewModelFactory(application.hdRezkaRepository, postId = postId)
    }
    
    private lateinit var commentsAdapter: CommentsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInsets()
        setupViews()
        observeViewModel()
    }

    private fun setupInsets() {
        val bottomNavHeight = (80 * resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.toolbarContainer.updatePadding(top = systemBars.top)
            binding.commentsRecyclerView.updatePadding(bottom = systemBars.bottom + bottomNavHeight)
            binding.loadMoreProgressBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom + bottomNavHeight + (16 * resources.displayMetrics.density).toInt()
            }
            insets
        }
    }

    private fun setupViews() {
        commentsAdapter = CommentsAdapter(emptyList())
        val linearLayoutManager = LinearLayoutManager(requireContext())
        binding.commentsRecyclerView.apply {
            layoutManager = linearLayoutManager
            adapter = commentsAdapter
            addOnScrollListener(object: RecyclerView.OnScrollListener(){
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val lastVisibleItemPosition = linearLayoutManager.findLastVisibleItemPosition()
                    val totalItemCount = linearLayoutManager.itemCount
                    if (viewModel.state.value.canLoadMore && lastVisibleItemPosition >= totalItemCount - 5) {
                        viewModel.loadNextPage()
                    }
                }
            })
        }

        binding.backButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        
        binding.retryButton.setOnClickListener {
            viewModel.retry()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                binding.shimmerViewContainer.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                binding.commentsRecyclerView.visibility = if (state.comments.isNotEmpty()) View.VISIBLE else View.GONE
                binding.errorView.visibility = if (state.error != null && state.comments.isEmpty()) View.VISIBLE else View.GONE
                binding.loadMoreProgressBar.visibility = if (state.isLoadingNextPage) View.VISIBLE else View.GONE

                if (state.error != null) {
                    binding.errorMessage.text = state.error
                    binding.retryButton.visibility = View.VISIBLE
                } else if (!state.isLoading && state.comments.isEmpty()) {
                    binding.errorMessage.text = "Комментариев пока нет"
                    binding.retryButton.visibility = View.GONE
                } else {
                    binding.errorView.visibility = View.GONE
                }

                if (state.comments.isNotEmpty()) {
                    commentsAdapter.updateData(state.comments)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}