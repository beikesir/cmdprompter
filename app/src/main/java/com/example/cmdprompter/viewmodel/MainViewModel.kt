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
import com.example.cmdprompter.util.TemplateRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ViewMode { RIGHT, LEFT }

/** 右视图：组 + 组内命令（展开态由 UI 折叠状态决定） */
data class GroupSection(
    val group: CommandGroup,
    val commands: List<Command>,
    val collapsed: Boolean
)

/** 平台分区 */
data class PlatformSection(
    val name: String,
    val commands: List<Command>,          // 左视图全部命令 / 右视图"未分组"命令
    val groups: List<GroupSection>,       // 仅右视图
    val collapsed: Boolean
)

data class DisplayData(
    val platforms: List<PlatformSection> = emptyList()
)

data class MainUiState(
    val loading: Boolean = true,
    val commands: List<Command> = emptyList(),
    val groups: List<CommandGroup> = emptyList(),
    val docs: Map<String, String> = emptyMap(),

    val currentView: ViewMode = ViewMode.RIGHT,
    val collapsedPlatforms: Set<String> = emptySet(),
    val collapsedGroups: Set<String> = emptySet(),

    val selectedCmdId: String? = null,
    val docMode: Boolean = false,
    val docCmdId: String? = null,

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

    /** 命令索引：id -> Command */
    private var commandIndex: Map<String, Command> = emptyMap()
    /** 平台展示顺序（与工具文件加载顺序一致） */
    private var platformOrder: List<String> = emptyList()

    /** 派生渲染数据：已应用视图模式、折叠、搜索过滤 */
    val display: StateFlow<DisplayData> = _ui
        .map { buildDisplay(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DisplayData())

    init {
        load()
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

    private fun applyResult(result: LoadResult) {
        val commands = result.config.commands
        val groups = result.config.groups
        commandIndex = commands.associateBy { it.id }
        platformOrder = buildList {
            commands.forEach { if (it.platform.isNotBlank() && it.platform !in this) add(it.platform) }
            groups.forEach { if (it.platform.isNotBlank() && it.platform !in this) add(it.platform) }
        }
        _ui.update {
            it.copy(
                loading = false,
                commands = commands,
                groups = groups,
                docs = result.docs,
                selectedCmdId = null,
                docMode = false,
                docCmdId = null
            )
        }
    }

    private fun reindex() {
        val s = _ui.value
        commandIndex = s.commands.associateBy { it.id }
        platformOrder = buildList {
            s.commands.forEach { if (it.platform.isNotBlank() && it.platform !in this) add(it.platform) }
            s.groups.forEach { if (it.platform.isNotBlank() && it.platform !in this) add(it.platform) }
        }
    }

    private fun buildDisplay(s: MainUiState): DisplayData {
        if (s.loading) return DisplayData()
        val searching = s.query.isNotBlank()
        val sections = platformOrder.mapNotNull { platform ->
            val commands = s.commands.filter { it.platform == platform }
            val groups = s.groups.filter { it.platform == platform }
            val groupedIds = groups.flatMap { it.orderedIds() }.toSet()
            val looseCommands = commands.filter { it.id !in groupedIds }

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
                    val shownGroups = if (searching) groupSections.filter { it.group.matches(s.query) } else groupSections
                    val shownLoose = if (searching) looseCommands.filter { it.matches(s.query) } else looseCommands
                    if (shownGroups.isEmpty() && shownLoose.isEmpty()) return@mapNotNull null
                    PlatformSection(
                        name = platform,
                        commands = shownLoose,
                        groups = shownGroups,
                        collapsed = !searching && s.collapsedPlatforms.contains(platform)
                    )
                }
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

    /** 点击命令项：按 §5.8 状态机处理 */
    fun onCommandClick(id: String) {
        _ui.update { s ->
            if (s.docMode) {
                // 文档态下点击任意命令 → 回到参数编辑
                s.copy(docMode = false, docCmdId = null, selectedCmdId = id, focusIndex = -1)
            } else {
                // 已选中同一命令：保持不变（仍选中）
                s.copy(selectedCmdId = id, docMode = false, docCmdId = null)
            }
        }
        ensureParamDefaults(id)
    }

    /** 点击 📄：进入文档态 */
    fun onDocClick(id: String) {
        _ui.update { s -> s.copy(docMode = true, docCmdId = id, selectedCmdId = null) }
    }

    /** 列表滚动：清除选中与文档态 */
    fun onListScroll() {
        val s = _ui.value
        if (s.selectedCmdId != null || s.docMode || s.docCmdId != null) {
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

    /** 📝 循环聚焦下一个参数，返回需要聚焦的参数下标 */
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
        return repo.exportJson(AppConfig(s.commands, s.groups), s.docs)
    }

    /** 导出文件（供 FileProvider 分享） */
    fun exportFile(): java.io.File {
        val s = _ui.value
        return repo.writeExportFile(AppConfig(s.commands, s.groups), s.docs)
    }

    fun commandById(id: String?): Command? = id?.let { commandIndex[it] }

    fun importFromText(text: String?): Boolean {
        val config = text?.let { repo.parseImportText(it) }
        if (config == null) {
            postMessage("导入失败：不是合法的配置文件")
            return false
        }
        applyResult(LoadResult(config, _ui.value.docs))
        persist()
        postMessage("导入成功")
        return true
    }

    fun importFromUri(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch(Dispatchers.IO) {
            val config = try {
                repo.importFromUri(uri)
            } catch (e: Exception) {
                null
            }
            withContext(Dispatchers.Main) {
                if (config == null) {
                    postMessage("导入失败：无法解析该文件")
                } else {
                    applyResult(LoadResult(config, _ui.value.docs))
                    persist()
                    postMessage("导入成功")
                }
            }
        }
    }

    fun resetBuiltIn() {
        viewModelScope.launch(Dispatchers.IO) {
            repo.resetToBuiltIn()
            val result = repo.load()
            applyResult(result)
            withContext(Dispatchers.Main) { postMessage("已恢复内置配置") }
        }
    }

    // ------------------------------------------------------------ 工具

    private fun persist() {
        val s = _ui.value
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.saveUserConfig(AppConfig(s.commands, s.groups), s.docs) }
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
        focusIndex = -1
    )
}

@Suppress("UNCHECKED_CAST")
class MainViewModelFactory(private val repo: ConfigRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(repo) as T
    }
}
