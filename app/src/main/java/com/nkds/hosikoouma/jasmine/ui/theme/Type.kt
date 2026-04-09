package com.nkds.hosikoouma.jasmine.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nkds.hosikoouma.jasmine.R

// Google Sans
val GoogleSans = FontFamily(
    Font(R.font.gsr, FontWeight.Normal),
    Font(R.font.gsm, FontWeight.Medium),
    Font(R.font.gsb, FontWeight.Bold)
)

// JetBrains Mono Nerd
val JetBrainsMonoNerd = FontFamily(
    Font(R.font.jetbrains_nerd_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_nerd_mono_bold, FontWeight.Bold),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium)
)

fun getTypography(fontFamily: FontFamily): Typography {
    return Typography(
        headlineLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp
        ),
        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        ),
        labelMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp
        )
    )
}

// Default Typography
val Typography = getTypography(FontFamily.Default)
