# format-md - Markdown文档格式化技能

自动格式化markdown文档，支持标题层级规范化、目录生成、链接检查等功能。

## 功能特性

- ✅ 标题层级规范化检查
- ✅ 自动生成/更新目录(TOC)
- ✅ 相对链接有效性检查
- ✅ Prettier自动格式化
- ✅ Markdown Lint代码检查
- ✅ 支持单文件或目录批量处理

## 安装依赖

```bash
# 安装prettier (格式化)
npm install -g prettier

# 安装markdownlint (代码检查)
npm install -g markdownlint-cli
```

## 使用方法

### 基本语法

```bash
/format-md <path> [--flags]
```

### 常用命令

```bash
# 格式化单个文件
/format-md docs/README.md

# 格式化整个目录
/format-md docs/

# 仅检查不修改
/format-md docs/ --check

# 生成/更新目录
/format-md docs/README.md --toc

# 检查链接有效性
/format-md docs/ --links

# 执行lint检查
/format-md docs/ --lint

# 组合使用所有功能
/format-md docs/ --toc --links --lint
```

### 标志说明

| 标志 | 说明 |
|------|------|
| `--check` | 仅检查模式，显示需要格式化的内容但不修改 |
| `--toc` | 自动生成或更新文档目录 |
| `--links` | 检查并报告无效的相对链接 |
| `--lint` | 执行markdownlint规则检查 |

## 目录(TOC)使用说明

要使用自动目录功能，需要在文档中添加标记：

```markdown
# 文档标题

<!-- TOC START -->
<!-- TOC END -->

## 第一节
...
```

如果没有标记，脚本会在第一个H1标题后自动插入目录。

## 格式化规则

基于Prettier的默认markdown格式化规则，包括：

- 统一使用2空格缩进
- 列表项自动对齐
- 代码块标记规范
- 行宽自动调整(默认80字符)
- 标题前后空行规范

## Lint规则

基于markdownlint默认规则，检查常见问题：

- MD001: 标题层级递增
- MD002: 第一个标题应该是H2
- MD003: 标题样式一致
- MD004: 列表样式一致
- MD009: 行尾无多余空格
- MD010: 不使用硬制表符
- MD012: 无多余空行
- MD013: 行长度限制
- MD029: 有序列表前缀正确
- MD032: 列表周围有空行

## 示例输出

```
[INFO] 处理文件: docs/README.md
[WARN] docs/README.md:5: 标题层级跳跃：从 H1 跳到 H3
[INFO] 正在生成目录...
[INFO] 检查链接...
[WARN] docs/README.md: 链接不存在: ./missing-file.md
[INFO] docs/README.md: 已格式化
[INFO] 执行markdownlint检查...
[INFO] 格式化完成！
```
