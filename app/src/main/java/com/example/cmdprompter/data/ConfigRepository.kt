package com.example.cmdprompter.data

import android.content.Context
import android.net.Uri
import com.example.cmdprompter.data.model.AppConfig
import com.example.cmdprompter.data.model.Command
import com.example.cmdprompter.data.model.CommandGroup
import com.example.cmdprompter.data.model.Manifest
import com.example.cmdprompter.data.model.MergedConfig
import com.example.cmdprompter.data.model.ToolConfig
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * 配置加载结果。
 *
 * @param config 统一的内存模型
 * @param docs   docKey -> 文档全文（导出时需要回写）
 */
data class LoadResult(
    val config: AppConfig,
    val docs: Map<String, String> = emptyMap()
)

/**
 * 配置仓库：负责内置配置复制、多文件体系装配、用户配置读写、导入导出。
 *
 * 目录约定：
 * - assets/config/      内置只读配置（manifest.json + tools/ + docs/）
 * - filesDir/config/    可写副本，首次启动复制
 * - filesDir/config/user-config.json  用户导入 / 删除命令后的整体快照，存在时优先加载
 */
class ConfigRepository(private val context: Context) {

    private val configDir: File get() = File(context.filesDir, "config")
    private val userFile: File get() = File(configDir, USER_CONFIG)
    private val cacheDir: File get() = File(context.cacheDir, "export")

    // ---------------------------------------------------------------- 加载

    /** 读取当前生效配置 */
    fun load(): LoadResult {
        copyAssetsIfNeeded()
        // 用户配置优先（整体替换策略）
        if (userFile.exists() && userFile.length() > 0) {
            runCatching {
                val merged = appJson.decodeFromString(MergedConfig.serializer(), userFile.readText())
                if (merged.commands.isNotEmpty() || merged.groups.isNotEmpty()) {
                    return LoadResult(
                        config = AppConfig(merged.commands, merged.groups),
                        docs = merged.docs
                    )
                }
            }
        }
        return loadFromDir(configDir)
    }

    /** 从多文件体系目录装配配置 */
    fun loadFromDir(base: File): LoadResult {
        val manifestFile = File(base, "manifest.json")
        if (!manifestFile.exists()) return LoadResult(AppConfig())

        val manifest = runCatching {
            appJson.decodeFromString(Manifest.serializer(), manifestFile.readText())
        }.getOrNull() ?: return LoadResult(AppConfig())

        // 1) 文档：key -> 全文
        val docs = linkedMapOf<String, String>()
        manifest.docs.forEach { (key, relPath) ->
            docs[key] = runCatching { File(base, relPath).readText() }.getOrDefault("")
        }

        // 2) 工具文件：注入 platform + 装配 doc
        val commandMap = linkedMapOf<String, Command>()
        val groups = mutableListOf<CommandGroup>()

        manifest.tools.forEach { rel ->
            val file = File(base, rel)
            if (!file.exists()) return@forEach
            val tool = runCatching {
                appJson.decodeFromString(ToolConfig.serializer(), file.readText())
            }.getOrNull() ?: return@forEach

            tool.commands.forEach { raw ->
                val docText = raw.doc.ifBlank { raw.docRef?.let { docs[it] }.orEmpty() }
                val cmd = raw.copy(
                    platform = raw.platform.ifBlank { tool.platform },
                    doc = docText
                )
                if (cmd.id.isBlank()) return@forEach
                commandMap[cmd.id] = cmd   // id 重复：后加载覆盖先加载
            }

            tool.groups.forEach { raw ->
                if (raw.id.isBlank()) return@forEach
                groups.add(raw.copy(platform = raw.platform.ifBlank { tool.platform }))
            }
        }

        return LoadResult(AppConfig(commandMap.values.toList(), groups), docs)
    }

    // ---------------------------------------------------------------- 保存

    /** 将当前配置整体写回用户配置文件（删除命令、导入后调用） */
    fun saveUserConfig(config: AppConfig, docs: Map<String, String>) {
        val merged = MergedConfig(config.commands, config.groups, docs)
        configDir.mkdirs()
        userFile.writeText(appJson.encodeToString(MergedConfig.serializer(), merged))
    }

    /** 丢弃用户配置，恢复内置配置 */
    fun resetToBuiltIn() {
        if (userFile.exists()) userFile.delete()
        copyAssetsIfNeeded(force = true)
    }

    // ---------------------------------------------------------------- 资产

    fun copyAssetsIfNeeded(force: Boolean = false) {
        val manifestFile = File(configDir, "manifest.json")
        if (!force && manifestFile.exists()) return
        runCatching { copyAssetRec("config", configDir) }
    }

    private fun copyAssetRec(assetPath: String, dest: File) {
        val names = context.assets.list(assetPath)
        if (names.isNullOrEmpty()) {
            dest.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
        } else {
            dest.mkdirs()
            names.forEach { name ->
                copyAssetRec(if (assetPath.isEmpty()) name else "$assetPath/$name", File(dest, name))
            }
        }
    }

    // ---------------------------------------------------------------- 导出

    /** 生成合并 JSON 文本 */
    fun exportJson(config: AppConfig, docs: Map<String, String>): String {
        val merged = MergedConfig(config.commands, config.groups, docs)
        return appJson.encodeToString(MergedConfig.serializer(), merged)
    }

    /** 写入 cacheDir/export/ 并返回文件，供 FileProvider 分享 */
    fun writeExportFile(config: AppConfig, docs: Map<String, String>): File {
        cacheDir.mkdirs()
        val file = File(cacheDir, "cmdprompter-config.json")
        file.writeText(exportJson(config, docs))
        return file
    }

    // ---------------------------------------------------------------- 导入

    /**
     * 解析导入的 JSON 文本。
     * 依次尝试：合并 JSON（含 commands）→ 单工具文件（含 platform + commands）→ manifest 结构。
     */
    fun parseImportText(text: String): AppConfig? {
        if (text.isBlank()) return null

        runCatching {
            val merged = appJson.decodeFromString(MergedConfig.serializer(), text)
            if (merged.commands.isNotEmpty()) {
                return AppConfig(merged.commands, merged.groups)
            }
        }

        runCatching {
            val tool = appJson.decodeFromString(ToolConfig.serializer(), text)
            if (tool.commands.isNotEmpty()) {
                val cmds = tool.commands.map { it.copy(platform = it.platform.ifBlank { tool.platform }) }
                val groups = tool.groups.map { it.copy(platform = it.platform.ifBlank { tool.platform }) }
                return AppConfig(cmds, groups)
            }
        }

        runCatching {
            val app = appJson.decodeFromString(AppConfig.serializer(), text)
            if (app.commands.isNotEmpty()) return app
        }

        return null
    }

    /** 从 Uri（.json 文本 / .zip 多文件体系）导入 */
    fun importFromUri(uri: Uri): AppConfig? {
        val name = uri.lastPathSegment.orEmpty()
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull() ?: return null

        if (name.endsWith(".zip", ignoreCase = true)) {
            return importFromZipBytes(text)
        }

        // 先按文本解析
        parseImportText(String(text))?.let { return it }

        // 再尝试按 zip 解析（无扩展名时）
        return importFromZipBytes(text)
    }

    private fun importFromZipBytes(bytes: ByteArray): AppConfig? {
        return runCatching {
            val tempDir = File(context.cacheDir, "import-${System.currentTimeMillis()}")
            tempDir.mkdirs()
            ZipInputStream(bytes.inputStream()).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val outFile = File(tempDir, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { zis.copyTo(it) }
                    }
                    entry = zis.nextEntry
                }
            }
            val result = loadFromDir(findManifestRoot(tempDir))
            tempDir.deleteRecursively()
            result.config
        }.getOrNull()
    }

    /** zip 里可能多包一层目录，向上/向下找到 manifest.json 所在目录 */
    private fun findManifestRoot(dir: File): File {
        if (File(dir, "manifest.json").exists()) return dir
        dir.listFiles()?.forEach { child ->
            if (child.isDirectory) {
                val found = findManifestRoot(child)
                if (File(found, "manifest.json").exists()) return found
            }
        }
        return dir
    }

    @Suppress("unused")
    fun importFromStream(stream: InputStream): AppConfig? {
        val text = runCatching { stream.use { it.readBytes() } }.getOrNull() ?: return null
        return parseImportText(String(text)) ?: importFromZipBytes(text)
    }

    companion object {
        const val USER_CONFIG = "user-config.json"
    }
}
