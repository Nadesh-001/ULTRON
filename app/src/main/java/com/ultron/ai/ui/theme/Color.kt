package com.ultron.ai.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * ULTRON AI - Cinematic dark-metallic HUD and neural core design system.
 *
 * Strict dark-mode palette anchored by deep obsidian void (#050505),
 * dark metallic gunmetal (#101216), Ultron Red (#FF3030), and Crimson (#A50000).
 */

// --- Primary & Secondary Energy Accents -----------------------------------
/** Ultron Red (#FF3030): AI core, active voice states, primary interactive triggers. */
val UltronRed = Color(0xFFFF3030)
val ArcCyan = UltronRed

/** Secondary deep crimson (#A50000): energy gradients, orbital rings, pressed states. */
val CrimsonRed = Color(0xFFA50000)
val ArcCyanDim = CrimsonRed

/** Hot neon core bloom / high-energy highlight. */
val UltronRedHot = Color(0xFFFFDAD6)
val ArcCyanHot = UltronRedHot

/** Tactical Amber/Orange: telemetry warnings and computing states. */
val ArcAmber = Color(0xFFF59E0B)

/** Success green for telemetry checks. */
val SuccessGreen = Color(0xFF22C55E)

// --- Obsidian Void & Metallic Gunmetal Surfaces ----------------------------
val BackgroundDark = Color(0xFF050505)       // Deep obsidian void canvas
val SurfaceContainerLow = Color(0xFF0B0C0F)  // Recessed panels
val SurfaceDark = Color(0xFF101216)          // Standard HUD containers & cards
val SurfaceVariant = Color(0xFF181B22)       // Elevated gunmetal surfaces
val SurfaceContainerHighest = Color(0xFF222630)

// --- Structural Outlines & Framing ----------------------------------------
val OutlineDim = Color(0xFF2A2F3A)           // Crisp hairline steel outline
val OutlineVariant = Color(0xFF1A1D24)

// --- Typography & Readouts ------------------------------------------------
val OnBackground = Color(0xFFFFFFFF)         // Crisp white for primary telemetry & titles
val OnSurface = Color(0xFFFFFFFF)
val OnSurfaceMuted = Color(0xFF929292)       // Metallic grey for secondary metadata
val OnPrimary = Color(0xFF050505)            // Obsidian ink on lit red button

val ErrorColor = Color(0xFFFF3030)

// --- Chat & Bubble Tokens -------------------------------------------------
val UserBubble = Color(0xFF3B0808)           // Primary dark container
val AssistantBubble = Color(0xFF101216)      // Surface container

// Kept for seamless backwards compatibility with existing UI components
val PrimaryDark = UltronRed
val PrimaryVariant = UltronRedHot
val SecondaryDark = ArcAmber

