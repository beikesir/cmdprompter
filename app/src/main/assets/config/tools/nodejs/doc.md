# Node.js 命令手册

## `node-run` 运行脚本

**模板**：`node {{file}}`

直接执行本地 JS 文件。

### 常用变体

```bash
node --inspect-brk {{file}}     # 断点调试，等待调试器接入
node --watch {{file}}           # 文件变更自动重启（Node 18+）
```

## `node-eval` 内联代码

**模板**：`node -e '{{code}}'`

不落地文件，直接执行一段代码，适合快速验证 API 行为。

### 注意

单引号内不要再出现单引号；Windows CMD 里建议改用双引号包裹。

## `npx-run` npx 临时运行

**模板**：`npx {{package}} {{args}}`

免全局安装临时执行包命令，例如 `npx create-react-app my-app`。

### 注意

- 包不存在时会临时下载到 npm 缓存中再执行
- 想指定版本用 `npx {{package}}@18`
