package com.example.cmdprompter.util

/**
 * 命令模板渲染：将 {{key}} / {{ key }} 占位符替换为参数值。
 */
object TemplateRenderer {

    /**
     * @param template 命令模板
     * @param values   key -> 当前值
     * @return 渲染后的完整命令；未提供的参数保留原占位符
     */
    fun render(template: String, values: Map<String, String>): String {
        if (values.isEmpty()) return template
        var out = template
        values.forEach { (key, value) ->
            if (key.isBlank()) return@forEach
            val regex = Regex("\\{\\{\\s*${Regex.escape(key)}\\s*\\}\\}")
            // 使用 transform 重载，避免 replacement 中的 $ / \ 被转义
            out = out.replace(regex) { value }
        }
        return out
    }

    /** 提取模板中出现的全部参数 key（按出现顺序，去重） */
    fun extractKeys(template: String): List<String> {
        val regex = Regex("\\{\\{\\s*([^}]+?)\\s*\\}\\}")
        return regex.findAll(template)
            .map { it.groupValues[1].trim() }
            .distinct()
            .toList()
    }
}
