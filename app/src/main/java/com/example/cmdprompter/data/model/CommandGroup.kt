package com.example.cmdprompter.data.model

import kotlinx.serialization.Serializable

/**
 * 命令组：按 order 顺序执行 / 显示一组命令。
 * cmdIds 可跨文件引用其它平台的命令 id。
 */
@Serializable
data class CommandGroup(
    val id: String = "",
    val name: String = "",
    val platform: String = "",
    val tags: List<String> = emptyList(),
    val cmdIds: List<String> = emptyList(),
    val order: List<String> = emptyList()
) {
    /** order 缺省时按 cmdIds 顺序 */
    fun orderedIds(): List<String> = order.ifEmpty { cmdIds }

    fun matches(query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim().lowercase()
        return name.lowercase().contains(q) ||
            tags.any { it.lowercase().contains(q) }
    }
}
