package com.ultron.ai.ui.theme

import androidx.compose.ui.graphics.Color

// Refined: added surface scale, agentAccentSoft/Ghost, glowRed

/**
 * ULTRON AI - Cinematic dark-metallic HUD and neural core design system.
 *
 * Strict dark-mode palette anchored by deep obsidian void (#050505),
 * dark metallic gunmetal (#101216), Ultron Red (#FF3030), and Crimson (#A50000).
 */

// --- Primary & Secondary Energy Accents -----------------------------------
val UltronRed = Color(0xFFFF3030)
val ArcCyan = UltronRed

val CrimsonRed = Color(0xFFA50000)
val ArcCyanDim = CrimsonRed
val Crimson = CrimsonRed

val UltronRedHot = Color(0xFFFFDAD6)
val ArcCyanHot = UltronRedHot

val ArcAmber = Color(0xFFF59E0B)
val SuccessGreen = Color(0xFF22C55E)

// Sub-Agent Accents
val JarvisBlue = Color(0xFF3B82F6)
val FridayAmber = Color(0xFFF59E0B)
val EdithGreen = Color(0xFF22C55E)
val RouterGrey = Color(0xFF929292)

// --- 5-Step Surface Scale for Layering ------------------------------------
val SurfaceDim = Color(0xFF020202)
val Surface = Color(0xFF050505)
val SurfaceBright = Color(0xFF0B0C0F)
val SurfaceContainer = Color(0xFF101216)
val SurfaceContainerHigh = Color(0xFF181B22)
val SurfaceContainerHighest = Color(0xFF222630)

val BackgroundDark = Surface
val SurfaceContainerLow = SurfaceBright
val SurfaceDark = SurfaceContainer
val SurfaceVariant = SurfaceContainerHigh

// --- Structural Outlines & Framing ----------------------------------------
val OutlineDim = Color(0xFF2A2F3A)
val Outline = OutlineDim
val OutlineVariant = Color(0xFF1A1D24)

// --- Typography & Readouts ------------------------------------------------
val OnBackground = Color(0xFFFFFFFF)
val OnSurface = Color(0xFFFFFFFF)
val OnSurfaceMuted = Color(0xFF929292)
val MetallicGrey = OnSurfaceMuted
val OnPrimary = Color(0xFF050505)

val ErrorColor = Color(0xFFFF3030)

// --- Glow & Atmospheric Tokens --------------------------------------------
val GlowRed = UltronRed.copy(alpha = 0.30f)

fun agentAccentColor(agentName: String): Color = when (agentName.uppercase()) {
    "JARVIS" -> JarvisBlue
    "FRIDAY" -> FridayAmber
    "EDITH" -> EdithGreen
    else -> UltronRed
}

fun agentAccentSoft(accent: Color): Color = accent.copy(alpha = 0.20f)
fun agentAccentGhost(accent: Color): Color = accent.copy(alpha = 0.08f)

// --- Chat & Bubble Tokens -------------------------------------------------
val UserBubble = Color(0xFF3B0808)
val AssistantBubble = Color(0xFF101216)

val PrimaryDark = UltronRed
val PrimaryVariant = UltronRedHot
val SecondaryDark = ArcAmber

