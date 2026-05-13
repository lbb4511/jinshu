# Changelog

本项目变更日志基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 规范。

版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

---

## [Unreleased]

### 新增

- ✅ 引入 Python 3.12+ 作为 AI/数据科学辅助语言（决策 007）
  - 新增 `ai-services/` 模块骨架（pyproject.toml + src/jinshu_ai/）
  - 适用范围：LLM 报表生成、Pandas/NumPy 分析、运维脚本
  - 禁止用于业务核心逻辑与直连业务数据库写操作
- ✅ 引入 Rust 1.80+ 作为高性能/系统级辅助语言（决策 008）
  - 新增 `native/` Cargo workspace 骨架（excel-parser / wasm-grid / cli）
  - 适用范围：高性能 Excel 解析、WASM 前端组件、租户 CLI 工具
  - 禁止用于业务编排逻辑与强业务约束模块
- ✅ `docs/01-立项与架构/03.技术选型决策记录.md` 新增决策 007、008 与 3 项待评估决策

### 修复

- 🐛 修正 README.md 中"流式处理OOM"语义歧义为"流式处理防OOM"
- 🐛 同步 README.md 技术栈版本与 CHANGELOG（React 19 / Spring Boot 3.3 / PostgreSQL 16 / JDK 21 / Node 20）

---

## [0.1.0] - 2026-05-13

### 新增

- ✅ 项目初始化架构与文档体系
- ✅ 后端 Gradle 多模块结构
  - `common` 公共模块
  - `api` API 服务模块
  - `batch` 批处理模块
  - `worker` 工作节点模块
  - `data-sync` 数据同步模块
- ✅ 前端基础项目结构（React + TypeScript + Vite）
- ✅ 依赖升级到最新稳定版本
  - 后端：Spring Boot 3.3.5, Gradle 9.5.1
  - 前端：React 19.2.6, React Router DOM 7.15.0, ECharts 6.0.0, TypeScript 5.8.3, Vite 6.2.4
- ✅ 文档体系：
  - 01-立项与架构（100% 完成）
  - 02-流程与规范（100% 完成）
  - 03-模块设计（40% 完成）
  - 05-安全与合规（66% 完成）
  - 07-测试（75% 完成）
- ✅ 新增 04.技术版本管理规范

### 技术栈

| 组件 | 版本 |
|------|------|
| Java | 21+ |
| Spring Boot | 3.3.5 |
| Gradle | 9.5.1 |
| MyBatis Spring Boot Starter | 3.2.0 |
| PostgreSQL | 16 |
| PostgreSQL Driver | 42.7.6 |
| Redis | 7.4 |
| RabbitMQ | 3.13 |
| EasyExcel | 3.5.2 |
| Apache PDFBox | 2.0.36 |
| Lombok | 1.18.40 |
| React | 19.2.6 |
| React DOM | 19.2.6 |
| React Router DOM | 7.15.0 |
| ECharts | 6.0.0 |
| PrimeReact | 14.5.0 |
| PrimeIcons | 7.0.0 |
| Axios | 1.8.4 |
| Day.js | 1.11.19 |
| TypeScript | 6.0.3 |
| Vite | 8.0.12 |
| Vite React Plugin | 6.0.1 |

---

## 待发布版本变更记录模板

下一个版本的变更记录在此处预先填写：

```markdown
## [0.2.0] - YYYY-MM-DD
### 新增
- 功能 A
### 改进
- 优化 B 性能
### 修复
- 修复 C 问题
```
