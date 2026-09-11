package com.example.cmdprompter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.example.cmdprompter.data.model.CommandGroup
import com.example.cmdprompter.ui.theme.GroupBg
import com.example.cmdprompter.ui.theme.GroupTagBg

/**
 * 命令组折叠头（仅右视图）。
 */
@Composable
fun GroupHeader(
    group: CommandGroup,
    collapsed: Boolean,
    commandCount: Int,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(GroupBg)
            .clickable { onToggle() }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (collapsed) "▶" else "▼",
            fontSize = 9.sp,
            color = Color(0xFF666666),
            modifier = Modifier.width(12.dp)
        )
        Text(
            text = group.name,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF222222)
        )
        Spacer(Modifier.width(6.dp))
        group.tags.forEach { tag ->
            TagChip(text = tag, background = GroupTagBg)
            Spacer(Modifier.width(4.dp))
        }
        Spacer(Modifier.weight(1f))
        Text(text = "$commandCount 条命令", fontSize = 10.sp, color = Color(0xFF888888))
    }
}
