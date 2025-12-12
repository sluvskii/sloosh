package com.slooshfilm.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.slooshfilm.app.R
import com.slooshfilm.app.data.LocalStorage
import com.slooshfilm.app.ui.theme.SlooshTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SettingsPlayerFragment : Fragment() {

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
                    isAmoled = currentThemeMode == LocalStorage.THEME_MODE_AMOLED
                ) {
                    SettingsPlayerScreen(
                        onBackClick = { findNavController().popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsPlayerScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val localStorage = remember { LocalStorage(context) }

    var seekForward by remember { mutableStateOf(localStorage.getSeekForwardInterval()) }
    var seekBackward by remember { mutableStateOf(localStorage.getSeekBackwardInterval()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        TopBar(title = "Плеер", onBackClick = onBackClick)
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(16.dp))

            SettingsSection(title = "Перемотка") {
                IntervalStepper(
                    label = "Вперед (сек)",
                    value = seekForward,
                    onValueChange = {
                        val newValue = (seekForward + it).coerceIn(5, 100)
                        localStorage.saveSeekForwardInterval(newValue)
                        seekForward = newValue
                    }
                )
                Divider(color = MaterialTheme.colorScheme.background, thickness = 1.dp)
                IntervalStepper(
                    label = "Назад (сек)",
                    value = seekBackward,
                    onValueChange = {
                        val newValue = (seekBackward + it).coerceIn(5, 100)
                        localStorage.saveSeekBackwardInterval(newValue)
                        seekBackward = newValue
                    }
                )
            }
        }
    }
}

@Composable
fun IntervalStepper(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepperButton(enabled = value > 5, onStep = { onValueChange(-5) }) {
                Icon(painter = painterResource(id = R.drawable.ic_remove), contentDescription = "Уменьшить")
            }
            Text(
                text = value.toString(),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            StepperButton(enabled = value < 100, onStep = { onValueChange(5) }) {
                Icon(painter = painterResource(id = R.drawable.ic_add), contentDescription = "Увеличить")
            }
        }
    }
}

@Composable
fun StepperButton(enabled: Boolean, onStep: () -> Unit, content: @Composable () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed && enabled) {
            delay(500) // Initial long press delay
            while (true) {
                onStep()
                delay(100) // Continuous step delay
            }
        }
    }

    val alpha by animateFloatAsState(targetValue = if (enabled) 1f else 0.4f, label = "alpha")
    val size by animateDpAsState(targetValue = if (enabled) 36.dp else 32.dp, label = "size")

    IconButton(
        onClick = { if (enabled) onStep() },
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = Modifier.size(size).alpha(alpha)
    ) {
        val contentColor = if(enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}
