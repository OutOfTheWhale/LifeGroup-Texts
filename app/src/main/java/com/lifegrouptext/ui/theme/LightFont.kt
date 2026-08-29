package com.lifegrouptext.ui.theme

import android.graphics.fonts.SystemFonts
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * The Light Phone III ships with **Akkurat** as a system font, which is what gives
 * LightOS its distinctive look. We use it when present so text matches the device
 * 1:1; on any other device (emulator, phones) it falls back to the platform default.
 *
 * Akkurat is a commercial typeface, so it is intentionally NOT bundled — we only reuse
 * the copy already installed on the LP3. Approach ported from the MIT-licensed Light
 * SDK (:sdk:ui, LightFont.kt).
 */
fun lightFontFamily(): FontFamily {
    val akkurat = SystemFonts.getAvailableFonts()
        .filter { it.file?.name?.startsWith("Akkurat", ignoreCase = true) == true }
        .mapNotNull { font ->
            val file = font.file ?: return@mapNotNull null
            Font(
                file = file,
                weight = FontWeight(font.style.weight),
                style = if (font.style.slant != 0) FontStyle.Italic else FontStyle.Normal,
            )
        }
    return if (akkurat.isNotEmpty()) FontFamily(akkurat) else FontFamily.Default
}
