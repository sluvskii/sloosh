package com.slooshfilm.app.data.models.auth

import androidx.compose.runtime.Immutable
import com.google.gson.annotations.SerializedName

@Immutable
data class UserProfile(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String? = null,
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("tag") val tag: String? = null,
    @SerializedName("photoURL") val photoURL: String? = null,
    @SerializedName("isAnonymous") val isAnonymous: Boolean = true,
    @SerializedName("provider") val provider: String = "anonymous", // "email", "google", "anonymous"
    @SerializedName("idToken") val idToken: String? = null,
    @SerializedName("refreshToken") val refreshToken: String? = null,
    @SerializedName("createdAt") val createdAt: Long = System.currentTimeMillis()
) {
    val displayTitle: String
        get() {
            if (isAnonymous) return "Гостевой аккаунт"
            if (!displayName.isNullOrBlank()) return displayName
            if (!tag.isNullOrBlank()) return "@$tag"
            if (!email.isNullOrBlank()) return email.substringBefore("@")
            return "Пользователь sloosh"
        }

    val displayTag: String
        get() = if (!tag.isNullOrBlank()) "@$tag" else ""

    val displaySubtitle: String
        get() {
            if (isAnonymous) return "Войдите для синхронизации"
            if (!tag.isNullOrBlank()) return "@$tag"
            if (!email.isNullOrBlank()) return email
            return "Аккаунт sloosh"
        }

    val avatarInitials: String
        get() {
            if (isAnonymous) return "👤"
            if (!displayName.isNullOrBlank()) {
                val trimmed = displayName.trim()
                if (trimmed.isNotEmpty()) return trimmed.take(1).uppercase()
            }
            if (!tag.isNullOrBlank()) {
                val clean = tag.trim()
                if (clean.isNotEmpty()) return clean.take(1).uppercase()
            }
            if (!email.isNullOrBlank()) {
                return email.take(1).uppercase()
            }
            return "S"
        }
}
