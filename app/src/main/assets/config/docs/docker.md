# Docker 命令手册

## 运行容器 (docker-run)

**模板**：`docker run -d --name {{name}} -p {{hostPort}}:{{containerPort}} {{image}}`

**参数说明**

- `-d`：后台运行
- `--name`：指定容器名，后续 `docker logs` / `docker stop` 都用它
- `-p`：端口映射，格式为 宿主机端口:容器端口

**注意**

- 宿主机端口被占用会启动失败，换端口或先停止占用进程
- 镜像不存在时会先自动 pull

## 查看容器日志 (docker-logs)

**模板**：`docker logs -f --tail {{lines}} {{name}}`

- `-f`：持续跟踪输出
- `--tail`：只看最后 N 行

## 命令组：快速部署 Web 服务 (deploy-web)

按顺序执行：运行容器 → 拉取最新代码 → SSH 连接。

1. 先在目标机启动/重启容器
2. 拉取最新代码保证镜像构建内容最新
3. 通过 SSH 登录做最后的检查与验证
