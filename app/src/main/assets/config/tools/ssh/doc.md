# SSH 命令手册

## `ssh-connect` SSH 连接

**模板**：`ssh -p {{port}} {{user}}@{{host}}`

### 注意

- 默认端口 22，生产环境常改成其它端口
- 首次连接会提示确认主机指纹，输入 `yes` 继续

### 常用变体

```bash
ssh -i ~/.ssh/id_ed25519 {{user}}@{{host}}   # 指定私钥
ssh -v {{user}}@{{host}}                     # 排错时打开详细日志
```

## `ssh-copy-id` 配置免密登录

**模板**：`ssh-copy-id -p {{port}} {{user}}@{{host}}`

把本机 `~/.ssh/id_rsa.pub` 追加到远程主机的 `~/.ssh/authorized_keys`。

### 步骤

1. 本机没有密钥时先执行 `ssh-keygen -t rsa`
2. 执行本命令，输入一次密码即可
3. 之后 `ssh` 登录不再需要密码

### 注意

- 远程主机 `~/.ssh` 权限应为 700、`authorized_keys` 应为 600
- 仍要输密码时，用 `ssh -vvv` 看服务端是否拒绝了公钥
