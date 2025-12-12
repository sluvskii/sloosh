package com.slooshfilm.app.data

import com.slooshfilm.app.data.models.Film
import org.jsoup.nodes.Document

object FilmsListModel {
    fun getFilmsFromPage(doc: Document): ArrayList<Film> {
        val films = ArrayList<Film>()
        val filmElements = doc.select("div.b-content__inline_item")

        for (element in filmElements) {
            val id = element.attr("data-id")
            val title = element.select("div.b-content__inline_item-link a").text()
            val posterUrl = element.select("div.b-content__inline_item-cover img").attr("src")
            val year = element.select("div.b-content__inline_item-link").text().split(",")[0]
            val ratingString = element.select("div.b-content__inline_item-rating").text()
            val rating = ratingString.toFloatOrNull() ?: 0.0f

            films.add(Film(id, title, posterUrl, year, rating))
        }

        return films
    }
}