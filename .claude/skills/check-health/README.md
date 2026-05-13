# check-health 技能使用指南

检查锦书企业级报表系统所有服务的健康状态。

## 用法

```bash
/check-health
```

## 功能说明

### 默认模式（全量检查）

检查所有服务的健康状态：

1. ✅ 基础设施（PostgreSQL、Redis、RabbitMQ）
2. ✅ 后端 API 服务
3. ✅ 前端开发服务器

## 选项说明

| 选项 | 说明 |
|-----|------|
| `--infra-only` | 仅检查基础设施健康状态 |
| `--backend-only` | 仅检查后端服务健康状态 |
| `--frontend-only` | 仅检查前端服务健康状态 |
| `--watch` | 持续监控模式，每 10 秒检查一次 |
| `--json` | 输出 JSON 格式结果（便于脚本处理） |

## 使用示例

### 检查所有服务

```bash
/check-health
```

### 仅检查基础设施

```bash
/check-health --infra-only
```

### 持续监控

```bash
/check-health --watch
```

### 输出 JSON

```bash
/check-health --json
```

## 检查项

### 基础设施检查

| 服务 | 检查方法 | 健康状态 |
|-----|---------|---------|
| PostgreSQL | `pg_isready -U jinshu -d jinshu` | ✅ 正常 / ❌ 异常 |
| Redis | `redis-cli ping` | ✅ PONG / ❌ 异常 |
| RabbitMQ | `rabbitmq-diagnostics ping` | ✅ Ping ok / ❌ 异常 |

### 后端检查

| 端点 | 说明 | 预期结果 |
|-----|------|---------|
| `/api/health` | 综合健康检查 | `status: "UP"` |
| `/api/ready` | 就绪探针 | `ready: true` |
| `/api/live` | 存活探针 | `live: true` |

### 前端检查

| 端点 | 说明 | 预期结果 |
|-----|------|---------|
| `http://localhost:3000` | 前端首页 | HTTP 200 |

## 输出示例

### 文本格式

```
╔══════════════════════════════════════════════════════════════╗
║           锦书企业级报表系统 - 健康检查报告                     ║
╚══════════════════════════════════════════════════════════════╝

[基础设施]
  ✅ PostgreSQL (localhost:5432) - 正常
  ✅ Redis (localhost:6379) - 正常
  ✅ RabbitMQ (localhost:5672) - 正常

[后端服务]
  ✅ 健康检查 - UP (数据库连接正常)
  ✅ 就绪探针 - 已就绪
  ✅ 存活探针 - 存活

[前端服务]
  ✅ 前端开发服务器 (localhost:3000) - 正常

╔══════════════════════════════════════════════════════════════╗
║  总体状态: ✅ 全部正常                                          ║
╚══════════════════════════════════════════════════════════════╝
```

### JSON 格式

```json
{
  "timestamp": "2026-05-13T10:30:00Z",
  "overall": "healthy",
  "infra": {
    "postgres": "healthy",
    "redis": "healthy",
    "rabbitmq": "healthy"
  },
  "backend": {
    "health": "healthy",
    "ready": "ready",
    "live": "live"
  },
  "frontend": {
    "dev-server": "healthy"
  }
}
```

## 健康状态定义

| 状态 | 说明 | 处理方式 |
|-----|------|---------|
| ✅ Healthy | 服务正常运行 | - |
| ⚠️ Degraded | 服务运行但有问题 | 查看日志 |
| ❌ Unhealthy | 服务异常 | 重启服务 |

## 常见问题

### 后端健康检查失败

检查：
- 数据库是否正在运行
- 数据库连接配置是否正确
- 后端服务是否已启动

查看后端日志：
```bash
cd backend
./gradlew api:bootRun
```

### 前端无法访问

检查：
- 前端开发服务器是否已启动
- 端口 3000 是否被占用

查看前端日志：
```bash
cd frontend
pnpm dev
```

### RabbitMQ 管理界面无法访问

确认：
- RabbitMQ 容器是否正在运行
- 端口 15672 是否被占用
- 访问地址是否正确：http://localhost:15672

## 相关文档

- [start-all](../start-all/README.md) - 启动所有服务
- [stop-all](../stop-all/README.md) - 停止所有服务
- [HealthController.java](../../backend/api/src/main/java/com/jinshu/api/controller/HealthController.java) - 健康检查实现
