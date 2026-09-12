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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.example.cmdprompter.AppVersion
import com.example.cmdprompter.ui.components.CommandEditDialog
import com.example.cmdprompter.ui.components.CommandItem
import com.example.cmdprompter.ui.components.DocPanel
import com.example.cmdprompter.ui.components.WorkflowEditDialog
import com.example.cmdprompter.ui.components.GroupCard
import com.example.cmdprompter.ui.components.PlatformHeader
import com.example.cmdprompter.ui.components.SearchSlide
import com.example.cmdprompter.ui.components.SettingsDialog
import com.example.cmdprompter.ui.components.TerminalBar
import com.example.cmdprompter.ui.components.TopBar
import com.example.cmdprompter.ui.theme.TerminalBg
import com.example.cmdprompter.util.ClipboardUtil
import com.example.cmdprompter.viewmodel.DisplayData
import com.example.cmdprompter.viewmodel.MainUiState
import com.example.cmdprompter.viewmodel.MainViewModel
import com.example.cmdprompter.viewmodel.EditTarget
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
    val maxPanelHeight = (LocalConfiguration.current.screenHeightDp * 0.45f).dp

    val pickFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (!uris.isNullOrEmpty()) vm.importFromUris(uris)
    }
    val pickFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) vm.importFromTree(uri)
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

    // 导入 / 导出通用动作（设置页与配置管理页共用）
    val doPickFiles: () -> Unit = { runCatching { pickFilesLauncher.launch(arrayOf("*/*")) } }
    val doPickFolder: () -> Unit = { runCatching { pickFolderLauncher.launch(null) } }
    val doExportZip: () -> Unit = {
        vm.exportZipWithProgress { file ->
            if (file == null) {
                vm.postMessage("导出失败")
            } else {
                runCatching {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "导出配置 ZIP"))
                }.onFailure { vm.postMessage("导出失败：${it.message}") }
            }
        }
    }
    val doCopyJson: () -> Unit = {
        ClipboardUtil.copy(context, vm.exportJsonText(), "cmdprompter-config")
        vm.postMessage("已复制配置 JSON")
    }
    val doShareJson: () -> Unit = {
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
            context.startActivity(Intent.createChooser(intent, "导出配置 JSON"))
        }.onFailure { vm.postMessage("导出失败：${it.message}") }
    }

    // 配置管理页：独立页面，覆盖主界面
    if (state.showConfigManager) {
        ConfigManagerScreen(
            summary = state.configSummary,
            expandedDirs = state.expandedConfigDirs,
            previewPath = state.previewConfigPath,
            previewText = state.previewConfigText,
            progress = state.progress,
            progressLabel = state.progressLabel,
            onToggleDir = vm::toggleConfigDir,
            onPreviewFile = vm::previewConfigFile,
            onPickFiles = doPickFiles,
            onPickFolder = doPickFolder,
            onExportZip = doExportZip,
            onCopyJson = doCopyJson,
            onShareJson = doShareJson,
            onBack = vm::closeConfigManager
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg)
            .navigationBarsPadding()
            .imePadding()
    ) {
        // 状态栏占位：与终端同色，终端内容不再被状态栏遮挡
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .background(TerminalBg)
                .statusBarsPadding()
        )

        // 顶部终端预览区
        TerminalBar(
            text = state.terminalText,
            expanded = state.terminalExpanded,
            onToggleExpand = vm::toggleTerminal
        )

        Column(modifier = Modifier.weight(1f).background(Color.White)) {
            // 顶栏
            TopBar(
                currentView = state.currentView,
                onViewChange = vm::setView,
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
                    state.currentView == ViewMode.WORKFLOW -> {
                        if (state.workflows.isEmpty()) {
                            Text(
                                text = "暂无工作流。\n打开任一命令组的文档，点「创建工作流」即可基于它创建。",
                                fontSize = 12.sp,
                                color = Color(0xFF888888),
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            WorkflowList(
                                workflows = state.workflows,
                                onOpen = vm::openWorkflow,
                                onDelete = vm::deleteWorkflow
                            )
                        }
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
                            onGroupDocClick = vm::onGroupDocClick,
                            onDeleteClick = vm::onDeleteRequest
                        )
                    }
                }
            }
        }


        // 底部面板：文档态（命令 / 命令组）优先，其次是命令参数编辑
        val docGroup = vm.groupById(state.docGroupId)
        val docCmd = vm.commandById(state.docCmdId)
        if (state.docMode && (docGroup != null || docCmd != null)) {
            val title = (docGroup?.docTitle ?: docCmd?.docTitle).orEmpty()
            val body = docGroup?.doc ?: docCmd?.doc.orEmpty()
            val source = docGroup?.docSource ?: docCmd?.docSource.orEmpty()
            val itemName = docGroup?.name ?: docCmd?.name.orEmpty()
            DocPanel(
                title = title,
                body = body,
                fullText = vm.fullTextOf(source),
                source = source,
                itemName = itemName,
                onBack = {
                    if (docCmd != null) vm.onCommandClick(docCmd.id) else vm.onListScroll()
                },
                onEdit = {
                    // 就地修改：命令 → EditTarget.Command；命令组 → EditTarget.Group
                    when {
                        docCmd != null -> vm.openEditor(EditTarget.Command(docCmd.id))
                        docGroup != null -> vm.openEditor(EditTarget.Group(docGroup.id))
                    }
                },
                onCreateWorkflow = if (docGroup != null) {
                    { vm.createWorkflowFromGroup(docGroup.id) }
                } else null,
                modifier = Modifier.heightIn(max = maxPanelHeight)
            )
        } else {
            val panelCmd = vm.commandById(state.selectedCmdId)
            if (panelCmd != null) {
                BottomPanel(
                    cmd = panelCmd,
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
                    focusRequestId = state.focusRequestId,
                    focusIndex = state.focusIndex,
                    modifier = Modifier.heightIn(max = maxPanelHeight)
                )
            }
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

    // 设置弹窗（仅保留手动新增 / 编辑）
    if (state.showSettings) {
        SettingsDialog(
            commands = state.commands,
            groups = state.groups,
            progress = state.progress,
            progressLabel = state.progressLabel,
            onNewCommand = { vm.openEditor(EditTarget.NewCommand) },
            onNewGroup = { vm.openEditor(EditTarget.NewGroup) },
            onEditCommand = { vm.openEditor(EditTarget.Command(it)) },
            onEditGroup = { vm.openEditor(EditTarget.Group(it)) },
            onDeleteItem = { vm.deleteCustom(it) },
            configFileCount = state.configSummary.fileCount,
            onOpenConfigManager = {
                vm.closeSettings()
                vm.openConfigManager()
            },
            version = AppVersion.NAME,
            onDismiss = vm::closeSettings
        )
    }

    // 工作流编辑弹窗
    val editingWf = state.editingWorkflow
    if (editingWf != null) {
        WorkflowEditDialog(
            workflow = editingWf,
            allCommands = state.commands,
            onChange = vm::updateEditingWorkflow,
            onAddCommand = vm::addWorkflowStep,
            onAddBlank = vm::addBlankWorkflowStep,
            onRemoveStep = vm::removeWorkflowStep,
            onMoveStep = { stepId, up -> vm.moveWorkflowStep(stepId, up) },
            onSave = vm::saveEditingWorkflow,
            onDismiss = vm::closeEditor
        )
    }

    // 手动新增 / 编辑弹窗
    val target = state.editTarget
    if (target != null) {
        val cmd = (target as? EditTarget.Command)?.id?.let { vm.commandById(it) }
        val group = (target as? EditTarget.Group)?.id?.let { vm.groupById(it) }
        CommandEditDialog(
            target = target,
            allCommands = state.commands,
            existingCommand = cmd,
            existingGroup = group,
            onSaveCommand = vm::saveCommand,
            onSaveGroup = vm::saveGroup,
            onDismiss = vm::closeEditor
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
    onGroupDocClick: (String) -> Unit,
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
                // 二级层级：平台 -> 命令（独立卡片）
                section.commands.forEach { cmd ->
                    item(key = "cmd_${cmd.id}") {
                        CommandRow(
                            cmd = cmd,
                            selected = cmd.id == selectedId,
                            docMode = cmd.id == docCmdId,
                            nested = false,
                            onCommandClick = onCommandClick,
                            onDocClick = onDocClick,
                            onDeleteClick = onDeleteClick
                        )
                    }
                }
            } else {
                // 三级层级：平台 -> 命令组卡片 -> 组内命令（嵌套在卡片内）
                section.groups.forEach { gs ->
                    item(key = "group_${gs.group.id}") {
                        GroupCard(
                            group = gs.group,
                            collapsed = gs.collapsed,
                            commandCount = gs.commands.size,
                            onToggle = { onToggleGroup(gs.group.id) },
                            onDocClick = { onGroupDocClick(gs.group.id) },
                            commands = {
                                gs.commands.forEach { cmd ->
                                    CommandRow(
                                        cmd = cmd,
                                        selected = cmd.id == selectedId,
                                        docMode = cmd.id == docCmdId,
                                        nested = true,
                                        onCommandClick = onCommandClick,
                                        onDocClick = onDocClick,
                                        onDeleteClick = onDeleteClick
                                    )
                                }
                            }
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
    nested: Boolean,
    onCommandClick: (String) -> Unit,
    onDocClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    CommandItem(
        cmd = cmd,
        selected = selected || docMode,
        nested = nested,
        onSelect = { onCommandClick(cmd.id) },
        onDocClick = { onDocClick(cmd.id) },
        onDeleteClick = { onDeleteClick(cmd.id) }
    )
}
