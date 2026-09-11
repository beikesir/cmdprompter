# npm 命令手册

## 初始化项目 (npm-init)

**模板**：`npm init -y`

`-y` 跳过交互式问答，直接生成默认 `package.json`。

## 安装依赖 (npm-install)

**模板**：`npm install {{mode}} {{package}}`

- `--save`：写入 `dependencies`（默认）
- `--save-dev`：写入 `devDependencies`（构建/测试工具）
- `--no-save`：只安装不改 `package.json`

## 运行脚本 (npm-run)

**模板**：`npm run {{script}}`

执行 `package.json` 中 `scripts` 字段定义的命令，例如 `dev`、`build`、`test`。
脚本里优先使用本地安装的依赖（如 vite、webpack），无需全局安装。
