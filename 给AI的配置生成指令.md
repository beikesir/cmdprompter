# 生成工具配置的 AI 指令

把下面「指令正文」整段复制给任意对话 AI（把尖括号里的占位内容替换成你的需求），
它就会产出可直接放进本工程的命令配置文件和文档。

---

## 指令正文（复制从这里开始 ↓）

我要为一个 Android 应用「命令提示器」生成**某个命令行工具**的配置文件和文档。请严格按下面的规范输出，不要改动规范之外的结构。

### 一、你要产出的东西

工具名：**\<工具名，例如 Redis / Kubernetes / FFmpeg\>**

产出两类文件：

1. **命令配置文件** JSON：1 到多个，定义"命令"和"命令组"
2. **文档** Markdown：1 到多个，为每条命令、每个命令组各写一个小节

目录约定（`<工具id>` 用小写英文或连字符，例如 `redis`、`k8s`）：

```
config/
└── tools/<工具id>/
    ├── cmds-core.json     ← 命令配置文件，可拆成 cmds-xxx.json 多个
    └── doc-core.md        ← 文档，可拆成 doc-xxx.md 多个
```

### 二、命令配置文件格式（JSON）

```json
{
  "commands": [
    {
      "id": "redis-flush-db",
      "name": "清空当前库",
      "desc": "一句话说明这条命令做什么，不超过 15 字",
      "tags": ["Redis", "危险操作"],
      "template": "redis-cli -n {{db}} -a {{password}} FLUSHDB",
      "params": [
        {
          "key": "db",
          "label": "库编号",
          "type": "text",
          "default": "0",
          "hint": "输入框占位提示"
        },
        {
          "key": "mode",
          "label": "模式",
          "type": "enum",
          "options": ["A", "B"],
          "default": "A"
        }
      ]
    }
  ],
  "groups": [
    {
      "id": "redis-backup-flow",
      "name": "备份与恢复流程",
      "tags": ["运维"],
      "cmdIds": ["redis-save", "redis-flush-db"],
      "order": ["redis-save", "redis-flush-db"]
    }
  ]
}
```

字段约束（务必遵守）：

- `id`：全局唯一，只能含小写字母、数字、连字符；命名建议 `<工具id>-<动作>`
- `template`：命令模板，参数写成 `{{key}}`；**占位符必须与 `params` 的 `key` 一一对应，不能多也不能少**
- `params[].type`：只有两种，`text`（文本框）或 `enum`（下拉）；`enum` 必须给 `options`
- `params[].default`：给一个能直接跑通的合理默认值
- `groups[].cmdIds`：组内包含的命令 id 列表；**可以引用本文件之外的命令 id**（跨文件、跨工具都可以）
- `groups[].order`：执行顺序，缺省时按 `cmdIds` 顺序
- `platform` 字段**不要写**（由 manifest 统一注入）
- 没有命令组时 `groups` 写成 `[]`

### 三、文档格式（Markdown）

```markdown
# <工具名> 命令手册

## `redis-flush-db` 清空当前库

**模板**：`redis-cli -n {{db}} -a {{password}} FLUSHDB`

一句话说明这条命令的用途。

### 步骤

1. 第一步
2. 第二步

### 注意

- 容易踩的坑
- 什么情况下会失败

### 常用变体

```bash
redis-cli -n 0 FLUSHALL
```
```

文档硬性规则：

1. **一级标题 `#`** 只能有一个，是整篇文档的标题
2. **二级标题 `##`** 是一个"节点"，对应一条命令或一个命令组
3. **节点标题必须以反引号包裹该命令/命令组的 `id`**，例如 `## \`redis-flush-db\` 清空当前库`
   —— 应用靠这个反引号里的 id 把文档和命令关联起来，**写错就关联不上**
4. 三级及以下标题（`###`、`####`）是节点内部的小节，常用小节名：`步骤`、`注意`、`参数说明`、`常用变体`
5. 支持：有序/无序列表、``` 代码块、行内 `code`、`**粗体**`
6. 命令组的节点要写清**执行顺序**（用有序列表按顺序列出组内命令）

### 四、manifest 登记（同样请一并给出）

在 `config/manifest.json` 的 `tools` 数组里追加：

```json
{
  "id": "<工具id>",
  "platform": "<主界面显示的平台名，例如 Redis>",
  "commands": ["tools/<工具id>/cmds-core.json"],
  "docs": ["tools/<工具id>/doc-core.md"]
}
```

如果拆成了多个文件，就把多个路径都列进数组，例如：

```json
"commands": ["tools/<工具id>/cmds-core.json", "tools/<工具id>/cmds-advanced.json"],
"docs": ["tools/<工具id>/doc-core.md", "tools/<工具id>/doc-advanced.md"]
```

### 五、内容质量要求

- 命令数量：**15 到 30 条**，覆盖该工具最常用的场景，按使用频率从高到低排列
- 命令组：**2 到 5 个**，是"按顺序执行多条命令"的实用流程（例如部署、排错、备份）
- 每条命令都要有对应的文档节点，**一条都不能漏**
- 命令必须真实可用，不要编造不存在的参数；危险命令（删库、强推、格式化）在 `tags` 里标注 `危险操作`，并在文档的"注意"里写清后果
- `desc` 控制在 15 字以内，标签 2 到 3 个

### 六、输出方式

按下面格式一次性给出**全部文件**，每个文件用文件名和内容两段标明，方便我直接复制保存：

```
### 文件：config/tools/<工具id>/cmds-core.json
```json
...完整 JSON...
```

### 文件：config/tools/<工具id>/doc-core.md
```markdown
...完整 Markdown...
```

### 文件：config/manifest.json（需要追加的条目）
...
```

**注意：只输出文件内容，不要解释、不要省略、不要用 `// 省略` 之类的占位。JSON 必须能被 `json.load()` 解析通过。**

（复制到这里结束 ↑）

---

## 拿到产出后怎么用

1. 在 `app/src/main/assets/config/tools/` 下新建 `<工具id>/` 目录，把 JSON 与 Markdown 放进去
2. 把 manifest 条目加进 `app/src/main/assets/config/manifest.json` 的 `tools` 数组
3. 推到 GitHub 让 Actions 重新编译，装到手机即可生效 —— **不需要改任何 Kotlin 代码**

## 自检建议

保存前可以用这段命令快速验证 JSON 合法性和文档节点是否与命令 id 对上：

```bash
python3 - <<'PY'
import json, os, re, glob
base = 'app/src/main/assets/config'
ids = set()
for p in glob.glob(base + '/tools/*/cmds*.json'):
    json.load(open(p, encoding='utf-8'))          # JSON 合法性
    d = json.load(open(p, encoding='utf-8'))
    ids |= {c['id'] for c in d['commands']}
    ids |= {g['id'] for g in d.get('groups', [])}
for p in glob.glob(base + '/tools/*/doc*.md'):
    text = open(p, encoding='utf-8').read()
    anchors = {m.group(1) for m in re.finditer(r'^##\s+`([^`]+)`', text, re.M)}
    miss = anchors - ids
    print(os.path.basename(p), '孤立节点:', miss or '无')
    for i in ids:
        pass
print('命令/组总数:', len(ids))
PY
```
