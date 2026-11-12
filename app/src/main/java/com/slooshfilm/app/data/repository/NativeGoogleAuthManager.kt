package com.slooshfilm.app.data.repository

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.slooshfilm.app.data.models.auth.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * 100% Native Google Play Services Authentication for Android.
 * Presents the native system Google Account picker bottom sheet inside the app (Zero browser).
 */
class NativeGoogleAuthManager private constructor(private val context: Context) {

    // Web Client ID extracted directly from google-services.json (client_type: 3)
    val webClientId = "535135371915-n2sr91it0ramcgv3078m8asvrf4dn0ri.apps.googleusercontent.com"
    private val firebaseApiKey = "AIzaSyAQussJy2N2Nfa6dwl7GuWJ7jAK7fI3Upc"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun getSignInIntent(): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .requestProfile()
            .build()

        val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)
        // Clear previous selection so account chooser always prompts if needed
        try {
            googleSignInClient.signOut()
        } catch (e: Exception) {}
        return googleSignInClient.signInIntent
    }

    suspend fun handleSignInResult(data: Intent?): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)

            val idToken = account.idToken
                ?: return@withContext Result.failure(Exception("Google не предоставил токен авторизации"))

            val user = signInWithFirebaseIdToken(
                googleIdToken = idToken,
                fallbackDisplayName = account.displayName,
                fallbackEmail = account.email,
                fallbackPhotoUrl = account.photoUrl?.toString()
            )

            Result.success(user)
        } catch (e: ApiException) {
            val statusCode = e.statusCode
            if (statusCode == CommonStatusCodes.SIGN_IN_REQUIRED || statusCode == 12501 || statusCode == 4) {
                Result.failure(Exception("Вход отменён"))
            } else if (statusCode == 10) {
                val sha1 = getAppCertificateSha1()
                Result.failure(Exception("Ошибка Google (код 10): отпечаток SHA-1 ($sha1) не зарегистрирован в Firebase Console"))
            } else if (statusCode == 12500) {
                val sha1 = getAppCertificateSha1()
                Result.failure(Exception("Ошибка Google (код 12500): сбой авторизации. Проверьте SHA-1 ($sha1) в Firebase Console"))
            } else if (statusCode == 7) {
                Result.failure(Exception("Ошибка сети Google Play Services (код 7). Проверьте интернет"))
            } else {
                Result.failure(Exception("Ошибка Google Play Services: $statusCode (${e.statusMessage ?: e.localizedMessage})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun signInWithFirebaseIdToken(
        googleIdToken: String,
        fallbackDisplayName: String?,
        fallbackEmail: String?,
        fallbackPhotoUrl: String?
    ): UserProfile = withContext(Dispatchers.IO) {
        val url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=$firebaseApiKey"
        val postBody = "id_token=$googleIdToken&providerId=google.com"

        val body = JsonObject().apply {
            addProperty("postBody", postBody)
            addProperty("requestUri", "http://localhost")
            addProperty("returnIdpCredential", true)
            addProperty("returnSecureToken", true)
        }

        val request = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            val errorJson = try { JsonParser.parseString(responseBody).asJsonObject } catch (_: Exception) { null }
            val errorMsg = errorJson?.getAsJsonObject("error")?.get("message")?.asString ?: responseBody
            throw Exception("Ошибка авторизации Firebase: $errorMsg")
        }

        val json = JsonParser.parseString(responseBody).asJsonObject
        val firebaseIdToken = json.get("idToken")?.asString
            ?: throw Exception("Firebase не вернул idToken")
        val refreshToken = json.get("refreshToken")?.asString
            ?: throw Exception("Firebase не вернул refreshToken")
        val localId = json.get("localId")?.asString
            ?: throw Exception("Firebase не вернул localId")

        val email = json.get("email")?.asString ?: fallbackEmail
        val displayName = json.get("displayName")?.asString ?: fallbackDisplayName
        val photoUrl = json.get("photoUrl")?.asString ?: fallbackPhotoUrl

        UserProfile(
            id = localId,
            email = email,
            displayName = displayName,
            photoURL = photoUrl,
            isAnonymous = false,
            provider = "google",
            idToken = firebaseIdToken,
            refreshToken = refreshToken
        )
    }

    private fun getAppCertificateSha1(): String {
        return try {
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.GET_SIGNATURES
                )
            }
            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            val cert = signatures?.firstOrNull()?.toByteArray() ?: return "не определён"
            val md = java.security.MessageDigest.getInstance("SHA-1")
            val digest = md.digest(cert)
            digest.joinToString(":") { "%02X".format(it) }
        } catch (e: Exception) {
            "не определён"
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: NativeGoogleAuthManager? = null

        fun getInstance(context: Context): NativeGoogleAuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NativeGoogleAuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
