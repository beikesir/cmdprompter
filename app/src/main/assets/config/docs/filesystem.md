# 文件系统命令手册

## 查找文件 (fs-find)

**模板**：`find {{path}} -type {{type}} -name '{{pattern}}'`

- `-type f` 文件 / `d` 目录 / `l` 符号链接
- 名称模式用单引号包裹，避免通配符被 Shell 提前展开

**常用变体**

- 按大小找大文件：`find . -type f -size +100M`
- 找后删除：`find . -name '*.tmp' -delete`

## 修改权限 (fs-chmod)

**模板**：`chmod {{mode}} {{path}}`

- `755`：所有者可读写执行，其它人只读执行（脚本常用）
- `644`：普通文件默认权限
- `600`：仅所有者可读写（密钥文件推荐）

目录需要执行权限才能进入，递归修改请加 `-R`。

## 打包压缩 (fs-tar)

**模板**：`tar -czf {{output}} {{source}}`

- `-c` 创建 / `-z` gzip 压缩 / `-f` 指定文件名
- 解压：`tar -xzf archive.tar.gz -C ./target`
