# npm 命令手册

## `npm-init` 初始化项目

**模板**：`npm init -y`

`-y` 跳过交互式问答，直接生成默认 `package.json`。

## `npm-install` 安装依赖

**模板**：`npm install {{mode}} {{package}}`

- `--save`：写入 `dependencies`（默认）
- `--save-dev`：写入 `devDependencies`（构建/测试工具）
- `--no-save`：只安装不改 `package.json`

### 常用变体

```bash
npm ci                 # 严格按 lock 文件安装，CI 推荐
npm install            # 按 package.json 装全部依赖
npm outdated           # 查看可升级的包
```

## `npm-run` 运行脚本

**模板**：`npm run {{script}}`

执行 `package.json` 中 `scripts` 字段定义的命令。

### 注意

脚本里优先使用本地安装的依赖（如 vite、webpack），无需全局安装。
直接 `npm run` 不带参数可列出所有可用脚本。
