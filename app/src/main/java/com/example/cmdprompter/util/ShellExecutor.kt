package com.example.cmdprompter.util

/**
 * Shell 执行器预留（F11，P2）。
 *
 * 1.0 阶段仅把命令填充到顶部终端预览区并给出提示，不真正执行。
 * 后续可在此接入 Runtime.getRuntime().exec() 或 Termux API。
 */
object ShellExecutor {

    fun isAvailable(): Boolean = false

    /**
     * 预留实现。1.0 不调用。
     */
    @Suppress("unused")
    fun execute(command: String): String {
        return "（预留）终端真实执行将在后续版本接入：$command"
    }
}
