# 数据库 DDL 说明

数据库 schema 变更已统一纳入 **Flyway** 管理，版本化迁移脚本位于：

```
backend/common/src/main/resources/db/migration/
```

## 目录结构

| 文件 | 说明 |
|------|------|
| `V1__init_schema.sql` | 初始 schema：sys / meta / data / workflow / audit / task |
| `V2__import_error_log.sql` | 导入错误日志表 |
| `V3__add_task_columns.sql` | 任务表扩展字段 |
| `V4__audit_previous_hash.sql` | 审计日志哈希链字段 |
| `V5__sync_poc_schema.sql` | POC 阶段 schema 同步 |

## 新增变更流程

1. 在 `backend/common/src/main/resources/db/migration/` 下新建版本号递增的脚本：
   ```
   V{版本号}__{简要描述}.sql
   ```
2. 脚本必须是**幂等**的（优先使用 `IF NOT EXISTS` / `IF EXISTS`）。
3. 禁止修改已发布的迁移脚本；如需修正，新建更高版本的脚本进行补偿。
4. 本地启动 `api` / `worker` / `batch` 任意服务时，Flyway 会自动执行未应用的迁移。

## 本地验证

```bash
cd backend
./gradlew :common:processResources
# 启动任意应用后观察 Flyway 日志
```

启动日志中应出现类似：

```
Flyway Community Edition ... by Redgate
Successfully validated 5 migrations
Creating schema "sys" ...
Creating schema "meta" ...
Current version of schema "public": 0
Migrating schema "public" to version "1 - init schema"
...
Successfully applied 5 migrations
```
