package com.slooshfilm.app.data.model

data class Actor(
    val name: String,
    val photo: String? = null,
    val link: String? = null,
    val isDirector: Boolean = false
)

