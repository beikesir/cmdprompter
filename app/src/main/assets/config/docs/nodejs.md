# Node.js 命令手册

## 运行脚本 (node-run)

**模板**：`node {{file}}`

直接执行本地 JS 文件。开启调试：`node --inspect-brk index.js`。

## 内联代码 (node-eval)

**模板**：`node -e '{{code}}'`

不落地文件，直接执行一段代码，适合快速验证 API 行为。
注意单引号内不要再出现单引号。

## npx 临时运行 (npx-run)

**模板**：`npx {{package}} {{args}}`

免全局安装临时执行包命令，例如 `npx create-react-app my-app`。
包不存在时会临时下载到 npm 缓存中再执行。
