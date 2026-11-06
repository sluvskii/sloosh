package com.slooshfilm.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.slooshfilm.app.data.api.MediaDto
import com.slooshfilm.app.data.api.canonicalMediaType
import com.slooshfilm.app.data.models.auth.FavoriteDto
import com.slooshfilm.app.data.models.auth.PlaybackMediaMetadata
import com.slooshfilm.app.data.models.auth.PlaybackProgressRecord
import com.slooshfilm.app.data.storage.FavoritesStore
import com.slooshfilm.app.data.storage.PlaybackProgressStore
import com.slooshfilm.app.data.storage.WatchProgressItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class CloudSyncService private constructor(private val context: Context) {
    private val databaseBaseURL = "https://sloosh-77434-default-rtdb.firebaseio.com/users"
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncDate = MutableStateFlow<Long?>(null)
    val lastSyncDate: StateFlow<Long?> = _lastSyncDate.asStateFlow()

    private var lastSyncAttempt: Long = 0L
    private val minSyncIntervalMs = 30_000L // 30 sec

    fun syncAllData(force: Boolean = false) {
        scope.launch {
            syncAllDataAsync(force)
        }
    }

    suspend fun syncAllDataAsync(force: Boolean = false) = withContext(Dispatchers.IO) {
        val authRepo = AuthRepository.getInstance(context)
        if (!authRepo.isAuthenticated) return@withContext
        val user = authRepo.currentUser.value
        val userId = user.id
        if (userId.isEmpty() || userId.startsWith("guest_")) return@withContext

        val now = System.currentTimeMillis()
        if (!force && (now - lastSyncAttempt) < minSyncIntervalMs) {
            return@withContext
        }
        lastSyncAttempt = now

        _isSyncing.value = true
        try {
            val token = authRepo.ensureFreshToken()

            // 1. Sync Favorites
            val favoritesStore = FavoritesStore.getInstance(context)
            val remoteFavorites = fetchRemoteFavorites(userId, token)
            val localFavorites = favoritesStore.favoritesFlow.value

            val mergedFavorites = mergeFavorites(localFavorites, remoteFavorites)
            favoritesStore.syncRemoteFavorites(mergedFavorites)
            pushRemoteFavorites(mergedFavorites.map { it.toFavoriteDto() }, userId, token)

            // 2. Sync Progress
            val progressStore = PlaybackProgressStore.getInstance(context)
            val remoteProgress = fetchRemoteProgress(userId, token)
            val remoteMetadata = fetchRemoteMetadata(userId, token)
            val localProgress = progressStore.itemsFlow.value

            val mergedProgress = mergeProgress(localProgress, remoteProgress, remoteMetadata)
            progressStore.syncRemoteProgress(mergedProgress)

            val (pushProgressList, pushMetadataList) = prepareProgressPush(mergedProgress)
            pushRemoteProgress(pushProgressList, userId, token)
            pushRemoteMetadata(pushMetadataList, userId, token)

            _lastSyncDate.value = System.currentTimeMillis()
        } catch (_: Exception) {
        } finally {
            _isSyncing.value = false
        }
    }

    private suspend fun makeUrl(path: String, idToken: String?): String {
        val encodedPath = path.split("/").joinToString("/") { URLEncoder.encode(it, "UTF-8") }
        var url = "$databaseBaseURL/$encodedPath.json"
        if (!idToken.isNullOrEmpty()) {
            url += "?auth=$idToken"
        }
        return url
    }

    // --- Favorites ---

    private suspend fun fetchRemoteFavorites(userId: String, idToken: String?): List<FavoriteDto> = withContext(Dispatchers.IO) {
        try {
            val url = makeUrl("$userId/favorites", idToken)
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful || body.isBlank() || body == "null") return@withContext emptyList()

            decodeFlexibleList<FavoriteDto>(body)
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun pushRemoteFavorites(favorites: List<FavoriteDto>, userId: String, idToken: String?) = withContext(Dispatchers.IO) {
        try {
            val url = makeUrl("$userId/favorites", idToken)
            val json = gson.toJson(favorites)
            val request = Request.Builder()
                .url(url)
                .put(json.toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute()
        } catch (_: Exception) {}
    }

    // --- Progress ---

    private suspend fun fetchRemoteProgress(userId: String, idToken: String?): List<PlaybackProgressRecord> = withContext(Dispatchers.IO) {
        try {
            val url = makeUrl("$userId/progress", idToken)
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful || body.isBlank() || body == "null") return@withContext emptyList()

            decodeFlexibleList<PlaybackProgressRecord>(body)
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun pushRemoteProgress(records: List<PlaybackProgressRecord>, userId: String, idToken: String?) = withContext(Dispatchers.IO) {
        try {
            val url = makeUrl("$userId/progress", idToken)
            val json = gson.toJson(records)
            val request = Request.Builder()
                .url(url)
                .put(json.toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute()
        } catch (_: Exception) {}
    }

    // --- Metadata ---

    private suspend fun fetchRemoteMetadata(userId: String, idToken: String?): List<PlaybackMediaMetadata> = withContext(Dispatchers.IO) {
        try {
            val url = makeUrl("$userId/metadata", idToken)
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful || body.isBlank() || body == "null") return@withContext emptyList()

            decodeFlexibleList<PlaybackMediaMetadata>(body)
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun pushRemoteMetadata(metadata: List<PlaybackMediaMetadata>, userId: String, idToken: String?) = withContext(Dispatchers.IO) {
        try {
            val url = makeUrl("$userId/metadata", idToken)
            val json = gson.toJson(metadata)
            val request = Request.Builder()
                .url(url)
                .put(json.toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute()
        } catch (_: Exception) {}
    }

    // --- Merging & Decoding Helpers ---

    private inline fun <reified T> decodeFlexibleList(jsonString: String): List<T> {
        return try {
            val element = JsonParser.parseString(jsonString)
            if (element.isJsonArray) {
                val listType = object : TypeToken<List<T>>() {}.type
                gson.fromJson<List<T>>(element, listType) ?: emptyList()
            } else if (element.isJsonObject) {
                val map = element.asJsonObject
                map.entrySet().mapNotNull { entry ->
                    try {
                        gson.fromJson(entry.value, T::class.java)
                    } catch (_: Exception) {
                        null
                    }
                }
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun mergeFavorites(local: List<MediaDto>, remote: List<FavoriteDto>): List<MediaDto> {
        val result = local.toMutableList()
        val existingKeys = result.map { it.favoriteIdentity }.toMutableSet()

        for (fav in remote) {
            val mediaDto = fav.toMediaDto()
            if (!existingKeys.contains(mediaDto.favoriteIdentity)) {
                result.add(mediaDto)
                existingKeys.add(mediaDto.favoriteIdentity)
            }
        }
        return result
    }

    private fun mergeProgress(
        local: List<WatchProgressItem>,
        remote: List<PlaybackProgressRecord>,
        metadata: List<PlaybackMediaMetadata>
    ): List<WatchProgressItem> {
        val metaMap = metadata.associateBy { it.detailsId }.toMutableMap()
        for (m in metadata) {
            if (!m.mediaKey.isNullOrEmpty()) metaMap[m.mediaKey] = m
            if (m.kpId > 0) metaMap["kp_${m.kpId}"] = m
        }

        val map = mutableMapOf<String, WatchProgressItem>()
        for (l in local) {
            val key = progressKey(l.mediaId, l.season, l.episode)
            map[key] = l
        }

        for (r in remote) {
            val cleanMediaId = r.mediaId.replace(Regex("_s\\d+_e\\d+$"), "")
            val key = progressKey(cleanMediaId, r.season, r.episode)
            val localItem = map[key]

            if (localItem == null || r.updatedAtMs > localItem.updatedAt) {
                val meta = metaMap[cleanMediaId] ?: metaMap[r.mediaId]
                val newItem = WatchProgressItem(
                    mediaId = cleanMediaId,
                    title = meta?.title ?: localItem?.title ?: "Видео",
                    posterUrl = meta?.posterUrl ?: localItem?.posterUrl,
                    backdropUrl = meta?.backdropUrl ?: localItem?.backdropUrl,
                    type = meta?.type ?: localItem?.type ?: if (r.season != null) "tv" else "movie",
                    season = r.season,
                    episode = r.episode,
                    voice = r.voiceover ?: localItem?.voice,
                    positionMs = (r.positionSec * 1000.0).toLong(),
                    durationMs = (r.durationSec * 1000.0).toLong(),
                    updatedAt = r.updatedAtMs
                )
                map[key] = newItem
            }
        }

        return map.values.sortedByDescending { it.updatedAt }
    }

    private fun progressKey(mediaId: String, season: Int?, episode: Int?): String {
        return if (season != null && episode != null) "${mediaId}_s${season}_e${episode}" else mediaId
    }

    private fun prepareProgressPush(items: List<WatchProgressItem>): Pair<List<PlaybackProgressRecord>, List<PlaybackMediaMetadata>> {
        val records = mutableListOf<PlaybackProgressRecord>()
        val metadataList = mutableListOf<PlaybackMediaMetadata>()
        val seenMedia = mutableSetOf<String>()

        for (item in items) {
            val rootKey = item.mediaId
            val recId = if (item.season != null && item.episode != null) "${rootKey}_s${item.season}_e${item.episode}" else rootKey

            records.add(
                PlaybackProgressRecord(
                    mediaId = recId,
                    kpId = rootKey.removePrefix("kp_").toIntOrNull() ?: 0,
                    tmdbId = rootKey.removePrefix("tmdb_").toIntOrNull(),
                    season = item.season,
                    episode = item.episode,
                    voiceover = item.voice,
                    positionSec = item.positionMs / 1000.0,
                    durationSec = item.durationMs / 1000.0,
                    watched = item.progress >= 0.93f,
                    updatedAtMs = item.updatedAt
                )
            )

            if (!seenMedia.contains(rootKey)) {
                seenMedia.add(rootKey)
                metadataList.add(
                    PlaybackMediaMetadata(
                        kpId = rootKey.removePrefix("kp_").toIntOrNull() ?: 0,
                        tmdbId = rootKey.removePrefix("tmdb_").toIntOrNull(),
                        detailsId = rootKey,
                        title = item.title,
                        type = item.type,
                        posterUrl = item.posterUrl,
                        backdropUrl = item.backdropUrl,
                        mediaKey = rootKey
                    )
                )
            }
        }
        return Pair(records, metadataList)
    }

    fun statusText(): String {
        if (_isSyncing.value) return "Синхронизация..."
        val last = _lastSyncDate.value
        if (last != null) {
            return "Синхронизировано"
        }
        val authRepo = AuthRepository.getInstance(context)
        if (authRepo.isAuthenticated) return "Облако подключено ☁️"
        return "Локальный режим"
    }

    companion object {
        @Volatile
        private var INSTANCE: CloudSyncService? = null

        fun getInstance(context: Context): CloudSyncService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CloudSyncService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

fun MediaDto.toFavoriteDto(): FavoriteDto {
    return FavoriteDto(
        id = cleanId,
        mediaId = cleanId,
        type = canonicalMediaType(type, originalId),
        title = displayTitle,
        posterUrl = getDisplayPosterUrl(),
        rating = rating,
        year = yearString,
        genres = genres
    )
}

fun FavoriteDto.toMediaDto(): MediaDto {
    return MediaDto(
        originalId = mediaId ?: id,
        title = title,
        originalTitle = null,
        year = year,
        rating = rating,
        posterUrl = posterUrl,
        type = type,
        genres = genres
    )
}
