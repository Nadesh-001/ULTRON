package com.ultron.ai.ui.theme

// Refined: provided LocalGlowStrength & LocalReduceMotion, layered vertical gradient root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush

val LocalGlowStrength = compositionLocalOf { 1f }
val LocalReduceMotion = compositionLocalOf { false }

private val ULTRONDarkScheme = darkColorScheme(
    primary = UltronRed,
    onPrimary = OnPrimary,
    primaryContainer = CrimsonRed,
    onPrimaryContainer = UltronRedHot,
    secondary = ArcAmber,
    onSecondary = OnPrimary,
    tertiary = UltronRedHot,
    onTertiary = OnPrimary,
    background = Surface,
    onBackground = OnBackground,
    surface = SurfaceContainer,
    onSurface = OnSurface,
    surfaceVariant = SurfaceContainerHigh,
    onSurfaceVariant = OnSurfaceMuted,
    outline = OutlineDim,
    outlineVariant = OutlineVariant,
    error = ErrorColor,
    onError = OnPrimary
)

@Composable
fun ULTRONTheme(
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    val glow = if (reduceMotion) 0f else 1f

    CompositionLocalProvider(
        LocalGlowStrength provides glow,
        LocalReduceMotion provides reduceMotion
    ) {
        MaterialTheme(
            colorScheme = ULTRONDarkScheme,
            typography = ULTRONTypography
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Surface, SurfaceDim)))
            ) {
                content()
            }
        }
    }
}
