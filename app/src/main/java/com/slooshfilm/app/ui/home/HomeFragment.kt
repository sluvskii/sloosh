package com.slooshfilm.app.ui.home

import android.animation.ArgbEvaluator
import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.AnticipateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.slooshfilm.app.R
import com.slooshfilm.app.SlooshApplication
import com.slooshfilm.app.data.repository.ContentCategory
import com.slooshfilm.app.databinding.FragmentHomeBinding
import com.slooshfilm.app.ui.adapters.MovieAdapter
import eightbitlab.com.blurview.RenderScriptBlur
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by activityViewModels { (requireActivity().application as SlooshApplication).viewModelFactory }
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private lateinit var watchHistoryAdapter: MovieAdapter

    private var isToolbarVisible = true
    private var isFabVisible = false
    private var sliderItemWidth = 0f

    private val colorEvaluator = ArgbEvaluator()
    private val primaryColor by lazy { ContextCompat.getColor(requireContext(), R.color.black) }
    private val secondaryColor by lazy { ContextCompat.getColor(requireContext(), R.color.text_secondary) }

    private val rotateAnimation: Animation by lazy {
        AnimationUtils.loadAnimation(requireContext(), R.anim.spinner_rotate)
    }

    private val easeInterpolator = DecelerateInterpolator()
    private val overshootInterpolator = OvershootInterpolator(2.5f)
    private val anticipateInterpolator = AnticipateInterpolator(1.5f)
    private val handler = Handler(Looper.getMainLooper())
    private var longClickRunnable: Runnable? = null
    private var isDragging = false
    private var isLongPressed = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupEdgeToEdge(view)
        setupViews()
        observeViewModel()
    }

    private fun setupEdgeToEdge(view: View) {
        val topPaddingDp = 80 // Height of the panel (48dp) + its margin (16dp) + extra gap (16dp)
        val topPaddingPx = (topPaddingDp * resources.displayMetrics.density).toInt()
        val bottomNavHeight = (80 * resources.displayMetrics.density).toInt()
        // ProgressBar height (48dp) + extra space (32dp)
        val loadMoreIndicatorAreaHeight = (80 * resources.displayMetrics.density).toInt()

        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            val totalTopPadding = systemBars.top + topPaddingPx
            val totalBottomPadding = systemBars.bottom + bottomNavHeight + loadMoreIndicatorAreaHeight
            viewModel.setInsets(totalTopPadding, totalBottomPadding)

            binding.toggleContainer.translationY = systemBars.top.toFloat()
            insets
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupViews() {
        viewPagerAdapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = viewPagerAdapter
        binding.viewPager.offscreenPageLimit = 1
        
        watchHistoryAdapter = MovieAdapter(1) { movieUrl ->
            val bundle = bundleOf("movie_url" to movieUrl)
            findNavController().navigate(R.id.action_global_to_movie_details, bundle)
        }
        binding.watchHistoryRecyclerView.adapter = watchHistoryAdapter
        binding.watchHistoryRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.watchHistoryRecyclerView.setHasFixedSize(true)
        binding.watchHistoryRecyclerView.itemAnimator = null

        binding.scrollToTopButton.setOnClickListener {
            val currentFragment = childFragmentManager.findFragmentByTag("f" + binding.viewPager.currentItem)
            if (currentFragment is MoviesFragment) {
                currentFragment.scrollToTop()
            } else if (currentFragment is SeriesFragment) {
                currentFragment.scrollToTop()
            }
        }

        // Blur view setup
        val radius = 16f
        val decorView = requireActivity().window.decorView
        val windowBackground = decorView.background
        val scrimColor = ContextCompat.getColor(requireContext(), R.color.scrim_color)
        
        binding.topBlurView.setupWith(binding.mainBlurTarget)
            .setFrameClearDrawable(windowBackground)
            .setBlurRadius(radius)
            .setBlurAutoUpdate(true)
            .setOverlayColor(scrimColor)

        binding.scrollToTopBlur.setupWith(binding.mainBlurTarget)
            .setFrameClearDrawable(windowBackground)
            .setBlurRadius(radius)
            .setBlurAutoUpdate(true)
            .setOverlayColor(scrimColor)

        // This is the key part: clip the BlurView to the CardView's rounded corners
        binding.toggleContainer.clipToOutline = true

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                updateToggleOnScroll(position, positionOffset)
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val contentType = if (position == 0) UiContentType.FILMS else UiContentType.SERIES
                if (viewModel.viewState.value.contentType != contentType) {
                    viewModel.setContentType(contentType)
                }
            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    binding.toggleContainer.animate().rotationY(0f).setDuration(300).start()
                    updateToggleOnScroll(binding.viewPager.currentItem, 0f)
                    if(!isDragging) view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
            }
        })

        binding.filmsText.doOnLayout {
            sliderItemWidth = it.width.toFloat()
            binding.sliderView.layoutParams.width = sliderItemWidth.toInt()
            binding.sliderView.requestLayout()
            updateToggleOnScroll(binding.viewPager.currentItem, 0f)
        }

        setupInteractiveToggle()
    }
    
    @SuppressLint("ClickableViewAccessibility")
    private fun setupInteractiveToggle() {
        var lastX = 0f

        longClickRunnable = Runnable { 
            isLongPressed = true
            if (binding.viewPager.isFakeDragging) {
                binding.viewPager.endFakeDrag()
            }
            showCategorySelectionDialog() 
        }

        val touchListener = View.OnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.rawX
                    isDragging = false
                    isLongPressed = false
                    handler.postDelayed(longClickRunnable!!, 500)
                    binding.toggleContainer.animate().scaleX(0.95f).scaleY(0.95f).setDuration(150).start()
                    binding.viewPager.beginFakeDrag()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - lastX
                    if (abs(dx) > 10 && !isLongPressed) {
                        handler.removeCallbacks(longClickRunnable!!)
                        if (!isDragging) isDragging = true
                    }
                    
                    if (isDragging) {
                        binding.viewPager.fakeDragBy(-dx)
                        lastX = event.rawX
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(longClickRunnable!!)
                    binding.toggleContainer.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                    
                    if (binding.viewPager.isFakeDragging) {
                        binding.viewPager.endFakeDrag()
                    }

                    if (!isDragging && !isLongPressed) {
                        val targetPage = if(event.x < view.width/2) 0 else 1
                        binding.viewPager.setCurrentItem(targetPage, true)
                    }

                    isDragging = false
                    isLongPressed = false
                    true
                }
                else -> false
            }
        }
        binding.toggleContainer.setOnTouchListener(touchListener)
    }

    private fun showCategorySelectionDialog() {
        val categories = ContentCategory.values()
        val currentCategory = viewModel.viewState.value.category
        val choices = categories.map { it.ru }.toTypedArray()
        val currentChoiceIndex = categories.indexOf(currentCategory)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Выберите категорию")
            .setSingleChoiceItems(choices, currentChoiceIndex) { dialog, which ->
                viewModel.setCategory(categories[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun updateToggleOnScroll(position: Int, positionOffset: Float) {
        if (sliderItemWidth == 0f) return

        val rawFadeFactor = when {
            positionOffset < 0.2f -> positionOffset / 0.2f
            positionOffset > 0.8f -> (1 - positionOffset) / 0.2f
            else -> 1f
        }
        val fadeFactor = easeInterpolator.getInterpolation(rawFadeFactor)

        val baseRotation = (positionOffset - 0.5f) * -15f
        binding.toggleContainer.rotationY = baseRotation * fadeFactor

        val slider = binding.sliderView
        val params = slider.layoutParams as ViewGroup.MarginLayoutParams
        
        if (positionOffset <= 0.5f) {
            val progress = positionOffset * 2
            params.width = (sliderItemWidth * (1 + progress)).toInt()
            params.marginStart = (position * sliderItemWidth).toInt()
        } else {
            val progress = (1 - positionOffset) * 2
            params.width = (sliderItemWidth * (1 + progress)).toInt()
            params.marginStart = ((position + 1 - progress) * sliderItemWidth).toInt()
        }
        slider.layoutParams = params
        
        val totalOffset = position + positionOffset
        val filmsColor = colorEvaluator.evaluate(totalOffset, primaryColor, secondaryColor) as Int
        val seriesColor = colorEvaluator.evaluate(totalOffset, secondaryColor, primaryColor) as Int

        binding.filmsText.setTextColor(filmsColor)
        binding.seriesText.setTextColor(seriesColor)

        binding.filmsText.setTypeface(binding.filmsText.typeface, if (totalOffset < 0.5f) Typeface.BOLD else Typeface.NORMAL)
        binding.seriesText.setTypeface(binding.seriesText.typeface, if (totalOffset > 0.5f) Typeface.BOLD else Typeface.NORMAL)
    }

    fun handleScroll(dy: Int, canScrollUp: Boolean = true) {
        val scrollThreshold = 15

        if (dy < -scrollThreshold && !isFabVisible && canScrollUp) {
            showFab()
        } else if ((dy > scrollThreshold || !canScrollUp) && isFabVisible) {
            hideFab()
        }

        if (dy > scrollThreshold && isToolbarVisible) {
            val lp = binding.toggleContainer.layoutParams as ViewGroup.MarginLayoutParams
            val targetY = -(binding.toggleContainer.height + lp.topMargin).toFloat()
            binding.toggleContainer.animate()
                .translationY(targetY)
                .scaleX(0.8f)
                .scaleY(0.9f)
                .setInterpolator(anticipateInterpolator) // Changed to Anticipate
                .setDuration(300)
                .start()
            isToolbarVisible = false
        } else if (dy < -scrollThreshold && !isToolbarVisible) {
            val systemBarsTop = ViewCompat.getRootWindowInsets(binding.root)?.getInsets(WindowInsetsCompat.Type.systemBars())?.top ?: 0
            binding.toggleContainer.animate()
                .translationY(systemBarsTop.toFloat())
                .scaleX(1f)
                .scaleY(1f)
                .setInterpolator(overshootInterpolator)
                .setDuration(400)
                .start()
            isToolbarVisible = true
        }
    }

    private fun showFab() {
        isFabVisible = true
        binding.scrollToTopCard.animate().cancel()
        binding.scrollToTopCard.visibility = View.VISIBLE
        binding.scrollToTopCard.animate().translationY(0f).setInterpolator(overshootInterpolator).setDuration(400).start()
    }

    private fun hideFab() {
        isFabVisible = false
        val lp = binding.scrollToTopCard.layoutParams as ViewGroup.MarginLayoutParams
        val targetY = (binding.scrollToTopCard.height + lp.bottomMargin).toFloat()
        binding.scrollToTopCard.animate().cancel()
        binding.scrollToTopCard.animate().translationY(targetY).setInterpolator(anticipateInterpolator).setDuration(300).start()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.viewState.collectLatest { state ->
                    if (_binding == null) return@collectLatest

                    if (state.isLoading) {
                        binding.homeProgressBar.startAnimation(rotateAnimation)
                    } else {
                        binding.homeProgressBar.clearAnimation()
                    }

                    binding.homeProgressBar.isVisible = state.isLoading
                    binding.errorContainer.isVisible = state.error != null
                    binding.viewPager.isVisible = state.error == null
                    binding.loadMoreProgressBar.isVisible = state.isLoadingMore

                    if (state.contentType.ordinal != binding.viewPager.currentItem) {
                        binding.viewPager.setCurrentItem(state.contentType.ordinal, true)
                    }

                    binding.watchHistoryRecyclerView.isVisible = state.watchHistory.isNotEmpty()
                    watchHistoryAdapter.submitList(state.watchHistory)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        longClickRunnable?.let { handler.removeCallbacks(it) }
    }
}
