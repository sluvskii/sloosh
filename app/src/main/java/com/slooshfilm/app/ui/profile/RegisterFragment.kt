package com.slooshfilm.app.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.slooshfilm.app.data.LocalStorage
import com.slooshfilm.app.ui.theme.SlooshTheme

class RegisterFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val localStorage = LocalStorage(requireContext())
                val currentThemeMode = localStorage.getThemeMode()
                // dynamic color removed
                val isSystemDark = isSystemInDarkTheme()

                val useDarkTheme = when (currentThemeMode) {
                    AppCompatDelegate.MODE_NIGHT_YES, LocalStorage.THEME_MODE_AMOLED -> true
                    AppCompatDelegate.MODE_NIGHT_NO -> false
                    else -> isSystemDark
                }

                SlooshTheme(
                    darkTheme = useDarkTheme,
                    // dynamicColor removed
                    isAmoled = currentThemeMode == LocalStorage.THEME_MODE_AMOLED
                ) {
                    RegisterScreen(onBackClick = { findNavController().popBackStack() })
                }
            }
        }
    }
}

@Composable
fun RegisterScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val brandColor = Color(0xFFCEDC00)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // TopBar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Text(
                text = "Регистрация",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Info Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = brandColor,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "В данный момент регистрация через приложение недоступна. Используйте email-бота для создания аккаунта.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            StepItem(
                number = "1",
                title = "Запрос регистрации",
                description = "Отправьте пустое письмо на почту для получения данных аккаунта.",
                actionTitle = "Написать письмо",
                icon = Icons.Default.Email,
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:mirror@hdrezka.org")
                        putExtra(Intent.EXTRA_SUBJECT, "Registration")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // handle error
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            StepItem(
                number = "2",
                title = "Восстановление пароля",
                description = "Если данные не пришли, попробуйте восстановить пароль.",
                actionTitle = "Восстановить",
                icon = Icons.Default.Email,
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:pswd@hdrezka.org")
                        putExtra(Intent.EXTRA_SUBJECT, "Password Recovery")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // handle error
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Уже есть данные?",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onBackClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = brandColor,
                    contentColor = Color.Black
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Login,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Войти в аккаунт",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StepItem(
    number: String,
    title: String,
    description: String,
    actionTitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Number circle
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onClick,
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = actionTitle)
            }
        }
    }
}
