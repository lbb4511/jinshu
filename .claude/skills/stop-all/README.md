# stop-all 技能使用指南

一键停止锦书企业级报表系统所有服务。

## 用法

```bash
/stop-all
```

## 功能说明

### 默认模式（优雅停止）

按顺序停止：

1. ✅ 前端开发服务器
2. ✅ 后端 API 服务
3. ✅ 基础设施（PostgreSQL、Redis、RabbitMQ）

## 选项说明

| 选项 | 说明 |
|-----|------|
| `--infra-only` | 仅停止基础设施（保留应用服务） |
| `--backend-only` | 仅停止后端 API 服务 |
| `--frontend-only` | 仅停止前端开发服务器 |
| `--purge` | 同时删除 Docker 容器和数据卷（危险！数据会丢失） |
| `--force` | 强制停止，立即杀死进程（SIGKILL） |

## 使用示例

### 优雅停止所有服务

```bash
/stop-all
```

### 仅停止基础设施

```bash
/stop-all --infra-only
```

### 仅停止应用服务（保留基础设施）

```bash
/stop-all --backend-only --frontend-only
```

### 强制停止（快速关闭）

```bash
/stop-all --force
```

### 完全清理（危险！）

```bash
/stop-all --purge
```

⚠️ **警告**: 这会删除所有 Docker 容器和数据卷，数据库数据会丢失！

## 停止顺序

服务按以下顺序停止：

```
1. 前端开发服务器
   ↓
2. 后端 API 服务
   ↓
3. RabbitMQ
   ↓
4. Redis
   ↓
5. PostgreSQL
```

## 验证停止

确认所有服务已停止：

```bash
# 检查 Docker 容器
docker-compose ps

# 检查端口是否释放
lsof -i :8080
lsof -i :3000
lsof -i :5432
lsof -i :6379
lsof -i :5672
```

## 优雅停止 vs 强制停止

### 优雅停止（默认）

- 发送 `SIGTERM` 信号
- 给进程时间清理资源
- 数据库会正确刷盘
- 推荐日常使用

### 强制停止（--force）

- 发送 `SIGKILL` 信号
- 立即终止进程
- 可能导致数据损坏
- 仅在紧急情况下使用

## 常见问题

### 进程无法停止

如果进程无法停止，尝试：

```bash
/stop-all --force
```

或者手动查找并杀死进程：

```bash
# 查找占用 8080 端口的进程
lsof -ti:8080 | xargs kill -9

# 查找占用 3000 端口的进程
lsof -ti:3000 | xargs kill -9
```

### Docker 容器无法停止

```bash
docker-compose down
# 如果还不行
docker-compose down --volumes --remove-orphans
```

### 忘记停止服务

下次启动时可能会提示端口被占用，先停止之前的服务：

```bash
/stop-all
```

## 相关文档

- [start-all](../start-all/README.md) - 启动所有服务
- [docker-compose.yml](../../docker-compose.yml) - 基础设施配置
