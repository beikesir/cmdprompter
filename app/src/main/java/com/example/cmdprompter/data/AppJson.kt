package com.example.cmdprompter.data

import kotlinx.serialization.json.Json

/** 全局 JSON 实例：宽松解析，导入外部配置时不因多字段而失败 */
val appJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
    coerceInputValues = true
    prettyPrint = true
}
