# 锦书企业级报表系统 - Claude 项目配置

> 🤖 本文件专为 Claude Code / Claude AI 助手设计，帮助 AI 快速理解项目结构和开发规范。

---

## 📋 项目概览

**项目名称**: 锦书企业级报表系统  
**技术栈**: Spring Boot 4.x + React 19 + PostgreSQL 16 + RabbitMQ + Redis  
**核心特性**: 多租户架构、百万级数据处理、高性能PDF生成、审计日志防篡改  
**当前版本**: 0.1.0 (2026-05-13)

---

## 🏗️ 项目架构

### 模块划分

```
jinshu/
├── backend/                    # Java 后端 (Gradle Multi-Project)
│   ├── common/                # 公共模块（被其他模块依赖）
│   ├── api/                   # API 服务（主后端）
│   ├── batch/                 # Spring Batch 批处理
│   ├── worker/                # MQ 消费者 + PDF 生成
│   └── data-sync/             # 数据同步服务
├── frontend/                  # React 前端 (Vite)
├── ai-services/               # Python AI 模块（待实现）
├── native/                    # Rust 高性能模块（待实现）
├── sql/                       # 数据库脚本
│   └── ddl/                   # DDL 初始化脚本
├── docs/                      # 设计文档
│   ├── 01-立项与架构/        # 架构设计文档
│   ├── 02-流程与规范/        # 开发规范
│   └── 03-模块设计/          # 模块详细设计
└── docker-compose.yml         # 基础设施编排
```

### 技术栈详情

| 层级 | 技术选型 | 说明 |
|-----|---------|------|
| **后端** | Java 21, Spring Boot 4.0.6 | |
| **ORM** | MyBatis 3.0.5 | 非 JPA |
| **前端** | React 19, TypeScript 6, PrimeReact 10 | |
| **数据库** | PostgreSQL 16 | 所有表含 tenant_id |
| **消息队列** | RabbitMQ 3.13 | |
| **缓存** | Redis 7.4 | |
| **构建工具** | Gradle (Kotlin DSL) | 后端 |
| **包管理** | pnpm | 前端 |

---

## 📖 关键文档（先读这些！）

### 必读规范文档

| 文档 | 位置 | 重要性 | 核心内容 |
|-----|------|-------|---------|
| **AI 合约** | `AI_CONTRACT.md` | ⭐⭐⭐⭐⭐ | 禁止凭空编造，诚实原则 |
| **开放约束** | `OPEN_CONSTRAINTS.md` | ⭐⭐⭐⭐⭐ | 开源合规、架构约束 |
| **贡献指南** | `CONTRIBUTING.md` | ⭐⭐⭐⭐ | 开发流程、Git 规范 |
| **代码与设计对应** | `docs/代码与设计对应关系.md` | ⭐⭐⭐⭐ | 设计文档 ↔ 代码映射 |
| **一致性检查报告** | `docs/代码与设计一致性检查报告.md` | ⭐⭐⭐ | 当前实现状态 |

### 架构设计文档

| 文档 | 位置 | 说明 |
|-----|------|------|
| **概要设计** | `docs/01-立项与架构/02.概要设计.md` | 整体架构、模块划分 |
| **分层设计** | `docs/01-立项与架构/05.整体架构分层设计.md` | Controller/Service/DAO 职责 |
| **数据库设计** | `docs/01-立项与架构/06.数据库架构设计.md` | 表结构、索引规范 |
| **多租户设计** | `docs/01-立项与架构/04.多租户架构设计.md` | 租户隔离机制 |

### 模块设计文档

| 文档 | 位置 | 说明 |
|-----|------|------|
| **报表导入模块** | `docs/03-模块设计/01.报表导入模块设计.md` | 百万行 Excel 导入 |
| **审计日志设计** | `docs/03-模块设计/02.审计日志设计.md` | 防篡改、哈希链 |

---

## 🔑 核心约束（必须遵守！）

### 1. 多租户约束

- **所有业务表必须包含 `tenant_id BIGINT NOT NULL` 字段**
- SQL 通过 `TenantInterceptor` 自动注入租户过滤条件
- 绕过租户过滤使用 `@SkipTenantFilter` 注解
- 缓存 Key 必须包含 `tenant_id` 前缀

### 2. 审计日志约束

- 所有用户操作必须记录审计日志
- `audit_log` 表通过触发器禁止 UPDATE/DELETE
- 审计日志含哈希链，防篡改

### 3. 开源合规约束

- ❌ 禁止 GPL/LGPL 依赖
- ❌ 禁止商业付费代码
- ✅ 仅允许 Apache/MIT/BSD/PostgreSQL 协议

### 4. 设计先行约束

- **任何代码编写前必须先完成设计文档**
- 简单 Bug 修复除外
- 没有设计文档的 PR 一律打回

---

## 🗂️ 代码结构规范

### 后端分层架构

```
backend/api/src/main/java/com/jinshu/api/
├── ApiApplication.java           # 启动类
├── controller/                   # Controller 层（仅参数校验、调用 Service）
├── service/                      # Service 层（业务逻辑、事务控制）
├── dao/                          # DAO 层（MyBatis Mapper）
├── config/                       # 配置类（MyBatis、Security）
└── resources/
    ├── application.yml           # 配置文件
    └── mapper/                   # MyBatis XML 文件

backend/common/src/main/java/com/jinshu/common/
├── entity/                       # 实体类
├── context/                      # TenantContext、UserContext
├── result/                       # Result、PageResult（统一返回）
├── exception/                    # BusinessException、ErrorCode、GlobalExceptionHandler
├── security/                     # SkipTenantFilter（注解）
└── utils/                        # HashUtils 等工具类
```

### 分层调用规则

✅ **允许**: Controller → Service → DAO  
✅ **允许**: Service → Service（同模块内）  
❌ **禁止**: Controller → DAO（跳过 Service）  
❌ **禁止**: DAO 调用 Service  
❌ **禁止**: 跨模块直接调用（使用 HTTP 或 MQ）

### 前端结构

```
frontend/src/
├── main.tsx                     # 入口
├── App.tsx                      # 路由配置
├── pages/                       # 页面组件
│   ├── Login.tsx               # 登录页
│   └── Dashboard.tsx           # 仪表盘
├── services/                    # API 服务
│   ├── request.ts              # Axios 封装
│   └── health.ts               # 健康检查
├── types/                       # TypeScript 类型
│   └── index.ts
└── assets/styles/               # 全局样式
```

---

## 🔧 常用命令

### 后端开发

```bash
# 进入后端目录
cd backend

# 编译项目
./gradlew compileJava

# 运行 API 服务
./gradlew api:bootRun

# 运行所有服务（需要配置）
./gradlew bootRun
```

### 前端开发

```bash
# 进入前端目录
cd frontend

# 安装依赖
pnpm install

# 启动开发服务器
pnpm dev

# 构建
pnpm build
```

### 基础设施

```bash
# 启动 PostgreSQL、Redis、RabbitMQ
docker-compose up -d

# 查看状态
docker-compose ps

# 停止
docker-compose down
```

---

## 📊 数据库表清单

| 表名 | 说明 | 核心字段 |
|-----|------|---------|
| `tenant` | 租户表 | id, name, code, status |
| `users` | 用户表 | id, tenant_id, username, role |
| `report_metadata` | 报表元数据 | id, tenant_id, name, schema_version |
| `report_data` | 报表数据（宽表） | id, tenant_id, report_id, data_json |
| `report_workflows` | 工作流状态 | id, tenant_id, report_id, status |
| `audit_log` | 审计日志 | id, tenant_id, user_id, operation, log_hash |
| `audit_root_hash` | 审计哈希根 | id, tenant_id, hour_start, root_hash |
| `data_source` | 数据源配置 | id, tenant_id, name, connection_config |
| `task` | 任务表 | id, tenant_id, type, status, progress |

---

## ⚠️ 当前实现状态（2026-05-13）

### ✅ 已完成

- 项目骨架搭建（Gradle Multi-Project）
- 数据库 DDL 脚本
- 基础设施 Docker Compose
- 统一返回格式（Result、PageResult）
- 全局异常处理
- 租户上下文（TenantContext、UserContext）
- 健康检查 API（/health、/ready、/live）
- 前端基础框架 + 仪表盘
- 代码与设计对应文档

### ⏳ 部分完成

- TenantInterceptor（已存在，暂时禁用）
- AuditLogAspect（已存在，暂时禁用）
- SecurityConfig（已存在，简化配置）

### ❌ 待实现

- 用户认证与授权（JWT）
- 核心业务 CRUD（租户、用户、报表、数据源）
- 报表导入模块（Spring Batch + EasyExcel）
- 审计日志持久化
- 前端业务页面

---

## 🎯 AI 助手工作流程

### 标准 7 步工作流

```
1. 确认需求边界
   ↓
2. 检查设计文档是否存在
   ├─ 不存在 → 先写设计文档，结束本次编码
   └─ 已存在 → 进入下一步
   ↓
3. 完整阅读设计文档
   ├─ 重点关注：数据库设计、API 设计、异常处理
   └─ 有不明确的地方必须追问用户，不能假设
   ↓
4. 阅读相关规范文档
   ├─ 项目命名规约
   ├─ 统一返回与异常处理
   ├─ 安全红线
   └─ 开源协议说明
   ↓
5. 开始编码实现
   ├─ 严格按照设计文档实现
   ├─ 不得擅自修改设计（可以提建议，但不能自己改）
   └─ 代码必须符合所有规范
   ↓
6. 编写测试代码
   ├─ 正常流程测试
   ├─ 边界场景测试
   └─ 异常场景测试
   ↓
7. 代码自检
   ├─ 是否符合分层架构？
   ├─ 有没有绕过租户拦截器？
   ├─ 异常处理是否统一？
   ├─ 有没有引入不合规的依赖？
   └─ 有没有遗漏审计日志？
```

### 编码前自查清单

```
□ 1. 我确认已经完整阅读了对应模块的设计文档
□ 2. 设计文档中所有不明确的地方，我都已经问过用户
□ 3. 我确认设计文档已经通过评审（如果是新写的设计）
□ 4. 我理解数据库表结构和字段含义
□ 5. 我理解 API 的输入输出格式
□ 6. 我知道异常应该怎么抛出
□ 7. 我知道代码应该放在哪个目录、包名是什么
□ 8. 我知道命名规范应该遵守
□ 9. 我知道多租户 SQL 应该怎么写
□ 10. 我确认我不会编造任何不存在的类、方法、文件
```

---

## 📝 统一返回与异常处理

### Result 格式

```java
// 成功
Result.success(data)
{
  "code": 0,
  "message": "success",
  "data": {...}
}

// 失败
Result.error(errorCode, message)
{
  "code": xxx,
  "message": "...",
  "data": null
}
```

### 异常抛出

```java
// 业务异常
throw new BusinessException(ErrorCode.REPORT_NOT_FOUND);

// 带自定义消息
throw new BusinessException(ErrorCode.PARAM_ERROR, "文件大小超限");
```

---

## 🔍 搜索技巧

### 查找文件

```bash
# 查找 Java 类
find backend -name "*.java" | grep -i "tenant"

# 查找配置文件
find backend -name "application.yml"

# 查找前端组件
find frontend/src -name "*.tsx"
```

### 代码搜索

在代码库中搜索特定内容时，优先使用精确匹配，确认存在后再引用。

---

## ⚡ 快速参考

### 核心实体类

| 实体 | 位置 | 说明 |
|-----|------|------|
| Tenant | `backend/common/src/main/java/com/jinshu/common/entity/Tenant.java` | 租户 |
| User | `backend/common/src/main/java/com/jinshu/common/entity/User.java` | 用户 |
| Report | `backend/common/src/main/java/com/jinshu/common/entity/Report.java` | 报表元数据 |

### 核心工具类

| 类 | 位置 | 说明 |
|----|------|------|
| TenantContext | `backend/common/src/main/java/com/jinshu/common/context/TenantContext.java` | 租户上下文 |
| UserContext | `backend/common/src/main/java/com/jinshu/common/context/UserContext.java` | 用户上下文 |
| HashUtils | `backend/common/src/main/java/com/jinshu/common/utils/HashUtils.java` | 哈希工具 |
| Result | `backend/common/src/main/java/com/jinshu/common/result/Result.java` | 统一返回 |
| PageResult | `backend/common/src/main/java/com/jinshu/common/result/PageResult.java` | 分页返回 |
| ErrorCode | `backend/common/src/main/java/com/jinshu/common/exception/ErrorCode.java` | 错误码枚举 |
| BusinessException | `backend/common/src/main/java/com/jinshu/common/exception/BusinessException.java` | 业务异常 |

### API 端点（已实现）

| 端点 | 方法 | 说明 |
|-----|------|------|
| `/api/health` | GET | 综合健康检查 |
| `/api/ready` | GET | 就绪探针（K8s） |
| `/api/live` | GET | 存活探针（K8s） |

### 配置文件

| 文件 | 位置 | 说明 |
|-----|------|------|
| application.yml | `backend/api/src/main/resources/application.yml` | 主配置 |
| docker-compose.yml | `docker-compose.yml` | 基础设施编排 |
| .env | `.env` | 环境变量（复制自 .env.example） |

---

## 🚨 禁止事项（AI 助手特别注意）

1. ❌ **禁止编造不存在的文件/函数/类** — 先搜索确认
2. ❌ **禁止编造不存在的配置项/参数** — 查看实际配置文件
3. ❌ **禁止编造接口协议/返回格式** — 查看实际 Controller 代码
4. ❌ **禁止错误归因** — 确认技术栈后再回答
5. ❌ **禁止过度推断架构意图** — 只陈述代码和文档中确实存在的内容
6. ❌ **禁止一次做多件事** — 完成一件后等待用户确认再继续
7. ❌ **禁止不确认就继续往下做** — 遇到疑问必须停下来询问用户

---

## ✅ AI 助手正确做法

1. ✅ **先搜索，后回答** — 回答任何代码相关问题前必须先搜索确认
2. ✅ **引用时标注来源** — 文件路径 + 行号 / 文档名称 + 章节
3. ✅ **明确区分事实与建议** — 【已实现】vs【AI建议】
4. ✅ **诚实承认未知** — 明确告知"此功能暂未实现"
5. ✅ **标注版本时间** — 引用设计文档与代码时标注版本/日期

---

## 📞 获取帮助

如果遇到以下情况，请停下来询问用户：

- 设计文档中有不明确的地方
- 对技术选型有疑问
- 需要修改架构或核心设计
- 需要引入新的依赖
- 不确定用户需求的边界

---

> 🤖 **最后提示**: 本文件只是快速参考，AI 助手必须同时阅读 `AI_CONTRACT.md`、`OPEN_CONSTRAINTS.md` 和 `CONTRIBUTING.md` 这三个核心文档！
