package com.example.cmdprompter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cmdprompter.data.model.CommandGroup
import com.example.cmdprompter.data.model.Origin
import com.example.cmdprompter.ui.theme.GroupBg
import com.example.cmdprompter.ui.theme.GroupBorder
import com.example.cmdprompter.ui.theme.GroupChipBg
import com.example.cmdprompter.ui.theme.GroupChipFg
import com.example.cmdprompter.ui.theme.GroupTagBg

/**
 * 二级层级：命令组卡片（可折叠）。
 * 蓝底描边圆角卡片 + "组"标识 chip，视觉上明显区别于内部的白色命令行。
 *
 * @param commands 展开时渲染的组内命令，由调用方传入
 */
@Composable
fun GroupCard(
    group: CommandGroup,
    collapsed: Boolean,
    commandCount: Int,
    onToggle: () -> Unit,
    onDocClick: () -> Unit,
    modifier: Modifier = Modifier,
    commands: @Composable () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .background(GroupBg, RoundedCornerShape(10.dp))
            .border(1.dp, GroupBorder, RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(start = 8.dp, end = 2.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (collapsed) "▶" else "▼",
                fontSize = 8.sp,
                color = Color(0xFF5A6B85),
                modifier = Modifier.width(12.dp)
            )
            // 层级标识
            Text(
                text = "组",
                fontSize = 8.sp,
                lineHeight = 10.sp,
                color = GroupChipFg,
                modifier = Modifier
                    .background(GroupChipBg, RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = group.name,
                fontSize = 12.5f.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF173B6E),
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            group.tags.take(2).forEach { tag ->
                TagChip(text = tag, background = GroupTagBg)
                Spacer(Modifier.width(4.dp))
            }
            if (group.origin == Origin.USER || group.origin == Origin.EDITED) {
                Spacer(Modifier.width(4.dp))
                OriginMark(origin = group.origin)
            }
            Text(
                text = "$commandCount 条",
                fontSize = 9.sp,
                color = Color(0xFF6B7A8D)
            )
            Spacer(Modifier.width(2.dp))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clickable { onDocClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "\uD83D\uDCC4", fontSize = 12.sp)
            }
        }

        if (!collapsed && commandCount > 0) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 5.dp, vertical = 2.dp)
                    .background(Color.White, RoundedCornerShape(8.dp))
            ) {
                commands()
            }
            Spacer(Modifier.height(5.dp))
        }
    }
}
