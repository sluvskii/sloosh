package com.slooshfilm.app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.slooshfilm.app.R
import com.slooshfilm.app.SlooshApplication
import com.slooshfilm.app.data.LocalStorage
import com.slooshfilm.app.ui.theme.SlooshTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private val viewModel: ProfileViewModel by activityViewModels { (requireActivity().application as SlooshApplication).viewModelFactory }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        lifecycleScope.launch {
            viewModel.isLoggedIn.collectLatest {
                if (it) {
                    findNavController().popBackStack()
                }
            }
        }

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
                    isAmoled = currentThemeMode == LocalStorage.THEME_MODE_AMOLED
                ) {
                    val error by viewModel.error.collectAsState()
                    val isLoading by viewModel.isLoading.collectAsState()

                    if (error != null) {
                        // TODO: Show error in a more user-friendly way
                    }
                    
                    LoginScreen(
        isLoading = isLoading,
        onRegisterClick = { findNavController().navigate(R.id.action_loginFragment_to_registerFragment) },
        onLoginClick = { username, password -> viewModel.login(username, password) },
        onSettingsClick = { findNavController().navigate(R.id.settings_fragment) },
        onForgotPasswordClick = { findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment) }
    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    isLoading: Boolean,
    onRegisterClick: () -> Unit,
    onLoginClick: (String, String) -> Unit,
    onSettingsClick: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Brand colors kept consistent with profile screen
    val brandColor = Color(0xFFCEDC00)
    val darkYellowBg = Color(0xFF2E2E14)

    val scrollState = rememberScrollState()
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Используем TopBar без кнопки "назад"
        LoginTopBar(title = "Вход", onSettingsClick = onSettingsClick)

        // Center the auth block while keeping the TopBar at the top
        // Добавляем прокрутку и обработку клавиатуры
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .imePadding(), // Поднимает содержимое при появлении клавиатуры
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(scrollState), // Добавляем прокрутку для удобства
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Войдите в свой аккаунт",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // More strongly rounded fields
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = { Text("Имя пользователя или почта") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(40.dp),
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Пароль") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(40.dp),
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Забыли пароль?",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.End).clickable(
                        indication = LocalIndication.current,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onForgotPasswordClick
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Restore the branded narrow login button (centered)
                val brandColor = Color(0xFFCEDC00)
                val darkYellowBg = Color(0xFF2E2E14)

                Button(
                    onClick = { onLoginClick(username, password) },
                    modifier = Modifier.width(160.dp).height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = darkYellowBg, contentColor = brandColor),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = brandColor, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    } else {
                        Text(text = "Войти", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(painter = painterResource(id = R.drawable.ic_login), contentDescription = null)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row {
                    Text("Нет аккаунта?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Зарегистрируйтесь",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(
                            indication = LocalIndication.current,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onRegisterClick
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun LoginTopBar(title: String, onSettingsClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp) // Standard TopAppBar height
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.Center)
        )
        
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_settings),
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
