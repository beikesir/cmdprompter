package com.example.cmdprompter.data

import com.example.cmdprompter.data.model.Command
import com.example.cmdprompter.data.model.CommandGroup
import com.example.cmdprompter.data.model.Workflow

/**
 * 文档生成：把用户手动维护的命令 / 命令组写回 Markdown，
 * 节点 anchor 与 DocParser 的约定保持一致。
 */
object DocWriter {

    fun build(
        title: String,
        commands: List<Command>,
        groups: List<CommandGroup>
    ): String {
        val sb = StringBuilder()
        sb.append("# ").append(title).append("\n\n")
        if (commands.isEmpty() && groups.isEmpty()) {
            sb.append("（暂无手动添加的内容）\n")
            return sb.toString()
        }

        commands.forEach { cmd ->
            sb.append("## `").append(cmd.id).append("` ").append(cmd.name).append("\n\n")
            if (cmd.desc.isNotBlank()) {
                sb.append(cmd.desc).append("\n\n")
            }
            sb.append("**模板**：`").append(cmd.template).append("`\n")
            if (cmd.doc.isNotBlank()) {
                sb.append("\n").append(cmd.doc.trim()).append("\n")
            }
            sb.append("\n")
        }

        groups.forEach { g ->
            sb.append("## `").append(g.id).append("` ").append(g.name).append("\n\n")
            val ids = g.orderedIds()
            if (ids.isNotEmpty()) {
                sb.append("### 执行顺序\n\n")
                ids.forEachIndexed { i, id ->
                    sb.append(i + 1).append(". `").append(id).append("`\n")
                }
                sb.append("\n")
            }
            if (g.doc.isNotBlank()) {
                sb.append(g.doc.trim()).append("\n")
            }
            sb.append("\n")
        }

        return sb.toString().trimEnd() + "\n"
    }

    /** 生成工作流说明文档 */
    fun buildWorkflowDoc(workflows: List<Workflow>): String {
        val sb = StringBuilder()
        sb.append("# 工作流手册\n\n")
        if (workflows.isEmpty()) {
            sb.append("（暂无工作流）\n")
            return sb.toString()
        }
        workflows.forEach { wf ->
            sb.append("## `").append(wf.id).append("` ").append(wf.name).append("\n\n")
            if (wf.desc.isNotBlank()) sb.append(wf.desc).append("\n\n")
            if (wf.note.isNotBlank()) {
                sb.append("**说明**：").append(wf.note).append("\n\n")
            }
            if (wf.sourceGroupName.isNotBlank()) {
                sb.append("**来源命令组**：").append(wf.sourceGroupName).append("\n\n")
            }
            if (wf.steps.isNotEmpty()) {
                sb.append("### 执行步骤\n\n")
                wf.steps.forEachIndexed { i, step ->
                    sb.append(i + 1).append(". `").append(step.preview()).append("`")
                    if (step.name.isNotBlank()) sb.append(" — ").append(step.name)
                    sb.append("\n")
                }
                sb.append("\n")
            }
        }
        return sb.toString().trimEnd() + "\n"
    }
}
