# 锦书企业级报表系统 - Skill 总览

这里是锦书系统所有可用的 Claude Code Skill 总览。

## 快速索引

### 🚀 开发流程

| Skill | 说明 | 用途 |
|-------|------|------|
| [setup-dev](./setup-dev/README.md) | 初始化开发环境 | 开始新项目开发时 |
| [start-all](./start-all/README.md) | 启动所有服务 | 开始开发时启动 |
| [stop-all](./stop-all/README.md) | 停止所有服务 | 结束开发时停止 |
| [check-health](./check-health/README.md) | 检查系统健康状态 | 排查问题、验证部署 |

### 🏗️ 构建相关

| Skill | 说明 | 用途 |
|-------|------|------|
| [build-backend](./build-backend/README.md) | 构建后端项目 | 后端代码变更后 |
| [build-frontend](./build-frontend/README.md) | 构建前端项目 | 前端代码变更后 |
| [format-code](./format-code/README.md) | 格式化所有代码 | 编码风格统一 |
| [format-md](./format-md/README.md) | 格式化 Markdown 文档 | 编写文档后 |

### ✅ 测试检查

| Skill | 说明 | 用途 |
|-------|------|------|
| [run-tests](./run-tests/README.md) | 运行测试 | 验证代码修改 |
| [pre-commit](./pre-commit/README.md) | 提交前完整检查 | Git 提交前 |
| [check-tenantsql](./check-tenantsql/README.md) | 多租户 SQL 检查 | SQL 修改后 |
| [review-test](./review-test/README.md) | 测试审查 | 代码审查时 |

### 📝 文档设计

| Skill | 说明 | 用途 |
|-------|------|------|
| [gen-design](./gen-design/README.md) | 生成设计文档 | 开始新功能开发前 |
| [review-design](./review-design/README.md) | 设计审查 | 设计文档完成后 |
| [gen-adr](./gen-adr/README.md) | 生成架构决策记录 | 做出架构决策时 |
| [update-doc-index](./update-doc-index/README.md) | 更新文档索引 | 新增文档后 |

### 🗄️ 数据库

| Skill | 说明 | 用途 |
|-------|------|------|
| [db-migrate](./db-migrate/README.md) | 数据库迁移管理 | 数据库变更时 |

### 🚢 发布部署

| Skill | 说明 | 用途 |
|-------|------|------|
| [release](./release/README.md) | 版本发布流程 | 发布新版本时 |

## 日常开发流程

### 1. 新环境初始化

```bash
# 首次使用
/setup-dev
```

### 2. 开始开发

```bash
# 启动所有服务
/start-all
```

### 3. 编码过程中

```bash
# 检查服务是否正常
/check-health

# 格式化代码
/format-code

# 运行测试
/run-tests
```

### 4. 提交前

```bash
# 完整的代码质量检查
/pre-commit
```

### 5. 提交后

```bash
# 停止服务（可选）
/stop-all
```

### 6. 发布版本

```bash
# 执行发布流程
/release 1.0.0
```

## Skill 开发与贡献

### 添加新 Skill

1. 在 `.claude/skills/` 下创建新目录
2. 创建 `skill.json` 配置文件
3. 创建 `README.md` 使用文档
4. 更新本总览文档

### Skill 配置文件格式

```json
{
  "name": "skill-name",
  "description": "Skill 描述",
  "author": "jinshu-team",
  "version": "1.0.0",
  "entry": "README.md",
  "args": [
    {
      "name": "arg-name",
      "description": "参数说明",
      "required": true
    }
  ],
  "flags": {
    "flag-name": "选项说明"
  }
}
```

## 相关文档

- [CLAUDE.md](../../CLAUDE.md) - Claude AI 项目配置
- [CONTRIBUTING.md](../../CONTRIBUTING.md) - 贡献指南
- [README.md](../../README.md) - 项目说明
- [AI_CONTRACT.md](../../AI_CONTRACT.md) - AI 助手合约

---

*最后更新：2026-05-13*
