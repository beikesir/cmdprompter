package com.example.cmdprompter.data.model

import com.example.cmdprompter.util.TemplateRenderer
import kotlinx.serialization.Serializable

/** 条目来源：区分内置配置与用户手动新增 / 修改的条目 */
object Origin {
    const val BUILTIN = "builtin"
    /** 用户手动新建 */
    const val USER = "user"
    /** 用户在内置条目基础上修改过 */
    const val EDITED = "edited"
}

fun String.isCustomOrigin(): Boolean = this != Origin.BUILTIN

@Serializable
data class ParamDef(
    val key: String = "",
    val label: String = "",
    val type: String = "text",
    val options: List<String> = emptyList(),
    val default: String = "",
    val hint: String = ""
) {
    val displayLabel: String
        get() = label.ifBlank { key }

    val isEnum: Boolean
        get() = type.equals("enum", ignoreCase = true)
}

/** 文档节点：Markdown 中一个 `##` 级小节，anchor 对应命令或命令组 id */
data class DocNode(
    val anchor: String = "",
    val title: String = "",
    val body: String = "",
    val docName: String = "",
    val order: Int = 0
)

/** 一份解析后的文档 */
data class ToolDoc(
    val path: String = "",
    val name: String = "",
    val title: String = "",
    val fullText: String = "",
    val nodes: List<DocNode> = emptyList()
) {
    fun find(anchor: String): DocNode? =
        nodes.firstOrNull { it.anchor.equals(anchor, ignoreCase = true) }
}

@Serializable
data class Command(
    val id: String = "",
    val name: String = "",
    val desc: String = "",
    val platform: String = "",
    val tags: List<String> = emptyList(),
    val template: String = "",
    val params: List<ParamDef> = emptyList(),
    val doc: String = "",
    val docTitle: String = "",
    val docSource: String = "",
    val origin: String = Origin.BUILTIN,
    /** 是否被修改过（1.2 起记录，后续版本用于回写已有配置文件） */
    val modified: Boolean = false,
    /** 最后修改时间（epoch millis，0 表示未修改） */
    val modifiedAt: Long = 0L
) {
    val isCustom: Boolean get() = origin.isCustomOrigin()

    fun matches(query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim().lowercase()
        return name.lowercase().contains(q) ||
            desc.lowercase().contains(q) ||
            tags.any { it.lowercase().contains(q) }
    }
}

@Serializable
data class CommandGroup(
    val id: String = "",
    val name: String = "",
    val platform: String = "",
    val tags: List<String> = emptyList(),
    val cmdIds: List<String> = emptyList(),
    val order: List<String> = emptyList(),
    val doc: String = "",
    val docTitle: String = "",
    val docSource: String = "",
    val origin: String = Origin.BUILTIN,
    /** 是否被修改过（1.2 起记录，后续版本用于回写已有配置文件） */
    val modified: Boolean = false,
    /** 最后修改时间（epoch millis，0 表示未修改） */
    val modifiedAt: Long = 0L
) {
    val isCustom: Boolean get() = origin.isCustomOrigin()

    fun orderedIds(): List<String> = order.ifEmpty { cmdIds }

    fun matches(query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim().lowercase()
        return name.lowercase().contains(q) ||
            tags.any { it.lowercase().contains(q) }
    }
}

/** 内存统一配置模型 */
@Serializable
data class AppConfig(
    val commands: List<Command> = emptyList(),
    val groups: List<CommandGroup> = emptyList()
)

/**
 * 命令配置文件（每个工具 1..n 个）：配置命令与命令组。
 * platform 可省略，省略时继承 manifest 中该工具的 platform。
 */
@Serializable
data class CommandFile(
    val platform: String = "",
    val commands: List<Command> = emptyList(),
    val groups: List<CommandGroup> = emptyList()
)

/** manifest 中的一个工具条目：1..n 个命令配置文件 + 1..n 个文档 */
@Serializable
data class ToolEntry(
    val id: String = "",
    val platform: String = "",
    val commands: List<String> = emptyList(),
    val docs: List<String> = emptyList(),
    val builtin: Boolean = true
)

/** 设置页展示用的工具信息 */
data class ToolInfo(
    val id: String = "",
    val platform: String = "",
    val commandFiles: List<String> = emptyList(),
    val docFiles: List<String> = emptyList(),
    val builtin: Boolean = true
) {
    val displayName: String
        get() = platform.ifBlank { id }
}

/** manifest.json */
@Serializable
data class Manifest(
    val version: String = "1.0",
    val description: String = "",
    val tools: List<ToolEntry> = emptyList()
)

/** 合并 JSON 模式（导入 / 导出使用） */
@Serializable
data class MergedConfig(
    val commands: List<Command> = emptyList(),
    val groups: List<CommandGroup> = emptyList(),
    val docFiles: Map<String, String> = emptyMap(),
    val tools: List<ToolEntry> = emptyList()
)

/** 用户手动维护条目的专用工具标识 */
object UserTool {
    const val ID = "_user"
    const val PLATFORM = "我的命令"
    const val CMD_FILE = "tools/_user/cmds-user.json"
    const val DOC_FILE = "tools/_user/doc-user.md"
}

/**
 * 配置管理页的文件树节点。
 *
 * @param path         相对 config 目录的路径
 * @param name         文件 / 目录名
 * @param isDirectory  是否目录
 * @param size         文件字节数（目录为其中文件合计）
 * @param lastModified 最后修改时间（epoch millis）
 * @param children     子目录内容（文件为空列表）
 */
data class ConfigNode(
    val path: String = "",
    val name: String = "",
    val isDirectory: Boolean = false,
    val size: Long = 0L,
    val lastModified: Long = 0L,
    val children: List<ConfigNode> = emptyList()
) {
    /** 递归统计的文件总数 */
    val fileCount: Int
        get() = if (isDirectory) children.sumOf { if (it.isDirectory) it.fileCount else 1 } else 1
}

/** 配置目录摘要（配置管理页用） */
data class ConfigSummary(
    val tree: List<ConfigNode> = emptyList(),
    val fileCount: Int = 0,
    val totalSize: Long = 0L
)

/**
 * 工作流中的一个步骤：创建时对源命令做**快照**（复制模板与参数定义），
 * 之后源命令变化不影响已有工作流。
 *
 * @param id       步骤唯一 id（同一条命令可在工作流里出现多次）
 * @param cmdId    来源命令 id，仅用于溯源展示；为空表示自定义步骤
 * @param values   参数值：key -> value
 */
@Serializable
data class WorkflowStep(
    val id: String = "",
    val cmdId: String = "",
    val name: String = "",
    val template: String = "",
    val params: List<ParamDef> = emptyList(),
    val values: Map<String, String> = emptyMap(),
    val note: String = ""
) {
    /** 用当前参数值渲染出的命令文本 */
    fun preview(): String = TemplateRenderer.render(template, values)
}

/**
 * 工作流：基于某个命令组创建，可自行排序、增删命令、配置参数与说明。
 * 持久化在独立的 workflows.json 中。
 */
@Serializable
data class Workflow(
    val id: String = "",
    val name: String = "",
    val desc: String = "",
    val sourceGroupId: String = "",
    val sourceGroupName: String = "",
    /** 工作流说明，可随时编辑 */
    val note: String = "",
    val steps: List<WorkflowStep> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

/** 工作流配置文件：config/workflows/workflows.json */
@Serializable
data class WorkflowFile(
    val workflows: List<Workflow> = emptyList()
)
