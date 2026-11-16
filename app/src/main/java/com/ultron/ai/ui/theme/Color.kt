package com.ultron.ai.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Ultron Dark Red palette: commanding dark crimson and obsidian aesthetic.
 *
 * Crimson / deep red is emitted from active elements, HUD rings, reactor cores,
 * and highlights against a deep obsidian dark-red background.
 */

// --- The light (Dark Red / Crimson Glow) -----------------------------------
/** The core Ultron energy red. */
val ArcCyan = Color(0xFFFF2233)

/** Hot centre of the glow — used sparingly, at the middle of a bloom. */
val ArcCyanHot = Color(0xFFFF858F)

/** Red pushed back into the dark: rings, hairlines, inactive rails. */
val ArcCyanDim = Color(0xFF7A101A)

/** Stark amber/orange. The only other hue, for "working" / caution. */
val ArcAmber = Color(0xFFFF6A00)

// --- The room (Obsidian / Deep Dark Red) -----------------------------------
val BackgroundDark = Color(0xFF090203)   // near-black with deep crimson undertone
val SurfaceDark = Color(0xFF140507)      // panels
val SurfaceVariant = Color(0xFF220A0E)   // raised panels, input fields
val OutlineDim = Color(0xFF3D131A)       // hairline structure

// --- Text -----------------------------------------------------------------
val OnBackground = Color(0xFFFBE4E6)     // pale warm white
val OnSurface = Color(0xFFE8CBCE)
val OnSurfaceMuted = Color(0xFFA66F76)   // readable on SurfaceDark
val OnPrimary = Color(0xFF1A0003)        // dark ink on lit red surface

val ErrorColor = Color(0xFFFF453A)

// --- Chat -----------------------------------------------------------------
/** The user speaks from a lit crimson surface; Ultron answers from the dark. */
val UserBubble = Color(0xFF4D0C14)
val AssistantBubble = Color(0xFF160608)

// Kept so older call sites still compile
val PrimaryDark = ArcCyan
val PrimaryVariant = ArcCyanHot
val SecondaryDark = ArcAmber
