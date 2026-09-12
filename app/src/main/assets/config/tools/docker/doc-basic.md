# Docker 基础手册

## `docker-run` 运行容器

**模板**：`docker run -d --name {{name}} -p {{hostPort}}:{{containerPort}} {{image}}`

### 参数说明

- `-d`：后台运行
- `--name`：指定容器名，后续 `docker logs` / `docker stop` 都用它
- `-p`：端口映射，格式为 宿主机端口:容器端口

### 注意

- 宿主机端口被占用会启动失败，换端口或先停止占用进程
- 镜像不存在时会先自动 pull

## `docker-logs` 查看容器日志

**模板**：`docker logs -f --tail {{lines}} {{name}}`

- `-f`：持续跟踪输出
- `--tail`：只看最后 N 行

### 常用变体

```bash
docker logs --since 10m {{name}}    # 只看最近 10 分钟
docker logs {{name}} 2>&1 | grep error
```

## `deploy-web` 快速部署 Web 服务

按顺序执行：**运行容器 → 拉取最新代码 → SSH 连接**。

### 执行顺序

1. `docker-run`：在目标机启动或重建容器
2. `git-pull`：拉取最新代码，保证构建内容最新
3. `ssh-connect`：登录目标机做最后检查

### 说明

该组跨 Docker / Git / SSH 三个工具引用命令，体现"流程编排"能力。
