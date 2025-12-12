package com.slooshfilm.app.ui.filmography

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.slooshfilm.app.R
import com.slooshfilm.app.SlooshApplication
import com.slooshfilm.app.data.model.Movie
import com.slooshfilm.app.data.model.MovieParcelable
import com.slooshfilm.app.databinding.FragmentFilmographyBinding
import com.slooshfilm.app.ui.adapters.MovieAdapter

class FilmographyFragment : Fragment() {

    private var _binding: FragmentFilmographyBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilmographyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val roleTitle = arguments?.getString("role_title") ?: "Фильмография"
        @Suppress("UNCHECKED_CAST")
        val films = (arguments?.getParcelableArray("films") as? Array<MovieParcelable>)
            ?.map { Movie.fromParcelable(it) } ?: emptyList()

        binding.title.text = roleTitle

        setupUI()
        setupRecyclerView(films)
    }

    private fun setupUI() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.filmsRecyclerView.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            insets
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

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupRecyclerView(films: List<Movie>) {
        val localStorage = (requireActivity().application as SlooshApplication).localStorage
        val columns = localStorage.getGridColumns()
        binding.filmsRecyclerView.layoutManager = GridLayoutManager(requireContext(), columns)
        
        val adapter = MovieAdapter(columns) { movieUrl ->
            val bundle = Bundle().apply {
                putString("movie_url", movieUrl)
            }
            findNavController()
                .navigate(R.id.action_global_to_movie_details, bundle)
        }
        adapter.submitList(films)
        
        binding.filmsRecyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
