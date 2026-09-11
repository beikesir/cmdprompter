package com.example.cmdprompter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cmdprompter.data.model.Command
import com.example.cmdprompter.ui.theme.Blue007AFF
import com.example.cmdprompter.ui.theme.BorderLight
import com.example.cmdprompter.ui.theme.SelectedBg
import com.example.cmdprompter.ui.theme.TagBg
import com.example.cmdprompter.ui.theme.TextGray

/**
 * 单条命令项：两行紧凑布局
 * 第一行 = 命令名，第二行 = 描述 + 标签（标签右对齐，最多 2 个）。
 * 操作按钮阻止冒泡，不触发选中。
 */
@Composable
fun CommandItem(
    cmd: Command,
    selected: Boolean,
    indentDp: Int = 0,
    onSelect: () -> Unit,
    onDocClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(if (selected) SelectedBg else Color.White)
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier.heightIn(min = 42.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 选中态左侧竖条
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(3.dp)
                    .background(if (selected) Blue007AFF else Color.Transparent)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = (8 + indentDp).dp, end = 2.dp, top = 4.dp, bottom = 4.dp)
            ) {
                // 第 1 行：命令名
                Text(
                    text = cmd.name,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222222),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // 第 2 行：描述 + 标签
                if (cmd.desc.isNotBlank() || cmd.tags.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 1.dp)
                    ) {
                        if (cmd.desc.isNotBlank()) {
                            Text(
                                text = cmd.desc,
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                color = TextGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                        Spacer(Modifier.width(6.dp))
                        cmd.tags.take(2).forEach { tag ->
                            TagChip(text = tag, background = TagBg)
                            Spacer(Modifier.width(4.dp))
                        }
                    }
                }
            }

            // 操作区
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clickable { onDocClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "\uD83D\uDCC4", fontSize = 13.sp)
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clickable { onDeleteClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "\uD83D\uDDD1\uFE0F", fontSize = 13.sp)
            }
        }

        // 分隔线（只画底部，避免相邻条目出现双线）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(BorderLight)
        )
    }
}

@Composable
fun TagChip(
    text: String,
    background: Color = TagBg,
    textColor: Color = Color(0xFF55617A)
) {
    Text(
        text = text,
        fontSize = 8.sp,
        lineHeight = 10.sp,
        color = textColor,
        maxLines = 1,
        modifier = Modifier
            .background(background, RoundedCornerShape(8.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    )
}
