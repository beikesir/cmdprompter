package com.example.cmdprompter.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.cmdprompter.ui.components.BottomPanel
import com.example.cmdprompter.ui.components.CommandItem
import com.example.cmdprompter.ui.components.GroupHeader
import com.example.cmdprompter.ui.components.PlatformHeader
import com.example.cmdprompter.ui.components.SearchSlide
import com.example.cmdprompter.ui.components.SettingsDialog
import com.example.cmdprompter.ui.components.TerminalBar
import com.example.cmdprompter.ui.components.TopBar
import com.example.cmdprompter.util.ClipboardUtil
import com.example.cmdprompter.viewmodel.DisplayData
import com.example.cmdprompter.viewmodel.MainUiState
import com.example.cmdprompter.viewmodel.MainViewModel
import com.example.cmdprompter.viewmodel.ViewMode
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@Composable
fun MainScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val state: MainUiState by vm.ui.collectAsState()
    val display: DisplayData by vm.display.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val maxPanelHeight = (LocalConfiguration.current.screenHeightDp * 0.55f).dp

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) vm.importFromUri(uri)
    }

    // 提示消息
    LaunchedEffect(state.message) {
        val msg = state.message ?: return@LaunchedEffect
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        vm.consumeMessage()
    }

    // 列表滚动 → 取消选中与文档态
    LaunchedEffect(Unit) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { it }
            .collect { vm.onListScroll() }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // 顶部终端预览区
        TerminalBar(
            text = state.terminalText,
            expanded = state.terminalExpanded,
            onToggleExpand = vm::toggleTerminal
        )

        // 顶栏
        TopBar(
            isRightView = state.currentView == ViewMode.RIGHT,
            onViewChange = { right -> vm.setView(if (right) ViewMode.RIGHT else ViewMode.LEFT) },
            onSearchClick = vm::toggleSearch,
            onSettingsClick = vm::openSettings,
            onLogoClick = {
                vm.onListScroll()
                scope.launch { listState.animateScrollToItem(0) }
            }
        )

        // 搜索滑条
        SearchSlide(
            expanded = state.searchExpanded,
            query = state.query,
            onQueryChange = vm::onQueryChange
        )

        // 主内容区
        Box(modifier = Modifier.weight(1f)) {
            when {
                state.loading -> {
                    Text(
                        text = "加载配置中…",
                        fontSize = 12.sp,
                        color = Color(0xFF888888),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                display.platforms.isEmpty() -> {
                    Text(
                        text = if (state.query.isBlank()) "暂无命令，请在设置中导入配置" else "没有匹配的命令或命令组",
                        fontSize = 12.sp,
                        color = Color(0xFF888888),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    CommandList(
                        display = display,
                        view = state.currentView,
                        selectedId = state.selectedCmdId,
                        docCmdId = if (state.docMode) state.docCmdId else null,
                        listState = listState,
                        onTogglePlatform = vm::togglePlatform,
                        onToggleGroup = vm::toggleGroup,
                        onCommandClick = vm::onCommandClick,
                        onDocClick = vm::onDocClick,
                        onDeleteClick = vm::onDeleteRequest
                    )
                }
            }
        }

        // 底部面板
        val panelId = if (state.docMode) state.docCmdId else state.selectedCmdId
        val panelCmd = vm.commandById(panelId)
        if (panelCmd != null) {
            BottomPanel(
                cmd = panelCmd,
                docMode = state.docMode,
                preview = vm.previewFor(panelCmd.id),
                paramValue = { key -> vm.paramValue(panelCmd.id, key) },
                onParamChange = { key, value -> vm.onParamChange(panelCmd.id, key, value) },
                onParamFocused = vm::onParamFocused,
                onFocusNext = { vm.focusNextParam() },
                onCopy = {
                    val text = vm.previewFor(state.selectedCmdId)
                    if (text.isBlank()) {
                        vm.postMessage("没有可复制的命令")
                    } else {
                        ClipboardUtil.copy(context, text)
                        vm.postMessage("已复制到剪贴板")
                    }
                },
                onExecute = vm::onExecute,
                onMore = { vm.postMessage("更多功能将在后续版本开放") },
                onBackToEdit = { vm.onCommandClick(panelCmd.id) },
                focusRequestId = state.focusRequestId,
                focusIndex = state.focusIndex,
                modifier = Modifier.heightIn(max = maxPanelHeight)
            )
        }
    }

    // 删除确认
    if (state.pendingDeleteId != null) {
        val target = vm.commandById(state.pendingDeleteId)
        AlertDialog(
            onDismissRequest = vm::cancelDelete,
            title = { Text(text = "删除命令", fontSize = 14.sp) },
            text = {
                Text(
                    text = "确定删除「${target?.name ?: "该命令"}」？该命令也会从所有命令组中移除。",
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                TextButton(onClick = vm::confirmDelete) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = vm::cancelDelete) { Text("取消") }
            }
        )
    }

    // 设置弹窗
    if (state.showSettings) {
        SettingsDialog(
            exportText = state.exportText,
            importText = state.settingsText,
            onImportTextChange = vm::onSettingsTextChange,
            onImportText = { vm.importFromText(state.settingsText) },
            onPickFile = { runCatching { pickFileLauncher.launch("*/*") } },
            onShare = {
                runCatching {
                    val file = vm.exportFile()
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "导出配置"))
                }.onFailure {
                    vm.postMessage("导出失败：${it.message}")
                }
            },
            onCopyExport = {
                ClipboardUtil.copy(context, state.exportText, "cmdprompter-config")
                vm.postMessage("已复制配置 JSON")
            },
            onReset = vm::resetBuiltIn,
            onDismiss = vm::closeSettings
        )
    }
}

@Composable
private fun CommandList(
    display: DisplayData,
    view: ViewMode,
    selectedId: String?,
    docCmdId: String?,
    listState: LazyListState,
    onTogglePlatform: (String) -> Unit,
    onToggleGroup: (String) -> Unit,
    onCommandClick: (String) -> Unit,
    onDocClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        display.platforms.forEach { section ->
            item(key = "platform_${section.name}") {
                PlatformHeader(
                    name = section.name,
                    collapsed = section.collapsed,
                    countLabel = if (view == ViewMode.LEFT) {
                        "${section.commands.size} 条"
                    } else {
                        "${section.groups.size} 个组"
                    },
                    onToggle = { onTogglePlatform(section.name) }
                )
            }

            if (section.collapsed) return@forEach

            if (view == ViewMode.LEFT) {
                section.commands.forEach { cmd ->
                    item(key = "cmd_${cmd.id}") {
                        CommandRow(
                            cmd = cmd,
                            selected = cmd.id == selectedId,
                            docMode = cmd.id == docCmdId,
                            indentDp = 0,
                            onCommandClick = onCommandClick,
                            onDocClick = onDocClick,
                            onDeleteClick = onDeleteClick
                        )
                    }
                }
            } else {
                // 组
                section.groups.forEach { gs ->
                    item(key = "group_${gs.group.id}") {
                        GroupHeader(
                            group = gs.group,
                            collapsed = gs.collapsed,
                            commandCount = gs.commands.size,
                            onToggle = { onToggleGroup(gs.group.id) }
                        )
                    }
                    if (!gs.collapsed) {
                        gs.commands.forEach { cmd ->
                            item(key = "gcmd_${gs.group.id}_${cmd.id}") {
                                CommandRow(
                                    cmd = cmd,
                                    selected = cmd.id == selectedId,
                                    docMode = cmd.id == docCmdId,
                                    indentDp = 10,
                                    onCommandClick = onCommandClick,
                                    onDocClick = onDocClick,
                                    onDeleteClick = onDeleteClick
                                )
                            }
                        }
                    }
                }
                // 未分组命令
                section.commands.forEach { cmd ->
                    item(key = "loose_${cmd.id}") {
                        CommandRow(
                            cmd = cmd,
                            selected = cmd.id == selectedId,
                            docMode = cmd.id == docCmdId,
                            indentDp = 0,
                            onCommandClick = onCommandClick,
                            onDocClick = onDocClick,
                            onDeleteClick = onDeleteClick
                        )
                    }
                }
            }
        }

        item {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
            )
        }
    }
}

@Composable
private fun CommandRow(
    cmd: com.example.cmdprompter.data.model.Command,
    selected: Boolean,
    docMode: Boolean,
    indentDp: Int,
    onCommandClick: (String) -> Unit,
    onDocClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    CommandItem(
        cmd = cmd,
        selected = selected || docMode,
        indentDp = indentDp,
        onSelect = { onCommandClick(cmd.id) },
        onDocClick = { onDocClick(cmd.id) },
        onDeleteClick = { onDeleteClick(cmd.id) }
    )
}
