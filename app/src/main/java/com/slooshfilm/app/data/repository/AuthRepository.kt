package com.slooshfilm.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.slooshfilm.app.data.models.auth.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class AuthRepository private constructor(private val context: Context) {
    private val prefs = context.getSharedPreferences("sloosh_auth", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val userDefaultsKey = "sloosh_user_profile"

    private val firebaseApiKey = "AIzaSyAQussJy2N2Nfa6dwl7GuWJ7jAK7fI3Upc"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val _currentUser = MutableStateFlow(loadStoredUser())
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private var tokenLastRefreshedAt: Long = 0L
    private val tokenRefreshCooldownMs: Long = 50 * 60 * 1000L // 50 mins

    val isAuthenticated: Boolean
        get() = !_currentUser.value.isAnonymous

    val isAnonymous: Boolean
        get() = _currentUser.value.isAnonymous

    private fun loadStoredUser(): UserProfile {
        val json = prefs.getString(userDefaultsKey, null)
        if (!json.isNullOrBlank()) {
            try {
                val user = gson.fromJson(json, UserProfile::class.java)
                if (user != null) {
                    if (user.idToken != null) {
                        tokenLastRefreshedAt = System.currentTimeMillis()
                    }
                    return user
                }
            } catch (_: Exception) {}
        }
        val guest = UserProfile(
            id = "guest_${UUID.randomUUID().toString().take(8)}",
            isAnonymous = true,
            provider = "anonymous"
        )
        saveUserInternal(guest)
        return guest
    }

    fun saveUser(user: UserProfile) {
        saveUserInternal(user)
        _currentUser.value = user
    }

    private fun saveUserInternal(user: UserProfile) {
        if (user.idToken != null) {
            tokenLastRefreshedAt = System.currentTimeMillis()
        }
        val json = gson.toJson(user)
        prefs.edit().putString(userDefaultsKey, json).apply()
    }

    fun clearError() {
        _lastError.value = null
    }

    suspend fun signUp(email: String, password: String, displayName: String? = null): Boolean = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _lastError.value = null
        try {
            val cleanEmail = email.trim().lowercase()

            if (!isValidEmail(cleanEmail)) {
                _lastError.value = "Введите корректный Email (например, name@domain.com)"
                return@withContext false
            }

            if (password.length < 6) {
                _lastError.value = "Пароль должен содержать не менее 6 символов"
                return@withContext false
            }

            val url = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$firebaseApiKey"
            val body = JsonObject().apply {
                addProperty("email", cleanEmail)
                addProperty("password", password)
                addProperty("returnSecureToken", true)
            }

            val request = Request.Builder()
                .url(url)
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                _lastError.value = parseFirebaseError(responseBody)
                return@withContext false
            }

            val json = JsonParser.parseString(responseBody).asJsonObject
            val idToken = json.get("idToken")?.asString
            val refreshToken = json.get("refreshToken")?.asString
            val localId = json.get("localId")?.asString

            if (idToken.isNullOrEmpty() || refreshToken.isNullOrEmpty() || localId.isNullOrEmpty()) {
                _lastError.value = "Неожиданный ответ от сервера"
                return@withContext false
            }

            val finalName = displayName?.trim()?.ifEmpty { null }
            if (finalName != null) {
                updateDisplayNameInternal(idToken, finalName)
            }

            val newUser = UserProfile(
                id = localId,
                email = cleanEmail,
                displayName = finalName ?: cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                tag = null,
                photoURL = null,
                isAnonymous = false,
                provider = "email",
                idToken = idToken,
                refreshToken = refreshToken
            )
            saveUser(newUser)

            CloudSyncService.getInstance(context).syncAllData(force = true)
            true
        } catch (e: Exception) {
            _lastError.value = "Ошибка сети: ${e.localizedMessage ?: "Проверьте интернет-соединение"}"
            false
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun signIn(email: String, password: String): Boolean = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _lastError.value = null
        try {
            val cleanEmail = email.trim().lowercase()

            if (!isValidEmail(cleanEmail)) {
                _lastError.value = "Введите корректный Email"
                return@withContext false
            }

            if (password.isEmpty()) {
                _lastError.value = "Введите пароль"
                return@withContext false
            }

            val url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$firebaseApiKey"
            val body = JsonObject().apply {
                addProperty("email", cleanEmail)
                addProperty("password", password)
                addProperty("returnSecureToken", true)
            }

            val request = Request.Builder()
                .url(url)
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                _lastError.value = parseFirebaseError(responseBody)
                return@withContext false
            }

            val json = JsonParser.parseString(responseBody).asJsonObject
            val idToken = json.get("idToken")?.asString
            val refreshToken = json.get("refreshToken")?.asString
            val localId = json.get("localId")?.asString

            if (idToken.isNullOrEmpty() || refreshToken.isNullOrEmpty() || localId.isNullOrEmpty()) {
                _lastError.value = "Неожиданный ответ от сервера"
                return@withContext false
            }

            val serverDisplayName = json.get("displayName")?.asString?.ifEmpty { null }
            val displayName = serverDisplayName ?: cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }

            val user = UserProfile(
                id = localId,
                email = cleanEmail,
                displayName = displayName,
                tag = null,
                photoURL = null,
                isAnonymous = false,
                provider = "email",
                idToken = idToken,
                refreshToken = refreshToken
            )
            saveUser(user)

            CloudSyncService.getInstance(context).syncAllData(force = true)
            true
        } catch (e: Exception) {
            _lastError.value = "Ошибка сети: ${e.localizedMessage ?: "Проверьте интернет-соединение"}"
            false
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun signInWithGoogleNative(data: android.content.Intent?, context: Context): Boolean = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _lastError.value = null
        try {
            val result = NativeGoogleAuthManager.getInstance(context).handleSignInResult(data)
            if (result.isSuccess) {
                val user = result.getOrThrow()
                saveUser(user)
                CloudSyncService.getInstance(context).syncAllData(force = true)
                true
            } else {
                val err = result.exceptionOrNull()?.message
                if (err != null && !err.contains("cancelled", ignoreCase = true) && !err.contains("отмен", ignoreCase = true)) {
                    _lastError.value = err
                }
                false
            }
        } catch (e: Exception) {
            _lastError.value = e.localizedMessage ?: "Ошибка авторизации через Google"
            false
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun resetPassword(email: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        if (!isValidEmail(cleanEmail)) {
            return@withContext Pair(false, "Введите корректный Email")
        }

        try {
            val url = "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=$firebaseApiKey"
            val body = JsonObject().apply {
                addProperty("requestType", "PASSWORD_RESET")
                addProperty("email", cleanEmail)
            }

            val request = Request.Builder()
                .url(url)
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return@withContext Pair(false, parseFirebaseError(responseBody))
            }

            Pair(true, "Ссылка для сброса пароля отправлена на $cleanEmail")
        } catch (e: Exception) {
            Pair(false, "Ошибка сети: ${e.localizedMessage ?: "Попробуйте позже"}")
        }
    }

    fun signOut() {
        val guest = UserProfile(
            id = "guest_${UUID.randomUUID().toString().take(8)}",
            isAnonymous = true,
            provider = "anonymous"
        )
        saveUser(guest)
        _lastError.value = null
    }

    suspend fun ensureFreshToken(force: Boolean = false): String? = withContext(Dispatchers.IO) {
        val user = _currentUser.value
        if (user.isAnonymous || user.refreshToken.isNullOrEmpty()) {
            return@withContext user.idToken
        }

        val now = System.currentTimeMillis()
        if (!force && !user.idToken.isNullOrEmpty() && (now - tokenLastRefreshedAt) < tokenRefreshCooldownMs) {
            return@withContext user.idToken
        }

        try {
            val url = "https://securetoken.googleapis.com/v1/token?key=$firebaseApiKey"
            val formBody = "grant_type=refresh_token&refresh_token=${user.refreshToken}"

            val request = Request.Builder()
                .url(url)
                .post(formBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) return@withContext user.idToken

            val json = JsonParser.parseString(responseBody).asJsonObject
            val newIdToken = json.get("id_token")?.asString ?: return@withContext user.idToken
            val newRefreshToken = json.get("refresh_token")?.asString ?: user.refreshToken

            tokenLastRefreshedAt = System.currentTimeMillis()
            val updatedUser = user.copy(
                idToken = newIdToken,
                refreshToken = newRefreshToken
            )
            saveUser(updatedUser)
            newIdToken
        } catch (_: Exception) {
            user.idToken
        }
    }

    private suspend fun updateDisplayNameInternal(idToken: String, name: String) = withContext(Dispatchers.IO) {
        try {
            val url = "https://identitytoolkit.googleapis.com/v1/accounts:update?key=$firebaseApiKey"
            val body = JsonObject().apply {
                addProperty("idToken", idToken)
                addProperty("displayName", name)
                addProperty("returnSecureToken", false)
            }

            val request = Request.Builder()
                .url(url)
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute()
        } catch (_: Exception) {}
    }

    private fun parseFirebaseError(jsonString: String): String {
        return try {
            val json = JsonParser.parseString(jsonString).asJsonObject
            val error = json.getAsJsonObject("error")
            val message = error?.get("message")?.asString ?: ""

            when {
                message.contains("EMAIL_EXISTS") -> "Этот Email уже зарегистрирован. Войдите в аккаунт."
                message.contains("INVALID_PASSWORD") || message.contains("INVALID_LOGIN_CREDENTIALS") || message.contains("INVALID_EMAIL") -> "Неверный Email или пароль"
                message.contains("EMAIL_NOT_FOUND") -> "Пользователь с таким Email не найден"
                message.contains("USER_DISABLED") -> "Учётная запись заблокирована"
                message.contains("TOO_MANY_ATTEMPTS_TRY_LATER") -> "Слишком много попыток. Попробуйте позже."
                message.contains("WEAK_PASSWORD") -> "Пароль слишком слабый (минимум 6 символов)"
                message.contains("OPERATION_NOT_ALLOWED") -> "Вход по Email/паролю не включён в Firebase Console"
                message.contains("USER_NOT_FOUND") -> "Пользователь не найден"
                else -> if (message.isNotEmpty()) message else "Произошла ошибка. Попробуйте ещё раз."
            }
        } catch (_: Exception) {
            "Произошла ошибка. Попробуйте ещё раз."
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val pattern = Pattern.compile("^[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}$")
        return pattern.matcher(email).matches()
    }

    companion object {
        @Volatile
        private var INSTANCE: AuthRepository? = null

        fun getInstance(context: Context): AuthRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
