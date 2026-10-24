package com.slooshfilm.app.data.storage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WatchProgressItem(
    val mediaId: String,
    val title: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val type: String = "movie",
    val season: Int? = null,
    val episode: Int? = null,
    val voice: String? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val progress: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val episodeLabel: String?
        get() = if (season != null && episode != null) "S${season} E${episode}" else null
}

class PlaybackProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("sloosh_watch_progress", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val key = "progress_items"

    private val _itemsFlow = MutableStateFlow<List<WatchProgressItem>>(loadItems())
    val itemsFlow: StateFlow<List<WatchProgressItem>> = _itemsFlow.asStateFlow()

    // Gson serialization of the whole list runs off the main thread (the player saves every
    // 5 seconds during playback); SharedPreferences.apply() already flushes to disk async.
    // Single-flight: only the latest list gets persisted when several saves queue up.
    private val writeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pendingWrite: Job? = null

    private fun loadItems(): List<WatchProgressItem> {
        val json = prefs.getString(key, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<WatchProgressItem>>() {}.type
            gson.fromJson<List<WatchProgressItem>>(json, type)?.sortedByDescending { it.updatedAt } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveItems(list: List<WatchProgressItem>) {
        _itemsFlow.value = list
        pendingWrite?.cancel()
        pendingWrite = writeScope.launch {
            val json = gson.toJson(list)
            prefs.edit().putString(key, json).apply()
        }
    }

    fun getProgress(mediaId: String): WatchProgressItem? {
        return _itemsFlow.value.firstOrNull { it.mediaId == mediaId }
    }

    /**
     * Прогресс конкретной серии (1:1 с iOS `PlaybackProgressStore.mediaId = <root>_s<S>_e<E>`).
     * Без сезона/серии возвращает прогресс фильма.
     */
    fun getProgress(mediaId: String, season: Int?, episode: Int?): WatchProgressItem? {
        return _itemsFlow.value.firstOrNull {
            it.mediaId == mediaId && it.season == season && it.episode == episode
        }
    }

    /**
     * Таймкод для возобновления (1:1 с iOS):
     * серия считается досмотренной при >=93% или если до конца осталось <=90 c — тогда начинаем с нуля.
     */
    fun resumePositionMs(mediaId: String, season: Int?, episode: Int?): Long {
        val item = getProgress(mediaId, season, episode) ?: return 0L
        val pos = item.positionMs
        val dur = item.durationMs
        if (pos <= 2_000L) return 0L
        if (dur <= 0L) return pos
        val doneByFraction = pos.toFloat() / dur.toFloat() >= 0.93f
        val doneByTail = dur >= 180_000L && dur - pos <= 90_000L
        val legacyTail = dur < 180_000L && (pos >= dur - 15_000L || pos.toFloat() / dur.toFloat() > 0.95f)
        return if (doneByFraction || doneByTail || legacyTail) 0L else pos
    }

    fun saveProgress(item: WatchProgressItem) {
        val current = _itemsFlow.value.toMutableList()
        // Заменяем только запись этой же серии: у тайтла может быть прогресс по нескольким сериям.
        current.removeAll {
            it.mediaId == item.mediaId && it.season == item.season && it.episode == item.episode
        }
        current.add(0, item.copy(updatedAt = System.currentTimeMillis()))
        // Потолок хранения: без него список растёт с каждой серией и сериализация дорожает.
        val capped = if (current.size > MAX_STORED_ITEMS) {
            current.sortedByDescending { it.updatedAt }.take(MAX_STORED_ITEMS)
        } else {
            current
        }
        saveItems(capped)
    }

    fun deleteProgress(mediaId: String) {
        val current = _itemsFlow.value.toMutableList()
        val removed = current.removeAll { it.mediaId == mediaId }
        if (removed) {
            saveItems(current)
        }
    }

    fun deleteProgress(mediaId: String, season: Int?, episode: Int?) {
        val current = _itemsFlow.value.toMutableList()
        val removed = current.removeAll {
            it.mediaId == mediaId && it.season == season && it.episode == episode
        }
        if (removed) {
            saveItems(current)
        }
    }

    fun clearAll() {
        saveItems(emptyList())
    }

    companion object {
        private const val MAX_STORED_ITEMS = 400

        @Volatile
        private var INSTANCE: PlaybackProgressStore? = null

        fun getInstance(context: Context): PlaybackProgressStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PlaybackProgressStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
