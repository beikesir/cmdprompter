package com.example.cmdprompter.data.model

import kotlinx.serialization.Serializable

/**
 * 参数定义。对应模板中的 {{key}} 占位符。
 *
 * @param key     参数标识，对应模板 {{key}}
 * @param label   显示名
 * @param type    "text" | "enum"（1.0 支持）；"bool" 预留
 * @param options type=="enum" 时的候选项
 * @param default 默认值
 * @param hint    输入框占位提示
 */
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
