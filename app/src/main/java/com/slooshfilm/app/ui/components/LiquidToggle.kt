package com.slooshfilm.app.ui.components

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable

/**
 * Minimal replacement for the removed LiquidToggle component.
 * Keeps the same public shape (selected/onSelect) but uses a plain Switch so the UI remains stable.
 */
@Composable
fun LiquidToggle(selected: () -> Boolean, onSelect: (Boolean) -> Unit, backdrop: Any? = null) {
    // Use a standard Switch as a drop-in replacement.
    Switch(checked = selected(), onCheckedChange = { checked -> onSelect(checked) })
}
