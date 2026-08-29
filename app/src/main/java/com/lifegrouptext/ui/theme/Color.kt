package com.lifegrouptext.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette matching the Light Phone III's design system exactly (ported from the
 * MIT-licensed Light SDK :sdk:ui — LightThemeColors.Dark):
 *   background #000000, content #FFFFFF, contentSecondary #BBBBBB.
 * The remaining tokens (dividers / raised surfaces) are ours, kept subtle.
 */
val Ink = Color(0xFFFFFFFF)        // content — primary text / active
val Paper = Color(0xFF000000)      // background
val Graphite = Color(0xFFCFCFCF)   // strong secondary
val Slate = Color(0xFFBBBBBB)      // contentSecondary — muted text / inactive
val Ash = Color(0xFF6E6E6E)        // hints / placeholders
val Hairline = Color(0xFF262626)   // dividers / borders on black
val Mist = Color(0xFF141414)       // subtle raised surface
