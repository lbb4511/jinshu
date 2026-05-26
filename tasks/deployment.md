# 部署任务

> 所属: [PROGRESS.md](PROGRESS.md) | 进度: 7/12 (58%) | 状态: 🔶 进行中

---

## 3.1 容器化 ✅ 已完成

- [x] Docker Compose 基础设施编排 (PostgreSQL 16 + Redis 7.4 + RabbitMQ 3.13)
- [x] 后端 Dockerfile
- [x] 健康检查探针 (/health, /ready, /live)

## 3.2 编排与调度 🔶 进行中

- [x] K8s 基础清单 (namespace, configmap, secret, deployment, service, ingress)
- [x] K8s PVC 持久化存储
- [x] K8s RBAC 权限

| # | 任务 | 优先级 | 依赖 |
|---|------|--------|------|
| T01 | K3s 集群搭建 | P0 | K8s 清单 |
| T02 | Kustomize 环境分层 (dev/staging/prod) | P1 | T01 |

## 3.3 CI/CD ✅ 已完成

- [x] GitHub Actions 编译/测试/许可证检查
- [x] Gitea Actions SSH 部署管道
- [x] PR/Issue 模板

## 3.4 待完成 ❌

| # | 任务 | 优先级 | 依赖 |
|---|------|--------|------|
| T03 | 前端 Dockerfile 与 Nginx 配置 | P1 | 无 |
| T04 | 前端静态资源 CDN 部署 | P2 | T03 |
| T05 | 数据库备份定时任务 (CronJob) | P1 | 设计 (备份方案) |
| T06 | SSL/TLS 证书自动化 | P1 | T01 |
| T07 | 灰度发布/金丝雀部署 | P2 | T01, T02 |
