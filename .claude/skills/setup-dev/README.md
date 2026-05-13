# setup-dev 技能使用指南

一键初始化锦书企业级报表系统开发环境。

## 用法

```bash
/setup-dev
```

## 功能说明

本技能会按顺序执行以下步骤：

### 1. 依赖检查 ✅

检查开发环境必备的依赖：

| 依赖 | 要求版本 | 检查命令 |
|-----|---------|---------|
| Java | 21+ | `java -version` |
| Node.js | 18+ | `node -v` |
| pnpm | 8+ | `pnpm -v` |
| Docker | 24+ | `docker -v` |
| Docker Compose | 2+ | `docker-compose -v` |

### 2. 环境文件初始化 ✅

检查并初始化 `.env` 文件：

- 如果不存在，从 `.env.example` 复制
- 如果存在，提示是否需要重置

### 3. 启动基础设施 ✅

使用 Docker Compose 启动：

- PostgreSQL 16 (端口: 5432)
- Redis 7.4 (端口: 6379)
- RabbitMQ 3.13 (端口: 5672, 管理界面: 15672)

默认账号密码：

| 服务 | 用户 | 密码 |
|-----|------|------|
| PostgreSQL | jinshu | jinshu123 |
| RabbitMQ | guest | guest |

### 4. 等待服务健康 ✅

等待所有基础设施服务通过健康检查：

- PostgreSQL: `pg_isready`
- Redis: `redis-cli ping`
- RabbitMQ: `rabbitmq-diagnostics ping`

### 5. 初始化数据库 ✅

执行 DDL 脚本：

```bash
psql -h localhost -U jinshu -d jinshu -f sql/ddl/01_init_schema.sql
```

### 6. 安装前端依赖 ✅

```bash
cd frontend
pnpm install
```

## 选项说明

| 选项 | 说明 |
|-----|------|
| `--skip-deps` | 跳过依赖检查，直接开始后续步骤 |
| `--skip-infra` | 不启动 Docker 基础设施（假设已在运行） |
| `--skip-db` | 跳过数据库初始化 |
| `--force` | 强制重新初始化：删除现有容器和数据卷，重新创建 |

## 使用示例

### 完整初始化（推荐）

```bash
/setup-dev
```

### 跳过依赖检查（已知环境已就绪）

```bash
/setup-dev --skip-deps
```

### 仅初始化数据库（基础设施已在运行）

```bash
/setup-dev --skip-deps --skip-infra
```

### 强制重置（清理所有数据重新开始）

```bash
/setup-dev --force
```

## 验证环境

初始化完成后，运行以下命令验证：

```bash
# 检查 Docker 容器
docker-compose ps

# 检查后端是否可以启动
cd backend
./gradlew api:bootRun

# 检查前端是否可以启动
cd frontend
pnpm dev
```

## 访问地址

环境就绪后，可以访问：

| 服务 | 地址 | 说明 |
|-----|------|------|
| 前端开发服务器 | http://localhost:3000 | 需手动启动 |
| 后端 API | http://localhost:8080/api | 需手动启动 |
| RabbitMQ 管理界面 | http://localhost:15672 | guest/guest |
| PostgreSQL | localhost:5432 | jinshu/jinshu123 |
| Redis | localhost:6379 | 无密码 |

## 故障排除

### Docker 端口被占用

如果端口被占用，编辑 `.env` 修改端口映射：

```bash
# 修改 .env 文件
DB_PORT=5433
REDIS_PORT=6380
RABBITMQ_PORT=5673
```

### 数据库连接失败

检查 PostgreSQL 容器是否健康运行：

```bash
docker-compose logs postgres
docker-compose exec postgres pg_isready -U jinshu -d jinshu
```

### 前端依赖安装失败

尝试清理缓存重新安装：

```bash
cd frontend
rm -rf node_modules pnpm-lock.yaml
pnpm install
```

## 相关文档

- [README.md](../../README.md) - 项目概览
- [docker-compose.yml](../../docker-compose.yml) - 基础设施配置
- [01_init_schema.sql](../../sql/ddl/01_init_schema.sql) - 数据库初始化脚本
