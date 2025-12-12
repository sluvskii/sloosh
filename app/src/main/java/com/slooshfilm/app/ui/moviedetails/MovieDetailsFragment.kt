package com.slooshfilm.app.ui.moviedetails

import android.content.Intent
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.widget.NestedScrollView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.slooshfilm.app.ui.adapters.SeasonAdapter
import com.slooshfilm.app.ui.adapters.SeasonWithEpisodes
import com.slooshfilm.app.data.model.Episode
import com.slooshfilm.app.R
import com.slooshfilm.app.SlooshApplication
import com.slooshfilm.app.data.model.MovieDetails
import com.slooshfilm.app.data.model.MoviePart
import com.slooshfilm.app.databinding.FragmentMovieDetailsBinding
import com.slooshfilm.app.ui.moviedetails.dialog.VideoSelectionDialogFragment
import com.slooshfilm.app.ui.player.PlayerActivity
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.launch
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.AdapterView
import android.widget.Toast

class MovieDetailsFragment : Fragment() {

    private var _binding: FragmentMovieDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MovieDetailsViewModel by activityViewModels { (requireActivity().application as SlooshApplication).viewModelFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMovieDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.contentView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val bottomNavHeight = (80 * resources.displayMetrics.density).toInt()
            v.updatePadding(bottom = systemBars.bottom + bottomNavHeight)
            insets
        }

        val movieUrl = arguments?.getString("movie_url")
        if (movieUrl == null) {
            parentFragmentManager.popBackStack()
            return
        }

        val radius = 20f
        val decorView = requireActivity().window.decorView
        val windowBackground = decorView.background
        val blurTarget = binding.blurTarget
        val scrimColor = ContextCompat.getColor(requireContext(), R.color.scrim_color)

        binding.backButtonBlur.setupWith(blurTarget)
            .setFrameClearDrawable(windowBackground)
            .setBlurRadius(radius)
            .setOverlayColor(scrimColor)

        binding.shareButtonBlur.setupWith(blurTarget)
            .setFrameClearDrawable(windowBackground)
            .setBlurRadius(radius)
            .setOverlayColor(scrimColor)

        binding.bookmarkButtonBlur.setupWith(blurTarget)
            .setFrameClearDrawable(windowBackground)
            .setBlurRadius(radius)
            .setOverlayColor(scrimColor)

        viewModel.loadMovieDetails(movieUrl)
        observeViewModel(movieUrl)
        setupScrollListener()
        binding.backButton.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.bookmarkButton.setOnClickListener { handleBookmarkClick() }
        binding.retryButton.setOnClickListener {
            binding.errorView.isVisible = false
            viewModel.loadMovieDetails(movieUrl)
        }
    }

    private fun setupScrollListener() {
        binding.contentView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            if (_binding == null) return@OnScrollChangeListener

            val movieTitleView = binding.movieTitle
            val toolbarTitleView = binding.toolbarMovieTitle

            val movieTitleY = movieTitleView.y
            // Start animation when the main title is about to scroll off screen
            val animationStartScroll = movieTitleY - (movieTitleView.height * 1.5f)

            if (scrollY < animationStartScroll) {
                movieTitleView.alpha = 1f
                toolbarTitleView.alpha = 0f
                return@OnScrollChangeListener
            }

            // Animate over a distance of 1.5x the title's height
            val animationDistance = movieTitleView.height * 1.5f
            val progress = ((scrollY - animationStartScroll) / animationDistance).coerceIn(0f, 1f)

            // Fade out the main title
            movieTitleView.alpha = 1 - progress

            // Fade in and slide down the toolbar title
            toolbarTitleView.alpha = progress
        })
    }

    private fun handleBookmarkClick() {
        val isBookmarked = viewModel.viewState.value?.isBookmarked ?: false
        if (isBookmarked) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Удалить из закладок?")
                .setPositiveButton("Удалить") { _, _ -> viewModel.toggleBookmark() }
                .setNegativeButton("Отмена", null)
                .show()
        } else {
            lifecycleScope.launch {
                val repository = (requireActivity().application as SlooshApplication).hdRezkaRepository
                val bookmarksPage = repository.getBookmarksPage()
                val categories = bookmarksPage.categories

                val categoryNames = categories.map { it.name }.toMutableList()
                categoryNames.add("Создать новую")

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Добавить в закладку")
                    .setItems(categoryNames.toTypedArray()) { _, which ->
                        if (which < categories.size) {
                            viewModel.toggleBookmark(categories[which].id)
                        } else {
                            showCreateBookmarkDialog()
                        }
                    }
                    .show()
            }
        }
    }

    private fun showCreateBookmarkDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_bookmark, null)
        val bookmarkNameEditText = dialogView.findViewById<TextInputEditText>(R.id.bookmark_name_edit_text)

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton(R.string.add) { _, _ ->
                val name = bookmarkNameEditText.text.toString()
                if (name.isNotBlank()) {
                    lifecycleScope.launch {
                        val repository = (requireActivity().application as SlooshApplication).hdRezkaRepository
                        val success = repository.createBookmarkCategory(name)
                        if (success) {
                            handleBookmarkClick()
                        }
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun observeViewModel(movieUrl: String) {
        viewModel.viewState.observe(viewLifecycleOwner) { state ->
            binding.shimmerViewContainer.isVisible = state.isLoading
            binding.contentView.isVisible = !state.isLoading && state.error == null
            binding.errorView.isVisible = state.error != null

            state.movieDetails?.let { displayMovieDetails(it, movieUrl) }
            state.error?.let {
                binding.errorMessage.text = it
            }
            updateBookmarkIcon(state.isBookmarked)
        }
    }

    private fun updateBookmarkIcon(isBookmarked: Boolean) {
        if (!isAdded || _binding == null) return
        val icon = if (isBookmarked) R.drawable.ic_bookmark else R.drawable.bookmark
        binding.bookmarkButton.setImageDrawable(ContextCompat.getDrawable(requireContext(), icon))
    }

    private fun displayMovieDetails(movie: MovieDetails, movieUrl: String) {
        Glide.with(this)
            .load(movie.posterUrl)
            .thumbnail(0.25f)
            .dontAnimate()
            .placeholder(R.drawable.placeholder_movie)
            .into(binding.moviePoster)

        Glide.with(this)
            .load(movie.posterUrl)
            .apply(RequestOptions.bitmapTransform(MultiTransformation(CenterCrop(), BlurTransformation(25, 3))))
            .thumbnail(0.25f)
            .dontAnimate()
            .placeholder(R.drawable.placeholder_movie)
            .into(binding.backgroundPoster)

        binding.movieTitle.text = movie.title
        binding.toolbarMovieTitle.text = movie.title
        setupDescription(movie.description)

        val kpRatingTextView = binding.ratingKinopoisk
        val kinopoiskRatingText = movie.ratingKinopoisk ?: "-"
        kpRatingTextView.text = kinopoiskRatingText
        val paint = kpRatingTextView.paint
        val width = paint.measureText(kinopoiskRatingText)
        val textShader: Shader = LinearGradient(
            0f, 0f, width, kpRatingTextView.textSize,
            intArrayOf(
                ContextCompat.getColor(requireContext(), R.color.kinopoisk_orange),
                ContextCompat.getColor(requireContext(), R.color.kinopoisk_yellow)
            ),
            null, Shader.TileMode.CLAMP
        )
        kpRatingTextView.paint.shader = textShader

        val imdbRatingTextView = binding.ratingImdb
        imdbRatingTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.imdb_yellow))
        imdbRatingTextView.text = movie.ratingImdb ?: "-"

        binding.movieDuration.text = movie.duration
        if (movie.directors.isEmpty() && !movie.director.isNullOrEmpty()) {
            binding.movieDirector.text = movie.director
        }
        binding.releaseDate.text = movie.releaseDate
        binding.movieCountry.text = movie.country
        binding.movieAgeLimit.text = movie.ageLimit

        binding.watchButton.setOnClickListener {
            VideoSelectionDialogFragment.newInstance()
                .show(childFragmentManager, VideoSelectionDialogFragment.TAG)
        }

        setupTrailer(movie.trailerUrl)
        setupActors(movie.actorsWithPhotos)
        setupDirectors(movie.directors)
        setupComments(movie.postId, arguments?.getString("movie_url"))
        setupFranchise(movie.franchise, movie, movieUrl)
        setupEpisodesSchedule(movie)

        // Setup translator spinner when multiple translators present
        val translators = movie.translators
        val spinner = binding.root.findViewById<Spinner>(R.id.episodes_translator_spinner)
        if (translators.size <= 1) {
            spinner.visibility = View.GONE
            return
        }

        // Build lists for display and id mapping
        val translatorList = translators.values.toList()
        val names = translatorList.map { it.name }

        spinner.visibility = View.VISIBLE
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
        spinner.adapter = adapter

        // Pre-select the translator that originally supplied seasons (if any)
        val selectedId = movie.selectedTranslatorId
        val initialIndex = translatorList.indexOfFirst { it.id == selectedId }.takeIf { it >= 0 } ?: 0
        spinner.setSelection(initialIndex)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val translator = translatorList[position]
                val tid = translator.id
                val postId = movie.postId
                if (tid == null || postId == null) return

                // If already loaded seasons correspond to this translator, do nothing
                if (movie.selectedTranslatorId == tid && movie.seasons.isNotEmpty()) return

                lifecycleScope.launch {
                    try {
                        val seasons = viewModel.getEpisodes(postId, tid, movie.pageUrl)
                        val updatedMovie = movie.copy(seasons = seasons, selectedTranslatorId = tid)
                        setupEpisodesSchedule(updatedMovie)
                    } catch (e: Exception) {
                        if (!isAdded) return@launch
                        Toast.makeText(requireContext(), "Не удалось загрузить сезоны для выбранного перевода", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupDescription(description: String) {
        var isDescriptionExpanded = false
        binding.movieDescription.text = description
        binding.movieDescription.post {
            if (!isAdded) return@post
            val lineCount = binding.movieDescription.lineCount
            val hasEllipsis = binding.movieDescription.layout?.getEllipsisCount(lineCount - 1) ?: 0 > 0
            if (lineCount > 5 || hasEllipsis || description.length > 300) {
                binding.expandDescriptionButton.visibility = View.VISIBLE
                binding.expandDescriptionButton.text = "Развернуть"
                binding.movieDescription.maxLines = 5
                binding.expandDescriptionButton.setOnClickListener { 
                    isDescriptionExpanded = !isDescriptionExpanded
                    if (isDescriptionExpanded) {
                        binding.movieDescription.maxLines = Int.MAX_VALUE
                        binding.expandDescriptionButton.text = "Свернуть"
                    } else {
                        binding.movieDescription.maxLines = 5
                        binding.expandDescriptionButton.text = "Развернуть"
                    }
                }
            } else {
                binding.expandDescriptionButton.visibility = View.GONE
            }
        }
    }

    private fun setupFranchise(franchise: List<com.slooshfilm.app.data.model.MoviePart>, movie: MovieDetails, movieUrl: String) {
        val movieTitle = movie.title
        if (franchise.isEmpty()) {
            binding.franchiseLabel.visibility = View.GONE
            binding.franchiseRecyclerView.visibility = View.GONE
            return
        }

        binding.franchiseLabel.visibility = View.VISIBLE
        binding.franchiseRecyclerView.visibility = View.VISIBLE

        val franchiseName = extractFranchiseName(movieTitle, franchise)
        binding.franchiseLabel.text = "Все части \"$franchiseName\""

        // Add the current movie to the list if it's not already there
        val updatedFranchise = franchise.toMutableList()
        if (franchise.none { it.name == movieTitle }) {
            val year = movie.releaseDate?.takeIf { it.isNotBlank() }?.let { "\\d{4}".toRegex().find(it)?.value }?.let { "$it год" }
            updatedFranchise.add(MoviePart(name = movieTitle, link = movieUrl, year = year, rating = movie.ratingImdb))
        }
        updatedFranchise.sortBy { it.year?.filter { it.isDigit() } }

        val adapter = com.slooshfilm.app.ui.adapters.FranchiseAdapter(updatedFranchise, movieTitle) { link ->
            val bundle = bundleOf("movie_url" to link)
            findNavController().navigate(R.id.action_global_to_movie_details, bundle)
        }

        binding.franchiseRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.franchiseRecyclerView.adapter = adapter
    }

    private fun extractFranchiseName(movieTitle: String, franchise: List<com.slooshfilm.app.data.model.MoviePart>): String {
        val commonParts = mutableSetOf<String>()
        franchise.forEach { part ->
            val words = part.name.split(":", ",", "-").map { it.trim() }
            if (words.isNotEmpty()) {
                commonParts.add(words[0])
            }
        }
        return if (commonParts.size == 1) commonParts.first() else movieTitle.split(":", ",", "-").firstOrNull()?.trim() ?: movieTitle
    }

    private fun setupEpisodesSchedule(movie: MovieDetails) {
        val seasons = movie.seasons

        if (seasons.isEmpty()) {
            binding.episodesScheduleLabel.visibility = View.GONE
            binding.episodesRecyclerView.visibility = View.GONE
            binding.openFullScheduleButton.visibility = View.GONE
            return
        }

        binding.episodesScheduleLabel.visibility = View.VISIBLE
        binding.episodesRecyclerView.visibility = View.VISIBLE
        binding.openFullScheduleButton.visibility = View.VISIBLE

        // Преобразуем Map<Int, List<Episode>> в List<SeasonWithEpisodes>
        val seasonsWithEpisodes = seasons.map { (seasonNum, episodes) ->
            SeasonWithEpisodes(seasonNum, episodes, isExpanded = false)
        }.sortedBy { it.seasonNumber }

        // Выберем переводчика, который был использован для получения сезонов (если указан)
        val translatorUsed = movie.selectedTranslatorId?.let { id -> movie.translators[id] } ?: movie.translators.values.firstOrNull()

        // Создаем адаптер с callback для клика по эпизоду
        val seasonAdapter = SeasonAdapter(seasonsWithEpisodes, null) { episode ->
            // При клике по эпизоду открываем плеер
            // Выбираем переводчика: сначала тот, что заполнил сезоны (если есть), иначе первый доступный
            val translator = translatorUsed ?: movie.translators.values.firstOrNull()
            if (translator != null) {
                val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                    putExtra("url", movie.pageUrl)
                    putExtra("title", movie.title)
                    putExtra("season", episode.season)
                    putExtra("episode", episode.number)
                    putExtra("translator", translator.name)
                }
                startActivity(intent)
            }
        }

        binding.episodesRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.episodesRecyclerView.adapter = seasonAdapter
        binding.episodesRecyclerView.setHasFixedSize(false) // Динамическая высота при разворотах
        binding.episodesRecyclerView.itemAnimator = null

        // Кнопка открытия полного расписания
        binding.openFullScheduleButton.setOnClickListener {
            // Открываем отдельный экран со всем расписанием (если нужно)
            android.util.Log.d("MovieDetailsFragment", "Open full schedule - TODO")
        }
    }

    private fun setupTrailer(trailerUrl: String?) {
        if (trailerUrl.isNullOrEmpty()) {
            binding.trailerLabel.visibility = View.GONE
            binding.trailerContainer.visibility = View.GONE
            return
        }

        val videoId = extractYouTubeVideoId(trailerUrl)
        if (videoId == null) {
            binding.trailerLabel.visibility = View.GONE
            binding.trailerContainer.visibility = View.GONE
            return
        }

        binding.trailerLabel.visibility = View.VISIBLE
        binding.trailerContainer.visibility = View.VISIBLE

        // Load YouTube thumbnail with beautiful gradient placeholder
        // Try multiple thumbnail URLs with fallbacks for regions where YouTube is blocked
        val thumbnailUrls = listOf(
            "https://img.youtube.com/vi/$videoId/sddefault.jpg",
            "https://img.youtube.com/vi/$videoId/maxresdefault.jpg",
            "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
            "https://img.youtube.com/vi/$videoId/default.jpg"
        )
        
        Glide.with(this)
            .load(thumbnailUrls[0])
            .thumbnail(
                Glide.with(this).load(thumbnailUrls[1]),
                Glide.with(this).load(thumbnailUrls[2]),
                Glide.with(this).load(thumbnailUrls[3])
            )
            .centerCrop()
            .placeholder(R.drawable.trailer_thumbnail_placeholder)
            .error(R.drawable.trailer_thumbnail_placeholder)
            .into(binding.trailerThumbnail)

        val clickListener = View.OnClickListener { openYouTubeVideo(trailerUrl, videoId) }
        binding.trailerContainer.setOnClickListener(clickListener)
        binding.trailerPlayButton.setOnClickListener(clickListener)
    }

    private fun extractYouTubeVideoId(url: String): String? {
        val patterns = listOf(
            "youtube.com/watch\\?v=([^&]+)",
            "youtu.be/([^?]+)",
            "youtube.com/embed/([^?]+)"
        )
        for (pattern in patterns) {
            val regex = pattern.toRegex()
            val match = regex.find(url)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1]
            }
        }
        return null
    }

    private fun openYouTubeVideo(url: String, videoId: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("vnd.youtube:$videoId"))
            intent.putExtra("VIDEO_ID", videoId)
            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
            } else {
                startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
            }
        } catch (e: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
            } catch (e2: Exception) {
                Log.e("MovieDetailsFragment", "Failed to open YouTube", e2)
            }
        }
    }

    private fun setupActors(actors: List<com.slooshfilm.app.data.model.Actor>) {
        if (actors.isEmpty()) {
            binding.actorsLabel.visibility = View.GONE
            binding.actorsRecyclerView.visibility = View.GONE
            binding.actorsGradientLeft.visibility = View.GONE
            binding.actorsGradientRight.visibility = View.GONE
            return
        }

        binding.actorsLabel.visibility = View.VISIBLE
        binding.actorsRecyclerView.visibility = View.VISIBLE
        // Инициально скрываем градиенты, они появятся при скролле
        binding.actorsGradientLeft.visibility = View.GONE
        binding.actorsGradientRight.visibility = View.GONE
        binding.actorsGradientLeft.alpha = 0f
        binding.actorsGradientRight.alpha = 0f

        val adapter = com.slooshfilm.app.ui.adapters.ActorAdapter { actor ->
            if (!actor.link.isNullOrEmpty()) {
                val bundle = bundleOf("actor_link" to actor.link)
                findNavController().navigate(R.id.action_global_to_actor_details, bundle)
            }
        }

        binding.actorsRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext(), androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)
        binding.actorsRecyclerView.adapter = adapter
        binding.actorsRecyclerView.setHasFixedSize(true)
        binding.actorsRecyclerView.itemAnimator = null
        
        // Добавляем OnScrollListener для элегантного паралакс эффекта и управления градиентом
        binding.actorsRecyclerView.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                val layoutManager = recyclerView.layoutManager as androidx.recyclerview.widget.LinearLayoutManager
                val childCount = layoutManager.childCount
                
                if (childCount == 0) return
                
                val itemCount = layoutManager.itemCount
                val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
                val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
                
                // === Логика левого градиента ===
                // Просто: если есть элементы слева от первого видимого - показываем градиент полностью
                if (firstVisiblePosition > 0) {
                    binding.actorsGradientLeft.visibility = View.VISIBLE
                    binding.actorsGradientLeft.alpha = 1f
                } else {
                    binding.actorsGradientLeft.visibility = View.GONE
                    binding.actorsGradientLeft.alpha = 0f
                }
                
                // === Логика правого градиента ===
                // Просто: если есть элементы справа от последнего видимого - показываем градиент полностью
                if (lastVisiblePosition < itemCount - 1) {
                    binding.actorsGradientRight.visibility = View.VISIBLE
                    binding.actorsGradientRight.alpha = 1f
                } else {
                    binding.actorsGradientRight.visibility = View.GONE
                    binding.actorsGradientRight.alpha = 0f
                }
                
                // === Паралакс эффект ===
                for (i in 0 until childCount) {
                    val child = layoutManager.getChildAt(i) ?: continue
                    
                    // Вычисляем позицию элемента относительно центра
                    val centerX = recyclerView.width / 2
                    val itemCenterX = child.left + child.width / 2
                    val distanceFromCenter = kotlin.math.abs(itemCenterX - centerX).toFloat()
                    val maxDistance = recyclerView.width / 2
                    
                    // Progress от 0 (в центре) до 1 (на краю)
                    val progress = (distanceFromCenter / maxDistance).coerceIn(0f, 1f)
                    
                    // Применяем паралакс эффект через масштабирование и смещение
                    val photoView = child.findViewById<com.google.android.material.imageview.ShapeableImageView>(com.slooshfilm.app.R.id.actor_photo)
                    if (photoView != null) {
                        // Небольшое уменьшение масштаба только на краях (от 1.0 до 0.95)
                        val scale = 1f - (progress * 0.05f)
                        photoView.scaleX = scale
                        photoView.scaleY = scale
                        
                        // Легкое смещение вверх/вниз для ощущения глубины
                        photoView.translationY = progress * 8f
                    }
                }
            }
        })
        
        adapter.submitList(actors)
    }

    private fun setupDirectors(directors: List<com.slooshfilm.app.data.model.Actor>) {
        if (directors.isEmpty()) return

        val directorText = directors.joinToString(", ") { it.name }
        val spannable = android.text.SpannableString(directorText)

        directors.forEach { director ->
            val start = directorText.indexOf(director.name)
            if (start >= 0) {
                val end = start + director.name.length
                spannable.setSpan(android.text.style.UnderlineSpan(), start, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (!director.link.isNullOrEmpty()) {
                    spannable.setSpan(object : android.text.style.ClickableSpan() {
                        override fun onClick(widget: View) {
                            val bundle = bundleOf("actor_link" to director.link)
                            findNavController().navigate(R.id.action_global_to_actor_details, bundle)
                        }

                        override fun updateDrawState(ds: android.text.TextPaint) {
                            super.updateDrawState(ds)
                            ds.color = binding.movieDirector.currentTextColor
                        }
                    }, start, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }

        binding.movieDirector.text = spannable
        binding.movieDirector.movementMethod = android.text.method.LinkMovementMethod.getInstance()
        binding.movieDirector.highlightColor = android.graphics.Color.TRANSPARENT
    }

    private fun setupComments(postId: String?, movieUrl: String?) {
        if (postId.isNullOrEmpty()) {
            binding.commentsButton.visibility = View.GONE
            return
        }

        binding.commentsButton.visibility = View.VISIBLE
        binding.commentsButton.text = "Комментарии (0)"

        lifecycleScope.launch {
            try {
                val repository = (requireActivity().application as SlooshApplication).hdRezkaRepository
                val count = repository.getCommentsCount(postId, movieUrl)
                if (isAdded) {
                    binding.commentsButton.text = "Комментарии ($count)"
                }
            } catch (e: Exception) {
                // Do nothing, button remains visible with (0)
            }
        }

        binding.commentsButton.setOnClickListener {
            val bundle = bundleOf("post_id" to postId)
            findNavController().navigate(R.id.action_global_to_comments, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
