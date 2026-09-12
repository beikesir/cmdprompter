package com.example.cmdprompter.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.example.cmdprompter.data.model.AppConfig
import com.example.cmdprompter.data.model.Command
import com.example.cmdprompter.data.model.CommandFile
import com.example.cmdprompter.data.model.ConfigNode
import com.example.cmdprompter.data.model.CommandGroup
import com.example.cmdprompter.data.model.ConfigSummary
import com.example.cmdprompter.data.model.Manifest
import com.example.cmdprompter.data.model.MergedConfig
import com.example.cmdprompter.data.model.Origin
import com.example.cmdprompter.data.model.ToolEntry
import com.example.cmdprompter.data.model.ToolDoc
import com.example.cmdprompter.data.model.ToolInfo
import com.example.cmdprompter.data.model.UserTool
import com.example.cmdprompter.data.model.Workflow
import com.example.cmdprompter.data.model.WorkflowFile
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 配置加载结果。
 *
 * @param config    统一内存模型（命令 / 命令组已装配文档正文）
 * @param docFiles  文档全文：相对路径 -> 内容
 * @param tools     工具清单（设置页展示，导入的来源会一并保留）
 */
data class LoadResult(
    val config: AppConfig = AppConfig(),
    val docFiles: Map<String, String> = emptyMap(),
    val tools: List<ToolInfo> = emptyList()
) {
    val isEmpty: Boolean
        get() = config.commands.isEmpty() && config.groups.isEmpty()
}

/**
 * 配置仓库：内置配置复制、多文件体系装配、用户配置读写、
 * 手动条目持久化、导入（文件 / 文件夹 / zip）与导出（zip / json）。
 *
 * 目录约定：
 * - assets/config                    内置只读配置
 * - filesDir/config                  可写副本（首次启动复制）
 * - filesDir/config/user-config.json 整体快照（存在时优先加载）
 * - filesDir/config/tools/_user/     手动新增 / 修改的条目与文档
 */
class ConfigRepository(private val context: Context) {

    private val configDir: File get() = File(context.filesDir, "config")
    private val userFile: File get() = File(configDir, USER_CONFIG)
    private val exportDir: File get() = File(context.cacheDir, "export")

    private val userToolDir: File get() = File(configDir, "tools/${UserTool.ID}")
    private val userCmdFile: File get() = File(configDir, UserTool.CMD_FILE)
    private val userDocFile: File get() = File(configDir, UserTool.DOC_FILE)

    // ---------------------------------------------------------------- 加载

    fun load(): LoadResult {
        copyAssetsIfNeeded()
        if (userFile.exists() && userFile.length() > 0) {
            runCatching {
                val merged = appJson.decodeFromString(MergedConfig.serializer(), userFile.readText())
                if (!merged.commands.isNullOrEmpty() || !merged.groups.isNullOrEmpty()) {
                    return LoadResult(
                        config = AppConfig(merged.commands, merged.groups),
                        docFiles = merged.docFiles,
                        tools = merged.tools.map { e ->
                            ToolInfo(
                                id = e.id,
                                platform = e.platform,
                                commandFiles = e.commands,
                                docFiles = e.docs,
                                builtin = e.builtin
                            )
                        }
                    )
                }
            }
        }
        return loadFromDir(configDir)
    }

    /** 目录装配：优先按 manifest，缺失时按散落文件兜底 */
    fun loadFromDir(base: File): LoadResult = assemble(base, preferManifest = true)

    private fun assemble(base: File, preferManifest: Boolean): LoadResult {
        val manifestFile = File(base, "manifest.json")
        if (preferManifest && manifestFile.exists()) {
            val manifest = runCatching {
                appJson.decodeFromString(Manifest.serializer(), manifestFile.readText())
            }.getOrNull()
            if (manifest != null) return assembleManifest(base, manifest)
        }
        return assembleLoose(base)
    }

    private fun assembleManifest(base: File, manifest: Manifest): LoadResult {
        val commandMap = linkedMapOf<String, Command>()
        val groups = mutableListOf<CommandGroup>()
        val docFiles = linkedMapOf<String, String>()
        val tools = mutableListOf<ToolInfo>()

        // 记录 id -> 所属工具下标，便于只在自己的文档里定位节点
        val cmdOwner = mutableListOf<Pair<Int, String>>()
        val groupOwner = mutableListOf<Pair<Int, Int>>()
        val toolDocs = mutableListOf<List<ToolDoc>>()

        manifest.tools.forEachIndexed { index, entry ->
            val platform = entry.platform.ifBlank { entry.id }
            var cmdCount = 0
            var grpCount = 0

            entry.commands.forEach { rel ->
                val file = File(base, rel)
                if (!file.exists()) return@forEach
                val cf = runCatching {
                    appJson.decodeFromString(CommandFile.serializer(), file.readText())
                }.getOrNull() ?: return@forEach
                cf.commands.forEach { raw ->
                    if (raw.id.isBlank()) return@forEach
                    commandMap[raw.id] = raw.copy(platform = raw.platform.ifBlank { platform })
                    cmdOwner += index to raw.id
                    cmdCount++
                }
                cf.groups.forEach { raw ->
                    if (raw.id.isBlank()) return@forEach
                    groups.add(raw.copy(platform = raw.platform.ifBlank { platform }))
                    groupOwner += index to (groups.size - 1)
                    grpCount++
                }
            }

            val docs = entry.docs.mapNotNull { rel ->
                val file = File(base, rel)
                if (!file.exists()) return@mapNotNull null
                val text = runCatching { file.readText() }.getOrNull() ?: return@mapNotNull null
                docFiles[rel] = text
                DocParser.parse(rel, text)
            }
            toolDocs.add(docs)

            tools.add(
                ToolInfo(
                    id = entry.id,
                    platform = platform,
                    commandFiles = entry.commands,
                    docFiles = entry.docs
                )
            )
        }

        // 手动维护的条目（单独配置文件 + 文档）
        val userIndex = manifest.tools.size
        val userDocs = mutableListOf<ToolDoc>()
        if (userCmdFile.exists()) {
            val cf = runCatching {
                appJson.decodeFromString(CommandFile.serializer(), userCmdFile.readText())
            }.getOrNull()
            if (cf != null) {
                cf.commands.forEach { raw ->
                    if (raw.id.isBlank()) return@forEach
                    commandMap[raw.id] = raw.copy(
                        platform = UserTool.PLATFORM,
                        origin = raw.origin.takeIf { it.isNotBlank() } ?: Origin.USER
                    )
                    cmdOwner += userIndex to raw.id
                }
                cf.groups.forEach { raw ->
                    if (raw.id.isBlank()) return@forEach
                    groups.add(
                        raw.copy(
                            platform = UserTool.PLATFORM,
                            origin = raw.origin.takeIf { it.isNotBlank() } ?: Origin.USER
                        )
                    )
                    groupOwner += userIndex to (groups.size - 1)
                }
            }
        }
        if (userDocFile.exists()) {
            val text = runCatching { userDocFile.readText() }.getOrDefault("")
            docFiles[UserTool.DOC_FILE] = text
            userDocs.add(DocParser.parse(UserTool.DOC_FILE, text))
        }
        toolDocs.add(userDocs)
        val hasUserItems = commandMap.values.any { it.isCustom } || groups.any { it.isCustom }
        if (hasUserItems || userCmdFile.exists()) {
            tools.add(
                ToolInfo(
                    id = UserTool.ID,
                    platform = UserTool.PLATFORM,
                    commandFiles = listOf(UserTool.CMD_FILE),
                    docFiles = if (userDocFile.exists()) listOf(UserTool.DOC_FILE) else emptyList(),
                    builtin = false
                )
            )
        }

        attachDocs(commandMap, groups, cmdOwner, groupOwner, toolDocs)
        return LoadResult(AppConfig(commandMap.values.toList(), groups), docFiles, tools)
    }

    /** 没有 manifest 时：扫描目录里的所有 json / md */
    private fun assembleLoose(base: File): LoadResult {
        val commandMap = linkedMapOf<String, Command>()
        val groups = mutableListOf<CommandGroup>()
        val docFiles = linkedMapOf<String, String>()
        val jsonFiles = mutableListOf<String>()
        val mdFiles = mutableListOf<String>()

        val docs = mutableListOf<ToolDoc>()
        base.walkTopDown().filter { it.isFile }.forEach { file ->
            val rel = file.relativeTo(base).path.replace('\\', '/')
            when {
                file.name.endsWith(".json", true) && file.name != "manifest.json" -> {
                    jsonFiles += rel
                    runCatching {
                        appJson.decodeFromString(CommandFile.serializer(), file.readText())
                    }.getOrNull()?.let { cf ->
                        cf.commands.forEach { raw ->
                            if (raw.id.isNotBlank()) {
                                commandMap[raw.id] = raw.copy(platform = raw.platform.ifBlank { cf.platform })
                            }
                        }
                        cf.groups.forEach { raw ->
                            if (raw.id.isNotBlank()) groups.add(raw.copy(platform = raw.platform.ifBlank { cf.platform }))
                        }
                    } ?: runCatching {
                        appJson.decodeFromString(MergedConfig.serializer(), file.readText())
                    }.getOrNull()?.let { merged ->
                        merged.commands.forEach { raw ->
                            if (raw.id.isNotBlank()) commandMap[raw.id] = raw
                        }
                        groups.addAll(merged.groups.filter { it.id.isNotBlank() })
                    }
                }
                file.name.endsWith(".md", true) -> {
                    mdFiles += rel
                    val text = runCatching { file.readText() }.getOrNull() ?: return@forEach
                    docFiles[rel] = text
                    docs.add(DocParser.parse(rel, text))
                }
            }
        }

        // 文档节点按 id 匹配（不区分所属工具）
        commandMap.keys.toList().forEach { id ->
            val node = docs.firstNotNullOfOrNull { it.find(id) } ?: return@forEach
            commandMap[id] = commandMap[id]!!.copy(
                doc = node.body,
                docTitle = node.title,
                docSource = node.docName
            )
        }
        groups.indices.forEach { i ->
            val node = docs.firstNotNullOfOrNull { it.find(groups[i].id) } ?: return@forEach
            groups[i] = groups[i].copy(doc = node.body, docTitle = node.title, docSource = node.docName)
        }

        val platform = commandMap.values.firstOrNull()?.platform.orEmpty()
        val tools = if (commandMap.isEmpty() && groups.isEmpty()) {
            emptyList()
        } else {
            listOf(
                ToolInfo(
                    id = base.name.ifBlank { "imported" },
                    platform = platform,
                    commandFiles = jsonFiles,
                    docFiles = mdFiles,
                    builtin = false
                )
            )
        }
        return LoadResult(AppConfig(commandMap.values.toList(), groups), docFiles, tools)
    }

    private fun attachDocs(
        commandMap: LinkedHashMap<String, Command>,
        groups: MutableList<CommandGroup>,
        cmdOwner: List<Pair<Int, String>>,
        groupOwner: List<Pair<Int, Int>>,
        toolDocs: List<List<ToolDoc>>
    ) {
        cmdOwner.forEach { (toolIndex, id) ->
            val node = toolDocs.getOrNull(toolIndex)?.firstNotNullOfOrNull { it.find(id) } ?: return@forEach
            val cmd = commandMap[id] ?: return@forEach
            commandMap[id] = cmd.copy(
                doc = node.body,
                docTitle = node.title.ifBlank { cmd.name },
                docSource = node.docName
            )
        }
        groupOwner.forEach { (toolIndex, index) ->
            val group = groups.getOrNull(index) ?: return@forEach
            val node = toolDocs.getOrNull(toolIndex)?.firstNotNullOfOrNull { it.find(group.id) } ?: return@forEach
            groups[index] = group.copy(
                doc = node.body,
                docTitle = node.title.ifBlank { group.name },
                docSource = node.docName
            )
        }
    }

    // ---------------------------------------------------------------- 保存

    fun saveUserConfig(config: AppConfig, docFiles: Map<String, String>, tools: List<ToolEntry>) {
        val merged = MergedConfig(config.commands, config.groups, docFiles, tools)
        configDir.mkdirs()
        userFile.writeText(appJson.encodeToString(MergedConfig.serializer(), merged))
    }

    /** 把手动新增 / 修改过的条目写入独立配置文件与文档 */
    fun persistCustom(commands: List<Command>, groups: List<CommandGroup>) {
        val customCommands = commands.filter { it.isCustom }
        val customGroups = groups.filter { it.isCustom }
        if (customCommands.isEmpty() && customGroups.isEmpty()) {
            if (userCmdFile.exists()) userCmdFile.delete()
            if (userDocFile.exists()) userDocFile.delete()
            return
        }
        userToolDir.mkdirs()
        val cf = CommandFile(
            platform = UserTool.PLATFORM,
            commands = customCommands.map { it.copy(platform = UserTool.PLATFORM) },
            groups = customGroups.map { it.copy(platform = UserTool.PLATFORM) }
        )
        userCmdFile.writeText(appJson.encodeToString(CommandFile.serializer(), cf))
        userDocFile.writeText(
            DocWriter.build("我的命令", customCommands, customGroups)
        )
    }

    fun resetToBuiltIn() {
        if (userFile.exists()) userFile.delete()
        if (userCmdFile.exists()) userCmdFile.delete()
        if (userDocFile.exists()) userDocFile.delete()
        copyAssetsIfNeeded(force = true)
    }

    // ---------------------------------------------------------------- 资产

    fun copyAssetsIfNeeded(force: Boolean = false) {
        val manifestFile = File(configDir, "manifest.json")
        if (!force && manifestFile.exists()) return
        runCatching { copyAssetRec("config", configDir) }
    }

    private fun copyAssetRec(assetPath: String, dest: File) {
        val names = context.assets.list(assetPath)
        if (names.isNullOrEmpty()) {
            dest.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
        } else {
            dest.mkdirs()
            names.forEach { name ->
                copyAssetRec(if (assetPath.isEmpty()) name else "$assetPath/$name", File(dest, name))
            }
        }
    }

    // ---------------------------------------------------------------- 导入

    /**
     * 导入一个或多个文件（json / md / zip）。
     * @param onProgress (0..1, 当前文件名)
     */
    fun importFromUris(
        uris: List<Uri>,
        onProgress: (Float, String) -> Unit
    ): LoadResult? {
        if (uris.isEmpty()) return null
        val tempRoot = File(context.cacheDir, "import-${System.currentTimeMillis()}")
        tempRoot.mkdirs()
        return try {
            val result = runCatching {
                uris.forEachIndexed { i, uri ->
                    onProgress(i.toFloat() / uris.size, displayName(uri))
                    copyUriInto(uri, tempRoot)
                }
                onProgress(0.9f, "解析配置…")
                val parsed = parseImportDir(tempRoot)
                onProgress(1f, "完成")
                parsed
            }.getOrNull()
            result?.takeIf { !it.isEmpty }
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    /** 导入整个文件夹（SAF 目录树） */
    fun importFromTree(
        uri: Uri,
        onProgress: (Float, String) -> Unit
    ): LoadResult? {
        val tempRoot = File(context.cacheDir, "import-tree-${System.currentTimeMillis()}")
        tempRoot.mkdirs()
        return try {
            val result = runCatching {
                val root = DocumentFile.fromTreeUri(context, uri) ?: return@runCatching null
                onProgress(0.05f, "读取目录…")
                copyTree(root, tempRoot, onProgress)
                onProgress(0.9f, "解析配置…")
                val parsed = parseImportDir(tempRoot)
                onProgress(1f, "完成")
                parsed
            }.getOrNull()
            result?.takeIf { !it.isEmpty }
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    private fun parseImportDir(dir: File): LoadResult {
        val manifestRoot = findManifestRoot(dir)
        if (File(manifestRoot, "manifest.json").exists()) return assemble(manifestRoot, preferManifest = true)
        // 目录里可能有 zip，先展开
        dir.walkTopDown().filter { it.isFile && it.name.endsWith(".zip", true) }.forEach { zip ->
            runCatching {
                val target = File(dir, "unzipped-${zip.nameWithoutExtension}")
                target.mkdirs()
                unzip(zip, target)
            }
        }
        return assemble(dir, preferManifest = false)
    }

    private fun copyUriInto(uri: Uri, destDir: File) {
        val name = displayName(uri).ifBlank { "import-${System.currentTimeMillis()}" }
        val safe = name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val out = File(destDir, safe)
        context.contentResolver.openInputStream(uri)?.use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
        } ?: return
        if (out.name.endsWith(".zip", true)) {
            runCatching { unzip(out, File(destDir, "unzipped-${out.nameWithoutExtension}")) }
        }
    }

    private fun copyTree(dir: DocumentFile, dest: File, onProgress: (Float, String) -> Unit) {
        dest.mkdirs()
        val files = dir.listFiles()
        var done = 0
        files.forEach { child ->
            done++
            onProgress(0.05f + 0.8f * done / (files.size + 1), child.name ?: "")
            if (child.isDirectory) {
                copyTree(child, File(dest, child.name ?: "dir"), onProgress)
            } else if (child.isFile) {
                val out = File(dest, child.name ?: "file")
                runCatching {
                    context.contentResolver.openInputStream(child.uri)?.use { input ->
                        out.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            }
        }
    }

    private fun unzip(zip: File, dest: File) {
        dest.mkdirs()
        ZipInputStream(zip.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val out = File(dest, entry.name)
                if (entry.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    out.outputStream().use { zis.copyTo(it) }
                }
                entry = zis.nextEntry
            }
        }
    }

    private fun findManifestRoot(dir: File): File {
        if (File(dir, "manifest.json").exists()) return dir
        dir.listFiles()?.forEach { child ->
            if (child.isDirectory) {
                val found = findManifestRoot(child)
                if (File(found, "manifest.json").exists()) return found
            }
        }
        return dir
    }

    fun displayName(uri: Uri): String {
        return runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
            }
        }.getOrNull() ?: uri.lastPathSegment.orEmpty()
    }

    fun parseImportText(text: String): AppConfig? {
        if (text.isBlank()) return null
        runCatching {
            val merged = appJson.decodeFromString(MergedConfig.serializer(), text)
            if (!merged.commands.isNullOrEmpty()) return AppConfig(merged.commands, merged.groups)
        }
        runCatching {
            val cf = appJson.decodeFromString(CommandFile.serializer(), text)
            if (!cf.commands.isNullOrEmpty()) return AppConfig(cf.commands, cf.groups)
        }
        return null
    }

    // ---------------------------------------------------------------- 导出

    fun exportJson(config: AppConfig, docFiles: Map<String, String>, tools: List<ToolEntry>): String {
        val merged = MergedConfig(config.commands, config.groups, docFiles, tools)
        return appJson.encodeToString(MergedConfig.serializer(), merged)
    }

    fun writeExportJson(
        config: AppConfig,
        docFiles: Map<String, String>,
        tools: List<ToolEntry>
    ): File {
        exportDir.mkdirs()
        val file = File(exportDir, "cmdprompter-config.json")
        file.writeText(exportJson(config, docFiles, tools))
        return file
    }

    /** 导出完整配置目录为 zip（多文件体系），带进度回调 */
    fun exportZip(onProgress: (Float, String) -> Unit): File? {
        exportDir.mkdirs()
        val out = File(exportDir, "cmdprompter-config.zip")
        if (out.exists()) out.delete()
        val files = configDir.walkTopDown().filter { it.isFile }.toList()
        if (files.isEmpty()) return null
        return try {
            ZipOutputStream(out.outputStream()).use { zos ->
                files.forEachIndexed { i, file ->
                    onProgress(i.toFloat() / files.size, file.name)
                    val name = file.relativeTo(configDir).path.replace('\\', '/')
                    zos.putNextEntry(ZipEntry(name))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
            onProgress(1f, "完成")
            out
        } catch (e: Exception) {
            null
        }
    }

    // ---------------------------------------------------------------- 配置管理

    /** 递归构建配置目录文件树（相对 config 目录） */
    fun listConfigTree(): List<ConfigNode> {
        val root = configDir
        if (!root.exists()) return emptyList()
        return buildNode(root, "")
    }

    private fun buildNode(dir: File, prefix: String): List<ConfigNode> {
        val nodes = mutableListOf<ConfigNode>()
        val files = dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: return nodes
        files.forEach { f ->
            val rel = if (prefix.isEmpty()) f.name else "$prefix/${f.name}"
            if (f.isDirectory) {
                val children = buildNode(f, rel)
                nodes.add(
                    ConfigNode(
                        path = rel,
                        name = f.name,
                        isDirectory = true,
                        size = children.sumOf { it.size },
                        lastModified = f.lastModified(),
                        children = children
                    )
                )
            } else {
                nodes.add(
                    ConfigNode(
                        path = rel,
                        name = f.name,
                        isDirectory = false,
                        size = f.length(),
                        lastModified = f.lastModified()
                    )
                )
            }
        }
        return nodes
    }

    /** 读取某个配置文件的文本内容（配置管理页预览用） */
    fun readConfigFile(path: String, maxChars: Int = 4000): String {
        val file = File(configDir, path)
        if (!file.exists() || !file.isFile) return ""
        return runCatching { file.readText() }.getOrDefault("").let {
            if (it.length > maxChars) it.take(maxChars) + "\n…（已截断）" else it
        }
    }

    /** 配置目录摘要：文件树 + 统计 */
    fun configSummary(): ConfigSummary {
        val tree = listConfigTree()
        fun count(nodes: List<ConfigNode>): Pair<Int, Long> {
            var files = 0
            var size = 0L
            nodes.forEach { n ->
                if (n.isDirectory) {
                    val (f, s) = count(n.children)
                    files += f
                    size += s
                } else {
                    files++
                    size += n.size
                }
            }
            return files to size
        }
        val (files, size) = count(tree)
        return ConfigSummary(tree = tree, fileCount = files, totalSize = size)
    }

    // ---------------------------------------------------------------- 工作流

    /** 读取工作流配置文件（config/workflows/workflows.json） */
    fun loadWorkflows(): List<Workflow> {
        val file = File(configDir, WORKFLOW_FILE)
        if (!file.exists()) return emptyList()
        return runCatching {
            appJson.decodeFromString(WorkflowFile.serializer(), file.readText()).workflows
        }.getOrDefault(emptyList())
    }

    /** 写入工作流配置文件，同时生成配套说明文档 */
    fun saveWorkflows(workflows: List<Workflow>) {
        val dir = File(configDir, WORKFLOW_DIR)
        dir.mkdirs()
        val file = File(configDir, WORKFLOW_FILE)
        if (workflows.isEmpty()) {
            if (file.exists()) file.delete()
            File(configDir, WORKFLOW_DOC).let { if (it.exists()) it.delete() }
            return
        }
        file.writeText(
            appJson.encodeToString(WorkflowFile.serializer(), WorkflowFile(workflows))
        )
        File(configDir, WORKFLOW_DOC).writeText(DocWriter.buildWorkflowDoc(workflows))
    }

    companion object {
        const val USER_CONFIG = "user-config.json"
        const val WORKFLOW_DIR = "workflows"
        const val WORKFLOW_FILE = "workflows/workflows.json"
        const val WORKFLOW_DOC = "workflows/workflow-docs.md"
    }
}
