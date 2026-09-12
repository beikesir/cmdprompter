package com.example.cmdprompter.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cmdprompter.data.ConfigRepository
import com.example.cmdprompter.data.LoadResult
import com.example.cmdprompter.data.model.AppConfig
import com.example.cmdprompter.data.model.Command
import com.example.cmdprompter.data.model.CommandGroup
import com.example.cmdprompter.data.model.ConfigSummary
import com.example.cmdprompter.data.model.Origin
import com.example.cmdprompter.data.model.ToolEntry
import com.example.cmdprompter.data.model.ToolInfo
import com.example.cmdprompter.data.model.UserTool
import com.example.cmdprompter.data.model.Workflow
import com.example.cmdprompter.data.model.WorkflowStep
import com.example.cmdprompter.util.TemplateRenderer
import kotlinx.coroutines.Dispatchers
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ViewMode { RIGHT, LEFT, WORKFLOW }

/** 设置页里正在编辑的目标（手动新增 / 修改条目与文档） */
sealed class EditTarget {
    object NewCommand : EditTarget()
    data class Command(val id: String) : EditTarget()
    object NewGroup : EditTarget()
    data class Group(val id: String) : EditTarget()
    /** 从命令组创建工作流 */
    data class NewWorkflowFromGroup(val groupId: String) : EditTarget()
    /** 编辑已有工作流 */
    data class Workflow(val id: String) : EditTarget()
}

/** 右视图：一个命令组 + 展开后的组内命令 */
data class GroupSection(
    val group: CommandGroup,
    val commands: List<Command>,
    val collapsed: Boolean
)

/** 平台分区 */
data class PlatformSection(
    val name: String,
    val commands: List<Command>,      // 左视图：该平台全部命令；右视图恒为空
    val groups: List<GroupSection>,   // 右视图：该平台配置的命令组
    val collapsed: Boolean
)

data class DisplayData(
    val platforms: List<PlatformSection> = emptyList()
)

data class MainUiState(
    val loading: Boolean = true,
    val commands: List<Command> = emptyList(),
    val groups: List<CommandGroup> = emptyList(),
    /** 文档全文：路径 -> 内容（导出归档用） */
    val docFiles: Map<String, String> = emptyMap(),
    /** 设置页展示的工具清单 */
    val tools: List<ToolInfo> = emptyList(),

    /** 导入 / 导出进度 0..1，null 表示不显示 */
    val progress: Float? = null,
    val progressLabel: String = "",
    /** 手动编辑弹窗目标 */
    val editTarget: EditTarget? = null,

    /** 工作流列表 */
    val workflows: List<Workflow> = emptyList(),
    /** 正在编辑的工作流 id（弹窗内编辑，保存后落盘） */
    val editingWorkflow: Workflow? = null,

    /** 配置管理页 */
    val showConfigManager: Boolean = false,
    val configSummary: ConfigSummary = ConfigSummary(),
    val expandedConfigDirs: Set<String> = emptySet(),
    val previewConfigPath: String? = null,
    val previewConfigText: String = "",

    val currentView: ViewMode = ViewMode.RIGHT,
    val collapsedPlatforms: Set<String> = emptySet(),
    val collapsedGroups: Set<String> = emptySet(),

    val selectedCmdId: String? = null,
    val docMode: Boolean = false,
    /** 文档态目标：命令 id（与 docGroupId 互斥） */
    val docCmdId: String? = null,
    /** 文档态目标：命令组 id */
    val docGroupId: String? = null,

    val query: String = "",
    val searchExpanded: Boolean = false,

    val terminalText: String = IDLE_TERMINAL,
    val terminalExpanded: Boolean = false,

    /** key = "${cmdId}\u0000${paramKey}" */
    val paramValues: Map<String, String> = emptyMap(),
    val focusIndex: Int = -1,
    val focusRequestId: Int = 0,

    val showSettings: Boolean = false,
    val pendingDeleteId: String? = null,
    val message: String? = null,
    /** 设置弹窗：粘贴导入的文本 */
    val settingsText: String = "",
    /** 设置弹窗：导出的合并 JSON 文本 */
    val exportText: String = ""
) {
    companion object {
        const val IDLE_TERMINAL = "\$ 等待命令..."
    }
}

class MainViewModel(private val repo: ConfigRepository) : ViewModel() {

    private val _ui = MutableStateFlow(MainUiState())
    val ui: StateFlow<MainUiState> = _ui

    private var commandIndex: Map<String, Command> = emptyMap()
    private var groupIndex: Map<String, CommandGroup> = emptyMap()
    private var platformOrder: List<String> = emptyList()

    val display: StateFlow<DisplayData> = _ui
        .map { buildDisplay(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DisplayData())

    init {
        load()
        loadWorkflows()
    }

    // ------------------------------------------------------------ 加载

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            val result: LoadResult = try {
                repo.load()
            } catch (e: Exception) {
                LoadResult(AppConfig())
            }
            applyResult(result)
        }
    }

    private fun loadWorkflows() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = runCatching { repo.loadWorkflows() }.getOrDefault(emptyList())
            withContext(Dispatchers.Main) { _ui.update { it.copy(workflows = list) } }
        }
    }

    private fun applyResult(result: LoadResult) {
        val commands = result.config.commands
        val groups = result.config.groups
        commandIndex = commands.associateBy { it.id }
        groupIndex = groups.associateBy { it.id }
        platformOrder = orderPlatforms(commands, groups)
        _ui.update {
            it.copy(
                loading = false,
                commands = commands,
                groups = groups,
                docFiles = result.docFiles,
                tools = result.tools,
                selectedCmdId = null,
                docMode = false,
                docCmdId = null,
                docGroupId = null
            )
        }
    }

    private fun reindex() {
        val s = _ui.value
        commandIndex = s.commands.associateBy { it.id }
        groupIndex = s.groups.associateBy { it.id }
        platformOrder = orderPlatforms(s.commands, s.groups)
    }

    /** 平台顺序：「我的命令」置顶，其余按加载顺序 */
    private fun orderPlatforms(commands: List<Command>, groups: List<CommandGroup>): List<String> {
        val list = buildList {
            commands.forEach { if (it.platform.isNotBlank() && it.platform !in this) add(it.platform) }
            groups.forEach { if (it.platform.isNotBlank() && it.platform !in this) add(it.platform) }
        }
        return if (UserTool.PLATFORM in list) {
            listOf(UserTool.PLATFORM) + list.filter { it != UserTool.PLATFORM }
        } else list
    }

    /**
     * 右视图（组视图）只呈现"配置进命令组的命令"；
     * 未编组的命令不在此视图出现，需要查看全部命令请切到左视图（命令）。
     */
    private fun buildDisplay(s: MainUiState): DisplayData {
        if (s.loading) return DisplayData()
        val searching = s.query.isNotBlank()
        val sections = platformOrder.mapNotNull { platform ->
            val commands = s.commands.filter { it.platform == platform }
            val groups = s.groups.filter { it.platform == platform }

            val groupSections = groups.map { g ->
                GroupSection(
                    group = g,
                    commands = g.orderedIds().mapNotNull { commandIndex[it] },
                    collapsed = !searching && s.collapsedGroups.contains(g.id)
                )
            }

            when (s.currentView) {
                ViewMode.LEFT -> {
                    val shown = if (searching) commands.filter { it.matches(s.query) } else commands
                    if (shown.isEmpty()) return@mapNotNull null
                    PlatformSection(
                        name = platform,
                        commands = shown,
                        groups = emptyList(),
                        collapsed = !searching && s.collapsedPlatforms.contains(platform)
                    )
                }
                ViewMode.RIGHT -> {
                    val shownGroups =
                        if (searching) groupSections.filter { it.group.matches(s.query) } else groupSections
                    if (shownGroups.isEmpty()) return@mapNotNull null
                    PlatformSection(
                        name = platform,
                        commands = emptyList(),
                        groups = shownGroups,
                        collapsed = !searching && s.collapsedPlatforms.contains(platform)
                    )
                }
                ViewMode.WORKFLOW -> return@mapNotNull null
            }
        }
        return DisplayData(sections)
    }

    // ------------------------------------------------------------ 交互状态机

    fun setView(mode: ViewMode) {
        _ui.update { it.copy(currentView = mode).cleared() }
    }

    fun togglePlatform(name: String) {
        _ui.update { s ->
            val set = s.collapsedPlatforms.toMutableSet()
            if (!set.add(name)) set.remove(name)
            s.copy(collapsedPlatforms = set)
        }
    }

    fun toggleGroup(id: String) {
        _ui.update { s ->
            val set = s.collapsedGroups.toMutableSet()
            if (!set.add(id)) set.remove(id)
            s.copy(collapsedGroups = set)
        }
    }

    /** 点击命令项：文档态下回到参数编辑，否则选中该命令 */
    fun onCommandClick(id: String) {
        _ui.update { s ->
            s.copy(docMode = false, docCmdId = null, docGroupId = null, selectedCmdId = id, focusIndex = -1)
        }
        ensureParamDefaults(id)
    }

    /** 点击命令的 📄：打开该命令的文档节点 */
    fun onDocClick(id: String) {
        _ui.update { s -> s.copy(docMode = true, docCmdId = id, docGroupId = null, selectedCmdId = null) }
    }

    /** 点击命令组的 📄：打开该命令组的文档节点 */
    fun onGroupDocClick(id: String) {
        _ui.update { s -> s.copy(docMode = true, docGroupId = id, docCmdId = null, selectedCmdId = null) }
    }

    /** 列表滚动：清除选中与文档态 */
    fun onListScroll() {
        val s = _ui.value
        if (s.selectedCmdId != null || s.docMode || s.docCmdId != null || s.docGroupId != null) {
            _ui.update { it.cleared() }
        }
    }

    fun onDeleteRequest(id: String) {
        _ui.update { it.copy(pendingDeleteId = id) }
    }

    fun cancelDelete() {
        _ui.update { it.copy(pendingDeleteId = null) }
    }

    fun confirmDelete() {
        val id = _ui.value.pendingDeleteId ?: return
        val s = _ui.value
        val newCommands = s.commands.filter { it.id != id }
        val newGroups = s.groups
            .map { g -> g.copy(cmdIds = g.cmdIds - id, order = g.order - id) }
            .filter { g -> g.cmdIds.isNotEmpty() }
        _ui.update {
            it.copy(
                commands = newCommands,
                groups = newGroups,
                pendingDeleteId = null,
                message = "已删除命令"
            ).cleared()
        }
        reindex()
        persist()
    }

    // ------------------------------------------------------------ 搜索

    fun toggleSearch() {
        _ui.update { s ->
            val expanded = !s.searchExpanded
            s.copy(
                searchExpanded = expanded,
                query = if (expanded) s.query else "",
            ).cleared()
        }
    }

    fun onQueryChange(q: String) {
        _ui.update { s -> s.copy(query = q).cleared() }
    }

    // ------------------------------------------------------------ 参数与预览

    private fun paramKey(cmdId: String, key: String) = "$cmdId\u0000$key"

    private fun ensureParamDefaults(cmdId: String) {
        val cmd = commandIndex[cmdId] ?: return
        if (cmd.params.isEmpty()) return
        _ui.update { s ->
            val map = s.paramValues.toMutableMap()
            var changed = false
            cmd.params.forEach { p ->
                val k = paramKey(cmdId, p.key)
                if (!map.containsKey(k)) {
                    map[k] = p.default
                    changed = true
                }
            }
            if (changed) s.copy(paramValues = map) else s
        }
    }

    fun paramValue(cmdId: String, key: String, fallback: String = ""): String =
        _ui.value.paramValues[paramKey(cmdId, key)] ?: fallback

    fun onParamChange(cmdId: String, key: String, value: String) {
        _ui.update { s ->
            val map = s.paramValues.toMutableMap()
            map[paramKey(cmdId, key)] = value
            s.copy(paramValues = map)
        }
    }

    /** 当前命令渲染后的完整命令文本 */
    fun previewFor(cmdId: String?): String {
        val id = cmdId ?: return ""
        val cmd = commandIndex[id] ?: return ""
        val values = cmd.params.associate { p -> p.key to paramValue(id, p.key, p.default) }
        return TemplateRenderer.render(cmd.template, values)
    }

    /** 📝 循环聚焦下一个参数 */
    fun focusNextParam(): Int {
        val id = _ui.value.selectedCmdId ?: return -1
        val cmd = commandIndex[id] ?: return -1
        if (cmd.params.isEmpty()) {
            postMessage("该命令没有参数")
            return -1
        }
        val cur = _ui.value.focusIndex
        val next = (cur + 1) % cmd.params.size
        _ui.update { it.copy(focusIndex = next, focusRequestId = it.focusRequestId + 1) }
        return next
    }

    fun onParamFocused(index: Int) {
        if (_ui.value.focusIndex != index) {
            _ui.update { it.copy(focusIndex = index) }
        }
    }

    // ------------------------------------------------------------ 终端 / 执行

    fun toggleTerminal() {
        _ui.update { it.copy(terminalExpanded = !it.terminalExpanded) }
    }

    /** ▶️ 执行（1.0 仅填充 + 提示） */
    fun onExecute() {
        val id = _ui.value.selectedCmdId ?: return
        val cmd = previewFor(id)
        if (cmd.isBlank()) return
        _ui.update {
            it.copy(
                terminalText = "\$ $cmd",
                message = "执行命令（原型）：$cmd"
            )
        }
    }

    // ------------------------------------------------------------ 设置 / 导入导出

    fun openSettings() = _ui.update {
        it.copy(
            showSettings = true,
            settingsText = "",
            exportText = exportJsonText()
        )
    }

    fun closeSettings() = _ui.update { it.copy(showSettings = false) }
    fun onSettingsTextChange(text: String) = _ui.update { it.copy(settingsText = text) }

    fun exportJsonText(): String {
        val s = _ui.value
        return repo.exportJson(
            AppConfig(s.commands, s.groups),
            s.docFiles,
            s.tools.map { ToolEntry(it.id, it.platform, it.commandFiles, it.docFiles, it.builtin) }
        )
    }

    /** 导出文件（供 FileProvider 分享） */
    fun exportFile(): java.io.File {
        val s = _ui.value
        return repo.writeExportJson(
            AppConfig(s.commands, s.groups),
            s.docFiles,
            s.tools.map { ToolEntry(it.id, it.platform, it.commandFiles, it.docFiles, it.builtin) }
        )
    }

    fun commandById(id: String?): Command? = id?.let { commandIndex[it] }
    fun groupById(id: String?): CommandGroup? = id?.let { groupIndex[it] }

    /** 按文档名（不含路径）取全文，用于"查看完整文档" */
    fun fullTextOf(source: String): String {
        if (source.isBlank()) return ""
        val target = if (source.endsWith(".md", ignoreCase = true)) source else "$source.md"
        return _ui.value.docFiles.entries
            .firstOrNull { it.key.substringAfterLast('/').equals(target, ignoreCase = true) }
            ?.value
            .orEmpty()
    }

    fun importFromText(text: String?): Boolean {
        val config = text?.let { repo.parseImportText(it) }
        if (config == null) {
            postMessage("导入失败：不是合法的配置文件")
            return false
        }
        applyResult(LoadResult(config, _ui.value.docFiles))
        persist()
        postMessage("导入成功")
        return true
    }

    // ------------------------------------------------------------ 手动新增 / 编辑

    fun openEditor(target: EditTarget) {
        _ui.update { it.copy(editTarget = target) }
    }

    fun closeEditor() {
        _ui.update { it.copy(editTarget = null, editingWorkflow = null) }
    }

    /** 保存一条命令：新增或覆盖（内置命令被修改后标记为 edited） */
    fun saveCommand(cmd: Command) {
        if (cmd.id.isBlank()) {
            postMessage("命令 id 不能为空")
            return
        }
        val s = _ui.value
        val platform = cmd.platform.ifBlank { UserTool.PLATFORM }
        val origin = if (s.commands.any { it.id == cmd.id }) {
            s.commands.first { it.id == cmd.id }.origin.let {
                if (it == Origin.BUILTIN) Origin.EDITED else it
            }
        } else Origin.USER
        val saved = cmd.copy(
            platform = platform,
            origin = origin,
            modified = true,
            modifiedAt = System.currentTimeMillis()
        )
        val newCommands = buildList {
            add(saved)
            s.commands.forEach { if (it.id != saved.id) add(it) }
        }
        _ui.update {
            it.copy(
                commands = newCommands,
                editTarget = null,
                message = if (origin == Origin.USER) "已新增命令" else "已保存修改"
            )
        }
        reindex()
        persist()
        refreshConfigSummary()
    }

    /** 保存一个命令组 */
    fun saveGroup(group: CommandGroup) {
        if (group.id.isBlank()) {
            postMessage("命令组 id 不能为空")
            return
        }
        val s = _ui.value
        val platform = group.platform.ifBlank { UserTool.PLATFORM }
        val origin = if (s.groups.any { it.id == group.id }) {
            s.groups.first { it.id == group.id }.origin.let {
                if (it == Origin.BUILTIN) Origin.EDITED else it
            }
        } else Origin.USER
        val saved = group.copy(
            platform = platform,
            origin = origin,
            cmdIds = group.orderedIds(),
            order = group.orderedIds(),
            modified = true,
            modifiedAt = System.currentTimeMillis()
        )
        val newGroups = buildList {
            add(saved)
            s.groups.forEach { if (it.id != saved.id) add(it) }
        }
        _ui.update {
            it.copy(
                groups = newGroups,
                editTarget = null,
                message = if (origin == Origin.USER) "已新增命令组" else "已保存修改"
            )
        }
        reindex()
        persist()
        refreshConfigSummary()
    }

    /** 删除手动维护的条目（命令或命令组） */
    fun deleteCustom(id: String) {
        val s = _ui.value
        val isGroup = s.groups.any { it.id == id }
        _ui.update {
            it.copy(
                commands = it.commands.filter { c -> c.id != id },
                groups = it.groups
                    .filter { g -> g.id != id }
                    .map { g -> g.copy(cmdIds = g.cmdIds - id, order = g.order - id) }
                    .filter { g -> g.cmdIds.isNotEmpty() },
                message = if (isGroup) "已删除命令组" else "已删除命令"
            ).cleared()
        }
        reindex()
        persist()
    }

    /** 手动维护的条目（用于在设置页管理、在主界面打标记） */
    fun customItems(): Pair<List<Command>, List<CommandGroup>> =
        _ui.value.commands.filter { it.isCustom } to _ui.value.groups.filter { it.isCustom }

    // ------------------------------------------------------------ 导入 / 导出（带进度）

    fun importFromUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { _ui.update { it.copy(progress = 0f, progressLabel = "准备导入…") } }
            val result = runCatching {
                repo.importFromUris(uris) { f, label ->
                    viewModelScope.launch(Dispatchers.Main) {
                        _ui.update { it.copy(progress = f, progressLabel = label) }
                    }
                }
            }.getOrNull()
            withContext(Dispatchers.Main) {
                _ui.update { it.copy(progress = null, progressLabel = "") }
                applyImported(result)
            }
        }
    }

    fun importFromTree(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { _ui.update { it.copy(progress = 0f, progressLabel = "读取文件夹…") } }
            val result = runCatching {
                repo.importFromTree(uri) { f, label ->
                    viewModelScope.launch(Dispatchers.Main) {
                        _ui.update { it.copy(progress = f, progressLabel = label) }
                    }
                }
            }.getOrNull()
            withContext(Dispatchers.Main) {
                _ui.update { it.copy(progress = null, progressLabel = "") }
                applyImported(result)
            }
        }
    }

    private fun applyImported(result: LoadResult?) {
        if (result == null || result.isEmpty) {
            postMessage("导入失败：没有解析到命令")
            return
        }
        val s = _ui.value
        val commandMap = s.commands.associateBy { it.id }.toMutableMap()
        result.config.commands.forEach { commandMap[it.id] = it }
        val groupMap = s.groups.associateBy { it.id }.toMutableMap()
        result.config.groups.forEach { groupMap[it.id] = it }
        val mergedTools = (s.tools + result.tools).distinctBy { it.id }
        _ui.update {
            it.copy(
                commands = commandMap.values.toList(),
                groups = groupMap.values.toList(),
                docFiles = it.docFiles + result.docFiles,
                tools = mergedTools
            ).cleared()
        }
        reindex()
        persist()
        refreshConfigSummary()
        postMessage("导入成功：${result.config.commands.size} 条命令")
    }

    /** 导出为 zip（多文件体系），返回待分享的文件 */
    fun exportZipFile(): File? {
        return repo.exportZip { _, _ -> }
    }

    /** 导出 zip 并显示进度，完成后回调分享 */
    fun exportZipWithProgress(onDone: (File?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { _ui.update { it.copy(progress = 0f, progressLabel = "准备导出…") } }
            var last = 0f
            val file = repo.exportZip { f, label ->
                if (f - last > 0.02f || f >= 1f) {
                    last = f
                    viewModelScope.launch(Dispatchers.Main) {
                        _ui.update { it.copy(progress = f, progressLabel = label) }
                    }
                }
            }
            withContext(Dispatchers.Main) {
                _ui.update { it.copy(progress = null, progressLabel = "") }
                onDone(file)
            }
        }
    }

    // ------------------------------------------------------------ 工作流

    /**
     * 从命令组创建工作流：**快照**复制组内命令的模板与参数定义，
     * 之后源命令组变化不影响该工作流。
     */
    fun createWorkflowFromGroup(groupId: String) {
        val group = groupIndex[groupId] ?: return
        val now = System.currentTimeMillis()
        val steps = group.orderedIds().mapNotNull { id ->
            commandIndex[id]?.let { cmd ->
                WorkflowStep(
                    id = "step-${System.nanoTime()}-${id.hashCode()}",
                    cmdId = cmd.id,
                    name = cmd.name,
                    template = cmd.template,
                    params = cmd.params,
                    values = cmd.params.associate { p -> p.key to p.default }
                )
            }
        }
        val wf = Workflow(
            id = "wf-${now}",
            name = "${group.name} 工作流",
            desc = group.name,
            sourceGroupId = group.id,
            sourceGroupName = group.name,
            note = "",
            steps = steps,
            createdAt = now,
            updatedAt = now
        )
        _ui.update { it.copy(editTarget = EditTarget.Workflow(wf.id), editingWorkflow = wf) }
    }

    /** 打开已有工作流进行编辑 */
    fun openWorkflow(id: String) {
        val wf = _ui.value.workflows.firstOrNull { it.id == id } ?: return
        _ui.update { it.copy(editTarget = EditTarget.Workflow(id), editingWorkflow = wf) }
    }

    /** 编辑中：修改名称 / 说明 / 描述 */
    fun updateEditingWorkflow(wf: Workflow) {
        _ui.update { it.copy(editingWorkflow = wf.copy(updatedAt = System.currentTimeMillis())) }
    }

    /** 编辑中：新增一个步骤（从已有命令快照） */
    fun addWorkflowStep(cmdId: String) {
        val wf = _ui.value.editingWorkflow ?: return
        val cmd = commandIndex[cmdId] ?: return
        val step = WorkflowStep(
            id = "step-${System.nanoTime()}",
            cmdId = cmd.id,
            name = cmd.name,
            template = cmd.template,
            params = cmd.params,
            values = cmd.params.associate { p -> p.key to p.default }
        )
        updateEditingWorkflow(wf.copy(steps = wf.steps + step))
    }

    /** 编辑中：新增一个空白自定义步骤 */
    fun addBlankWorkflowStep() {
        val wf = _ui.value.editingWorkflow ?: return
        val step = WorkflowStep(id = "step-${System.nanoTime()}", name = "自定义步骤")
        updateEditingWorkflow(wf.copy(steps = wf.steps + step))
    }

    fun removeWorkflowStep(stepId: String) {
        val wf = _ui.value.editingWorkflow ?: return
        updateEditingWorkflow(wf.copy(steps = wf.steps.filter { it.id != stepId }))
    }

    fun moveWorkflowStep(stepId: String, up: Boolean) {
        val wf = _ui.value.editingWorkflow ?: return
        val list = wf.steps.toMutableList()
        val idx = list.indexOfFirst { it.id == stepId }
        if (idx < 0) return
        val target = if (up) idx - 1 else idx + 1
        if (target !in list.indices) return
        val tmp = list[idx]
        list[idx] = list[target]
        list[target] = tmp
        updateEditingWorkflow(wf.copy(steps = list))
    }

    /** 编辑中：修改某步骤的参数值 */
    fun setWorkflowStepValue(stepId: String, key: String, value: String) {
        val wf = _ui.value.editingWorkflow ?: return
        val steps = wf.steps.map { st ->
            if (st.id != stepId) st
            else st.copy(values = st.values.toMutableMap().apply { put(key, value) })
        }
        updateEditingWorkflow(wf.copy(steps = steps))
    }

    /** 编辑中：修改某步骤的模板 / 名称 / 备注 */
    fun updateWorkflowStep(step: WorkflowStep) {
        val wf = _ui.value.editingWorkflow ?: return
        updateEditingWorkflow(
            wf.copy(steps = wf.steps.map { if (it.id == step.id) step else it })
        )
    }

    /** 保存工作流并落盘到独立配置文件 */
    fun saveEditingWorkflow() {
        val wf = _ui.value.editingWorkflow ?: return
        if (wf.name.isBlank()) {
            postMessage("工作流名称不能为空")
            return
        }
        val saved = wf.copy(updatedAt = System.currentTimeMillis())
        val list = buildList {
            add(saved)
            _ui.value.workflows.forEach { if (it.id != saved.id) add(it) }
        }
        _ui.update {
            it.copy(
                workflows = list,
                editingWorkflow = null,
                editTarget = null,
                message = "已保存工作流"
            )
        }
        persistWorkflows(list)
    }

    fun deleteWorkflow(id: String) {
        val list = _ui.value.workflows.filter { it.id != id }
        _ui.update { it.copy(workflows = list, editingWorkflow = null, editTarget = null, message = "已删除工作流") }
        persistWorkflows(list)
    }

    private fun persistWorkflows(list: List<Workflow>) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.saveWorkflows(list) }
        }
    }

    fun workflowById(id: String?): Workflow? = id?.let { wid -> _ui.value.workflows.firstOrNull { it.id == wid } }

    // ------------------------------------------------------------ 配置管理

    fun openConfigManager() {
        viewModelScope.launch(Dispatchers.IO) {
            val summary = runCatching { repo.configSummary() }.getOrDefault(ConfigSummary())
            withContext(Dispatchers.Main) {
                _ui.update {
                    it.copy(
                        showConfigManager = true,
                        configSummary = summary,
                        // 默认展开一级目录
                        expandedConfigDirs = if (it.expandedConfigDirs.isEmpty()) {
                            summary.tree.filter { n -> n.isDirectory }.map { n -> n.path }.toSet()
                        } else it.expandedConfigDirs
                    )
                }
            }
        }
    }

    fun closeConfigManager() {
        _ui.update {
            it.copy(
                showConfigManager = false,
                previewConfigPath = null,
                previewConfigText = ""
            )
        }
    }

    fun toggleConfigDir(path: String) {
        _ui.update { s ->
            val set = s.expandedConfigDirs.toMutableSet()
            if (!set.add(path)) set.remove(path)
            s.copy(expandedConfigDirs = set)
        }
    }

    fun previewConfigFile(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val text = runCatching { repo.readConfigFile(path) }.getOrDefault("")
            withContext(Dispatchers.Main) {
                _ui.update { it.copy(previewConfigPath = path, previewConfigText = text) }
            }
        }
    }

    /** 配置变更后刷新文件树 */
    private fun refreshConfigSummary() {
        if (!_ui.value.showConfigManager) return
        openConfigManager()
    }


    // ------------------------------------------------------------ 工具

    private fun persist() {
        val s = _ui.value
        val tools: List<ToolEntry> = s.tools.map { info ->
            ToolEntry(
                id = info.id,
                platform = info.platform,
                commands = info.commandFiles,
                docs = info.docFiles,
                builtin = info.builtin
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repo.saveUserConfig(AppConfig(s.commands, s.groups), s.docFiles, tools)
                repo.persistCustom(s.commands, s.groups)
            }
        }
    }


    fun postMessage(text: String) {
        _ui.update { it.copy(message = text) }
    }

    fun consumeMessage() {
        if (_ui.value.message != null) _ui.update { it.copy(message = null) }
    }

    /** 清除所有选中 / 文档态 */
    private fun MainUiState.cleared(): MainUiState = copy(
        selectedCmdId = null,
        docMode = false,
        docCmdId = null,
        docGroupId = null,
        focusIndex = -1
    )
}

@Suppress("UNCHECKED_CAST")
class MainViewModelFactory(private val repo: ConfigRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(repo) as T
    }
}
