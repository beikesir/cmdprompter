# CMD 命令手册（Windows）

## `cmd-ipconfig` 查看网络配置

**模板**：`ipconfig /all`

显示所有网卡的 IP、子网掩码、网关、DNS 与 MAC 地址。

### 常用变体

- 刷新 DNS 缓存：`ipconfig /flushdns`
- 释放并重取 IP：`ipconfig /release` → `ipconfig /renew`

## `cmd-ping` 持续 Ping

**模板**：`ping {{host}} -t`

- `-t` 表示一直 ping，按 `Ctrl + C` 停止
- 观察是否有丢包与延迟抖动，判断网络质量

### 注意

不通不代表主机宕机，可能是对方禁 ICMP。可改用 `Test-NetConnection` 测端口。

## `cmd-tasklist` 列出进程

**模板**：`tasklist | findstr {{keyword}}`

按关键字过滤进程列表。结束进程可用：

```bat
taskkill /PID 1234 /F
taskkill /IM node.exe /F
```
