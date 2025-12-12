package com.slooshfilm.app.ui.watchlist

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
import android.view.animation.AnticipateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.slooshfilm.app.R
import com.slooshfilm.app.SlooshApplication
import com.slooshfilm.app.databinding.FragmentWatchlistBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

class WatchlistFragment : Fragment() {

    private var _binding: FragmentWatchlistBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WatchlistViewModel by viewModels { (requireActivity().application as SlooshApplication).viewModelFactory }

    private val _insets = MutableStateFlow(Pair(0, 0))
    val insets = _insets.asStateFlow()

    private val tabs = listOf("Все", "Фильмы", "Сериалы", "Мультики", "Аниме")
    private var isToolbarVisible = true
    private var sliderItemWidth = 0f

    private val colorEvaluator = ArgbEvaluator()
    private val primaryColor by lazy { ContextCompat.getColor(requireContext(), R.color.black) }
    private val secondaryColor by lazy { ContextCompat.getColor(requireContext(), R.color.text_secondary) }
    private val easeInterpolator = DecelerateInterpolator()
    private val overshootInterpolator = OvershootInterpolator(2.5f)
    private val anticipateInterpolator = AnticipateInterpolator(1.5f)
    private val handler = Handler(Looper.getMainLooper())
    private var longClickRunnable: Runnable? = null
    private var isDragging = false
    private var isLongPressed = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWatchlistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEdgeToEdge()
        setupViews()
        observeViewModel()
    }

    private fun setupEdgeToEdge() {
        val topPaddingDp = 80 // Height of the panel (48dp) + its margin (16dp) + extra gap (16dp)
        val topPaddingPx = (topPaddingDp * resources.displayMetrics.density).toInt()
        val bottomNavHeight = (80 * resources.displayMetrics.density).toInt()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val totalTopPadding = systemBars.top + topPaddingPx
            _insets.value = Pair(totalTopPadding, systemBars.bottom + bottomNavHeight)
            binding.toggleContainer.translationY = systemBars.top.toFloat()
            insets
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupViews() {
        val viewPagerAdapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = tabs.size
            override fun createFragment(position: Int): Fragment = WatchlistTabFragment.newInstance(tabs[position])
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

        binding.toggleContainer.clipToOutline = true

        binding.viewPager.adapter = viewPagerAdapter
        binding.viewPager.offscreenPageLimit = 1
        (binding.viewPager.getChildAt(0) as? RecyclerView)?.itemAnimator = null

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                updateToggleOnScroll(position, positionOffset)
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

        // First, add the tabs to the container
        setupTabs()

        // Then, wait for the container to be laid out to measure the first tab
        binding.tabsContainer.doOnLayout {
            val container = it as ViewGroup
            if (container.childCount > 0) {
                val firstTab = container.getChildAt(0)
                sliderItemWidth = firstTab.width.toFloat()
                binding.sliderView.layoutParams.width = sliderItemWidth.toInt()
                binding.sliderView.requestLayout()
                updateToggleOnScroll(binding.viewPager.currentItem, 0f)
            }
        }

        setupInteractiveToggle()
    }

    private fun setupTabs() {
        binding.tabsContainer.removeAllViews()
        tabs.forEachIndexed { index, title ->
            val textView = (layoutInflater.inflate(R.layout.item_tab_textview, binding.tabsContainer, false) as TextView).apply {
                text = title
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            }
            binding.tabsContainer.addView(textView)
        }
    }
    
    @SuppressLint("ClickableViewAccessibility")
    private fun setupInteractiveToggle() {
        var lastX = 0f
        longClickRunnable = Runnable { 
            isLongPressed = true
            if (binding.viewPager.isFakeDragging) {
                binding.viewPager.endFakeDrag()
            }
            // No long click action for watchlist
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
                        if (!isDragging) {
                            isDragging = true
                        }
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
                        val itemWidth = view.width / tabs.size
                        val targetPage = (event.x / itemWidth).toInt().coerceIn(0, tabs.size - 1)
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
        for (i in 0 until binding.tabsContainer.childCount) {
            val textView = binding.tabsContainer.getChildAt(i) as TextView
            val distance = abs(i - totalOffset)
            val color = colorEvaluator.evaluate(distance.coerceAtMost(1f), primaryColor, secondaryColor) as Int
            textView.setTextColor(color)
            textView.setTypeface(textView.typeface, if (distance < 0.5f) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    fun handleScroll(dy: Int) {
        val scrollThreshold = 15
        if (dy > scrollThreshold && isToolbarVisible) {
            val lp = binding.toggleContainer.layoutParams as ViewGroup.MarginLayoutParams
            val targetY = -(binding.toggleContainer.height + lp.topMargin).toFloat()
            binding.toggleContainer.animate()
                .translationY(targetY)
                .scaleX(0.8f)
                .scaleY(0.9f)
                .setInterpolator(anticipateInterpolator)
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

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.watchlistState.collectLatest { state ->
                    binding.progressBar.isVisible = state is WatchlistState.Loading
                    binding.emptyTextView.isVisible = state is WatchlistState.Empty
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
