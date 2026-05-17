# check-tenantsql 技能使用指南

检查SQL是否符合多租户隔离规范，防止跨租户数据泄露安全风险。

## 用法

```bash
/check-tenantsql <文件或目录路径>
```

## 示例

```bash
# 检查单个文件
/check-tenantsql backend/src/main/resources/mapper/ReportMapper.xml

# 检查整个目录
/check-tenantsql backend/src/main/resources/mapper/

# 检查所有SQL相关文件
/check-tenantsql backend/src --all

# 严格模式（同时检查@SkipTenantFilter注解使用）
/check-tenantsql backend/src/main/java --strict

# 尝试自动修复（仅.sql文件）
/check-tenantsql sql/ddl/init_schema.sql --fix
```

## 检查项

### ✅ 必须检查
1. **所有SELECT/UPDATE/DELETE语句必须包含tenant_id过滤条件**
   ```sql
   -- ✅ 正确
   SELECT * FROM report WHERE tenant_id = ? AND id = ?

   -- ❌ 错误（缺少tenant_id）
   SELECT * FROM report WHERE id = ?
   ```

2. **UPDATE/DELETE必须限制tenant_id范围**
   ```sql
   -- ✅ 正确
   UPDATE report SET status = ? WHERE tenant_id = ? AND id = ?

   -- ❌ 错误（可能批量更新所有租户）
   UPDATE report SET status = ?
   ```

### ⚠️ 警告检查
1. 使用`@SkipTenantFilter`注解的方法必须：
   - 方法上必须有注释说明为什么需要绕过
   - 必须经过安全评审
   - 代码中必须做额外的权限校验

2. SQL中直接写`tenant_id = 0`或硬编码值
   - 可能是管理员全局查询，但需要确认
   - 应该通过参数传入`tenant_id`

### ❌ 高危问题
1. `SELECT * FROM table` 没有任何WHERE条件
2. `UPDATE table` 没有tenant_id限制
3. `DELETE FROM table` 没有tenant_id限制
4. JOIN查询中任何一张表缺少tenant_id过滤

## 检查结果说明

| 标记 | 级别 | 说明 | 处理方式 |
|------|------|------|---------|
| 🔴 | 高危 | 完全没有tenant_id过滤，可能导致全表跨租户泄露 | 必须修复 |
| 🟡 | 警告 | 可能有问题（如硬编码tenant_id） | 需要确认 |
| 🟢 | 通过 | 符合多租户规范 | - |

## 豁免规则

以下情况可以豁免tenant_id检查：
1. **系统表查询**（如`sys_config`, `audit_log`等全局表）
2. **DDL语句**（CREATE/ALTER/DROP）
3. **COUNT统计**且明确是全局统计（需注释说明）
4. **使用@SkipTenantFilter注解并附有安全说明**

## 最佳实践

1. **永远不要在代码中绕过SQL拦截器**，除非迫不得已
2. **所有手动写的SQL都必须包含tenant_id**，不要依赖拦截器
3. **复杂查询在WHERE条件最前面写tenant_id**，便于检查
4. **MyBatis Mapper方法命名规范**：建议包含`ByTenant`等标识

   ```java
   // ✅ 好的命名，一看就知道是按租户查询
   List<Report> selectByTenantIdAndId(@Param("tenantId") Long tenantId, @Param("id") Long id);

   // ❌ 不好的命名，不知道是否有租户隔离
   List<Report> selectById(@Param("id") Long id);
   ```

## 相关文档

- [多租户架构设计.md](../../../../docs/01-立项与架构/04.多租户架构设计.md)
- [安全原则与红线.md](../../../../docs/03-安全与合规/README.md)
