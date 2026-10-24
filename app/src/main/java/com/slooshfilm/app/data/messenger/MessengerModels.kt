package com.slooshfilm.app.data.messenger

import com.slooshfilm.app.data.api.normalizeImageUrl

data class SlooshUser(
    val id: String,
    val displayName: String,
    val tag: String? = null,
    val avatarUrl: String? = null,
    val isOnline: Boolean? = false,
    val lastSeenMs: Long? = null
) {
    val displayTitle: String
        get() = if (displayName.isNotBlank()) displayName else if (!tag.isNullOrBlank()) "@${tag.trimStart('@')}" else "Пользователь Sloosh"

    val displayTag: String
        get() = if (!tag.isNullOrBlank()) "@${tag.trimStart('@')}" else ""

    val avatarInitials: String
        get() {
            val name = if (displayName.isNotBlank()) displayName else (tag ?: "S")
            return name.trim().take(1).uppercase()
        }
}

data class MediaCardPayload(
    val mediaId: String,
    val type: String = "movie",
    val title: String,
    val posterUrl: String? = null,
    val rating: Double? = null,
    val year: String? = null
) {
    val cleanId: String get() = mediaId.replace("tv_", "").replace("movie_", "")
    val isTvSeries: Boolean get() = type.lowercase() in listOf("tv", "series", "serial", "show")
    fun getDisplayPosterUrl(): String? = normalizeImageUrl(posterUrl, mediaId, isLowQuality = true)
}

data class ChatMessage(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val text: String? = null,
    val media: MediaCardPayload? = null,
    val timestampMs: Long = System.currentTimeMillis(),
    val isRead: Boolean = true
)

data class ChatConversation(
    val chatId: String,
    val peerUser: SlooshUser,
    val lastMessageText: String,
    val unreadCount: Int = 0,
    val updatedAtMs: Long = System.currentTimeMillis()
)

data class ChannelModel(
    val id: String,
    val tag: String,
    val name: String,
    val description: String = "",
    val avatarEmoji: String? = null,
    val avatarUrl: String? = null,
    val ownerName: String = "Sloosh Team",
    val subscriberCount: Int = 1,
    val isVerified: Boolean = true,
    val lastPostText: String? = null,
    val lastPostTimestampMs: Long? = null
) {
    val displayTag: String get() = "@${tag.trimStart('@')}"
}

data class ChannelPost(
    val id: String,
    val channelId: String,
    val channelName: String,
    val channelTag: String,
    val channelAvatarEmoji: String? = null,
    val title: String? = null,
    val text: String,
    val media: MediaCardPayload? = null,
    val timestampMs: Long = System.currentTimeMillis(),
    val viewsCount: Int = 120,
    val reactionsCount: Int = 15
)
