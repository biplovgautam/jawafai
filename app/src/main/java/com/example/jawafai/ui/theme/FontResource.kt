package com.example.jawafai.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.jawafai.R

/**
 * Centralized font definitions for the app to avoid conflicting declarations
 */
object AppFonts {
    // KaiseiDecol font family used throughout the app
    val KaiseiDecolFontFamily = FontFamily(
        Font(R.font.kaiseidecol_regular, FontWeight.Normal),
        Font(R.font.kaiseidecol_bold, FontWeight.Bold),
        Font(R.font.kaiseidecol_medium, FontWeight.Medium)
    )

    // Regular font family (alias for consistency)
    val KaiseiRegularFontFamily = FontFamily(
        Font(R.font.kaiseidecol_regular, FontWeight.Normal)
    )
}
