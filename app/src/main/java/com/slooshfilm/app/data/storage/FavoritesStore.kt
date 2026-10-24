package com.slooshfilm.app.data.storage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.slooshfilm.app.data.api.MediaDto
import com.slooshfilm.app.data.api.canonicalMediaType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FavoritesStore(context: Context) {
    private val prefs = context.getSharedPreferences("sloosh_favorites", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val key = "favorites_list"

    private val _favoritesFlow = MutableStateFlow<List<MediaDto>>(loadFavorites())
    val favoritesFlow: StateFlow<List<MediaDto>> = _favoritesFlow.asStateFlow()

    // MediaDto is a wide model; serialize off the main thread so toggling a favorite
    // never costs a frame. Single-flight: only the latest list gets persisted.
    private val writeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pendingWrite: Job? = null

    private fun loadFavorites(): List<MediaDto> {
        val json = prefs.getString(key, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<MediaDto>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveFavorites(list: List<MediaDto>) {
        _favoritesFlow.value = list
        pendingWrite?.cancel()
        pendingWrite = writeScope.launch {
            val json = gson.toJson(list)
            prefs.edit().putString(key, json).apply()
        }
    }

    fun isFavorite(id: String): Boolean {
        return _favoritesFlow.value.any { it.cleanId == id }
    }

    fun isFavorite(id: String, mediaType: String): Boolean {
        val canonicalType = canonicalMediaType(mediaType)
        return _favoritesFlow.value.any {
            it.cleanId == id && canonicalMediaType(it.type, it.originalId) == canonicalType
        }
    }

    fun toggleFavorite(media: MediaDto): Boolean {
        val current = _favoritesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.favoriteIdentity == media.favoriteIdentity }
        val isNowFavorite = if (index >= 0) {
            current.removeAt(index)
            false
        } else {
            current.add(0, media)
            true
        }
        saveFavorites(current)
        return isNowFavorite
    }

    fun removeFavorite(id: String) {
        val current = _favoritesFlow.value.toMutableList()
        val removed = current.removeAll { it.cleanId == id }
        if (removed) {
            saveFavorites(current)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: FavoritesStore? = null

        fun getInstance(context: Context): FavoritesStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FavoritesStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
