# CMD 命令手册（Windows）

## 查看网络配置 (cmd-ipconfig)

**模板**：`ipconfig /all`

显示所有网卡的 IP、子网掩码、网关、DNS 与 MAC 地址。
排查网络问题时常用 `ipconfig /flushdns` 刷新 DNS 缓存。

## 持续 Ping (cmd-ping)

**模板**：`ping {{host}} -t`

- `-t` 表示一直 ping，按 `Ctrl + C` 停止
- 观察是否有丢包与延迟抖动，判断网络质量

## 列出进程 (cmd-tasklist)

**模板**：`tasklist | findstr {{keyword}}`

按关键字过滤进程列表。结束进程可用：

```
taskkill /PID 1234 /F
```
