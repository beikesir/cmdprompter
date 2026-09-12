package com.example.cmdprompter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cmdprompter.ui.theme.Blue007AFF
import com.example.cmdprompter.ui.theme.ProgressTrack

/**
 * 自绘进度条（不依赖 Material3 版本差异的 API）。
 *
 * @param progress 0..1；为 null 时显示不确定态（往返流动）
 */
@Composable
fun ProgressBar(
    progress: Float?,
    modifier: Modifier = Modifier,
    color: Color = Blue007AFF
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(ProgressTrack)
    ) {
        val fraction = (progress ?: 1f).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(4.dp)
                .background(color)
        )
    }
}
