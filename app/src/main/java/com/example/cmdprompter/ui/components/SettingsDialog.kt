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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.cmdprompter.data.model.Command
import com.example.cmdprompter.data.model.CommandGroup
import com.example.cmdprompter.ui.theme.Blue007AFF
import com.example.cmdprompter.ui.theme.BorderLight
import com.example.cmdprompter.ui.theme.DividerColor
import com.example.cmdprompter.ui.theme.PanelTitleBg
import com.example.cmdprompter.ui.theme.TextGray

/**
 * 设置弹窗：
 * - 已配置工具与对应配置文件 / 文档名
 * - 导入（文件多选 / 文件夹）+ 进度条
 * - 导出（zip 多文件体系 / JSON）+ 进度条
 * - 手动新增、编辑命令与命令组
 */
@Composable
fun SettingsDialog(
    commands: List<Command>,
    groups: List<CommandGroup>,
    progress: Float?,
    progressLabel: String,
    onNewCommand: () -> Unit,
    onNewGroup: () -> Unit,
    onEditCommand: (String) -> Unit,
    onEditGroup: (String) -> Unit,
    onDeleteItem: (String) -> Unit,
    configFileCount: Int,
    onOpenConfigManager: () -> Unit,
    version: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = { if (progress == null) onDismiss() }) {
        Surface(shape = RoundedCornerShape(12.dp), color = Color.White) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 580.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {
                Text("设置", fontSize = 15.sp, fontWeight = FontWeight.Bold)

                // ---------- 进度条 ----------
                if (progress != null) {
                    Spacer(Modifier.height(8.dp))
                    ProgressBar(progress = progress)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = progressLabel.ifBlank { "处理中…" },
                        fontSize = 10.sp,
                        color = TextGray
                    )
                }

                // ---------- 配置管理 ----------
                SectionTitle("配置管理")
                Text(
                    text = "查看配置目录下所有文件夹与文件，导入 / 导出配置。",
                    fontSize = 10.sp, color = TextGray
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PanelTitleBg, RoundedCornerShape(8.dp))
                        .border(1.dp, BorderLight, RoundedCornerShape(8.dp))
                        .clickable { onOpenConfigManager() }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "\uD83D\uDCC1", fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("打开配置管理", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("共 $configFileCount 个文件", fontSize = 9.sp, color = TextGray)
                    }
                    Text(text = "\u203A", fontSize = 16.sp, color = TextGray)
                }

                // ---------- 手动管理 ----------
                Spacer(Modifier.height(14.dp))
                SectionTitle("手动新增 / 编辑")
                Text(
                    text = "新增或修改后的条目会带「自建 / 已改」标记，并写入独立的配置文件与文档。",
                    fontSize = 10.sp, color = TextGray
                )
                Spacer(Modifier.height(6.dp))
                Row {
                    ActionButton("+ 新建命令", modifier = Modifier.weight(1f), enabled = progress == null) { onNewCommand() }
                    Spacer(Modifier.width(8.dp))
                    ActionButton("+ 新建命令组", modifier = Modifier.weight(1f), enabled = progress == null) { onNewGroup() }
                }

                val customCommands = commands.filter { it.isCustom }
                val customGroups = groups.filter { it.isCustom }
                if (customCommands.isEmpty() && customGroups.isEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text("还没有手动维护的条目", fontSize = 10.sp, color = TextGray)
                } else {
                    Spacer(Modifier.height(8.dp))
                    customCommands.forEach { cmd ->
                        CustomRow(
                            title = cmd.name,
                            subtitle = "命令 · ${cmd.id} · ${cmd.platform}",
                            origin = cmd.origin,
                            onEdit = { onEditCommand(cmd.id) },
                            onDelete = { onDeleteItem(cmd.id) }
                        )
                    }
                    customGroups.forEach { g ->
                        CustomRow(
                            title = g.name,
                            subtitle = "命令组 · ${g.id} · ${g.orderedIds().size} 条命令",
                            origin = g.origin,
                            onEdit = { onEditGroup(g.id) },
                            onDelete = { onDeleteItem(g.id) }
                        )
                    }
                }

                // ---------- 其它 ----------
                Spacer(Modifier.height(14.dp))
                SectionTitle("其它")
                Spacer(Modifier.height(6.dp))
                ActionButton("关闭", filled = false, enabled = progress == null) { onDismiss() }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = "命令提示器 v$version",
                    fontSize = 9.sp,
                    color = TextGray,
                    modifier = Modifier.fillMaxWidth()
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
private fun CustomRow(
    title: String,
    subtitle: String,
    origin: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, DividerColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(Modifier.width(4.dp))
                OriginMark(origin = origin)
            }
            Text(text = subtitle, fontSize = 9.sp, color = TextGray, maxLines = 1)
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clickable { onEdit() },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "✏️", fontSize = 12.sp)
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clickable { onDelete() },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "\uD83D\uDDD1\uFE0F", fontSize = 12.sp)
        }
    }
}

@Composable
private fun ActionButton(
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
