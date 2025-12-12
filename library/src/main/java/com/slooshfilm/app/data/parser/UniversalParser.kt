
package com.slooshfilm.app.data.parser

import android.util.Log
import com.slooshfilm.app.data.model.Actor
import com.slooshfilm.app.data.model.Episode
import com.slooshfilm.app.data.model.EpisodeStatus
import com.slooshfilm.app.data.model.MovieDetails
import com.slooshfilm.app.data.model.MoviePart
import com.slooshfilm.app.data.model.Translator
import org.jsoup.Jsoup
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class UniversalParser {

    fun parseMovieDetails(html: String, pageUrl: String, config: MovieDetailsSelectors): MovieDetails? {
        return try {
            val doc = Jsoup.parse(html, pageUrl)

            val title = doc.selectFirst(config.titleSelector)?.text() ?: ""
            val description = doc.select(config.descriptionSelector).text()
            val posterUrl = doc.selectFirst(config.posterUrlSelector)?.absUrl("src")
            val infoTable = doc.select(config.infoTableSelector)

            var imdbRating: String? = null
            var imdbVotes: String? = null
            var kpRating: String? = null
            var kpVotes: String? = null

            val ratingsCellText = infoTable.firstOrNull { it.selectFirst("td")?.text()?.startsWith("Рейтинг") == true }
                ?.select("td")
                ?.getOrNull(1)
                ?.text()

            if (ratingsCellText != null) {
                val imdbRegex = "IMDb: ([0-9.]+) \\(([^)]+)\\)".toRegex()
                val kpRegex = "Кинопоиск: ([0-9.]+) \\(([^)]+)\\)".toRegex()

                imdbRegex.find(ratingsCellText)?.let {
                    imdbRating = it.groupValues[1]
                    imdbVotes = it.groupValues[2]
                }

                kpRegex.find(ratingsCellText)?.let {
                    kpRating = it.groupValues[1]
                    kpVotes = it.groupValues[2]
                }
            }

            val year = infoTable.firstOrNull { it.text().contains("Год") }?.select("a")?.text()
            val country = infoTable.firstOrNull { it.text().contains("Страна") }?.select("a")?.joinToString(", ") { it.text() }
            val director = infoTable.firstOrNull { it.text().contains("Режиссер") }?.select("a")?.firstOrNull()?.text()
            val genres = infoTable.firstOrNull { it.text().contains("Жанр") }?.select("a")?.map { it.text() }
            val actors = infoTable.firstOrNull { it.text().contains("В ролях") }?.select("a")?.map { it.text() }
            val duration = infoTable.firstOrNull { it.text().contains("Время") }?.select("td")?.lastOrNull()?.text()
            val releaseDate = infoTable.firstOrNull { it.text().contains("Дата выхода") }?.select("td")?.lastOrNull()?.text()
            val ageLimit = infoTable.firstOrNull { it.text().contains("Возраст") }?.select("td")?.lastOrNull()?.text()?.let { "${it.filter { c -> c.isDigit() }}+" }

            val actorsWithPhotos = mutableListOf<Actor>()
            val directors = mutableListOf<Actor>()
            doc.select(".persons-list-holder .person-name-item").forEach { personEl ->
                val name = personEl.select("span").firstOrNull()?.text() ?: ""
                val photo = personEl.attr("data-photo").takeIf { it.isNotEmpty() && it != "null" }
                val link = personEl.select("a").firstOrNull()?.absUrl("href")
                val job = personEl.attr("data-job").takeIf { it.isNotEmpty() }

                if (name.isNotEmpty()) {
                    val actor = Actor(
                        name = name,
                        photo = photo,
                        link = link,
                        isDirector = job?.contains("режиссер", ignoreCase = true) == true
                    )
                    if (actor.isDirector) {
                        directors.add(actor)
                    } else {
                        actorsWithPhotos.add(actor)
                    }
                }
            }

            val postId = doc.selectFirst(config.postIdSelector)?.attr("data-post_id")
            val isSerial = doc.selectFirst(config.isSerialSelector)?.attr("content") == "video.tv_series"
            val trailerDataId = doc.selectFirst(config.trailerDataIdSelector)?.attr("data-id")
            Log.d("UniversalParser", "Trailer selector: ${config.trailerDataIdSelector}, Found ID: $trailerDataId")

            val translators = doc.select(config.translatorsSelector).mapNotNull { el ->
                val id = el.attr("data-translator_id")
                val name = el.attr("title")
                if (id.isNotEmpty() && name.isNotEmpty()) name to Translator(name, id, emptyMap())
                else null
            }.toMap().toMutableMap()

            if (translators.isEmpty()) {
                val scriptHtml = doc.html()
                val movieRegex = "initCDNMoviesEvents\\(\\s*\\d+\\s*,\\s*(\\d+)".toRegex()
                val seriesRegex = "initCDNSeriesEvents\\(\\s*\\d+\\s*,\\s*(\\d+)".toRegex()
                
                val foundId = movieRegex.find(scriptHtml)?.groupValues?.get(1)
                    ?: seriesRegex.find(scriptHtml)?.groupValues?.get(1)
                
                if (foundId != null) {
                    translators["Default"] = Translator("По умолчанию", foundId, emptyMap())
                }
            }

            val seasons = mutableMapOf<Int, MutableList<Episode>>()
            if (isSerial) {
                doc.select(".b-postepisodes .b-postepisode").forEach { epEl ->
                    val link = epEl.selectFirst("a")?.attr("href") ?: return@forEach
                    val seasonNum = epEl.attr("data-season").toIntOrNull() ?: 1
                    val epNum = epEl.attr("data-episode").toIntOrNull() ?: 0
                    
                    // Try multiple selectors for title
                    var titleEp = epEl.selectFirst(".ep-title")?.text()
                    if (titleEp.isNullOrBlank()) {
                        titleEp = epEl.selectFirst("a")?.attr("title")
                    }
                    if (titleEp.isNullOrBlank()) {
                        titleEp = epEl.selectFirst("a")?.text()?.trim()
                    }
                    // Fallback: use episode number as display
                    if (titleEp.isNullOrBlank()) {
                        titleEp = "Серия $epNum"
                    }
                    
                    // Try to extract release date
                    var releaseDate: String? = null
                    var status = EpisodeStatus.UPCOMING
                    
                    val dateText = epEl.selectFirst(".date")?.text() 
                        ?: epEl.selectFirst(".ep-date")?.text()
                        ?: epEl.attr("data-date")
                    
                    if (!dateText.isNullOrBlank()) {
                        releaseDate = dateText
                        // Try to determine status based on date
                        try {
                            val formatter = DateTimeFormatter.ofPattern("d.M.yyyy")
                            val epDate = LocalDate.parse(dateText, formatter)
                            val today = LocalDate.now()
                            status = when {
                                epDate < today -> EpisodeStatus.RELEASED
                                epDate == today -> EpisodeStatus.CURRENT
                                else -> EpisodeStatus.UPCOMING
                            }
                        } catch (e: Exception) {
                            Log.d("UniversalParser", "Could not parse date '$dateText': ${e.message}")
                        }
                    }
                    
                    if (seasonNum > 0 && epNum > 0) {
                        seasons.getOrPut(seasonNum) { mutableListOf() }
                            .add(Episode(seasonNum, epNum, titleEp, link, releaseDate, status))
                    }
                }
                Log.d("UniversalParser", "Parsed ${seasons.size} seasons with ${seasons.values.sumOf { it.size }} episodes")
            }

            val franchise = doc.select(".b-post__partcontent_item").mapNotNull { el ->
                try {
                    val titleEl = el.selectFirst(".title a")
                    val name = titleEl?.text() ?: el.selectFirst(".title")?.text() ?: return@mapNotNull null
                    val link = titleEl?.absUrl("href") ?: el.attr("data-url").takeIf { it.isNotEmpty() }
                    val year = el.selectFirst(".year")?.text()
                    val rating = el.selectFirst(".rating")?.text()?.takeIf { it != "dash" }

                    if (link.isNullOrEmpty()) return@mapNotNull null

                    com.slooshfilm.app.data.model.MoviePart(
                        name = name,
                        year = year,
                        rating = rating,
                        link = link
                    )
                } catch (e: Exception) {
                    Log.e("UniversalParser", "Failed to parse franchise item", e)
                    null
                }
            }

            MovieDetails(
                postId = postId,
                pageUrl = pageUrl,
                title = title,
                description = description,
                posterUrl = posterUrl ?: "",
                isSerial = isSerial,
                translators = translators,
                trailerDataId = trailerDataId,
                seasons = seasons,
                ratingImdb = imdbRating,
                ratingImdbVotes = imdbVotes,
                ratingKinopoisk = kpRating,
                ratingKinopoiskVotes = kpVotes,
                genres = genres,
                year = year,
                country = country,
                director = director,
                directors = directors,
                actors = actors,
                actorsWithPhotos = actorsWithPhotos,
                ageLimit = ageLimit,
                duration = duration,
                releaseDate = releaseDate,
                franchise = franchise
            )

        } catch (e: Exception) {
            Log.e("UniversalParser", "Failed to parse movie details", e)
            null
        }
    }
}
