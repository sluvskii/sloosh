package com.slooshfilm.app.data.models

import com.slooshfilm.app.data.model.Movie

data class BookmarksPage(
    val categories: List<Bookmark>,
    val films: List<Movie>
)
