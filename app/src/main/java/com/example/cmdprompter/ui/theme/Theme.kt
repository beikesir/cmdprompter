package com.example.cmdprompter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Blue007AFF,
    onPrimary = Color.White,
    secondary = Color(0xFF5856D6),
    background = Color.White,
    surface = Color.White,
    onSurface = TextDark
)

@Composable
fun CmdPrompterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // 1.0 固定使用浅色工具型配色，保证与文档规格一致
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography(),
        content = content
    )
}
