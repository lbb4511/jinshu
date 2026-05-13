# db-migrate 技能使用指南

锦书企业级报表系统数据库迁移管理。

## 用法

```bash
/db-migrate <操作> [名称]
```

## 功能说明

数据库迁移管理，支持：

- 📝 创建新迁移
- ⬆️ 执行迁移
- ⬇️ 回滚迁移
- 📊 查看迁移状态
- 📜 查看迁移历史

## 操作说明

| 操作 | 说明 | 额外参数 |
|-----|------|---------|
| `create` | 创建新迁移 | `name` - 迁移名称 |
| `up` | 执行迁移 | `--target`、`--all` |
| `down` | 回滚迁移 | `--target`、`--all` |
| `status` | 查看迁移状态 | - |
| `history` | 查看迁移历史 | - |

## 选项说明

| 选项 | 说明 |
|-----|------|
| `--dry-run` | 模拟运行，不实际执行，显示将要执行的 SQL |
| `--target` | 目标迁移版本（用于 up/down） |
| `--all` | 应用所有未执行的迁移（up）或回滚所有（down） |
| `--sql-only` | 仅生成 SQL，不执行（用于审查） |
| `--force` | 强制执行，忽略检查（小心使用！） |

## 使用示例

### 查看迁移状态

```bash
/db-migrate status
```

输出：

```
迁移状态
──────────────────────────────────────────
[✅] V1__init_schema.sql (已执行)
[⏳] V2__add_audit_logs.sql (待执行)
[⏳] V3__add_report_tables.sql (待执行)
```

### 创建新迁移

```bash
/db-migrate create add_user_table
```

这会在 `sql/migration/` 目录下创建新的迁移文件：

```
V4__add_user_table.up.sql   # 执行 SQL
V4__add_user_table.down.sql # 回滚 SQL
```

### 执行所有待执行的迁移

```bash
/db-migrate up --all
```

### 执行到指定版本

```bash
/db-migrate up --target V3__add_report_tables
```

### 回滚到上一个版本

```bash
/db-migrate down
```

### 回滚所有迁移

```bash
/db-migrate down --all
```

### 查看迁移历史

```bash
/db-migrate history
```

输出：

```
迁移历史
──────────────────────────────────────────
2026-05-13 10:00:00 - V1__init_schema.sql (执行成功)
2026-05-13 10:30:00 - V2__add_audit_logs.sql (执行成功)
```

### 模拟执行（预览）

```bash
/db-migrate up --all --dry-run
```

会显示将要执行的 SQL，但不会实际修改数据库。

## 迁移文件规范

### 文件命名

遵循以下命名规范：

```
V{版本号}__{描述}.{up|down}.sql
```

示例：

```
V1__init_schema.up.sql
V1__init_schema.down.sql
V2__add_audit_logs.up.sql
V2__add_audit_logs.down.sql
```

### 文件内容

Up 文件（执行迁移）：

```sql
-- V1__init_schema.up.sql

-- 创建租户表
CREATE TABLE tenant (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    code VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT NOW()
);

-- 注释
COMMENT ON TABLE tenant IS '租户表';
```

Down 文件（回滚迁移）：

```sql
-- V1__init_schema.down.sql

DROP TABLE tenant;
```

## 迁移最佳实践

### ✅ 推荐做法

1. **每个迁移只做一件事**
   - 不要在一个迁移中混多个功能

2. **总是提供回滚脚本**
   - 确保每个 up 都有对应的 down

3. **测试迁移脚本**
   - 在测试环境验证 up 和 down

4. **添加事务（如果支持）**
   ```sql
   BEGIN;
   -- 迁移内容
   COMMIT;
   ```

5. **添加说明注释**
   - 解释迁移的目的和影响

### ❌ 避免的做法

1. **不要修改已执行的迁移**
   - 如果需要修改，创建新的迁移

2. **不要在迁移中使用 DROP TABLE 等危险操作**
   - 除非你真的知道在做什么

3. **不要在迁移中依赖应用代码**
   - 迁移应该是独立的

4. **不要执行大量数据迁移**
   - 考虑分批处理，避免长时间锁表

## 多租户注意事项

所有迁移脚本都必须考虑多租户架构：

- ✅ 所有业务表包含 `tenant_id` 字段
- ✅ 索引以 `tenant_id` 开头
- ✅ 谨慎处理跨租户的数据

## 迁移表

系统使用 `schema_migrations` 表跟踪迁移状态：

```sql
CREATE TABLE schema_migrations (
    version VARCHAR(128) PRIMARY KEY,
    applied_at TIMESTAMP DEFAULT NOW(),
    execution_time INTEGER,
    status VARCHAR(32) NOT NULL
);
```

## 故障排除

### 迁移失败

迁移失败时，检查：

1. 数据库是否正在运行
2. 是否有足够的权限
3. SQL 语法是否正确
4. 是否有数据冲突

### 迁移卡住

如果迁移卡在中间状态：

1. 先手动修复数据库
2. 然后使用 `--force` 标记为完成或失败
3. 或手动修改 `schema_migrations` 表

### 需要重做迁移

如果需要重新运行迁移：

```bash
# 1. 先回滚
/db-migrate down --target V2__previous

# 2. 再执行
/db-migrate up --all
```

## 相关文档

- [sql/ddl/01_init_schema.sql](../../sql/ddl/01_init_schema.sql) - 初始化脚本
- [06.数据库架构设计.md](../../docs/01-立项与架构/06.数据库架构设计.md) - 数据库设计
- [04.多租户架构设计.md](../../docs/01-立项与架构/04.多租户架构设计.md) - 多租户设计
