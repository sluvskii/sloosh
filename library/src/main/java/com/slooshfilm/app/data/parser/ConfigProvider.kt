
package com.slooshfilm.app.data.parser

class ConfigProvider {

    // In the future, this will fetch the config from a remote server.
    fun getConfig(): ParsingConfig {
        return ParsingConfig(
            movieDetailsSelectors = MovieDetailsSelectors(
                titleSelector = "h1[itemprop=name]",
                descriptionSelector = "div.b-post__description_text",
                posterUrlSelector = ".b-sidecover img",
                infoTableSelector = "table.b-post__info tr",
                imdbRatingSelector = "IMDb: ([0-9.]+) \\(([^)]+)\\)",
                imdbVotesSelector = "IMDb: ([0-9.]+) \\(([^)]+)\\)",
                kpRatingSelector = "Кинопоиск: ([0-9.]+) \\(([^)]+)\\)",
                kpVotesSelector = "Кинопоиск: ([0-9.]+) \\(([^)]+)\\)",
                yearSelector = "Год",
                countrySelector = "Страна",
                directorSelector = "Режиссер",
                genreSelector = "Жанр",
                actorsSelector = "В ролях",
                durationSelector = "Время",
                releaseDateSelector = "Дата выхода",
                ageLimitSelector = "Возраст",
                postIdSelector = "div.b-userset__fav_holder",
                isSerialSelector = "meta[property=og:type]",
                trailerDataIdSelector = ".b-sidelinks__link.show-trailer",
                translatorsSelector = ".b-translator__item"
            )
        )
    }
}
