package com.slooshfilm.app.ui.actordetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.slooshfilm.app.R
import com.slooshfilm.app.SlooshApplication
import com.slooshfilm.app.data.model.ActorDetails
import com.slooshfilm.app.data.model.ActorRole
import com.slooshfilm.app.databinding.FragmentActorDetailsBinding
import com.slooshfilm.app.ui.AppViewModelFactory
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.launch

class ActorDetailsFragment : Fragment() {

    private var _binding: FragmentActorDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ActorDetailsViewModel by viewModels {
        val actorLink = arguments?.getString("actor_link")
        val application = requireActivity().application as SlooshApplication
        AppViewModelFactory(application.hdRezkaRepository, actorLink = actorLink)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActorDetailsBinding.inflate(inflater, container, false)
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

        setupBlurView()
        setupViews()
        observeViewModel()
    }

    private fun setupBlurView() {
        val radius = 20f
        val decorView = requireActivity().window.decorView
        val windowBackground = decorView.background
        val blurTarget = binding.blurTarget
        val scrimColor = ContextCompat.getColor(requireContext(), R.color.scrim_color)

        binding.backButtonBlur.setupWith(blurTarget)
            .setFrameClearDrawable(windowBackground)
            .setBlurRadius(radius)
            .setOverlayColor(scrimColor)
    }

    private fun setupViews() {
        binding.backButtonCard.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.retryButton.setOnClickListener {
            viewModel.retry()
        }

        binding.contentView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val titleAlpha = (scrollY.toFloat() / binding.actorPhoto.height).coerceIn(0f, 1f)
            binding.toolbarActorTitle.alpha = titleAlpha
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                binding.shimmerViewContainer.isVisible = state.isLoading
                binding.contentView.isVisible = state.actor != null && state.error == null
                binding.errorView.isVisible = state.error != null

                state.error?.let { binding.errorMessage.text = it }
                state.actor?.let { displayActorDetails(it) }
            }
        }
    }

    private fun displayActorDetails(actor: ActorDetails) {
        binding.toolbarActorTitle.text = actor.name
        binding.actorName.text = actor.name
        binding.actorOriginalName.text = actor.originalName
        binding.actorOriginalName.isVisible = actor.originalName != null

        if (!actor.photo.isNullOrEmpty()) {
            Glide.with(this)
                .load(actor.photo)
                .placeholder(R.drawable.placeholder_movie)
                .into(binding.actorPhoto)

            Glide.with(this)
                .load(actor.photo)
                .apply(RequestOptions.bitmapTransform(MultiTransformation(CenterCrop(), BlurTransformation(25, 3))))
                .thumbnail(0.25f)
                .dontAnimate()
                .placeholder(R.drawable.placeholder_movie)
                .into(binding.backgroundActorPhoto)
        }

        // Actor Info Section
        val hasDob = !actor.dob.isNullOrBlank()
        binding.dobLayout.isVisible = hasDob
        if (hasDob) {
            binding.actorDob.text = actor.dob
        }

        val hasBirthPlace = !actor.birthPlace.isNullOrBlank()
        binding.birthPlaceLayout.isVisible = hasBirthPlace
        if (hasBirthPlace) {
            binding.actorBirthPlace.text = actor.birthPlace
        }

        val hasHeight = !actor.height.isNullOrBlank()
        binding.heightLayout.isVisible = hasHeight
        if (hasHeight) {
            binding.actorHeight.text = actor.height
        }

        binding.actorInfoLayout.isVisible = hasDob || hasBirthPlace || hasHeight

        // Biography
        binding.actorBiography.text = actor.biography
        binding.biographyLabel.isVisible = !actor.biography.isNullOrEmpty()
        binding.actorBiography.isVisible = !actor.biography.isNullOrEmpty()

        // Filmography
        setupRoles(actor.roles)
    }

    private fun setupRoles(roles: List<ActorRole>) {
        val hasRoles = roles.isNotEmpty()
        binding.rolesLabel.isVisible = hasRoles
        binding.rolesRecyclerView.isVisible = hasRoles

        if (hasRoles) {
            binding.rolesRecyclerView.layoutManager = LinearLayoutManager(requireContext())

            val adapter = ActorRolesAdapter(
                roles,
                onMovieClick = { movie ->
                    val bundle = bundleOf("movie_url" to movie.url)
                    findNavController().navigate(R.id.action_global_to_movie_details, bundle)
                },
                onViewAllClick = { role ->
                    val films = role.films.map { it.toParcelable() }.toTypedArray()
                    val bundle = bundleOf(
                        "role_title" to role.role,
                        "films" to films
                    )
                    findNavController().navigate(R.id.action_global_to_filmography, bundle)
                }
            )
            binding.rolesRecyclerView.adapter = adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}