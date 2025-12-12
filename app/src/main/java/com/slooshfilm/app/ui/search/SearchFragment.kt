package com.slooshfilm.app.ui.search

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.AnticipateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.EditorInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.slooshfilm.app.R
import com.slooshfilm.app.SlooshApplication
import com.slooshfilm.app.data.LocalStorage
import com.slooshfilm.app.databinding.FragmentSearchBinding
import com.slooshfilm.app.ui.adapters.MovieAdapter
import java.util.Locale

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels { (requireActivity().application as SlooshApplication).viewModelFactory }
    private lateinit var movieAdapter: MovieAdapter
    private lateinit var searchListAdapter: SearchListAdapter
    private lateinit var localStorage: LocalStorage
    private var isToolbarVisible = true

    private val rotateAnimation: Animation by lazy {
        AnimationUtils.loadAnimation(requireContext(), R.anim.spinner_rotate)
    }

    private val overshootInterpolator = OvershootInterpolator(2.5f)
    private val anticipateInterpolator = AnticipateInterpolator(1.5f)

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            startVoiceSearch()
        }
    }

    private val voiceSearchResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val spokenText: String? = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.let { it[0] }
            binding.searchEditText.setText(spokenText)
            viewModel.onSearchConfirmed(spokenText ?: "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        localStorage = LocalStorage(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        setupEdgeToEdge()
        setupSearch()
        observeViewModel()

        binding.clearHistoryButton.setOnClickListener {
            localStorage.clearSearchHistory()
            updateHistoryViews(true)
        }
    }

    private fun observeViewModel() {
        viewModel.searchState.observe(viewLifecycleOwner) { state ->
            // Hide all by default
            binding.searchProgressBar.isVisible = false
            binding.searchResultsRecyclerView.isVisible = false
            binding.emptyResultPlaceholder.root.isVisible = false
            binding.searchHistoryContainer.isVisible = false
            binding.searchPlaceholder.root.isVisible = false
            binding.emptyResultPlaceholder.errorText.isVisible = false // Hide error text by default

            when (state) {
                is SearchState.Loading -> {
                    binding.searchProgressBar.isVisible = true
                    binding.searchProgressBar.startAnimation(rotateAnimation)
                }
                is SearchState.Content -> {
                    binding.searchResultsRecyclerView.isVisible = true
                    movieAdapter.submitList(state.results)
                    if (state.results.isNotEmpty()) {
                        localStorage.saveSearchQuery(binding.searchEditText.text.toString())
                    }
                }
                is SearchState.Idle -> {
                    updateHistoryViews()
                }
                is SearchState.Suggestions -> {
                    binding.searchHistoryContainer.isVisible = true
                    binding.historyHeader.isVisible = false
                    searchListAdapter.submitList(state.suggestions.map { SearchListItem(it, SearchListItemType.SUGGESTION) })
                }
                is SearchState.EmptyResult -> {
                    binding.emptyResultPlaceholder.root.isVisible = true
                }
                is SearchState.Error -> {
                    binding.emptyResultPlaceholder.root.isVisible = true
                    binding.emptyResultPlaceholder.emptyResultText.text = "Произошла ошибка"
                    binding.emptyResultPlaceholder.errorText.isVisible = true
                    binding.emptyResultPlaceholder.errorText.text = state.message
                }
            }
        }
    }

    private fun updateHistoryViews(forceShowPlaceholder: Boolean = false) {
        val history = localStorage.getSearchHistory()
        val hasHistory = history.isNotEmpty() && !forceShowPlaceholder
        
        binding.searchHistoryContainer.isVisible = hasHistory
        binding.searchPlaceholder.root.isVisible = !hasHistory
        binding.historyHeader.isVisible = hasHistory
        
        if(hasHistory) {
            searchListAdapter.submitList(history.map { SearchListItem(it, SearchListItemType.HISTORY) })
        } else {
            searchListAdapter.submitList(emptyList())
        }
    }

    private fun setupEdgeToEdge() {
        val topPaddingDp = 80 // Height of the panel (48dp) + its margin (16dp) + extra gap (16dp)
        val topPaddingPx = (topPaddingDp * resources.displayMetrics.density).toInt()
        val bottomNavHeight = (80 * resources.displayMetrics.density).toInt()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            binding.searchCardContainer.translationY = systemBars.top.toFloat()
            
            val totalTopPadding = systemBars.top + topPaddingPx
            val totalBottomPadding = systemBars.bottom + bottomNavHeight

            binding.searchResultsRecyclerView.updatePadding(top = totalTopPadding, bottom = totalBottomPadding)
            binding.searchHistoryContainer.updatePadding(top = totalTopPadding, bottom = totalBottomPadding)

            insets
        }
    }

    private fun setupViews() {
        val columns = localStorage.getGridColumns()
        movieAdapter = MovieAdapter(columns) { movieUrl ->
            val bundle = bundleOf("movie_url" to movieUrl)
            findNavController().navigate(R.id.action_global_to_movie_details, bundle)
        }
        
        // Blur view setup
        val radius = 16f
        val decorView = requireActivity().window.decorView
        val windowBackground = decorView.background
        binding.topBlurView.setupWith(binding.mainBlurTarget)
            .setFrameClearDrawable(windowBackground)
            .setBlurRadius(radius)
            .setBlurAutoUpdate(true)
            .setOverlayColor(ContextCompat.getColor(requireContext(), R.color.scrim_color))

        val layoutManager = GridLayoutManager(context, columns).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    if (movieAdapter.itemCount > position) {
                        return if (movieAdapter.getItemViewType(position) == 1) columns else 1
                    }
                    return 1
                }
            }
        }

        binding.searchResultsRecyclerView.apply {
            this.layoutManager = layoutManager
            adapter = movieAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    handleScroll(dy)
                }
            })
        }

        searchListAdapter = SearchListAdapter(
            onQueryClick = { query ->
                binding.searchEditText.setText(query)
                binding.searchEditText.setSelection(query.length)
                viewModel.onSearchConfirmed(query)
            },
            onDeleteClick = { query ->
                localStorage.removeSearchQuery(query)
                updateHistoryViews()
            }
        )
        binding.searchHistoryRecyclerView.apply {
            this.layoutManager = LinearLayoutManager(context)
            adapter = searchListAdapter
        }
    }

    private fun handleScroll(dy: Int) {
        val scrollThreshold = 15
        if (dy > scrollThreshold && isToolbarVisible) {
            val lp = binding.searchCardContainer.layoutParams as ViewGroup.MarginLayoutParams
            val targetY = -(binding.searchCardContainer.height + lp.topMargin).toFloat()
            binding.searchCardContainer.animate()
                .translationY(targetY)
                .scaleX(0.8f)
                .scaleY(0.9f)
                .setInterpolator(anticipateInterpolator)
                .setDuration(300)
                .start()
            isToolbarVisible = false
        } else if (dy < -scrollThreshold && !isToolbarVisible) {
            val systemBarsTop = ViewCompat.getRootWindowInsets(binding.root)?.getInsets(WindowInsetsCompat.Type.systemBars())?.top ?: 0
            binding.searchCardContainer.animate()
                .translationY(systemBarsTop.toFloat())
                .scaleX(1f)
                .scaleY(1f)
                .setInterpolator(overshootInterpolator)
                .setDuration(400)
                .start()
            isToolbarVisible = true
        }
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.clearButton.isVisible = !s.isNullOrEmpty()
                viewModel.onSearchQueryChanged(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        
        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.onSearchConfirmed(binding.searchEditText.text.toString())
                true
            } else {
                false
            }
        }

        binding.clearButton.setOnClickListener {
            binding.searchEditText.text.clear()
        }

        binding.voiceSearchButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startVoiceSearch()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startVoiceSearch() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Говорите")
        }
        voiceSearchResultLauncher.launch(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}