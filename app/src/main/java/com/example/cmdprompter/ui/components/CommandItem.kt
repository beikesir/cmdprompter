package com.example.cmdprompter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.cmdprompter.data.model.Origin
import com.example.cmdprompter.ui.theme.Blue007AFF
import com.example.cmdprompter.ui.theme.CommandBg
import com.example.cmdprompter.ui.theme.CommandBorder
import com.example.cmdprompter.ui.theme.CommandSelectedBorder
import com.example.cmdprompter.ui.theme.DividerColor
import com.example.cmdprompter.ui.theme.MarkEditBg
import com.example.cmdprompter.ui.theme.MarkEditFg
import com.example.cmdprompter.ui.theme.MarkUserBg
import com.example.cmdprompter.ui.theme.MarkUserFg
import com.example.cmdprompter.ui.theme.SelectedBg
import com.example.cmdprompter.ui.theme.TagBg
import com.example.cmdprompter.ui.theme.TextGray

/**
 * 命令行。
 *
 * @param nested 是否位于命令组卡片内部：
 *               true  → 三级层级，贴在卡片内、无外边距、行尾细分隔线；
 *               false → 二级层级（命令视图），独立圆角卡片 + 外边距。
 */
@Composable
fun CommandItem(
    cmd: Command,
    selected: Boolean,
    nested: Boolean,
    onSelect: () -> Unit,
    onDocClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = if (nested) RoundedCornerShape(0.dp) else RoundedCornerShape(8.dp)
    val bg = if (selected) SelectedBg else CommandBg
    val border = if (selected) CommandSelectedBorder else CommandBorder

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (nested) Modifier else Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
            .background(bg, shape)
            .border(if (selected) 1.5.dp else 1.dp, border, shape)
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 42.dp)
                .padding(end = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 选中态左侧竖条
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(3.dp)
                    .background(if (selected) Blue007AFF else Color.Transparent)
            )
            // 左内边距（层级由「是否被命令组卡片包含」体现，不再用箭头）
            Spacer(Modifier.width(8.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 4.dp, bottom = 4.dp, end = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = cmd.name,
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF222222),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (cmd.isCustom) {
                        Spacer(Modifier.width(4.dp))
                        OriginMark(origin = cmd.origin)
                    }
                }
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

        if (nested) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DividerColor)
            )
        }
    }
}

/** 手动新增 / 修改条目的小标记 */
@Composable
fun OriginMark(origin: String) {
    val isUser = origin == Origin.USER
    Text(
        text = if (isUser) "自建" else "已改",
        fontSize = 8.sp,
        lineHeight = 10.sp,
        color = if (isUser) MarkUserFg else MarkEditFg,
        modifier = Modifier
            .background(if (isUser) MarkUserBg else MarkEditBg, RoundedCornerShape(8.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    )
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
