# 命令提示器 · Android 1.0

面向开发者与运维的命令行速查与流程编排工具。Kotlin + Jetpack Compose（Material3）实现，单 Activity + MVVM，配置以 JSON 多文件体系打包在 `assets/config/`，可在 App 内导入 / 导出。

## 一、怎么用 GitHub Actions 编出 APK

1. 把本目录（含 `.github/`、`.gitignore`）整体推到一个新的 GitHub 仓库：

   ```bash
   git init
   git add .
   git commit -m "命令提示器 Android 1.0"
   git remote add origin git@github.com:<你的账号>/<仓库名>.git
   git branch -M main
   git push -u origin main
   ```

2. 推送后 Actions 会自动跑 `Build APK`，大约 5～10 分钟。
3. 进入仓库 **Actions → 最新一次运行 → Artifacts**，下载：
   - `cmdprompter-debug-apk`：可直接安装调试的 APK
   - `cmdprompter-release-apk`：用临时密钥签名的 Release APK

   也可以点右上角 **Run workflow** 手动触发。
4. 打标签可自动发布 Release 附件：

   ```bash
   git tag v1.0 && git push origin v1.0
   ```

> 说明：仓库不携带二进制的 `gradle-wrapper.jar`（避免污染仓库），CI 会先安装 Gradle 8.9 再就地生成 wrapper，因此**本地不需要任何额外准备**。

## 二、已实现的功能

| 编号 | 功能 | 状态 |
|------|------|------|
| F1 | 平台分组命令列表（左视图） | ✅ |
| F2 | 命令组列表（右视图，默认） | ✅ |
| F3 | 命令点击选中、滚动取消 | ✅ |
| F4 | 底部面板：参数编辑 + 实时预览 | ✅ |
| F5 | 底部工具栏：参数 / 复制 / 执行 / 更多 | ✅（执行、更多为预留） |
| F6 | 顶部终端预览区 | ✅ |
| F7 | 命令文档模式 | ✅ |
| F8 | 搜索过滤（名称 / 描述 / 标签） | ✅ |
| F9 | 配置导入 / 导出 | ✅ |
| F10 | 参数循环聚焦（📝） | ✅ |
| F11 | 终端真实执行 | ⏳ 1.0 仅填充命令 + 提示 |

## 三、内置命令库

7 个平台、19 条命令、2 个命令组：Git、Docker、SSH、文件系统、CMD、npm、Node.js。
命令组「快速部署 Web 服务」跨 Docker / Git / SSH 三个平台引用命令，展示组内顺序编排能力。

## 四、改配置（不需要改代码）

配置目录：`app/src/main/assets/config/`

```
config/
├── manifest.json          # 入口清单：登记 tools 与 docs
├── tools/*.json           # 每个工具一份，含该工具全部命令与命令组
└── docs/*.md              # 每个工具一份文档，用二级标题区分不同命令
```

新增一个工具只需三步：

1. 在 `config/tools/` 下新建 `mytool.json`，写 `platform` + `commands`（+ 可选 `groups`）；
2. 在 `config/docs/` 下新建 `mytool.md`；
3. 在 `manifest.json` 的 `tools` 数组里加 `"tools/mytool.json"`，在 `docs` 里加 `"mytool": "docs/mytool.md"`。

命令字段：

| 字段 | 说明 |
|------|------|
| `id` | 全局唯一，命令组通过它引用 |
| `template` | 命令模板，参数占位符 `{{key}}`（允许 `{{ key }}`） |
| `params[].type` | `text` 文本框 / `enum` 下拉（需 `options`）；`bool` 预留 |
| `docRef` | 指向 `manifest.docs` 的 key，运行时装配成 `doc` |

改完直接推到 GitHub 重新编译即可，无需改任何 Kotlin 代码。

## 五、App 内导入 / 导出

设置弹窗（顶栏 ⚙️）支持三种方式：

- **复制 JSON / 分享文件**：导出当前全部配置（commands + groups + docs）为单个合并 JSON；
- **粘贴导入**：直接粘贴合并 JSON 文本；
- **选择文件**：选 `.json`（合并 JSON）或 `.zip`（完整多文件目录体系）。

导入采用整体替换策略。删除命令会写回用户配置目录，持久化生效；「恢复内置配置」可回到出厂状态。

## 六、工程结构

```
app/src/main/java/com/example/cmdprompter
├── MainActivity.kt                单 Activity，Compose 宿主
├── ui/
│   ├── theme/                     配色与主题
│   ├── components/                TerminalBar / TopBar / SearchSlide /
│   │                              PlatformHeader / GroupHeader / CommandItem /
│   │                              BottomPanel / SettingsDialog
│   └── screens/MainScreen.kt      主界面组合
├── viewmodel/MainViewModel.kt      状态中枢（视图、选中、文档态、参数、终端）
├── data/
│   ├── model/                     Command / ParamDef / CommandGroup / AppConfig 等
│   ├── ConfigRepository.kt        资产复制、多文件装配、用户配置读写、导入导出
│   └── AppJson.kt                 JSON 解析实例
└── util/                          模板渲染、剪贴板、Shell 执行器（预留）
```

## 七、环境要求

- 编译：JDK 17、Android SDK（compileSdk 35）、Gradle 8.9
- 运行：Android 8.0（API 26）及以上

## 八、已知边界

- 终端区在 1.0 只做命令填充与提示，不真正执行 Shell（P2，代码已预留 `ShellExecutor`）。
- 文档区以纯文本渲染 Markdown（等宽/样式未做富文本解析）。
- 竖屏固定，未做平板/横屏适配。
