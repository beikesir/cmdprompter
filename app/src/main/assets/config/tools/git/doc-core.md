# Git 常用命令手册

## `git-pull` 拉取最新代码

**模板**：`git pull origin {{branch}}`

从远程分支拉取最新提交并自动合并到当前分支，等价于 `git fetch` + `git merge`。

### 注意

- 本地有未提交改动时可能冲突，可先 `git stash`
- 只想看远程变化不想合并时用 `git fetch`

## `git-commit-push` 提交并推送

**模板**：`git commit -m '{{message}}' && git push origin {{branch}}`

### 步骤

1. 先 `git add` 暂存更改
2. 执行本命令，填写提交信息与分支名

### 注意

- 提交信息用单引号包裹，避免 Shell 解析特殊字符
- 推送失败通常因远程有新提交，需先 `git pull`

## `git-switch-branch` 切换分支

**模板**：`git checkout -b {{branch}}`

`-b` 表示分支不存在时新建；去掉 `-b` 则切换到已有分支。

### 常用变体

```bash
git switch {{branch}}        # 新写法，语义更清晰
git checkout {{branch}}      # 切换到已存在的分支
```

## `git-stash-pop` 暂存并恢复改动

**模板**：`git stash && git pull origin {{branch}} && git stash pop`

把未提交的改动临时收进栈里，拉完代码再恢复，是处理"本地有改动又想更新"的标准套路。

### 注意

- `stash pop` 可能冲突，冲突需手工解决后再 `git stash drop`
- 只想查看栈内容用 `git stash list`
