package com.baszincir.satis.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

val BzNavy = Color(0xFF1E2A78)
val BzRed = Color(0xFFD32F2F)
val BzBackground = Color(0xFFF5F6FA)

private val colorScheme = lightColorScheme(
    primary = BzNavy,
    secondary = BzRed,
    background = BzBackground,
    surface = Color.White
)

private val typography = Typography(
    bodyLarge = TextStyle(fontSize = 16.sp),
    titleLarge = TextStyle(fontSize = 22.sp)
)

@Composable
fun BasZincirTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
