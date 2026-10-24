package com.slooshfilm.app.data.alloha

data class AllohaTranslation(
    val id: String,
    val name: String,
    val iframeUrl: String,
    val streamUrl: String? = null
)

data class AllohaEpisode(
    val season: Int,
    val episode: Int,
    val translations: List<AllohaTranslation>
)

data class AllohaSeason(
    val season: Int,
    val episodes: List<AllohaEpisode>
)

data class AllohaMovie(
    val title: String,
    val iframeUrl: String,
    val translations: List<AllohaTranslation>
)

data class AllohaApiResult(
    val title: String,
    val isSerial: Boolean,
    val movie: AllohaMovie?,
    val seasons: List<AllohaSeason>
) {
    val allTranslationNames: List<String>
        get() {
            if (isSerial) {
                val names = mutableListOf<String>()
                val seen = mutableSetOf<String>()
                for (season in seasons) {
                    for (episode in season.episodes) {
                        for (t in episode.translations) {
                            if (seen.add(t.name)) {
                                names.add(t.name)
                            }
                        }
                    }
                }
                return names
            } else if (movie != null) {
                return movie.translations.map { it.name }
            }
            return emptyList()
        }
}

data class QualityVariant(
    val label: String,
    val url: String
)

data class AudioVariant(
    val id: String,
    val title: String,
    val url: String,
    val qualityVariants: List<QualityVariant> = emptyList()
)

data class SubtitleTrack(
    val label: String,
    val language: String,
    val url: String
)

data class SkipTimeRange(
    val start: Double,
    val end: Double
)

data class AllohaResolvedStream(
    val videoUrl: String,
    val audioVariants: List<AudioVariant> = emptyList(),
    val qualityVariants: List<QualityVariant> = emptyList(),
    val subtitles: List<SubtitleTrack> = emptyList(),
    val headers: Map<String, String> = emptyMap(),
    val introRange: SkipTimeRange? = null,
    val outroRange: SkipTimeRange? = null
)
