package com.slooshfilm.app.data.model

data class ActorRole(
    val role: String, // "Актер", "Режиссер" и т.д.
    val info: String? = null, // Дополнительная информация
    val films: List<Movie> = emptyList()
)

data class ActorDetails(
    val name: String,
    val originalName: String? = null,
    val photo: String? = null,
    val careers: List<String> = emptyList(), // Список профессий
    val dob: String? = null, // Дата рождения
    val birthPlace: String? = null, // Место рождения
    val height: String? = null, // Рост
    val biography: String? = null, // Биография
    val roles: List<ActorRole> = emptyList() // Роли с фильмами
)

