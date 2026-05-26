-- =====================================================
-- 锦书企业级报表系统 - 数据库初始化脚本
-- 数据库版本: PostgreSQL 16+
-- 创建日期: 2026-05-12
-- 更新日期: 2026-05-24
-- 变更说明: 引入 schema 划分（sys/meta/data/workflow/audit/task）
-- 设计文档: docs/01-立项与架构/06.数据库架构设计.md (§5 Schema 划分方案)
-- 说明: 此脚本可重复执行，所有语句均为幂等
-- =====================================================

-- 启用 UUID 扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =====================================================
-- 0. 创建 Schema（6 个逻辑域）
-- =====================================================

CREATE SCHEMA IF NOT EXISTS sys;
CREATE SCHEMA IF NOT EXISTS meta;
CREATE SCHEMA IF NOT EXISTS data;
CREATE SCHEMA IF NOT EXISTS workflow;
CREATE SCHEMA IF NOT EXISTS audit;
CREATE SCHEMA IF NOT EXISTS task;

-- =====================================================
-- 1. sys — 系统管理 Schema（低频率变更、高安全等级）
-- =====================================================

CREATE TABLE IF NOT EXISTS sys.tenant (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(128) NOT NULL,
    code            VARCHAR(64) NOT NULL UNIQUE,
    status          VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    quota_config    JSONB,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE sys.tenant IS '租户表';
COMMENT ON COLUMN sys.tenant.quota_config IS '配额配置：存储容量、并发任务数等';

CREATE TABLE IF NOT EXISTS sys.users (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    username        VARCHAR(64) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    email           VARCHAR(128),
    phone           VARCHAR(20),
    display_name    VARCHAR(128),
    role            VARCHAR(32) NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    login_fail_count INTEGER NOT NULL DEFAULT 0,
    locked_until    TIMESTAMP,
    last_login_at   TIMESTAMP,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW(),
    UNIQUE(tenant_id, username)
);

CREATE INDEX IF NOT EXISTS idx_users_tenant ON sys.users(tenant_id);

-- =====================================================
-- 2. meta — 动态元数据 Schema（低频率读写、强一致性）
-- =====================================================

CREATE TABLE IF NOT EXISTS meta.report_metadata (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    category        VARCHAR(100),
    schema_version  INTEGER NOT NULL DEFAULT 1,
    columns_meta    JSONB DEFAULT '[]',
    data_source_id  BIGINT,
    template_config JSONB,
    created_by      BIGINT,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW(),
    is_deleted      BOOLEAN NOT NULL DEFAULT false,
    deleted_at      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_report_metadata_tenant ON meta.report_metadata(tenant_id);
CREATE INDEX IF NOT EXISTS idx_report_metadata_not_deleted
    ON meta.report_metadata(tenant_id, is_deleted) WHERE is_deleted = false;

COMMENT ON TABLE meta.report_metadata IS '报表元数据（含 columns_meta JSONB 列定义）';

CREATE TABLE IF NOT EXISTS meta.data_source (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    name            VARCHAR(128) NOT NULL,
    type            VARCHAR(32) NOT NULL,
    connection_config JSONB NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by      BIGINT,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_data_source_tenant ON meta.data_source(tenant_id);

-- =====================================================
-- 3. data — 报表数据 Schema（高频写入 + 高吞吐读取）
-- =====================================================

CREATE TABLE IF NOT EXISTS data.report_data (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    report_id       BIGINT NOT NULL,
    row_no          INTEGER NOT NULL,
    data_json       JSONB NOT NULL,
    created_at      TIMESTAMP DEFAULT NOW(),
    UNIQUE(tenant_id, report_id, row_no)
);

CREATE INDEX IF NOT EXISTS idx_report_data_report ON data.report_data(tenant_id, report_id);
CREATE INDEX IF NOT EXISTS idx_report_data_gin ON data.report_data USING GIN(data_json);

COMMENT ON TABLE data.report_data IS '报表数据宽表（JSONB 宽表模式，支持 GIN 索引）';

-- =====================================================
-- 4. workflow — 审批流程 Schema（中频事务）
-- =====================================================

CREATE TABLE IF NOT EXISTS workflow.report_workflows (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    report_id       BIGINT NOT NULL,
    status          VARCHAR(32) NOT NULL,
    current_step    VARCHAR(50),
    assignee_id     BIGINT,
    remark          TEXT,
    created_by      BIGINT NOT NULL,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW(),
    reviewed_by     BIGINT,
    reviewed_at     TIMESTAMP,
    review_comment  TEXT
);

CREATE INDEX IF NOT EXISTS idx_workflows_tenant ON workflow.report_workflows(tenant_id);

COMMENT ON TABLE workflow.report_workflows IS '报表审批工作流（状态机：DRAFT→PENDING_REVIEW→APPROVED/REJECTED→PUBLISHED）';

-- =====================================================
-- 5. audit — 审计日志 Schema（仅追加写入 + 只读查询）
-- =====================================================

CREATE TABLE IF NOT EXISTS audit.audit_log (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    username        VARCHAR(64) NOT NULL,
    operation       VARCHAR(32) NOT NULL,
    target_type     VARCHAR(32),
    target_id       BIGINT,
    target_name     VARCHAR(255),
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(512),
    request_params  JSONB,
    response_data   JSONB,
    status          VARCHAR(16) NOT NULL,
    error_message   TEXT,
    created_at      TIMESTAMP DEFAULT NOW(),
    log_hash        VARCHAR(64) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_tenant_time ON audit.audit_log(tenant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_user ON audit.audit_log(user_id);

-- 审计日志防篡改：禁止更新和删除
CREATE OR REPLACE FUNCTION audit.prevent_audit_modify() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION '审计日志不可修改或删除';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS audit_log_no_update ON audit.audit_log;
CREATE TRIGGER audit_log_no_update BEFORE UPDATE OR DELETE ON audit.audit_log
    FOR EACH ROW EXECUTE FUNCTION audit.prevent_audit_modify();

COMMENT ON TABLE audit.audit_log IS '审计日志（触发器禁止 UPDATE/DELETE，防篡改）';

CREATE TABLE IF NOT EXISTS audit.audit_root_hash (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    hour_start      TIMESTAMP NOT NULL,
    root_hash       VARCHAR(64) NOT NULL,
    log_count       INTEGER NOT NULL,
    created_at      TIMESTAMP DEFAULT NOW(),
    UNIQUE(tenant_id, hour_start)
);

CREATE INDEX IF NOT EXISTS idx_audit_root_tenant ON audit.audit_root_hash(tenant_id);

COMMENT ON TABLE audit.audit_root_hash IS '审计日志哈希根（每小时每租户一条，用于完整性校验）';

-- =====================================================
-- 6. task — 任务调度 Schema（中频读写）
-- =====================================================

CREATE TABLE IF NOT EXISTS task.task (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    type            VARCHAR(32) NOT NULL,
    status          VARCHAR(16) NOT NULL,
    priority        INTEGER NOT NULL DEFAULT 0,
    config          JSONB,
    progress        INTEGER DEFAULT 0,
    error_message   TEXT,
    created_by      BIGINT,
    created_at      TIMESTAMP DEFAULT NOW(),
    started_at      TIMESTAMP,
    completed_at    TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_task_tenant_status ON task.task(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_task_priority ON task.task(priority DESC, created_at);

COMMENT ON TABLE task.task IS '任务主表（导入/导出/PDF 生成等异步任务）';
