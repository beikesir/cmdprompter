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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.cmdprompter.data.model.Command
import com.example.cmdprompter.data.model.CommandGroup
import com.example.cmdprompter.data.model.ParamDef
import com.example.cmdprompter.ui.theme.Blue007AFF
import com.example.cmdprompter.ui.theme.BorderLight
import com.example.cmdprompter.ui.theme.TextGray
import com.example.cmdprompter.viewmodel.EditTarget

/**
 * 手动新增 / 编辑命令或命令组（文档为可选项）。
 */
@Composable
fun CommandEditDialog(
    target: EditTarget,
    allCommands: List<Command>,
    existingCommand: Command?,
    existingGroup: CommandGroup?,
    onSaveCommand: (Command) -> Unit,
    onSaveGroup: (CommandGroup) -> Unit,
    onDismiss: () -> Unit
) {
    val isGroup = target is EditTarget.NewGroup || target is EditTarget.Group
    val isEdit = target is EditTarget.Command || target is EditTarget.Group

    var id by remember { mutableStateOf(existingCommand?.id ?: existingGroup?.id ?: "") }
    var name by remember { mutableStateOf(existingCommand?.name ?: existingGroup?.name ?: "") }
    var desc by remember { mutableStateOf(existingCommand?.desc ?: "") }
    var tags by remember { mutableStateOf((existingCommand?.tags ?: existingGroup?.tags ?: emptyList()).joinToString(",")) }
    var template by remember { mutableStateOf(existingCommand?.template ?: "") }
    var doc by remember { mutableStateOf(existingCommand?.doc ?: existingGroup?.doc ?: "") }

    val params = remember {
        mutableStateListOf<ParamDef>().apply {
            existingCommand?.params?.let { addAll(it) }
        }
    }
    val selectedIds = remember {
        mutableStateListOf<String>().apply {
            addAll(existingGroup?.orderedIds() ?: emptyList())
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(12.dp), color = Color.White) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {
                Text(
                    text = if (isGroup) {
                        if (isEdit) "编辑命令组" else "新建命令组"
                    } else {
                        if (isEdit) "编辑命令" else "新建命令"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))

                Field("id（唯一标识，命令组通过它引用命令）") { id = it; Unit }
                Spacer(Modifier.height(6.dp))
                Field("名称", name) { name = it }
                if (!isGroup) {
                    Spacer(Modifier.height(6.dp))
                    Field("描述", desc) { desc = it }
                    Spacer(Modifier.height(6.dp))
                    Field("命令模板（参数写 {{key}}）", template) { template = it }
                }
                Spacer(Modifier.height(6.dp))
                Field("标签（英文逗号分隔）", tags) { tags = it }

                if (isGroup) {
                    Spacer(Modifier.height(10.dp))
                    Text("组内命令（按勾选顺序执行）", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .verticalScroll(rememberScrollState())
                            .border(1.dp, BorderLight, RoundedCornerShape(6.dp))
                    ) {
                        allCommands.forEach { cmd ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (selectedIds.contains(cmd.id)) selectedIds.remove(cmd.id)
                                        else selectedIds.add(cmd.id)
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedIds.contains(cmd.id),
                                    onCheckedChange = { checked ->
                                        if (checked) selectedIds.add(cmd.id) else selectedIds.remove(cmd.id)
                                    }
                                )
                                Text(text = cmd.name, fontSize = 11.sp, modifier = Modifier.weight(1f), maxLines = 1)
                                Text(text = cmd.platform, fontSize = 9.sp, color = TextGray)
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("参数", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(
                            text = "+ 添加参数",
                            fontSize = 11.sp,
                            color = Blue007AFF,
                            modifier = Modifier.clickable { params.add(ParamDef()) }
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    params.forEachIndexed { index, p ->
                        ParamEditor(
                            param = p,
                            onChange = { params[index] = it },
                            onRemove = { params.removeAt(index) }
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text("文档（可选）", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Field("支持 Markdown 小节", doc, singleLine = false) { doc = it }

                Spacer(Modifier.height(14.dp))
                Row {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .background(Blue007AFF, RoundedCornerShape(6.dp))
                            .clickable {
                                val tagList = tags.split(",", "，").map { it.trim() }.filter { it.isNotEmpty() }
                                if (isGroup) {
                                    onSaveGroup(
                                        (existingGroup ?: CommandGroup()).copy(
                                            id = id.trim(),
                                            name = name.trim(),
                                            tags = tagList,
                                            cmdIds = selectedIds.toList(),
                                            order = selectedIds.toList(),
                                            doc = doc
                                        )
                                    )
                                } else {
                                    onSaveCommand(
                                        (existingCommand ?: Command()).copy(
                                            id = id.trim(),
                                            name = name.trim(),
                                            desc = desc.trim(),
                                            tags = tagList,
                                            template = template,
                                            params = params.toList(),
                                            doc = doc
                                        )
                                    )
                                }
                            },
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
private fun Field(
    label: String,
    value: String = "",
    singleLine: Boolean = true,
    onValueChange: (String) -> Unit
) {
    Column {
        Text(text = label, fontSize = 9.sp, color = TextGray)
        Spacer(Modifier.height(2.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(fontSize = 11.sp, color = Color(0xFF222222)),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (singleLine) 30.dp else 60.dp)
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

@Composable
private fun ParamEditor(
    param: ParamDef,
    onChange: (ParamDef) -> Unit,
    onRemove: () -> Unit
) {
    var typeExpanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderLight, RoundedCornerShape(6.dp))
            .padding(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Field("key", param.key) { onChange(param.copy(key = it)) }
            Spacer(Modifier.width(6.dp))
            Field("显示名", param.label) { onChange(param.copy(label = it)) }
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 类型下拉
            Box {
                Row(
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(6.dp))
                        .border(1.dp, BorderLight, RoundedCornerShape(6.dp))
                        .clickable { typeExpanded = true }
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = if (param.isEnum) "enum" else "text", fontSize = 10.sp)
                    Text(text = "▾", fontSize = 9.sp, color = TextGray)
                }
                DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    listOf("text", "enum").forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t, fontSize = 11.sp) },
                            onClick = {
                                onChange(param.copy(type = t))
                                typeExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
            Box(modifier = Modifier.weight(1f)) {
                Field(
                    label = if (param.isEnum) "选项（逗号分隔）" else "默认值",
                    value = if (param.isEnum) param.options.joinToString(",") else param.default
                ) {
                    onChange(
                        if (param.isEnum) {
                            param.copy(options = it.split(",", "，").map { s -> s.trim() }.filter { s -> s.isNotEmpty() })
                        } else {
                            param.copy(default = it)
                        }
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onRemove() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "✕", fontSize = 12.sp, color = Color(0xFF999999))
            }
        }
    }
}
