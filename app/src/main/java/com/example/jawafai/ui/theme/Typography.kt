package com.example.jawafai.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.jawafai.R
import com.example.jawafai.view.KaiseiDecolFontFamily

// Define your custom font family
val KaiseiDecolFontFamily = FontFamily(
    Font(R.font.kaiseidecol_regular, FontWeight.Normal),
    Font(R.font.kaiseidecol_bold, FontWeight.Bold),
    Font(R.font.kaiseidecol_medium, FontWeight.Bold)

)

// Apply custom font across app
val AppTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = KaiseiDecolFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = KaiseiDecolFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp
    ),
    labelLarge = TextStyle(
        fontFamily = KaiseiDecolFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    )
)
