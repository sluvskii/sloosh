package com.slooshfilm.app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnticipateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textfield.TextInputEditText
import com.slooshfilm.app.R
import com.slooshfilm.app.SlooshApplication
import com.slooshfilm.app.data.models.Bookmark
import com.slooshfilm.app.databinding.FragmentUserProfileBinding
import com.slooshfilm.app.ui.bookmarks.BookmarksViewModel
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(), ScrollableFragment {

    private var _binding: FragmentUserProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by activityViewModels { (requireActivity().application as SlooshApplication).viewModelFactory }
    private val bookmarksViewModel: BookmarksViewModel by activityViewModels { (requireActivity().application as SlooshApplication).viewModelFactory }

    private lateinit var bookmarksAdapter: BookmarksAdapter
    private var isFabVisible = true

    private val overshootInterpolator = OvershootInterpolator(2.5f)
    private val anticipateInterpolator = AnticipateInterpolator(1.5f)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!viewModel.isLoggedIn.value) {
            findNavController().navigate(R.id.action_global_to_login)
            return
        }

        setupInsets()
        setupToolbar()
        setupViews()
        setupClickListeners()
        observeViewModel()
        bookmarksViewModel.getBookmarks()
    }

    override fun onStart() {
        super.onStart()
        // Setup BlurView here to prevent race conditions and crashes
        setupBlurView()
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(requireView()) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Set the height of the cover view
            binding.statusBarCover.updateLayoutParams {
                height = systemBars.top
            }

            // Set the top margin of the AppBarLayout
            binding.appbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = systemBars.top
            }

            insets
        }
    }

    private fun setupToolbar() {
        binding.toolbar.inflateMenu(R.menu.profile_toolbar_menu)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_settings -> {
                    findNavController().navigate(R.id.action_profileFragment_to_settingsFragment)
                    true
                }
                R.id.action_logout -> {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.logout_confirmation_title)
                        .setMessage(R.string.logout_confirmation_message)
                        .setPositiveButton(R.string.logout) { _, _ ->
                            viewModel.logout()
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupViews() {
        bookmarksAdapter = BookmarksAdapter(this)
        binding.bookmarksViewPager.adapter = bookmarksAdapter

        binding.bookmarksViewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val bookmark = bookmarksAdapter.getBookmarkAt(position)
                if (bookmark != null) {
                    bookmarksViewModel.selectBookmark(bookmark)
                }
            }
        })

        TabLayoutMediator(binding.bookmarksTabLayout, binding.bookmarksViewPager) { tab, position ->
            val bookmark = bookmarksAdapter.getBookmarkAt(position)
            if (bookmark != null) {
                tab.text = bookmark.name
            }
        }.attach()
    }

    private fun setupBlurView() {
        val radius = 20f
        val decorView = requireActivity().window.decorView
        val windowBackground = decorView.background
        val blurTarget = binding.profileBlurTarget
        val scrimColor = ContextCompat.getColor(requireContext(), R.color.scrim_color)

        binding.addBookmarkBlur.setupWith(blurTarget)
            .setFrameClearDrawable(windowBackground)
            .setBlurRadius(radius)
            .setOverlayColor(scrimColor)
    }

    private fun setupClickListeners() {
        binding.addBookmarkButton.setOnClickListener {
            showAddBookmarkDialog()
        }
    }

    private fun showAddBookmarkDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_bookmark, null)
        val bookmarkNameEditText = dialogView.findViewById<TextInputEditText>(R.id.bookmark_name_edit_text)

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton(R.string.add) { _, _ ->
                val name = bookmarkNameEditText.text.toString()
                if (name.isNotBlank()) {
                    bookmarksViewModel.createBookmarkCategory(name)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.username.collect { binding.toolbarTitle.text = it }
                }

                launch {
                    viewModel.isLoggedIn.collect { isLoggedIn ->
                        if (!isLoggedIn) {
                            findNavController().navigate(R.id.action_global_to_login)
                        }
                    }
                }

                launch {
                    bookmarksViewModel.viewState.collect { state ->
                        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                        bookmarksAdapter.setBookmarks(state.bookmarks)

                        val selectedIndex = state.bookmarks.indexOfFirst { it.id == state.selectedBookmarkId }
                        if (selectedIndex != -1 && binding.bookmarksViewPager.currentItem != selectedIndex) {
                            binding.bookmarksViewPager.currentItem = selectedIndex
                        }
                    }
                }
            }
        }
    }

    override fun onScroll(dy: Int) {
        val scrollThreshold = 15
        if (dy > scrollThreshold && isFabVisible) {
            hideFab()
        } else if (dy < -scrollThreshold && !isFabVisible) {
            showFab()
        }
    }

    private fun showFab() {
        isFabVisible = true
        binding.addBookmarkCard.animate().cancel()
        binding.addBookmarkCard.visibility = View.VISIBLE
        binding.addBookmarkCard.animate().translationY(0f).setInterpolator(overshootInterpolator).setDuration(400).start()
    }

    private fun hideFab() {
        isFabVisible = false
        val fab = binding.addBookmarkCard
        val lp = fab.layoutParams as ViewGroup.MarginLayoutParams
        val targetY = (fab.height + lp.bottomMargin).toFloat()
        fab.animate().cancel()
        fab.animate().translationY(targetY).setInterpolator(anticipateInterpolator).setDuration(300).start()
    }

    override fun onDestroyView() {
        binding.bookmarksViewPager.adapter = null
        super.onDestroyView()
        _binding = null
    }

    private class BookmarksAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        private var bookmarks: List<Bookmark> = emptyList()

        fun setBookmarks(newBookmarks: List<Bookmark>) {
            if (bookmarks != newBookmarks) {
                bookmarks = newBookmarks
                notifyDataSetChanged()
            }
        }

        fun getBookmarkAt(position: Int): Bookmark? = bookmarks.getOrNull(position)
        override fun getItemId(position: Int): Long = bookmarks[position].id.hashCode().toLong()
        override fun containsItem(itemId: Long): Boolean = bookmarks.any { it.id.hashCode().toLong() == itemId }
        override fun getItemCount(): Int = bookmarks.size
        override fun createFragment(position: Int): Fragment = BookmarkTabFragment.newInstance(bookmarks[position].id)
    }
}