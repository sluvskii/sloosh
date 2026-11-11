package com.slooshfilm.app.ui.pairing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slooshfilm.app.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private val PairingBackground = Color(0xE60A0F0D)
private val AccentGreen = Color(0xFF34D399)
private val TextPrimary = Color(0xFFF2F5F3)
private val TextSecondary = Color(0xFF9BA8A0)
private val FieldBackground = Color(0xFF15201B)

/**
 * Подтверждение привязки ТВ-устройства: код с экрана телевизора (из QR deep link
 * или введённый вручную). Читает pending-узел, шифрует refresh-токен пользователя
 * AES-GCM ключом от кода+nonce и публикует грант. Токен в открытом виде нигде
 * не появляется — расшифровать грант может только ТВ, знающее код.
 */
@Composable
fun PairConfirmScreen(
    initialCode: String,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepo = remember { AuthRepository.getInstance(context) }

    var code by remember { mutableStateOf(initialCode.uppercase().replace(" ", "")) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PairingBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                // На планшете и в ландшафте карточка не растягивается на всю ширину экрана.
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(FieldBackground, RoundedCornerShape(20.dp))
                // Телефон в горизонтали: карточка (~330dp) не помещается в доступную высоту
                // (~310dp), поэтому контент скроллится внутри неё. clip и background стоят до
                // verticalScroll — фон и скругление остаются неподвижными, скроллится только текст.
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (success) "ТВ привязан!" else "Подтвердите вход на ТВ",
                color = if (success) AccentGreen else TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            if (!success) {
                Text(
                    text = "На телевизоре открыт код привязки. Разрешив вход, вы " +
                        "подключите этот аккаунт к ТВ-приложению sloosh.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
            }

            if (!success && initialCode.isBlank()) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(8) },
                    label = { Text("Код с экрана ТВ", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (!success && code.isNotBlank()) {
                Text(
                    text = "Код: ${code.chunked(4).joinToString("-")}",
                    color = AccentGreen,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp
                )
            }

            message?.let {
                Text(
                    text = it,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            if (busy) {
                CircularProgressIndicator(color = AccentGreen, modifier = Modifier.height(32.dp))
            }

            if (!success && !busy) {
                Button(
                    onClick = {
                        busy = true
                        message = null
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                runCatching { grantPairing(context, code) }
                                    .getOrElse { "Ошибка сети: ${it.message}" }
                            }
                            busy = false
                            if (result == null) {
                                success = true
                            } else {
                                message = result
                            }
                        }
                    },
                    enabled = code.length >= 8,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Разрешить вход", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Отмена", color = TextSecondary)
                }
            }

            if (success) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Готово", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** null = успех, иначе текст ошибки. */
private fun grantPairing(context: android.content.Context, rawCodeInput: String): String? {
    val code = rawCodeInput.uppercase().replace("[^A-Z0-9]".toRegex(), "")
    if (code.length != 8) return "Код должен быть из 8 символов"

    val authRepo = AuthRepository.getInstance(context)
    val user = authRepo.currentUser.value
    if (user == null || user.isAnonymous) return "Сначала войдите в аккаунт в приложении"
    val refreshToken = user.refreshToken?.takeIf { it.isNotBlank() } ?: return "Не найден токен аккаунта — перезапустите приложение"
    val uid = user.id

    val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    val jsonType = "application/json".toMediaType()
    val hash = sha256(code)
    val nodeUrl = "https://sloosh-77434-default-rtdb.firebaseio.com/media_stats/pairing/$hash.json"

    // 1. Читаем узел (публичное чтение media_stats): должен быть свежий pending с nonce
    val readReq = Request.Builder().url(nodeUrl).get().build()
    val node: JSONObject = http.newCall(readReq).execute().use { resp ->
        val body = resp.body?.string()?.trim() ?: return "Узел привязки не найден"
        if (body == "null" || body.isEmpty()) return "Код не найден или истёк. Проверьте код на ТВ"
        JSONObject(body)
    }
    val nonce = node.optString("nonce", "").takeIf { it.isNotBlank() } ?: return "Некорректный узел привязки"
    val expiresAt = node.optLong("expiresAt", 0L)
    if (expiresAt <= 0 || System.currentTimeMillis() > expiresAt) return "Код истёк — обновите его на ТВ"
    if (node.optString("status") == "granted") return "Код уже использован"

    // 2. Шифруем refresh-токен ключом от кода+nonce и публикуем грант
    val ct = encryptGrant(refreshToken, code, nonce)
    val grant = JSONObject()
        .put("status", "granted")
        .put("ct", ct)
        .put("uid", uid)
        .put("grantedAt", System.currentTimeMillis())
    val patchReq = Request.Builder()
        .url(nodeUrl)
        .patch(grant.toString().toRequestBody(jsonType))
        .build()
    http.newCall(patchReq).execute().use { resp ->
        if (!resp.isSuccessful) return "Не удалось отправить подтверждение (${resp.code})"
    }
    return null
}

// Криптография — точная копия логики ТВ (PairingService): держим синхронно.
private fun sha256(value: String): String {
    return MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

private fun grantCipherKey(code: String, nonce: String): SecretKeySpec {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest("sloosh-pairing:$code:$nonce".toByteArray(Charsets.UTF_8))
    return SecretKeySpec(digest, "AES")
}

private fun encryptGrant(plainRefreshToken: String, code: String, nonce: String): String {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, grantCipherKey(code, nonce))
    val iv = cipher.iv
    val encrypted = cipher.doFinal(plainRefreshToken.toByteArray(Charsets.UTF_8))
    val out = ByteArray(iv.size + encrypted.size)
    System.arraycopy(iv, 0, out, 0, iv.size)
    System.arraycopy(encrypted, 0, out, iv.size, encrypted.size)
    return android.util.Base64.encodeToString(out, android.util.Base64.NO_WRAP)
}
