package com.example.cmdprompter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cmdprompter.ui.theme.PlatformBg

/**
 * 平台折叠头。
 */
@Composable
fun PlatformHeader(
    name: String,
    collapsed: Boolean,
    countLabel: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(PlatformBg)
            .clickable { onToggle() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (collapsed) "▶" else "▼",
            fontSize = 9.sp,
            color = Color(0xFF666666),
            modifier = Modifier.width(12.dp)
        )
        Text(
            text = name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF222222),
            modifier = Modifier.weight(1f)
        )
        Text(text = countLabel, fontSize = 10.sp, color = Color(0xFF888888))
    }
}
