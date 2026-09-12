# 命令提示器 · Android

当前版本：**1.3.0**（三段式 主版本.次版本.修订号）

版本定义在三处，需同步修改：

| 位置 | 字段 |
|------|------|
| `app/src/main/java/com/example/cmdprompter/AppVersion.kt` | `NAME` / `CODE`（App 设置页展示） |
| `app/build.gradle.kts` | `versionName` / `versionCode` |
| `.github/workflows/build-apk.yml` | `env.VERSION`（产物与 Release 标签命名） |

## 更新日志

### 1.3.0
- 新增**工作流**选项卡（顶栏第三个）：基于命令组创建工作流，可命名、编辑说明、排序、增删命令、配置参数
- 工作流采用**快照**模式：创建后与源命令组解耦，源组变化不影响已保存的工作流
- 持久化到独立配置文件 `config/workflows/workflows.json`，并自动生成 `workflow-docs.md`
- 文档界面（命令与命令组）标题栏新增「修改」按钮，就地编辑
- 命令组的文档界面新增「创建工作流」按钮，带上组内所有命令跳转新建

### 1.2.1
- 修复：SettingsDialog 中残留重复 @Composable 注解导致编译失败

### 1.2.0
- 新增「配置管理」独立页面：查看配置目录全部文件夹与文件（可展开、可预览），导入 / 导出集中在此
- 移除命令行遗留的展开箭头（当前恒为选择模式）
- 移除「恢复内置配置」功能
- 设置页只保留手动新建 / 编辑
- 模型新增 modified / modifiedAt 字段，为后续回写已有配置文件做准备

### 1.1.2
- 修复：CommandList 调用缺少 onGroupDocClick 参数导致编译失败
- CI：编译失败时输出 Kotlin 错误明细并上传完整日志

### 1.1.1
- 设置页重构：已配置工具清单、文件/文件夹导入、ZIP 导出，均带进度条
- 支持手动新增 / 编辑命令与命令组，条目带「自建 / 已改」标记，写入独立配置文件与文档
- 三级层级视觉区分（平台 / 命令组 / 命令），不使用缩进
- 修复：配置清单类型转换、列表函数嵌套、工具条目构造

### 1.0.0
- 首个可用版本：双视图、参数面板、文档定位、搜索、导入导出


面向开发者与运维的命令行速查与流程编排工具。Kotlin + Jetpack Compose（Material3）实现，单 Activity + MVVM，配置以「多文件体系」打包在 `assets/config/`，可在 App 内导入 / 导出。

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

> 仓库不携带二进制的 `gradle-wrapper.jar`，CI 会先装 Gradle 8.9 再就地生成 wrapper，本地无需任何准备。

## 二、两个视图

| 视图 | 内容 |
|------|------|
| **📂 组**（默认，右） | 只显示各工具**配置进命令组**的命令流程；没有配组的工具在此视图不出现 |
| **📋 命令**（左） | 显示所有平台的**全部命令**，按平台分组 |

想看某条零散命令，切到「📋 命令」视图即可。

## 三、已实现的功能

| 编号 | 功能 | 状态 |
|------|------|------|
| F1 | 平台分组命令列表（左视图） | ✅ |
| F2 | 命令组列表（右视图，默认） | ✅ |
| F3 | 命令点击选中、滚动取消 | ✅ |
| F4 | 底部面板：参数编辑 + 实时预览 | ✅ |
| F5 | 底部工具栏：参数 / 复制 / 执行 / 更多 | ✅（执行、更多为预留） |
| F6 | 顶部终端预览区 | ✅ |
| F7 | 文档模式（命令与命令组各自定位到文档节点） | ✅ |
| F8 | 搜索过滤（名称 / 描述 / 标签） | ✅ |
| F9 | 配置导入 / 导出 | ✅ |
| F10 | 参数循环聚焦（📝） | ✅ |
| F11 | 终端真实执行 | ⏳ 1.0 仅填充命令 + 提示 |

## 四、配置体系（多文件）

```
config/
├── manifest.json                 # 入口：登记每个工具的配置文件与文档
└── tools/
    ├── git/
    │   ├── cmds-core.json        # 命令配置文件（1..n 个）
    │   ├── cmds-flow.json
    │   ├── doc-core.md           # 文档（1..n 个）
    │   └── doc-flow.md
    ├── docker/{cmds.json, doc-basic.md, doc-advanced.md}
    └── …每个工具一个目录
```

### manifest.json

```json
{
  "version": "1.0",
  "tools": [
    {
      "id": "git",
      "platform": "Git",
      "commands": ["tools/git/cmds-core.json", "tools/git/cmds-flow.json"],
      "docs": ["tools/git/doc-core.md", "tools/git/doc-flow.md"]
    }
  ]
}
```

- 每个工具可配 **1..n 个命令配置文件**（都用来定义命令和命令组，按需拆分主题）
- 每个工具可配 **1..n 个文档**（按主题拆，例如基础 / 进阶）
- `platform` 由工具统一注入各条命令，命令文件里不用重复写

### 命令配置文件

```json
{
  "commands": [
    {
      "id": "git-pull",
      "name": "拉取最新代码",
      "desc": "从远程拉取并合并",
      "tags": ["Git", "日常"],
      "template": "git pull origin {{branch}}",
      "params": [{ "key": "branch", "label": "分支", "type": "text", "default": "main" }]
    }
  ],
  "groups": [
    {
      "id": "daily-git",
      "name": "日常开发提交流程",
      "tags": ["开发"],
      "cmdIds": ["git-pull", "git-commit-push"],
      "order": ["git-pull", "git-commit-push"]
    }
  ]
}
```

| 字段 | 说明 |
|------|------|
| `id` | 全局唯一；命令组通过它引用命令，**可跨文件、跨工具** |
| `template` | 占位符 `{{key}}`，必须与 `params[].key` 一一对应 |
| `params[].type` | `text` 文本 / `enum` 下拉（需 `options`）；`bool` 预留 |
| `groups[].order` | 执行顺序，缺省按 `cmdIds` |

### 文档（Markdown，多级易读）

````markdown
# Git 命令手册

## `git-pull` 拉取最新代码

**模板**：`git pull origin {{branch}}`

一句话说明用途。

### 注意

- 本地有未提交改动时可能冲突

### 常用变体

```bash
git fetch
```
````

- `#` 一级标题：文档标题
- `##` 二级标题：**文档节点**，标题中用反引号标出对应的命令或命令组 id
- `###` 及以下：节点内部小节

App 打开某条命令（或命令组）的文档时，会**直接截取**该 id 对应节点的内容展示，并可切换查看整篇文档。

## 五、让 AI 帮你生成配置

见 **[给AI的配置生成指令.md](给AI的配置生成指令.md)** —— 里面有一段可以直接复制给任意对话 AI 的指令，
把工具名填进去，它就会产出符合上述规范的命令配置文件、文档和 manifest 条目，直接放进工程即可，无需改代码。

## 六、内置命令库

7 个工具、21 条命令、3 个命令组：Git（4 命令 / 2 组）、Docker（3 / 1）、SSH（2）、文件系统（3）、CMD（3）、npm（3）、Node.js（3）。

「快速部署 Web 服务」组跨 Docker / Git / SSH 三个工具引用命令，展示跨工具流程编排能力。

## 七、设置页

顶栏 ⚙️ 打开设置，现在只保留**手动新增 / 编辑**：

- 新建命令 / 命令组，文档为可选项；参数可增删改（key / 显示名 / 类型 / 选项 / 默认值）
- 命令组通过勾选方式选择组内命令，勾选顺序即执行顺序
- 保存后写入**独立配置文件** `tools/_user/cmds-user.json` 与**独立文档** `tools/_user/doc-user.md`
- 主界面出现小标记：**自建**（新增，绿）/ **已改**（改过内置，橙），统一归入顶部「我的命令」分区
- 设置页可直接再编辑或删除这些条目

> 1.2 起**不再提供「恢复内置配置」**，也不支持修改已有配置文件中的条目。
> 后续版本会在已有配置文件中只标记 `modified` 与 `modifiedAt`（字段已预留），再逐步支持回写。

## 七之二、工作流

### 创建

在**命令组**的文档界面，标题栏点「创建工作流」→ 带上该组所有命令跳到工作流选项卡并打开编辑弹窗。

工作流采用**快照模式**：创建时复制组内每条命令的模板与参数定义，之后源命令组增删命令、改模板都**不影响**已保存的工作流。

### 编辑

编辑弹窗内可以：

- 改**名称**与**说明**（说明可随时编辑，会显示在工作流卡片上）
- **排序**：每个步骤右上角 ↑ ↓
- **增加命令**：「+ 添加命令」从命令库挑一条（同样做快照），或「+ 空白」加一个自定义步骤
- **配置参数**：每个步骤的参数逐个填写，下方实时显示渲染后的完整命令
- 直接改**命令模板**
- 删除步骤

### 持久化

保存在独立的 `config/workflows/workflows.json`，同时自动生成 `config/workflows/workflow-docs.md`（在配置管理页可查看）。

数据结构：

```json
{
  "workflows": [
    {
      "id": "wf-1710000000000",
      "name": "日常开发提交流程 工作流",
      "sourceGroupId": "daily-git",
      "sourceGroupName": "日常开发提交流程",
      "note": "每天开工前跑一遍",
      "steps": [
        {
          "id": "step-xxx",
          "cmdId": "git-pull",
          "name": "拉取最新代码",
          "template": "git pull origin {{branch}}",
          "params": [{ "key": "branch", "label": "分支", "type": "text", "default": "main" }],
          "values": { "branch": "develop" }
        }
      ],
      "createdAt": 1710000000000,
      "updatedAt": 1710000000000
    }
  ]
}
```

### 尚未实现

- **单条命令手动执行**与**一键执行整条工作流**：要等应用内终端交互（P2）落地后再接

## 八、配置管理页

设置页点「打开配置管理」进入独立页面：

- **文件树**：列出 `config/` 目录下全部文件夹与文件，可展开目录，点击文件预览内容（截断 4000 字符）
- 顶部显示文件总数与合计大小；每行显示大小与最后修改时间
- **导入**：选择文件（可多选 `.json` / `.md` / `.zip`）或选择整个文件夹，带进度条
- **导出**：导出 ZIP（完整多文件体系，可直接再导入）/ 分享 JSON / 复制 JSON，带进度条
- 导入或新建条目后文件树自动刷新

## 九、工程结构

```
app/src/main/java/com/example/cmdprompter
├── MainActivity.kt                单 Activity，边到边 + insets 处理
├── ui/
│   ├── theme/                     配色与主题
│   ├── components/                TerminalBar / TopBar / SearchSlide /
│   │                              PlatformHeader / GroupHeader / CommandItem /
│   │                              BottomPanel / DocPanel / MarkdownBlock /
│   │                              GroupCard / ProgressBar / CommandEditDialog /
│   │                              SettingsDialog
│   └── screens/MainScreen.kt      主界面组合
├── viewmodel/MainViewModel.kt      状态中枢（视图、选中、文档态、参数、终端）
├── data/
│   ├── model/                     Command / ParamDef / CommandGroup / AppConfig 等
│   ├── DocParser.kt               Markdown 节点解析（## 级 anchor 绑定）
│   ├── ConfigRepository.kt        资产复制、多文件装配、用户配置读写、导入导出
│   └── AppJson.kt                 JSON 解析实例
└── util/                          模板渲染、剪贴板、Shell 执行器（预留）
```

## 十、环境要求

- 编译：JDK 17、Android SDK（compileSdk 35）、Gradle 8.9
- 运行：Android 8.0（API 26）及以上

## 十一、已知边界

- 终端区 1.0 只做命令填充与提示，不真正执行 Shell（P2，`ShellExecutor` 已预留）
- 文档渲染为轻量 Markdown 子集（代码块、子标题、列表、行内 code、粗体）
- 竖屏固定，未做平板 / 横屏适配
