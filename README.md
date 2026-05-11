# 锦书企业级报表系统

<p align="center">
  <img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License">
  <img src="https://img.shields.io/badge/Java-17-orange.svg" alt="Java">
  <img src="https://img.shields.io/badge/React-18-blue.svg" alt="React">
  <img src="https://img.shields.io/badge/PostgreSQL-15-blue.svg" alt="PostgreSQL">
</p>

## 项目简介

"锦书"是一套高性能、高可用的企业级全生命周期报表管理平台。基于云原生架构，支持从多源数据导入、动态报表设计、全生命周期审批，到多格式（网页、HTML、PDF）输出的全流程自动化。

## 核心特性

- ✅ **动静同构**：React组件同时服务于动态网页与静态渲染
- ✅ **高性能PDF**：Chrome Headless渲染 + CMYK印刷级色彩支持
- ✅ **多租户架构**：租户数据隔离 + 公平调度防抢占
- ✅ **百万级数据**：支持百万行Excel导入导出，流式处理OOM
- ✅ **安全合规**：全量审计日志 + 水印防泄露 + 等保三级支持
- ✅ **云原生部署**：K3s容器化部署，水平自动扩缩容

## 技术栈

| 层级 | 技术选型 |
|-----|---------|
| **前端** | React 18, PrimeReact, ECharts |
| **后端** | Spring Boot 3.x, Spring Batch, Spring Security, MyBatis |
| **数据库** | PostgreSQL 15 |
| **消息队列** | RabbitMQ / RocketMQ |
| **PDF渲染** | Puppeteer (Chrome Headless), Apache PDFBox |
| **部署架构** | K3s + Docker |

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- PostgreSQL 15
- Docker / K3s (可选)

### 本地开发

```bash
# 1. 克隆项目
git clone <repository-url>
cd jinshu

# 2. 初始化数据库
psql -U postgres -f sql/ddl/init_schema.sql

# 3. 启动后端
cd backend
./mvnw spring-boot:run

# 4. 启动前端
cd frontend
npm install
npm start
```

## 项目结构

```
jinshu/
├── docs/                    # 项目文档
│   └── 01-立项与设计/      # 需求文档、概要设计、详细设计
├── backend/                 # Java后端服务
│   └── src/main/java/com/jinshu/
│       ├── controller/      # 控制层
│       ├── service/         # 业务逻辑层
│       ├── dao/             # 数据访问层
│       ├── entity/          # 实体类
│       ├── config/          # 配置类
│       └── common/          # 公共组件（AOP、异常、工具类）
├── frontend/                # React前端
│   ├── src/                 # 源码
│   └── public/              # 静态资源
├── k8s/                     # K3s部署配置
├── sql/                     # 数据库脚本
│   ├── ddl/                 # 表结构
│   └── dml/                 # 初始化数据
├── scripts/                 # 部署脚本
├── LICENSE                  # Apache 2.0
└── README.md                # 本文件
```

## 文档索引（20+ 份文档）

| 分类 | 文档 | 说明 |
|-----|------|------|
| 📚 导航 | [文档总索引](docs/README.md) | **先看这个**，完整文档地图 |
| 🔴 立项与架构 | [需求文档](docs/01-立项与架构/01.需求文档.md) | 功能清单、性能指标、安全要求 |
| | [概要设计](docs/01-立项与架构/02.概要设计.md) | 4层架构、模块划分、数据库设计 |
| | [技术选型决策记录](docs/01-立项与架构/03.技术选型决策记录.md) | 为什么选 MyBatis 不选 JPA |
| | [多租户架构设计](docs/01-立项与架构/04.多租户架构设计.md) | SQL自动注入、@SkipTenantFilter 用法 |
| | [整体架构分层设计](docs/01-立项与架构/05.整体架构分层设计.md) | Controller/Service/DAO 职责边界 |
| | [数据库架构设计](docs/01-立项与架构/06.数据库架构设计.md) | 命名规范、表清单、索引原则、分区策略 |
| 🟡 流程与规范 | [设计文档模板](docs/02-流程与规范/01.设计文档模板.md) | 11个必填章节，写完才可以编码 |
| | [CodeReview 指南](docs/02-流程与规范/02.CodeReview指南.md) | 3级评审标准 + Checklist |
| | [开源协议说明](docs/02-流程与规范/03.开源协议说明.md) | 禁止 GPL / 禁止商业付费代码 |
| | [项目命名规约](docs/02-流程与规范/04.项目命名规约.md) | 类名/方法名/数据库/API/Git 完整规范 |
| | [统一返回与异常处理设计](docs/02-流程与规范/05.统一返回与异常处理设计.md) | Result 五件套规范 |
| 🟢 模块设计 | [报表导入模块设计](docs/03-模块设计/01.报表导入模块设计.md) | 100万行导入、EasyExcel 流式读取 |
| | [审计日志设计](docs/03-模块设计/02.审计日志设计.md) | 防篡改、哈希链、等保三级 |
| 🔐 安全与合规 | [安全原则与红线](docs/05-安全与合规/README.md) | 7条安全红线、事故响应流程 |
| | [安全与合规架构设计](docs/05-安全与合规/01.架构设计.md) | 字段级加密、灾备 RPO/RTO |
## 性能基准

| 场景 | 指标 |
|-----|------|
| 小报表导入 (<1万行) | <3秒 |
| 大报表导入 (100万行) | <5分钟 |
| 简单PDF生成 (10页) | <5秒 |
| 复杂PDF生成 (100页) | <30秒 |
| 小任务最大等待时间 | <10秒 |

## 贡献指南

### 必读文档

| 文档 | 适用对象 | 核心内容 |
|-----|---------|---------|
| [CONTRIBUTING.md](CONTRIBUTING.md) | 人类开发者 | Git 规范、代码质量、测试要求 |
| [OPEN_CONSTRAINTS.md](OPEN_CONSTRAINTS.md) | 所有参与者 | 开源合规、架构约束、通用规则 |
| [AI_CONTRACT.md](AI_CONTRACT.md) | AI 助手 | 禁止凭空编造、诚实声明、知识边界 |

> 🤖 **给 AI 助手的特别提示**：你必须首先阅读并严格遵守 [AI_CONTRACT.md](AI_CONTRACT.md)。**禁止凭空编造任何不存在的代码、配置或功能。**

### 核心约束

1. **开源合规**：所有引入的第三方依赖必须采用 Apache/MIT 等开源协议，禁止引入任何商业付费代码
2. **多租户规范**：所有业务表必须包含 `tenant_id` 字段，SQL必须经过租户拦截器过滤
3. **审计日志**：所有用户操作必须记录审计日志，审计日志不可修改、不可删除
4. **代码质量**：新功能必须包含单元测试，核心模块测试覆盖率不低于 80%

## 开源协议

本项目采用 [Apache License 2.0](LICENSE) 协议，无需授权即可自由使用。

## 联系方式

- Issue 提交：[GitHub Issues](<repository-url>/issues)
- 讨论组：[Discussions](<repository-url>/discussions)
