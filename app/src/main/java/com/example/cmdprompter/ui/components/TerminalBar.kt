package com.example.cmdprompter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cmdprompter.ui.theme.TerminalBg
import com.example.cmdprompter.ui.theme.TerminalFg

/**
 * 顶部终端预览区（F6）。
 * 默认 42dp（约两行，超出滚动），展开 100dp；仅点击"执行"时才填充命令。
 */
@Composable
fun TerminalBar(
    text: String,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 42.dp, max = if (expanded) 100.dp else 42.dp)
            .background(TerminalBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .align(Alignment.TopStart)
        ) {
            Text(
                text = text,
                color = TerminalFg,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            )
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clickable { onToggleExpand() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = if (expanded) "▲" else "▼", color = TerminalFg, fontSize = 10.sp)
            }
        }
    }
}
