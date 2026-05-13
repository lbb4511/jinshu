# start-all 技能使用指南

一键启动锦书企业级报表系统所有服务。

## 用法

```bash
/start-all
```

## 功能说明

### 默认模式（全量启动）

按顺序启动：

1. ✅ 基础设施（PostgreSQL、Redis、RabbitMQ）
2. ✅ 后端 API 服务
3. ✅ 前端开发服务器

## 选项说明

| 选项 | 说明 |
|-----|------|
| `--infra-only` | 仅启动基础设施（数据库、Redis、RabbitMQ） |
| `--backend-only` | 仅启动后端 API 服务 |
| `--frontend-only` | 仅启动前端开发服务器 |
| `--detach` | 后台运行（仅基础设施支持，后端前端需要手动后台） |
| `--wait` | 等待所有服务健康检查通过后再完成启动 |

## 使用示例

### 完整启动（开发环境）

```bash
/start-all
```

这会在前台启动所有服务，按 `Ctrl+C` 停止。

### 仅启动基础设施

```bash
/start-all --infra-only --detach
```

### 仅启动后端（基础设施已运行）

```bash
/start-all --backend-only
```

### 仅启动前端

```bash
/start-all --frontend-only
```

### 后台启动基础设施，前台启动应用

```bash
/start-all --infra-only --detach
# 等待基础设施启动后
/start-all --backend-only --frontend-only
```

## 服务说明

### 基础设施服务

| 服务 | 地址 | 健康检查 |
|-----|------|---------|
| PostgreSQL | localhost:5432 | `pg_isready` |
| Redis | localhost:6379 | `redis-cli ping` |
| RabbitMQ | localhost:5672 | `rabbitmq-diagnostics ping` |
| RabbitMQ 管理界面 | http://localhost:15672 | 浏览器访问 |

### 后端服务

| 服务 | 地址 | 健康检查 |
|-----|------|---------|
| 后端 API | http://localhost:8080/api | http://localhost:8080/api/health |
| 就绪探针 | http://localhost:8080/api/ready | |
| 存活探针 | http://localhost:8080/api/live | |

### 前端服务

| 服务 | 地址 |
|-----|------|
| 前端开发服务器 | http://localhost:3000 |

## 启动顺序

服务启动有依赖关系，按以下顺序进行：

```
1. PostgreSQL
   ↓
2. Redis
   ↓
3. RabbitMQ
   ↓
4. 后端 API（依赖 1,2,3）
   ↓
5. 前端（依赖 4）
```

## 验证启动

启动完成后，可以通过以下方式验证：

### 检查基础设施

```bash
docker-compose ps
```

### 检查后端 API

```bash
curl http://localhost:8080/api/health
```

### 检查前端

```bash
curl http://localhost:3000
```

## 常见问题

### 端口被占用

如果端口被占用，可以：

1. 停止占用端口的程序
2. 修改配置文件使用其他端口

### 后端启动失败

检查：
- 数据库是否已启动
- 数据库连接配置是否正确
- 端口 8080 是否被占用

查看日志：
```bash
cd backend
./gradlew api:bootRun
```

### 前端启动失败

检查：
- Node.js 版本是否符合要求
- 依赖是否安装完成

查看日志：
```bash
cd frontend
pnpm dev
```

## 相关文档

- [stop-all](../stop-all/README.md) - 停止所有服务
- [setup-dev](../setup-dev/README.md) - 初始化开发环境
- [docker-compose.yml](../../docker-compose.yml) - 基础设施配置
