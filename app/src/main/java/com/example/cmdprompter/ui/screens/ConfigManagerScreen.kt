package com.example.cmdprompter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cmdprompter.data.model.ConfigNode
import com.example.cmdprompter.data.model.ConfigSummary
import com.example.cmdprompter.ui.components.ProgressBar
import com.example.cmdprompter.ui.theme.Blue007AFF
import com.example.cmdprompter.ui.theme.BorderLight
import com.example.cmdprompter.ui.theme.DividerColor
import com.example.cmdprompter.ui.theme.PanelTitleBg
import com.example.cmdprompter.ui.theme.TerminalBg
import com.example.cmdprompter.ui.theme.TextGray

/**
 * 配置管理页（独立页面）：
 * - 展示配置目录下所有文件夹与文件（可展开 / 预览）
 * - 导入（文件多选 / 整个文件夹）
 * - 导出（ZIP 多文件体系 / JSON 快照）
 */
@Composable
fun ConfigManagerScreen(
    summary: ConfigSummary,
    expandedDirs: Set<String>,
    previewPath: String?,
    previewText: String,
    progress: Float?,
    progressLabel: String,
    onToggleDir: (String) -> Unit,
    onPreviewFile: (String) -> Unit,
    onPickFiles: () -> Unit,
    onPickFolder: () -> Unit,
    onExportZip: () -> Unit,
    onCopyJson: () -> Unit,
    onShareJson: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 状态栏占位 + 标题栏
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .background(TerminalBg)
                .statusBarsPadding()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(PanelTitleBg)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "‹ 返回",
                fontSize = 12.sp,
                color = Blue007AFF,
                modifier = Modifier.clickable { onBack() }
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "配置管理",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${summary.fileCount} 个文件 · ${formatSize(summary.totalSize)}",
                fontSize = 10.sp,
                color = TextGray
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DividerColor)
        )

        // 进度条
        if (progress != null) {
            ProgressBar(progress = progress)
            Text(
                text = progressLabel.ifBlank { "处理中…" },
                fontSize = 10.sp,
                color = TextGray,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            // ---------- 导入 ----------
            SectionTitle("导入配置")
            Text(
                text = "选择 .json / .md / .zip 文件（可多选），或选择整个配置文件夹。导入会合并进当前配置。",
                fontSize = 10.sp,
                color = TextGray
            )
            Spacer(Modifier.height(6.dp))
            Row {
                ActionBtn("选择文件", modifier = Modifier.weight(1f), enabled = progress == null) { onPickFiles() }
                Spacer(Modifier.width(8.dp))
                ActionBtn("选择文件夹", modifier = Modifier.weight(1f), enabled = progress == null) { onPickFolder() }
            }

            // ---------- 导出 ----------
            Spacer(Modifier.height(14.dp))
            SectionTitle("导出配置")
            Text(
                text = "ZIP 导出完整多文件体系（可直接再导入）；JSON 为合并快照。",
                fontSize = 10.sp,
                color = TextGray
            )
            Spacer(Modifier.height(6.dp))
            Row {
                ActionBtn("导出 ZIP", modifier = Modifier.weight(1f), enabled = progress == null) { onExportZip() }
                Spacer(Modifier.width(8.dp))
                ActionBtn("分享 JSON", modifier = Modifier.weight(1f), enabled = progress == null) { onShareJson() }
            }
            Spacer(Modifier.height(6.dp))
            ActionBtn("复制 JSON", filled = false, enabled = progress == null) { onCopyJson() }

            // ---------- 文件树 ----------
            Spacer(Modifier.height(14.dp))
            SectionTitle("配置文件")
            Text(
                text = "config/ 目录下的全部内容，点击文件可预览。",
                fontSize = 10.sp,
                color = TextGray
            )
            Spacer(Modifier.height(6.dp))
            if (summary.tree.isEmpty()) {
                Text("暂无配置文件", fontSize = 11.sp, color = TextGray)
            } else {
                summary.tree.forEach { node ->
                    ConfigFileRow(
                        node = node,
                        depth = 0,
                        expandedDirs = expandedDirs,
                        previewPath = previewPath,
                        onToggleDir = onToggleDir,
                        onPreviewFile = onPreviewFile
                    )
                }
            }

            // ---------- 预览 ----------
            if (!previewPath.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                SectionTitle("预览：$previewPath")
                Spacer(Modifier.height(4.dp))
                Text(
                    text = previewText.ifBlank { "（空文件）" },
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF1F2933),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F6F8), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ConfigFileRow(
    node: ConfigNode,
    depth: Int,
    expandedDirs: Set<String>,
    previewPath: String?,
    onToggleDir: (String) -> Unit,
    onPreviewFile: (String) -> Unit
) {
    val expanded = expandedDirs.contains(node.path)
    val selected = previewPath == node.path
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (selected) Color(0xFFE4EDFA) else Color.Transparent,
                    RoundedCornerShape(6.dp)
                )
                .clickable {
                    if (node.isDirectory) onToggleDir(node.path) else onPreviewFile(node.path)
                }
                .padding(start = (6 + depth * 12).dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (node.isDirectory) (if (expanded) "\uD83D\uDCC2" else "\uD83D\uDCC1") else "\uD83D\uDCC4",
                fontSize = 12.sp
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = node.name,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = Color(0xFF222222),
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            if (node.isDirectory) {
                Text(
                    text = "${node.fileCount} 项",
                    fontSize = 9.sp,
                    color = TextGray
                )
            } else {
                Text(
                    text = formatSize(node.size),
                    fontSize = 9.sp,
                    color = TextGray
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = formatTime(node.lastModified),
                fontSize = 9.sp,
                color = Color(0xFFA0A8B5)
            )
        }
        if (node.isDirectory && expanded) {
            node.children.forEach { child ->
                ConfigFileRow(
                    node = child,
                    depth = depth + 1,
                    expandedDirs = expandedDirs,
                    previewPath = previewPath,
                    onToggleDir = onToggleDir,
                    onPreviewFile = onPreviewFile
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF222222))
}

@Composable
private fun ActionBtn(
    text: String,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val bg = if (!enabled) Color(0xFFD8DCE2) else if (filled) Blue007AFF else Color.White
    val fg = if (!enabled) Color(0xFF8A9099) else if (filled) Color.White else Color(0xFF333333)
    Box(
        modifier = modifier
            .height(34.dp)
            .background(bg, RoundedCornerShape(6.dp))
            .then(if (filled) Modifier else Modifier.border(1.dp, BorderLight, RoundedCornerShape(6.dp)))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 11.sp, color = fg)
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format("%.1f MB", bytes / 1024.0 / 1024.0)
}

private fun formatTime(millis: Long): String {
    if (millis <= 0L) return ""
    val sdf = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(millis))
}
