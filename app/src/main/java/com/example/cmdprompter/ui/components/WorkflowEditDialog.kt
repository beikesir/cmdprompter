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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.cmdprompter.data.model.Command
import com.example.cmdprompter.data.model.Workflow
import com.example.cmdprompter.data.model.WorkflowStep
import com.example.cmdprompter.ui.theme.Blue007AFF
import com.example.cmdprompter.ui.theme.BorderLight
import com.example.cmdprompter.ui.theme.DividerColor
import com.example.cmdprompter.ui.theme.TextGray

/**
 * 工作流编辑弹窗：命名、编辑说明、排序、增删命令、配置参数、保存。
 */
@Composable
fun WorkflowEditDialog(
    workflow: Workflow,
    allCommands: List<Command>,
    onChange: (Workflow) -> Unit,
    onAddCommand: (String) -> Unit,
    onAddBlank: () -> Unit,
    onRemoveStep: (String) -> Unit,
    onMoveStep: (String, Boolean) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var pickerExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(12.dp), color = Color.White) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 580.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {
                Text(
                    text = if (workflow.createdAt == workflow.updatedAt) "创建工作流" else "编辑工作流",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))

                // 名称
                WField("工作流名称", workflow.name) { onChange(workflow.copy(name = it)) }
                Spacer(Modifier.height(6.dp))
                // 说明
                WField("说明（可随时编辑）", workflow.note, singleLine = false) {
                    onChange(workflow.copy(note = it))
                }

                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "执行步骤（${workflow.steps.size}）",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    // 添加命令（从已有命令快照）
                    Box {
                        Text(
                            text = "+ 添加命令",
                            fontSize = 11.sp,
                            color = Blue007AFF,
                            modifier = Modifier.clickable { pickerExpanded = true }
                        )
                        androidx.compose.material3.DropdownMenu(
                            expanded = pickerExpanded,
                            onDismissRequest = { pickerExpanded = false }
                        ) {
                            allCommands.forEach { cmd ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(cmd.name, fontSize = 11.sp)
                                            Text(
                                                "${cmd.platform} · ${cmd.template}",
                                                fontSize = 9.sp,
                                                color = TextGray,
                                                maxLines = 1
                                            )
                                        }
                                    },
                                    onClick = {
                                        onAddCommand(cmd.id)
                                        pickerExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "+ 空白",
                        fontSize = 11.sp,
                        color = Blue007AFF,
                        modifier = Modifier.clickable { onAddBlank() }
                    )
                }
                Spacer(Modifier.height(4.dp))

                if (workflow.steps.isEmpty()) {
                    Text(
                        text = "还没有步骤，点「+ 添加命令」从命令库挑一条。",
                        fontSize = 10.sp,
                        color = TextGray
                    )
                } else {
                    workflow.steps.forEachIndexed { index, step ->
                        StepCard(
                            index = index,
                            step = step,
                            total = workflow.steps.size,
                            onValueChange = { key, value ->
                                val newValues = step.values.toMutableMap().apply { put(key, value) }
                                onChange(
                                    workflow.copy(
                                        steps = workflow.steps.map {
                                            if (it.id == step.id) it.copy(values = newValues) else it
                                        }
                                    )
                                )
                            },
                            onTemplateChange = { tpl ->
                                onChange(
                                    workflow.copy(
                                        steps = workflow.steps.map {
                                            if (it.id == step.id) it.copy(template = tpl) else it
                                        }
                                    )
                                )
                            },
                            onRemove = { onRemoveStep(step.id) },
                            onMoveUp = { onMoveStep(step.id, true) },
                            onMoveDown = { onMoveStep(step.id, false) }
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .background(Blue007AFF, RoundedCornerShape(6.dp))
                            .clickable { onSave() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("保存", fontSize = 12.sp, color = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .border(1.dp, BorderLight, RoundedCornerShape(6.dp))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("取消", fontSize = 12.sp, color = Color(0xFF333333))
                    }
                }
            }
        }
    }
}

@Composable
private fun StepCard(
    index: Int,
    step: WorkflowStep,
    total: Int,
    onValueChange: (String, String) -> Unit,
    onTemplateChange: (String) -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DividerColor, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${index + 1}.",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Blue007AFF,
                modifier = Modifier.width(18.dp)
            )
            Text(
                text = step.name.ifBlank { "未命名步骤" },
                fontSize = 11.5f.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            // 排序
            Text(
                text = "↑",
                fontSize = 12.sp,
                color = if (index > 0) Blue007AFF else Color(0xFFCCCCCC),
                modifier = Modifier
                    .size(24.dp)
                    .then(if (index > 0) Modifier.clickable { onMoveUp() } else Modifier)
                    .padding(3.dp)
            )
            Text(
                text = "↓",
                fontSize = 12.sp,
                color = if (index < total - 1) Blue007AFF else Color(0xFFCCCCCC),
                modifier = Modifier
                    .size(24.dp)
                    .then(if (index < total - 1) Modifier.clickable { onMoveDown() } else Modifier)
                    .padding(3.dp)
            )
            Text(
                text = "✕",
                fontSize = 12.sp,
                color = Color(0xFF999999),
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onRemove() }
                    .padding(3.dp)
            )
        }

        Spacer(Modifier.height(4.dp))
        // 模板（可直接改）
        WField("命令模板", step.template) { onTemplateChange(it) }

        // 参数
        if (step.params.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            step.params.forEach { p ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(
                        text = p.displayLabel,
                        fontSize = 10.sp,
                        color = Color(0xFF555555),
                        modifier = Modifier.width(72.dp),
                        maxLines = 1
                    )
                    WField(
                        label = p.hint.ifBlank { p.default },
                        value = step.values[p.key] ?: p.default,
                        modifier = Modifier.weight(1f)
                    ) { onValueChange(p.key, it) }
                }
            }
        }

        // 渲染预览
        val preview = step.preview()
        if (preview.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = preview,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF1F2933),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F6F8), RoundedCornerShape(5.dp))
                    .padding(horizontal = 7.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun WField(
    label: String,
    value: String = "",
    singleLine: Boolean = true,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    Column(modifier = modifier) {
        if (label.isNotBlank()) {
            Text(text = label, fontSize = 9.sp, color = TextGray)
            Spacer(Modifier.height(2.dp))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(fontSize = 11.sp, color = Color(0xFF222222)),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (singleLine) 30.dp else 50.dp)
                .background(Color.White, RoundedCornerShape(6.dp))
                .border(1.dp, BorderLight, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(text = label, fontSize = 10.sp, color = TextGray)
                    inner()
                }
            }
        )
    }
}
