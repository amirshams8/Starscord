package com.nexus.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NexusDarkColorScheme = darkColorScheme(
    primary         = NexusBlurple,
    onPrimary       = NexusTextPrimary,
    secondary       = NexusGreen,
    tertiary        = NexusYellow,
    background      = NexusDark,
    surface         = NexusDarkMedium,
    surfaceVariant  = NexusDarkLight,
    onBackground    = NexusTextPrimary,
    onSurface       = NexusTextPrimary,
    error           = NexusRed,
    outline         = NexusOutline,
)

@Composable
fun NexusTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NexusDarkColorScheme, typography = NexusTypography, content = content)
}
