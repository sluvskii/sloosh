package com.slooshfilm.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
// Use drawable resource for the forward arrow to avoid icon package compatibility issues
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slooshfilm.app.R

@Composable
fun TopBar(title: String, onBackClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        onBackClick?.let {
            IconButton(onClick = it, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "Назад",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, shape: Shape = RoundedCornerShape(30.dp), content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            content()
        }
    }
}

@Composable
fun SettingsRadioItem(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = LocalIndication.current,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        AnimatedVisibility(
            visible = selected,
            enter = slideInHorizontally(initialOffsetX = { it / 2 }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it / 2 }) + fadeOut()
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_check_circle),
                contentDescription = "Выбрано",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SettingsNavigationItem(text: String, icon: Painter, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = LocalIndication.current,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Icon(
            painter = painterResource(id = R.drawable.ic_arrow_forward),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isFirst: Boolean = false,
    isLast: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = LocalIndication.current,
                interactionSource = remember { MutableInteractionSource() }
            ) { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 4.dp), // Reduced vertical padding further
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
