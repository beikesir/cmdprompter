package com.example.cmdprompter.data

import com.example.cmdprompter.data.model.DocNode
import com.example.cmdprompter.data.model.ToolDoc
import java.io.File

/**
 * 文档解析器：把 Markdown 切成"节点"，每个节点用 anchor 绑定到一个命令或命令组 id。
 *
 * 约定格式（多级、易读）：
 *
 * ```
 * # 文档标题                      ← 一级标题：整篇文档标题（可选）
 *
 * ## `command-id` 命令名称        ← 二级标题：节点，反引号内为 anchor
 * 正文…
 * ### 步骤                        ← 三级及以下：节点内部小节
 * 1. …
 * ### 注意
 * - …
 * ```
 *
 * anchor 提取优先级：反引号内容 → 行尾 `{#id}` → 无（该节点不绑定任何命令）。
 */
object DocParser {

    private val HEADING = Regex("^(#{1,6})\\s+(.*)$")
    private val BACKTICK_ID = Regex("`([A-Za-z0-9._\\-]+)`")
    private val BRACE_ID = Regex("\\{#([A-Za-z0-9._\\-]+)\\}\\s*$")

    fun parse(path: String, text: String): ToolDoc {
        val file = File(path)
        val fallbackName = file.nameWithoutExtension
        var docTitle = ""
        val nodes = mutableListOf<DocNode>()

        var currentAnchor = ""
        var currentTitle = ""
        val buffer = mutableListOf<String>()

        fun flush() {
            if (currentAnchor.isEmpty() && currentTitle.isEmpty() && buffer.isEmpty()) return
            nodes.add(
                DocNode(
                    anchor = currentAnchor,
                    title = currentTitle,
                    body = buffer.joinToString("\n").trim(),
                    docName = fallbackName,
                    order = nodes.size
                )
            )
            buffer.clear()
        }

        for (line in text.lineSequence()) {
            val m = HEADING.find(line.trimEnd())
            if (m != null) {
                val level = m.groupValues[1].length
                val raw = m.groupValues[2].trim()
                when {
                    level <= 1 -> {
                        if (docTitle.isEmpty()) docTitle = stripAnchor(raw).first
                        // 一级标题不产生节点；其后的引言忽略
                        buffer.clear()
                    }
                    level == 2 -> {
                        flush()
                        val (title, anchor) = stripAnchor(raw)
                        currentTitle = title
                        currentAnchor = anchor
                    }
                    else -> {
                        // 三级及以下归入当前节点，保留原 Markdown 由渲染层处理
                        if (currentAnchor.isNotEmpty() || currentTitle.isNotEmpty()) {
                            buffer.add(line.trimEnd())
                        }
                    }
                }
            } else {
                if (currentAnchor.isNotEmpty() || currentTitle.isNotEmpty()) {
                    buffer.add(line.trimEnd())
                }
            }
        }
        flush()

        return ToolDoc(
            path = path,
            name = fallbackName,
            title = docTitle.ifBlank { fallbackName },
            fullText = text,
            nodes = nodes.filter { it.anchor.isNotEmpty() }
        )
    }

    /** 从标题文本中拆出「显示标题」与「anchor」 */
    private fun stripAnchor(raw: String): Pair<String, String> {
        val brace = BRACE_ID.find(raw)
        if (brace != null) {
            return raw.replace(brace.value, "").trim() to brace.groupValues[1]
        }
        val tick = BACKTICK_ID.find(raw)
        if (tick != null) {
            val anchor = tick.groupValues[1]
            val title = raw.replace(tick.value, "")
                .replace(Regex("\\s{2,}"), " ")
                .trim(' ', '-', '—', '–', '·')
            return title.ifEmpty { anchor } to anchor
        }
        return raw.trim() to ""
    }
}
