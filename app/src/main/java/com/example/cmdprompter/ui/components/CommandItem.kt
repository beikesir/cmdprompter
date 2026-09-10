package com.example.cmdprompter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
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
 * 单条命令项：名称 / 描述 / 标签 + 右侧 📄 🗑。
 * 操作按钮需阻止冒泡，不触发选中。
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (selected) SelectedBg else Color.White)
            .border(1.dp, BorderLight)
            .clickable { onSelect() }
    ) {
        // 选中态左侧竖条
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(if (cmd.desc.isBlank() && cmd.tags.isEmpty()) 40.dp else 52.dp)
                .background(if (selected) Blue007AFF else Color.Transparent)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = (6 + indentDp).dp, top = 5.dp, bottom = 5.dp, end = 4.dp)
        ) {
            Text(
                text = cmd.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (cmd.desc.isNotBlank()) {
                Text(
                    text = cmd.desc,
                    fontSize = 10.sp,
                    color = TextGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
            if (cmd.tags.isNotEmpty()) {
                Row(modifier = Modifier.padding(top = 3.dp)) {
                    cmd.tags.forEach { tag ->
                        TagChip(text = tag, background = TagBg)
                        Spacer(Modifier.width(4.dp))
                    }
                }
            }
        }
        // 操作区
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clickable { onDocClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "\uD83D\uDCC4", fontSize = 12.sp)
            }
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clickable { onDeleteClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "\uD83D\uDDD1\uFE0F", fontSize = 12.sp)
            }
        }
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
        color = textColor,
        modifier = Modifier
            .background(background, RoundedCornerShape(8.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    )
}
