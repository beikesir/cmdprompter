package com.example.cmdprompter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cmdprompter.ui.theme.PlatformAccent
import com.example.cmdprompter.ui.theme.PlatformBg
import com.example.cmdprompter.ui.theme.PlatformBorder

/**
 * 一级层级：平台头。
 * 通栏灰底 + 左侧蓝色竖条 + 上下分隔线，与下面的组 / 命令卡片形成强对比。
 */
@Composable
fun PlatformHeader(
    name: String,
    collapsed: Boolean,
    countLabel: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(PlatformBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(PlatformBorder)
                .align(Alignment.TopCenter)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clickable { onToggle() }
                .padding(start = 8.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(3.dp)
                    .padding(vertical = 6.dp)
                    .background(PlatformAccent)
            )
            Text(
                text = if (collapsed) "▶" else "▼",
                fontSize = 8.sp,
                color = Color(0xFF5A6B85),
                modifier = Modifier.padding(horizontal = 5.dp)
            )
            Text(
                text = name,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1B2733),
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            Text(
                text = countLabel,
                fontSize = 9.sp,
                lineHeight = 12.sp,
                color = Color(0xFF6B7A8D),
                modifier = Modifier
                    .background(Color(0xFFE2E7EE), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(PlatformBorder)
                .align(Alignment.BottomCenter)
        )
    }
}
