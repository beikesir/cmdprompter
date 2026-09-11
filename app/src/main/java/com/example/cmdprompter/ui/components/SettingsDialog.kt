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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.cmdprompter.ui.theme.Blue007AFF
import com.example.cmdprompter.ui.theme.BorderLight
import com.example.cmdprompter.ui.theme.TextGray

/**
 * 设置弹窗（F9）：导出配置 / 导入配置 / 恢复内置。
 */
@Composable
fun SettingsDialog(
    exportText: String,
    importText: String,
    onImportTextChange: (String) -> Unit,
    onImportText: () -> Unit,
    onPickFile: () -> Unit,
    onShare: () -> Unit,
    onCopyExport: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {
                Text("设置", fontSize = 14.sp, fontWeight = FontWeight.Bold)

                Spacer(Modifier.height(10.dp))
                SectionTitle("导出配置")
                Text(
                    text = "可复制下方 JSON 备份，或分享为文件。",
                    fontSize = 10.sp,
                    color = TextGray
                )
                Spacer(Modifier.height(6.dp))
                Row {
                    ActionButton(text = "复制 JSON", modifier = Modifier.weight(1f), onClick = onCopyExport)
                    Spacer(Modifier.width(8.dp))
                    ActionButton(text = "分享文件", modifier = Modifier.weight(1f), onClick = onShare)
                }
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 110.dp)
                        .border(1.dp, BorderLight, RoundedCornerShape(6.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(6.dp)
                ) {
                    Text(
                        text = exportText.ifBlank { "（暂无内容）" },
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF444444)
                    )
                }

                Spacer(Modifier.height(14.dp))
                SectionTitle("导入配置")
                Text(
                    text = "支持粘贴合并 JSON，或选择 .json / .zip 文件（整体替换当前配置）。",
                    fontSize = 10.sp,
                    color = TextGray
                )
                Spacer(Modifier.height(6.dp))
                ImportTextField(value = importText, onValueChange = onImportTextChange)
                Spacer(Modifier.height(8.dp))
                Row {
                    ActionButton(text = "粘贴导入", modifier = Modifier.weight(1f), onClick = onImportText)
                    Spacer(Modifier.width(8.dp))
                    ActionButton(text = "选择文件", modifier = Modifier.weight(1f), onClick = onPickFile)
                }

                Spacer(Modifier.height(14.dp))
                SectionTitle("内置配置")
                Spacer(Modifier.height(6.dp))
                Row {
                    ActionButton(text = "恢复内置配置", modifier = Modifier.weight(1f), onClick = onReset)
                    Spacer(Modifier.width(8.dp))
                    ActionButton(text = "关闭", modifier = Modifier.weight(1f), filled = false, onClick = onDismiss)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF222222))
}

@Composable
private fun ImportTextField(
    value: String,
    onValueChange: (String) -> Unit
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(fontSize = 10.sp, color = Color(0xFF222222)),
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .border(1.dp, BorderLight, RoundedCornerShape(6.dp))
            .padding(8.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(text = "在此粘贴配置 JSON…", fontSize = 10.sp, color = TextGray)
                }
                inner()
            }
        }
    )
}

@Composable
private fun ActionButton(
    text: String,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(32.dp)
            .background(
                if (filled) Blue007AFF else Color.White,
                RoundedCornerShape(6.dp)
            )
            .then(
                if (filled) Modifier else Modifier.border(1.dp, BorderLight, RoundedCornerShape(6.dp))
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = if (filled) Color.White else Color(0xFF333333)
        )
    }
}
