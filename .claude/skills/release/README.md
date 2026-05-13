# release 技能使用指南

锦书企业级报表系统发布流程。

## 用法

```bash
/release <版本号>
```

## 功能说明

### 默认模式（完整发布流程）

执行完整发布流程：

1. ✅ 检查当前分支是否为 main
2. ✅ 检查工作区是否干净
3. ✅ 运行完整的 pre-commit 检查
4. ✅ 更新版本号
5. ✅ 更新 CHANGELOG.md
6. ✅ 构建所有模块
7. ✅ 创建 Git tag
8. ✅ 生成发布说明
9. ✅ 推送到远程仓库

## 参数说明

| 参数 | 说明 | 必须 |
|-----|------|------|
| `version` | 要发布的版本号（如：1.0.0） | ✅ 是 |

## 选项说明

| 选项 | 说明 |
|-----|------|
| `--dry-run` | 模拟发布，显示要执行的操作但不真正执行 |
| `--skip-tests` | 跳过测试环节 |
| `--skip-build` | 跳过构建，使用已有构建产物 |
| `--tag-only` | 仅创建 Git tag，不做其他操作 |
| `--notes-only` | 仅生成发布说明，不做其他操作 |

## 使用示例

### 发布 1.0.0 版本

```bash
/release 1.0.0
```

### 模拟发布（查看流程）

```bash
/release 1.0.0 --dry-run
```

### 仅生成发布说明

```bash
/release 1.0.0 --notes-only
```

### 快速发布（跳过测试）

```bash
/release 1.0.0 --skip-tests
```

## 版本号规范

遵循 **Semantic Versioning** (语义化版本)：

```
MAJOR.MINOR.PATCH
  |     |     |
  |     |     └─ Bug 修复，不修改 API
  |     └─────── 新功能，向下兼容
  └───────────── 不兼容的 API 修改
```

示例：

- `1.0.0` - 首次正式发布
- `1.0.1` - Bug 修复
- `1.1.0` - 新功能
- `2.0.0` - 重大不兼容变更

## 发布流程详解

### 1. 预检查

- ✅ 当前分支必须是 `main` 或 `develop`
- ✅ 工作区必须干净（无未提交的修改）
- ✅ 本地分支与远程同步

### 2. 代码质量检查

运行完整的 pre-commit 检查：

```bash
/pre-commit
```

### 3. 更新版本号

更新以下文件中的版本号：

- `backend/gradle.properties`
- `frontend/package.json`
- `CHANGELOG.md`

### 4. 更新 CHANGELOG

自动生成 CHANGELOG 条目：

```markdown
## [1.0.0] - 2026-05-13

### 🆕 新增功能
- 用户认证和授权
- 多租户数据隔离

### 🐛 Bug 修复
- 修复健康检查偶尔失败的问题

### 📚 文档
- 添加部署文档
```

### 5. 构建项目

构建所有模块：

```bash
/build-backend --clean --package --all
/build-frontend --clean --lint --type-check
```

### 6. 创建 Git 提交和 Tag

```bash
git commit -m "chore: release version 1.0.0"
git tag -a v1.0.0 -m "Release version 1.0.0"
```

### 7. 生成发布说明

基于 Git 提交记录生成详细的发布说明。

### 8. 推送到远程仓库

```bash
git push
git push --tags
```

## 发布产物

构建完成后，产物包括：

| 产物 | 位置 | 说明 |
|-----|------|------|
| 后端 API JAR | `backend/api/build/libs/jinshu-report-api-1.0.0.jar` | 可直接运行 |
| 后端 Batch JAR | `backend/batch/build/libs/jinshu-report-batch-1.0.0.jar` | 可直接运行 |
| 后端 Worker JAR | `backend/worker/build/libs/jinshu-report-worker-1.0.0.jar` | 可直接运行 |
| 后端 DataSync JAR | `backend/data-sync/build/libs/jinshu-report-data-sync-1.0.0.jar` | 可直接运行 |
| 前端构建 | `frontend/dist/` | 静态资源 |
| Docker 镜像 | 本地（可选） | |

## 发布说明

生成的发布说明包括：

- 📋 版本概述
- 🆕 新增功能
- 🐛 Bug 修复
- 💔 不兼容变更
- 📚 文档更新
- 🛠️ 技术改进
- 🎯 升级指南

## 回滚流程

如果发布后发现严重问题，快速回滚：

```bash
# 回滚到上一个版本
git checkout v0.9.9

# 重新发布补丁版本
/release 0.9.10
```

## 预发布版本

如果是 pre-release 版本，在版本号后加上后缀：

```bash
/release 1.0.0-alpha.1
/release 1.0.0-beta.2
/release 1.0.0-rc.3
```

## 常见问题

### 工作区不干净

先提交或暂存修改：

```bash
git add .
git commit -m "..."
```

### 版本号已存在

选择新的版本号或删除旧 tag：

```bash
git tag -d v1.0.0
git push origin --delete v1.0.0
```

### 构建失败

先修复问题再发布：

```bash
/pre-commit
# 修复问题后再
/release 1.0.0
```

## 相关文档

- [pre-commit](../pre-commit/README.md) - 提交前检查
- [CHANGELOG.md](../../CHANGELOG.md) - 变更日志
- [CONTRIBUTING.md](../../CONTRIBUTING.md) - 贡献指南
- [README.md](../../README.md) - 项目说明
