package com.example.cmdprompter.data.model

import kotlinx.serialization.Serializable

/** 内存统一配置模型 */
@Serializable
data class AppConfig(
    val commands: List<Command> = emptyList(),
    val groups: List<CommandGroup> = emptyList()
)

/** 单个工具配置文件（tools 目录下的 JSON） */
@Serializable
data class ToolConfig(
    val platform: String = "",
    val commands: List<Command> = emptyList(),
    val groups: List<CommandGroup> = emptyList()
)

/** manifest.json */
@Serializable
data class Manifest(
    val version: String = "1.0",
    val description: String = "",
    val tools: List<String> = emptyList(),
    val docs: Map<String, String> = emptyMap()
)

/**
 * 合并 JSON 模式（导入 / 导出使用，与原型 localStorage 结构一致）。
 * 命令直接带 platform 与 doc 文本，而非 docRef。
 */
@Serializable
data class MergedConfig(
    val commands: List<Command> = emptyList(),
    val groups: List<CommandGroup> = emptyList(),
    val docs: Map<String, String> = emptyMap()
)
