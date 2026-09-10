# SSH 命令手册

## SSH 连接 (ssh-connect)

**模板**：`ssh -p {{port}} {{user}}@{{host}}`

**注意**

- 默认端口 22，生产环境常改成其它端口
- 首次连接会提示确认主机指纹，输入 `yes` 继续

## 配置免密登录 (ssh-copy-id)

**模板**：`ssh-copy-id -p {{port}} {{user}}@{{host}}`

把本机 `~/.ssh/id_rsa.pub` 追加到远程主机的 `~/.ssh/authorized_keys`。

**步骤**

1. 本机没有密钥时先执行 `ssh-keygen -t rsa`
2. 执行本命令，输入一次密码即可
3. 之后 `ssh` 登录不再需要密码
