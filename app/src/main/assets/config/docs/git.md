# Git 命令手册

## 提交并推送 (git-commit-push)

**模板**：`git commit -m '{{message}}' && git push origin {{branch}}`

**步骤**

1. 先 `git add` 暂存更改
2. 执行本命令，填写提交信息与分支名

**注意**

- 提交信息用单引号包裹，避免 Shell 解析特殊字符
- 推送失败通常因远程有新提交，需先 `git pull`

## 拉取最新代码 (git-pull)

**模板**：`git pull origin {{branch}}`

从远程分支拉取最新提交并自动合并到当前分支，等价于 `git fetch` + `git merge`。

**注意**

- 本地有未提交改动时可能冲突，可先 `git stash`

## 切换分支 (git-switch-branch)

**模板**：`git checkout -b {{branch}}`

`-b` 表示分支不存在时新建；去掉 `-b` 则切换到已有分支。
