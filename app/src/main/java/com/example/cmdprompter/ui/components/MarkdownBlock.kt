package com.example.cmdprompter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 轻量 Markdown 渲染（针对命令文档的子集语法）：
 * - ``` 代码块（等宽 + 底色）
 * - ### / #### 子标题
 * - `**粗体**`、`行内 code`
 * - 有序 / 无序列表
 * - 普通段落与空行
 */
@Composable
fun MarkdownBlock(
    text: String,
    modifier: Modifier = Modifier
) {
    if (text.isBlank()) return
    Column(modifier = modifier.fillMaxWidth()) {
        val lines = text.replace("\r\n", "\n").lines()
        var i = 0
        var inCode = false
        val codeBuf = mutableListOf<String>()

        while (i < lines.size) {
            val raw = lines[i]
            val trimmed = raw.trim()

            if (trimmed.startsWith("```")) {
                if (inCode) {
                    CodeBlock(codeBuf.joinToString("\n"))
                    codeBuf.clear()
                    inCode = false
                } else {
                    inCode = true
                }
                i++
                continue
            }
            if (inCode) {
                codeBuf.add(raw)
                i++
                continue
            }

            when {
                trimmed.isEmpty() -> {
                    Spacer(Modifier.height(6.dp))
                    i++
                }
                trimmed.startsWith("####") -> {
                    val content = trimmed.trimStart('#').trim()
                    if (content.isNotEmpty()) {
                        MarkdownLine(content, size = 11.sp, bold = true, topSpace = 6.dp)
                    }
                    i++
                }
                trimmed.startsWith("###") -> {
                    val content = trimmed.trimStart('#').trim()
                    if (content.isNotEmpty()) {
                        MarkdownLine(content, size = 12.sp, bold = true, topSpace = 8.dp, color = Color(0xFF111111))
                    }
                    i++
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    MarkdownLine(trimmed.substring(2), size = 12.sp, bullet = "• ")
                    i++
                }
                Regex("^\\d+[.)]\\s+.*").matches(trimmed) -> {
                    val idx = trimmed.indexOfFirst { it == '.' || it == ')' }
                    MarkdownLine(
                        text = trimmed.substring(idx + 1).trim(),
                        size = 12.sp,
                        bullet = "${trimmed.substring(0, idx)}. "
                    )
                    i++
                }
                trimmed.startsWith("> ") -> {
                    MarkdownLine(trimmed.substring(2), size = 11.sp, color = Color(0xFF666666))
                    i++
                }
                else -> {
                    MarkdownLine(trimmed, size = 12.sp)
                    i++
                }
            }
        }
        if (codeBuf.isNotEmpty()) CodeBlock(codeBuf.joinToString("\n"))
    }
}

@Composable
private fun MarkdownLine(
    text: String,
    size: androidx.compose.ui.unit.TextUnit,
    bold: Boolean = false,
    bullet: String = "",
    topSpace: androidx.compose.ui.unit.Dp = 2.dp,
    color: Color = Color(0xFF333333)
) {
    if (text.isBlank()) return
    Column(modifier = Modifier.padding(top = topSpace, bottom = 1.dp)) {
        Text(
            text = renderInline(bullet + text),
            fontSize = size,
            lineHeight = (size.value * 1.5f).sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = color
        )
    }
}

@Composable
private fun CodeBlock(code: String) {
    if (code.isBlank()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color(0xFFF5F6F8), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = code,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF1F2933)
        )
    }
}

/** 行内标记：**粗体** 与 `code` */
private fun renderInline(text: String): AnnotatedString {
    if (!text.contains('`') && !text.contains("**")) {
        return AnnotatedString(text)
    }
    return buildAnnotatedString {
        val regex = Regex("`([^`]+)`|\\*\\*([^*]+)\\*\\*")
        var cursor = 0
        regex.findAll(text).forEach { m ->
            if (m.range.first > cursor) append(text.substring(cursor, m.range.first))
            if (m.groupValues[1].isNotEmpty()) {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color(0xFFEFF1F4),
                        color = Color(0xFFB4293F)
                    )
                ) {
                    append(m.groupValues[1])
                }
            } else {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(m.groupValues[2])
                }
            }
            cursor = m.range.last + 1
        }
        if (cursor < text.length) append(text.substring(cursor))
    }
}
