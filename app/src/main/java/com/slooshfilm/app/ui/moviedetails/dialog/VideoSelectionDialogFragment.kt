
package com.slooshfilm.app.ui.moviedetails.dialog

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.slooshfilm.app.R
import com.slooshfilm.app.SlooshApplication
import com.slooshfilm.app.data.model.Episode
import com.slooshfilm.app.data.model.MovieDetails
import com.slooshfilm.app.data.model.Translator
import com.slooshfilm.app.databinding.DialogVideoSelectionBinding
import com.slooshfilm.app.databinding.ItemSeasonChipBinding
import com.slooshfilm.app.ui.moviedetails.MovieDetailsViewModel
import com.slooshfilm.app.ui.player.PlayerActivity
import com.slooshfilm.app.ui.player.PlayerViewModel
import com.slooshfilm.app.data.hdrezka.StreamInfo
import kotlinx.coroutines.launch

class VideoSelectionDialogFragment : BottomSheetDialogFragment() {

    private var _binding: DialogVideoSelectionBinding? = null
    private val binding get() = _binding!!

    private val movieDetailsViewModel: MovieDetailsViewModel by activityViewModels()
    private val playerViewModel: PlayerViewModel by activityViewModels { (requireActivity().application as SlooshApplication).viewModelFactory }

    // --- State --- //
    private var selectedTranslator: Translator? = null
    private var selectedSeasonNumber: Int? = null
    private var selectedEpisode: Episode? = null
    private var currentStreamInfo: StreamInfo? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { 
            view?.post { 
                setupInitialState()
            }
        }
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogVideoSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.closeButton.setOnClickListener { dismiss() }
        binding.watchButton.setOnClickListener { onWatchClicked() }

        viewLifecycleOwner.lifecycleScope.launch {
            playerViewModel.isLoading.collect { loading ->
                showLoading(loading)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            playerViewModel.streams.collect { streamInfo ->
                currentStreamInfo = streamInfo
                if (streamInfo != null && streamInfo.streams.isNotEmpty()) {
                    updateQualityUI(streamInfo.streams.keys.toList())
                } else {
                    binding.qualityLayout.isVisible = false
                }
            }
        }
    }

    private fun setupInitialState() {
        val details = movieDetailsViewModel.viewState.value?.movieDetails ?: return
        binding.titleText.text = details.title

        val translators = details.translators.values.toList()
        if (translators.isEmpty() && details.isSerial) { // Only show error for serials
            Toast.makeText(context, "Озвучки не найдены", Toast.LENGTH_SHORT).show()
            return
        }

        val translatorNames = translators.map { it.name }
        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, translatorNames)
        binding.translatorInput.setAdapter(adapter)
        binding.translatorInput.setOnItemClickListener { _, _, position, _ ->
            onTranslatorSelected(translators[position], details)
        }

        val preferred = translators.firstOrNull { t ->
            val n = t.name.lowercase()
            n.contains("hdrezka") || n.contains("hdrezka studio") || n.contains("studio")
        } ?: translators.firstOrNull()

        view?.post {
            if (preferred != null) {
                Log.d("VideoSelectionDialog", "Selecting preferred translator: name=${preferred.name}, id=${preferred.id}")
                binding.translatorInput.setText(preferred.name, false)
                onTranslatorSelected(preferred, details, isFirstTime = true)
            }
        }
    }

    private fun onTranslatorSelected(translator: Translator, details: MovieDetails, isFirstTime: Boolean = false) {
        selectedTranslator = translator
        if (isFirstTime) {
            binding.translatorInput.setText(translator.name, false)
        }

        resetAllDownstreamState()

        if (details.isSerial) {
            fetchSeasonsAndEpisodes(details)
        } else {
            fetchStreamsForFilm(details)
        }
    }

    private fun fetchSeasonsAndEpisodes(details: MovieDetails) {
        showLoading(true)
        lifecycleScope.launch {
            val parsedSeasons = details.seasons
            val seasons = if (parsedSeasons.isNotEmpty()) {
                Log.d("VideoSelectionDialog", "fetchSeasonsAndEpisodes: using parsed seasons from MovieDetails for postId=${details.postId}, translatorId=${selectedTranslator?.id}")
                parsedSeasons
            } else {
                Log.d("VideoSelectionDialog", "fetchSeasonsAndEpisodes: fetching seasons from network for postId=${details.postId}, translatorId=${selectedTranslator?.id}")
                val fetched = movieDetailsViewModel.getEpisodes(details.postId!!, selectedTranslator!!.id!!, details.pageUrl)
                fetched
            }

            showLoading(false)

            if (seasons.isEmpty()) return@launch

            val seasonNumbers = seasons.keys.sorted()
            selectedSeasonNumber = seasonNumbers.first()

            binding.seasonsRecycler.isVisible = true
            binding.seasonsRecycler.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            binding.seasonsRecycler.adapter = SeasonsAdapter(seasonNumbers) { seasonNum ->
                selectedSeasonNumber = seasonNum
                resetEpisodeAndQualityState()
                updateEpisodesUI(seasons[seasonNum] ?: emptyList())
            }
            updateEpisodesUI(seasons[selectedSeasonNumber] ?: emptyList())
        }
    }

    private fun updateEpisodesUI(episodes: List<Episode>) {
        binding.episodesChipGroup.removeAllViews()
        if (episodes.isEmpty()) return

        selectedEpisode = episodes.first()

        episodes.forEach { episode ->
            val chip = layoutInflater.inflate(R.layout.item_episode_chip, binding.episodesChipGroup, false) as Chip
            chip.text = "${episode.number} серия"
            chip.id = episode.number
            chip.isCheckable = true
            chip.setOnClickListener { 
                selectedEpisode = episode
                binding.episodesChipGroup.check(chip.id)
                fetchStreamsForSeries()
            }
            binding.episodesChipGroup.addView(chip)
        }
        
        binding.episodesChipGroup.check(selectedEpisode!!.number)
        binding.episodesChipGroup.isVisible = true
        fetchStreamsForSeries()
    }

    private fun fetchStreamsForFilm(details: MovieDetails) {
        showLoading(true)
        playerViewModel.getStreams(details.postId!!, selectedTranslator!!.id!!, null, null)
    }

    private fun fetchStreamsForSeries() {
        showLoading(true)
        resetQualityState()
        val details = movieDetailsViewModel.viewState.value?.movieDetails!!
        val (postId, translatorId) = details.postId!! to selectedTranslator!!.id!!
        val (season, episode) = selectedSeasonNumber!! to selectedEpisode!!.number

        playerViewModel.getStreams(postId, translatorId, season.toString(), episode.toString())
    }

    private fun updateQualityUI(qualities: List<String>) {
        binding.qualityLayout.isVisible = qualities.isNotEmpty()
        if (qualities.isEmpty()) return

        val sortedQualities = qualities.sortedByDescending { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, sortedQualities)
        binding.qualityInput.setAdapter(adapter)
        binding.qualityInput.setText(sortedQualities.first(), false)
    }

    private fun onWatchClicked() {
        val quality = binding.qualityInput.text.toString()
        if (quality.isBlank()) {
            Toast.makeText(context, "Выберите качество", Toast.LENGTH_SHORT).show()
            return
        }
        val streamInfo = currentStreamInfo ?: return
        val url = streamInfo.streams[quality] ?: return
        val details = movieDetailsViewModel.viewState.value?.movieDetails ?: return

        startActivity(PlayerActivity.newIntent(requireContext(), url, details.postId!!, details.title, selectedSeasonNumber, selectedEpisode?.number, selectedTranslator?.id, streamInfo))
        dismiss()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.watchButton.text = if (isLoading) "" else "Смотреть"
        binding.watchButtonProgress.isVisible = isLoading
        binding.watchButton.isClickable = !isLoading
    }
    
    private fun resetAllDownstreamState() {
        binding.seasonsRecycler.isVisible = false
        binding.seasonsRecycler.adapter = null
        selectedSeasonNumber = null
        resetEpisodeAndQualityState()
    }

    private fun resetEpisodeAndQualityState() {
        binding.episodesChipGroup.isVisible = false
        binding.episodesChipGroup.removeAllViews()
        selectedEpisode = null
        resetQualityState()
    }

    private fun resetQualityState() {
        binding.qualityLayout.isVisible = false
        binding.qualityInput.setText("", false)
        currentStreamInfo = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class SeasonsAdapter(private val seasons: List<Int>, private val onClick: (Int) -> Unit) : RecyclerView.Adapter<SeasonsAdapter.ViewHolder>() {
        private var selectedPosition = 0
        inner class ViewHolder(val binding: ItemSeasonChipBinding) : RecyclerView.ViewHolder(binding.root)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(ItemSeasonChipBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.binding.chip.text = "${seasons[position]} сезон"
            holder.binding.chip.isChecked = position == selectedPosition
            holder.binding.chip.setOnClickListener {
                if (selectedPosition != holder.bindingAdapterPosition) {
                    notifyItemChanged(selectedPosition)
                    selectedPosition = holder.bindingAdapterPosition
                    notifyItemChanged(selectedPosition)
                    onClick(seasons[position])
                }
            }
        }
        override fun getItemCount() = seasons.size
    }

    companion object {
        const val TAG = "VideoSelectionDialog"
        fun newInstance(): VideoSelectionDialogFragment {
            return VideoSelectionDialogFragment()
        }
    }
}
