package com.ultron.ai.ui.overlay

/**
 * The overlay is drawn with android.graphics, not Compose, so it needs plain
 * ARGB ints. These are the same values as the Compose palette in
 * `ui.theme.Color` — keep the two in step.
 */
object OverlayPalette {
    /** 94% opaque: the app underneath stays visible, the panel still reads as solid. */
    const val PANEL = 0xF0140507.toInt()
    const val PANEL_EDGE = 0x44FF2233
    const val CYAN = 0xFFFF2233.toInt()
    const val CYAN_HOT = 0xFFFF858F.toInt()
    const val AMBER = 0xFFFF6A00.toInt()
    const val TEXT = 0xFFFBE4E6.toInt()
    const val TEXT_MUTED = 0xFFA66F76.toInt()
}
