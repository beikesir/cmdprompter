package com.example.cmdprompter.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cmdprompter.ui.theme.Blue007AFF
import com.example.cmdprompter.ui.theme.DividerColor
import com.example.cmdprompter.ui.theme.PanelTitleBg
import com.example.cmdprompter.ui.theme.TextGray

/**
 * 文档面板：默认展示"该命令 / 命令组在文档中对应的节点"（截取），
 * 可切换查看整篇文档全文。
 *
 * @param title    节点标题
 * @param body     节点正文（Markdown 子集）
 * @param fullText 整篇文档全文（为空时隐藏"查看完整文档"）
 * @param source   来源文档名
 * @param itemName 命令 / 命令组名称，用于标题栏与空态提示
 * @param onEdit   修改当前条目（命令或命令组），就地打开编辑弹窗
 * @param onCreateWorkflow 仅命令组可用：带上组内所有命令去新建工作流；为 null 时不显示该按钮
 */
@Composable
fun DocPanel(
    title: String,
    body: String,
    fullText: String,
    source: String,
    itemName: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onCreateWorkflow: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showFull by rememberSaveable(title, source) { mutableStateOf(false) }
    val hasFull = fullText.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DividerColor)
        )

        // 标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PanelTitleBg)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.ifBlank { itemName },
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222),
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            if (source.isNotBlank()) {
                Text(text = source, fontSize = 9.sp, color = TextGray)
                Spacer(Modifier.width(8.dp))
            }
            // 修改：就地打开编辑弹窗
            Text(
                text = "修改",
                fontSize = 10.sp,
                color = Blue007AFF,
                modifier = Modifier
                    .clickable { onEdit() }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
            // 创建工作流（仅命令组）
            if (onCreateWorkflow != null) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "创建工作流",
                    fontSize = 10.sp,
                    color = Blue007AFF,
                    modifier = Modifier
                        .background(Color(0xFFE7F1FF), RoundedCornerShape(8.dp))
                        .clickable { onCreateWorkflow() }
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = "返回",
                fontSize = 10.sp,
                color = TextGray,
                modifier = Modifier.clickable { onBack() }
            )
        }

        // 正文
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            if (showFull && hasFull) {
                MarkdownBlock(text = fullText)
            } else if (body.isNotBlank()) {
                MarkdownBlock(text = body)
            } else {
                Text(
                    text = "「$itemName」暂无对应文档节点。\n在文档中为其添加小节即可自动关联：\n## `$itemName 的 id` 标题",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = TextGray
                )
            }

            if (hasFull) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (showFull) "收起，只看当前节点" else "查看《$source》完整文档",
                    fontSize = 11.sp,
                    color = Blue007AFF,
                    modifier = Modifier
                        .clickable { showFull = !showFull }
                        .padding(vertical = 6.dp)
                )
            }
        }
    }
}
