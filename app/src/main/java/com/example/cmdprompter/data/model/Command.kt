package com.example.cmdprompter.data.model

import kotlinx.serialization.Serializable

/**
 * 一条命令。
 *
 * @param platform 所属平台，多文件体系下由工具文件继承注入
 * @param docRef   指向 manifest.docs 的 key
 * @param doc      运行时装配的详细文档（Markdown 纯文本）
 */
@Serializable
data class Command(
    val id: String = "",
    val name: String = "",
    val desc: String = "",
    val platform: String = "",
    val tags: List<String> = emptyList(),
    val template: String = "",
    val params: List<ParamDef> = emptyList(),
    val docRef: String? = null,
    val doc: String = ""
) {
    /** 搜索用的扁平文本：名称 + 描述 + 标签 */
    fun matches(query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim().lowercase()
        return name.lowercase().contains(q) ||
            desc.lowercase().contains(q) ||
            tags.any { it.lowercase().contains(q) }
    }
}
