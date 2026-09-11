package com.example.cmdprompter.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cmdprompter.ui.theme.Blue007AFF
import com.example.cmdprompter.ui.theme.SegmentBg

/**
 * 顶栏：⚡ logo + [📂组|📋命令] 分段控件 + 🔍 + ⚙️（高 36dp）
 */
@Composable
fun TopBar(
    isRightView: Boolean,
    onViewChange: (Boolean) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(Color.White)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo
        Column(
            modifier = Modifier
                .clickable { onLogoClick() }
                .padding(end = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "⚡", fontSize = 18.sp)
            Text(text = "提示器", fontSize = 7.sp, color = Color(0xFF666666))
        }

        // 分段控件（占剩余宽度约 2/3）
        Row(
            modifier = Modifier
                .weight(2f)
                .height(24.dp)
                .background(SegmentBg, RoundedCornerShape(6.dp))
                .padding(2.dp)
        ) {
            SegmentItem(
                text = "\uD83D\uDCC2组",
                selected = isRightView,
                modifier = Modifier.weight(1f),
                onClick = { onViewChange(true) }
            )
            SegmentItem(
                text = "\uD83D\uDCCB命令",
                selected = !isRightView,
                modifier = Modifier.weight(1f),
                onClick = { onViewChange(false) }
            )
        }

        Spacer(Modifier.width(6.dp))

        IconTextButton(icon = "\uD83D\uDD0D", onClick = onSearchClick)
        IconTextButton(icon = "⚙\uFE0F", onClick = onSettingsClick)
    }
}

@Composable
private fun SegmentItem(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = if (selected) Color.White else Color.Transparent,
        shadowElevation = if (selected) 1.dp else 0.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontSize = 11.sp,
                color = if (selected) Blue007AFF else Color(0xFF666666)
            )
        }
    }
}

@Composable
private fun IconTextButton(icon: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = icon, fontSize = 14.sp)
    }
}
