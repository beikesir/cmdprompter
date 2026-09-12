# Docker 进阶手册

## `docker-exec` 进入容器终端

**模板**：`docker exec -it {{name}} {{shell}}`

- `-i` 保持标准输入打开，`-t` 分配伪终端，两者一起用才能交互
- 精简镜像（如 alpine）没有 `/bin/bash`，改用 `/bin/sh`

### 常见用途

- 查看容器内配置文件
- 临时执行诊断命令
- 不进入交互模式时去掉 `-it`，例如：

```bash
docker exec {{name}} nginx -t        # 校验配置
docker exec {{name}} env             # 查看环境变量
```

## docker-run 补充：资源与重启策略

基础篇给出的模板只覆盖常用参数，生产环境建议补齐：

```bash
docker run -d --name {{name}} \
  -p {{hostPort}}:{{containerPort}} \
  --restart unless-stopped \
  --memory 512m --cpus 0.5 \
  {{image}}
```

### 注意

- `--restart unless-stopped` 让容器随 Docker 守护进程自动拉起
- 限制内存与 CPU 可避免单个容器拖垮宿主机
