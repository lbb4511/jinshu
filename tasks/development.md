# 开发任务

> 所属: [PROGRESS.md](PROGRESS.md) | 进度: 14/52 (27%) | 状态: 🔶 进行中
>
> ⚠️ 说明：本次更新补充了 11 项此前遗漏的开发任务（标 🆕）。大量已勾选任务目前为"骨架可用"级别，距离生产可用仍需细化异常处理、性能优化和端到端测试。

---

## 2.1 后端基础设施 ✅ 已完成

- [x] Gradle 多模块项目骨架 (common, api, batch, worker, data-sync)
- [x] 统一返回格式 (Result, PageResult)
- [x] 全局异常处理 (BusinessException, ErrorCode, GlobalExceptionHandler)
- [x] 租户上下文 (TenantContext, UserContext)
- [x] @SkipTenantFilter 注解
- [x] HashUtils 哈希工具
- [x] MyBatis 配置 (下划线转驼峰, Mapper 扫描)
- [x] 数据库 DDL 初始化脚本 (10 张表)
- [x] 健康检查 API (/health, /ready, /live)
- [x] ReportMapper + XML (基础 CRUD 骨架)

## 2.2 后端核心业务 ✅ 已完成（骨架级）

- [x] 基础设施 Docker Compose (PostgreSQL 16 + Redis 7.4 + RabbitMQ 3.13)
- [x] JWT 认证与授权 (JwtTokenProvider + JwtAuthenticationFilter + AuthController)
- [x] TenantInterceptor 启用 (MyBatis 插件自动注入 tenant_id)
- [x] AuditLogAspect 实现 (@AuditLog 注解 + AOP + BlockingQueue)
- [x] 租户 CRUD (TenantController + TenantService + TenantMapper)
- [x] 用户 CRUD (UserController + UserService + UserMapper)
- [x] 报表元数据 CRUD + 状态机 (ReportController + ReportService)
- [x] 数据源配置 CRUD + 连接测试 (DataSourceController + DataSourceService)
- [x] 任务管理 API (TaskController + TaskService + TaskMapper)

## 2.3 报表导入模块 (batch/) 🔶 进行中

| # | 任务 | 优先级 | 依赖 | 状态 |
|---|------|--------|------|------|
| D09 | EasyExcel 流式读取 | P0 | D01, D06 | ✅ |
| D10 | 数据校验与清洗 | P0 | D09 | ✅ |
| D11 | 百万行数据分批写入 | P0 | D09, D10 | ✅ |
| D12 | 导入进度回调 | P1 | D09 | ❌ |
| D13 | 异常行记录与重试 | P1 | D09, D10 | ❌ |

## 2.4 报表导出模块 (worker/export/) 🔶 进行中

| # | 任务 | 优先级 | 依赖 | 状态 |
|---|------|--------|------|------|
| D14 | CSV/Excel 流式导出 | P0 | D01, D06 | ✅ |
| D15 | 大文件分片下载 | P1 | D14 | ❌ |
| D16 | 导出进度通知 | P1 | D14 | ❌ |
| D17 🆕 | Excel 模板驱动导出 | P1 | D14, 模板设计 | ❌ |

## 2.5 PDF 渲染模块 (worker/pdf/) ⏳ 进行中

| # | 任务 | 优先级 | 依赖 | 状态 |
|---|------|--------|------|------|
| D18 | Puppeteer 渲染池 | P0 | D01, D06 | ✅ |
| D19 | 分段渲染与合并 | P0 | D18 | ✅ |
| D20 | CMYK 色彩支持 | P1 | D18 | ❌ |
| D21 | PDF/A 归档格式 | P1 | D18 | ❌ |

## 2.6 消息队列消费 (worker/consumer/) 🔶 进行中

| # | 任务 | 优先级 | 依赖 | 状态 |
|---|------|--------|------|------|
| D22 | 导入任务消费 | P0 | D09 | ✅ |
| D23 | 导出任务消费 | P0 | D14 | ✅ |
| D24 | PDF 渲染任务消费 | P0 | D18 | ✅ |
| D25 | 死信队列处理 | P1 | D22, D23, D24 | ❌ |

## 2.7 数据同步服务 (data-sync/) ❌ 未开始

| # | 任务 | 优先级 | 依赖 |
|---|------|--------|------|
| D26 | 外部数据源连接器 | P1 | D07 |
| D27 | 增量同步策略 | P2 | D26 |
| D28 | 数据转换管道 | P2 | D26 |

## 2.8 前端基础框架 ✅ 已完成（骨架级）

- [x] React 19 + TypeScript + Vite 项目脚手架
- [x] PrimeReact 组件库集成
- [x] Axios 请求封装 (JWT 拦截器桩)
- [x] TypeScript 类型定义 (Tenant, User, Report, Task, PageResult, Result)
- [x] 登录页 (Login.tsx)
- [x] 仪表盘 (Dashboard.tsx)
- [x] 全局样式 (global.scss)

## 2.9 前端业务页面 ⏳ 进行中

| # | 任务 | 优先级 | 依赖 | 状态 |
|---|------|--------|------|------|
| D29 | 路由守卫与权限控制 | P0 | D01 | ✅ |
| D30 | 前端状态管理 | P0 | D29 | ✅ |
| D31 | 通用组件库 | P1 | D29 | ❌ |
| D32 | 报表列表页 | P0 | D29, D31 | ✅ |
| D33 | 报表详情页 | P0 | D29, D31, D18 | ✅ |
| D34 | 任务中心 | P0 | D29, D08 | ✅ |
| D35 | 数据源管理 | P0 | D29, D07 | ✅ |
| D36 | 审计日志查看器 | P1 | D29, D03 | ✅ |
| D37 🆕 | 报表新建/编辑页 | P0 | D32, D33 | ❌ |
| D38 🆕 | 租户管理页 | P0 | D29, D04 | ❌ |
| D39 🆕 | 用户管理页 | P0 | D29, D05 | ❌ |

## 2.10 AI 服务 (ai-services/) ❌ 未开始

| # | 任务 | 优先级 | 依赖 |
|---|------|--------|------|
| D40 | LLM 报表生成 | P2 | D06, D33 |
| D41 | 数据分析 | P2 | D27, D36 |
| D42 | 运维脚本 | P2 | 无 |

## 2.11 原生模块 (native/) 🔶 骨架就绪

- [x] Rust 工作空间配置
- [x] 三个 crate 骨架 (cli, excel-parser, wasm-grid)

| # | 任务 | 优先级 | 依赖 |
|---|------|--------|------|
| D43 | Excel 高性能解析 | P1 | D09, D14 |
| D44 | WASM 虚拟滚动表格 | P1 | D33 |
| D45 | 租户 CLI 工具 | P2 | D04, D05 |

## 2.12 安全与合规（本次补充）❌ 未开始

> 需求文档和等保清单要求，但代码尚未落地：

| # | 任务 | 优先级 | 依赖 | 状态 |
|---|------|--------|------|------|
| D46 🆕 | 敏感字段加密 TypeHandler | P0 | 加密标准 | ❌ |
| D47 🆕 | 敏感数据脱敏 (日志/导出/展示) | P1 | D46 | ❌ |
| D48 🆕 | API 限流与租户并发配额 | P1 | 调度设计 | ❌ |
| D49 🆕 | 报表预览水印与 PDF 隐形水印 | P1 | 水印设计 | ❌ |

## 2.13 数据库与运维（本次补充）❌ 未开始

| # | 任务 | 优先级 | 依赖 | 状态 |
|---|------|--------|------|------|
| D50 🆕 | Flyway/Liquibase 迁移工具引入 | P0 | DDL 脚本 | ❌ |
| D51 🆕 | RBAC 权限表与角色 API | P0 | RBAC 设计 | ❌ |
| D52 🆕 | 报表模板表与模板市场 API | P1 | 模板设计 | ❌ |
