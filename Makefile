# ============================================================
# 锦书企业级报表系统 - Makefile
# 集成所有开发、构建、部署命令
# 使用 `make` 或 `make help` 查看帮助
# ============================================================

SHELL := /bin/bash
.PHONY: help
.DEFAULT_GOAL := help

GRADLE := cd backend && ./gradlew
PNPM := cd frontend && pnpm
DOCKER_COMPOSE := docker compose

# ────────────────────────────────────────────────────────────
# 帮助（默认目标）
# ────────────────────────────────────────────────────────────

help: ## 显示此帮助
	@printf '\033[36m%s\033[0m\n' '锦书企业级报表系统 - Makefile 帮助'
	@printf '\033[36m%s\033[0m\n' '用法: make <target>'
	@printf '\033[36m%s\033[0m\n' ''
	@grep -E '^[a-zA-Z0-9_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[33m%-28s\033[0m %s\n", $$1, $$2}'
	@printf '\n'

# ────────────────────────────────────────────────────────────
# 环境初始化
# ────────────────────────────────────────────────────────────

env: ## 从 .env.example 创建 .env（不覆盖已有文件）
	@test -f .env || (cp .env.example .env && echo ".env 已创建")
	@test -f .env && echo ".env 已存在" || true

# ────────────────────────────────────────────────────────────
# 基础设施 (Docker Compose)
# ────────────────────────────────────────────────────────────

infra-up: ## 启动基础设施（PostgreSQL + Redis + RabbitMQ）
	$(DOCKER_COMPOSE) up -d

infra-down: ## 停止并移除基础设施容器
	$(DOCKER_COMPOSE) down

infra-reset: ## 停止并清除数据卷（⚠️ 丢失所有数据）
	$(DOCKER_COMPOSE) down -v

infra-logs: ## 查看基础设施日志
	$(DOCKER_COMPOSE) logs -f

infra-ps: ## 查看基础设施状态
	$(DOCKER_COMPOSE) ps

infra-init-db: infra-up ## 初始化数据库（启动基础设施后执行）
	@bash scripts/init_db.sh

# ────────────────────────────────────────────────────────────
# 后端开发 (Gradle)
# ────────────────────────────────────────────────────────────

backend-compile: ## 编译所有 Java 源代码
	$(GRADLE) compileJava

backend-test: ## 运行所有后端测试
	$(GRADLE) test

backend-build: ## 完整构建（编译 + 测试 + 报告）
	$(GRADLE) build

backend-clean: ## 清理构建输出
	$(GRADLE) clean

backend-api: ## 启动 API 服务（开发模式）
	$(GRADLE) api:bootRun

backend-batch: ## 启动 Batch 服务
	$(GRADLE) batch:bootRun

backend-worker: ## 启动 Worker 服务
	$(GRADLE) worker:bootRun

backend-data-sync: ## 启动 Data Sync 服务
	$(GRADLE) data-sync:bootRun

backend-jar-api: ## 构建 API 可执行 JAR
	$(GRADLE) api:bootJar

backend-jar-batch: ## 构建 Batch 可执行 JAR
	$(GRADLE) batch:bootJar

backend-jar-worker: ## 构建 Worker 可执行 JAR
	$(GRADLE) worker:bootJar

backend-jar-data-sync: ## 构建 Data Sync 可执行 JAR
	$(GRADLE) data-sync:bootJar

backend-jar-all: ## 构建所有模块可执行 JAR
	$(GRADLE) api:bootJar batch:bootJar worker:bootJar data-sync:bootJar

backend-coverage: ## 生成 JaCoCo 覆盖率报告
	$(GRADLE) jacocoRootReport

backend-check-license: ## 检查依赖许可证合规性
	$(GRADLE) checkLicense

# ────────────────────────────────────────────────────────────
# 前端开发 (pnpm)
# ────────────────────────────────────────────────────────────

frontend-install: ## 安装前端依赖
	$(PNPM) install

frontend-dev: ## 启动前端开发服务器
	$(PNPM) dev

frontend-build: ## 构建前端生产版本
	$(PNPM) build

frontend-preview: ## 预览生产构建
	$(PNPM) preview

# ────────────────────────────────────────────────────────────
# Docker 镜像构建
# ────────────────────────────────────────────────────────────

docker-backend: ## 构建后端 Docker 镜像 (jinshu-backend:latest)
	docker build -t jinshu-backend:latest backend/

docker-frontend: ## 构建前端 Docker 镜像 (jinshu-frontend:latest)
	docker build -t jinshu-frontend:latest frontend/

docker-all: docker-backend docker-frontend ## 构建所有 Docker 镜像

# ────────────────────────────────────────────────────────────
# K8s 部署 (Kustomize)
# ────────────────────────────────────────────────────────────

k8s-apply: ## 部署全部 K8s 资源
	kubectl apply -k k8s/

k8s-delete: ## 删除全部 K8s 资源
	kubectl delete -k k8s/

k8s-pods: ## 查看 Pod 状态
	kubectl get pods -n jinshu -o wide

k8s-logs-backend: ## 查看后端 Pod 日志
	@kubectl logs -n jinshu -l app=jinshu-backend --tail=100 -f

k8s-logs-frontend: ## 查看前端 Pod 日志
	@kubectl logs -n jinshu -l app=jinshu-frontend --tail=100 -f

k8s-logs-postgres: ## 查看 PostgreSQL Pod 日志
	@kubectl logs -n jinshu -l app=jinshu-postgres --tail=50 -f

k8s-restart-backend: ## 重启后端 Deployment
	kubectl rollout restart deployment/jinshu-backend -n jinshu

k8s-restart-frontend: ## 重启前端 Deployment
	kubectl rollout restart deployment/jinshu-frontend -n jinshu

k8s-status: ## 查看所有 K8s 资源状态
	@kubectl -n jinshu get all
	@printf '\n--- Ingress ---\n'
	@kubectl -n jinshu get ingress

k8s-describe: ## 查看后端 Pod 详细信息（用于调试）
	@kubectl -n jinshu describe pods -l app=jinshu-backend

# ────────────────────────────────────────────────────────────
# 部署 (CI/CD)
# ────────────────────────────────────────────────────────────

deploy: ## 执行完整部署流水线（构建镜像 + 推送 + 部署到 K8s）
	bash scripts/deploy.sh $(SHA)

# ────────────────────────────────────────────────────────────
# 开发快速启动
# ────────────────────────────────────────────────────────────

up: env infra-up infra-init-db ## 一键启动完整开发环境（需先安装 Docker、Java 21、pnpm）
	@printf '\033[32m%s\033[0m\n' '开发环境就绪！运行 make dev 启动前后端'

dev: ## 同时启动后端 API 和前端开发服务器（需先 make up）
	@printf '\033[33m%s\033[0m\n' '终止占用 8080 端口的进程...'
	@fuser -k 8080/tcp 2>/dev/null || true
	@printf '\033[33m%s\033[0m\n' '启动后端 API...'
	$(GRADLE) api:bootRun &
	@sleep 5
	@printf '\033[33m%s\033[0m\n' '终止占用 3000 端口的进程...'
	@fuser -k 3000/tcp 2>/dev/null || true
	@printf '\033[33m%s\033[0m\n' '启动前端开发服务器...'
	$(PNPM) dev

# ────────────────────────────────────────────────────────────
# 质量检查
# ────────────────────────────────────────────────────────────

check: backend-compile frontend-build ## 运行所有编译检查

ci: check backend-test ## 运行 CI 流水线（编译 + 测试）

# ────────────────────────────────────────────────────────────
# 清理
# ────────────────────────────────────────────────────────────

clean: backend-clean ## 清理所有构建产物
	@rm -rf frontend/dist
	@printf '清理完成\n'

# ────────────────────────────────────────────────────────────
# 实用工具
# ────────────────────────────────────────────────────────────

psql: ## 连接数据库（需先启动基础设施）
	@PGPASSWORD=jinshu123 psql -h localhost -p 5432 -U jinshu -d jinshu

redis-cli: ## 连接 Redis（需先启动基础设施）
	redis-cli -h localhost -p 6379

.PHONY: env infra-up infra-down infra-reset infra-logs infra-ps infra-init-db
.PHONY: backend-compile backend-test backend-build backend-clean
.PHONY: backend-api backend-batch backend-worker backend-data-sync
.PHONY: backend-jar-api backend-jar-batch backend-jar-worker backend-jar-data-sync backend-jar-all
.PHONY: backend-coverage backend-check-license
.PHONY: frontend-install frontend-dev frontend-build frontend-preview
.PHONY: docker-backend docker-frontend docker-all
.PHONY: k8s-apply k8s-delete k8s-pods k8s-logs-backend k8s-logs-frontend k8s-logs-postgres
.PHONY: k8s-restart-backend k8s-restart-frontend k8s-status k8s-describe
.PHONY: deploy up dev check ci clean psql redis-cli
