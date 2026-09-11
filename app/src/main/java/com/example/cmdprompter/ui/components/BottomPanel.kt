package com.example.cmdprompter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cmdprompter.data.model.Command
import com.example.cmdprompter.data.model.ParamDef
import com.example.cmdprompter.ui.theme.Blue007AFF
import com.example.cmdprompter.ui.theme.BorderLight
import com.example.cmdprompter.ui.theme.DividerColor
import com.example.cmdprompter.ui.theme.PanelTitleBg
import com.example.cmdprompter.ui.theme.PreviewBg
import com.example.cmdprompter.ui.theme.PreviewFg
import com.example.cmdprompter.ui.theme.TextGray
import kotlinx.coroutines.delay

/**
 * 底部面板：正常态（参数编辑 + 预览）与文档态。
 */
@Composable
fun BottomPanel(
    cmd: Command,
    docMode: Boolean,
    preview: String,
    paramValue: (String) -> String,
    onParamChange: (String, String) -> Unit,
    onParamFocused: (Int) -> Unit,
    onFocusNext: () -> Unit,
    onCopy: () -> Unit,
    onExecute: () -> Unit,
    onMore: () -> Unit,
    onBackToEdit: () -> Unit,
    focusRequestId: Int,
    focusIndex: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        // 顶部细线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DividerColor)
        )

        if (docMode) {
            DocView(cmd = cmd, onBackToEdit = onBackToEdit)
        } else {
            NormalView(
                cmd = cmd,
                preview = preview,
                paramValue = paramValue,
                onParamChange = onParamChange,
                onParamFocused = onParamFocused,
                onFocusNext = onFocusNext,
                onCopy = onCopy,
                onExecute = onExecute,
                onMore = onMore,
                focusRequestId = focusRequestId,
                focusIndex = focusIndex
            )
        }
    }
}

// ------------------------------------------------------------------ 正常态

@Composable
private fun NormalView(
    cmd: Command,
    preview: String,
    paramValue: (String) -> String,
    onParamChange: (String, String) -> Unit,
    onParamFocused: (Int) -> Unit,
    onFocusNext: () -> Unit,
    onCopy: () -> Unit,
    onExecute: () -> Unit,
    onMore: () -> Unit,
    focusRequestId: Int,
    focusIndex: Int
) {
    // 参数输入状态（TextFieldValue 便于循环聚焦时全选），首次组合即带默认值，避免首帧空白
    val fields = remember(cmd.id) {
        mutableStateMapOf<String, TextFieldValue>().apply {
            cmd.params.forEach { p -> put(p.key, TextFieldValue(paramValue(p.key))) }
        }
    }
    val requesters = remember(cmd.id) { cmd.params.map { FocusRequester() } }

    // 📝 循环聚焦：focusRequestId 变化时聚焦下一个参数并全选
    LaunchedEffect(focusRequestId, cmd.id) {
        val index = focusIndex
        if (index in cmd.params.indices) {
            val p = cmd.params[index]
            val current = fields[p.key] ?: TextFieldValue(paramValue(p.key))
            fields[p.key] = current.copy(selection = TextRange(0, current.text.length))
            runCatching { requesters[index].requestFocus() }
        }
    }

    // 1) 标题栏
    Text(
        text = cmd.name,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF222222),
        maxLines = 1,
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelTitleBg)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )

    // 2) 预览块（实时反映参数）
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .heightIn(max = 60.dp)
            .background(PreviewBg, RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        Text(
            text = preview.ifBlank { "（无命令模板）" },
            color = PreviewFg,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.verticalScroll(rememberScrollState())
        )
    }

    // 3) 参数区：限高 + 可滚动（此处不在 Row/Column 作用域内，故不用 weight）
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp)
    ) {
        if (cmd.params.isEmpty()) {
            Text(
                text = "该命令没有参数",
                fontSize = 10.sp,
                color = TextGray,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        } else {
            cmd.params.forEachIndexed { index, param ->
                val field = fields[param.key] ?: TextFieldValue(paramValue(param.key))
                ParamRow(
                    param = param,
                    field = field,
                    focusRequester = requesters[index],
                    onValueChange = {
                        fields[param.key] = it
                        onParamChange(param.key, it.text)
                    },
                    onFocused = { onParamFocused(index) }
                )
            }
        }
    }

    // 4) 工具栏
    ToolBar(
        onFocusNext = onFocusNext,
        onCopy = onCopy,
        onExecute = onExecute,
        onMore = onMore
    )
}

@Composable
private fun ParamRow(
    param: ParamDef,
    field: TextFieldValue,
    focusRequester: FocusRequester,
    onValueChange: (TextFieldValue) -> Unit,
    onFocused: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(72.dp)) {
            Text(text = param.displayLabel, fontSize = 9.sp, color = Color(0xFF333333))
            Text(text = param.key, fontSize = 8.sp, color = TextGray)
        }
        Spacer(Modifier.width(6.dp))
        if (param.isEnum) {
            EnumInput(
                options = param.options,
                value = field.text,
                modifier = Modifier.weight(1f)
            ) { onValueChange(TextFieldValue(it)) }
        } else {
            TextInput(
                value = field,
                hint = param.hint.ifBlank { param.default.ifBlank { "请输入" } },
                focusRequester = focusRequester,
                onValueChange = onValueChange,
                onFocused = onFocused,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TextInput(
    value: TextFieldValue,
    hint: String,
    focusRequester: FocusRequester,
    onValueChange: (TextFieldValue) -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(fontSize = 10.sp, color = Color(0xFF222222)),
        modifier = modifier
            .height(26.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.isFocused) onFocused() }
            .background(Color.White, RoundedCornerShape(4.dp))
            .border(1.dp, BorderLight, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.text.isEmpty()) {
                    Text(text = hint, fontSize = 10.sp, color = TextGray)
                }
                inner()
            }
        }
    )
}

@Composable
private fun EnumInput(
    options: List<String>,
    value: String,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.height(26.dp)) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White, RoundedCornerShape(4.dp))
                .border(1.dp, BorderLight, RoundedCornerShape(4.dp))
                .clickable { expanded = true }
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value.ifBlank { options.firstOrNull().orEmpty() },
                fontSize = 10.sp,
                color = Color(0xFF222222),
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            Text(text = "▾", fontSize = 9.sp, color = TextGray)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (options.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("（无选项）", fontSize = 10.sp) },
                    onClick = { expanded = false }
                )
            }
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 10.sp) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ToolBar(
    onFocusNext: () -> Unit,
    onCopy: () -> Unit,
    onExecute: () -> Unit,
    onMore: () -> Unit
) {
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(PanelTitleBg)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            ToolButton(icon = "\uD83D\uDCDD", label = "参数", modifier = Modifier.weight(1f)) { onFocusNext() }
            ToolButton(icon = if (copied) "✅" else "\uD83D\uDCCB", label = if (copied) "已复制" else "复制", modifier = Modifier.weight(1f)) {
                onCopy()
                copied = true
            }
            ToolButton(icon = "▶\uFE0F", label = "执行", modifier = Modifier.weight(1f)) { onExecute() }
            ToolButton(icon = "⋯", label = "更多", modifier = Modifier.weight(1f)) { onMore() }
        }
    }
}

@Composable
private fun ToolButton(
    icon: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = icon, fontSize = 16.sp)
        Text(text = label, fontSize = 9.sp, color = if (label == "已复制") Blue007AFF else TextGray)
    }
}

// ------------------------------------------------------------------ 文档态

@Composable
private fun DocView(
    cmd: Command,
    onBackToEdit: () -> Unit
) {
    Text(
        text = "${cmd.name} - 文档",
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF222222),
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelTitleBg)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 320.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = cmd.doc.ifBlank { "（该命令暂无详细文档）" },
            fontSize = 12.sp,
            lineHeight = 20.sp,
            color = Color(0xFF333333)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "点击命令可返回参数编辑",
            fontSize = 10.sp,
            color = TextGray,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onBackToEdit() }
                .padding(vertical = 6.dp)
        )
    }
}
