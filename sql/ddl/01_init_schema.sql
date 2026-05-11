-- =====================================================
-- 锦书企业级报表系统 - 数据库初始化脚本
-- 数据库版本: PostgreSQL 15+
-- 创建日期: 2026-05-12
-- =====================================================

-- 启用 UUID 扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =====================================================
-- 1. 租户与用户表
-- =====================================================

CREATE TABLE tenant (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(128) NOT NULL,
    code            VARCHAR(64) NOT NULL UNIQUE,
    status          VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    quota_config     JSONB,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE tenant IS '租户表';
COMMENT ON COLUMN tenant.quota_config IS '配额配置：存储容量、并发任务数等';

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    username        VARCHAR(64) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    email           VARCHAR(128),
    role            VARCHAR(32) NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW(),
    UNIQUE(tenant_id, username)
);

CREATE INDEX idx_users_tenant ON users(tenant_id);

-- =====================================================
-- 2. 报表元数据表
-- =====================================================

CREATE TABLE report_metadata (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    schema_version  INTEGER NOT NULL DEFAULT 1,
    data_source_id  BIGINT,
    template_config   JSONB,
    created_by      BIGINT,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_report_metadata_tenant ON report_metadata(tenant_id);

-- =====================================================
-- 3. 报表宽表（核心数据表
-- =====================================================

CREATE TABLE report_data (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    report_id       BIGINT NOT NULL REFERENCES report_metadata(id),
    row_no          INTEGER NOT NULL,
    data_json       JSONB NOT NULL,
    created_at      TIMESTAMP DEFAULT NOW(),
    UNIQUE(tenant_id, report_id, row_no)
);

CREATE INDEX idx_report_data_report ON report_data(tenant_id, report_id);
CREATE INDEX idx_report_data_gin ON report_data USING GIN(data_json);

-- =====================================================
-- 4. 工作流状态表
-- =====================================================

CREATE TABLE report_workflows (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    report_id       BIGINT NOT NULL REFERENCES report_metadata(id),
    status          VARCHAR(32) NOT NULL,
    created_by      BIGINT NOT NULL,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW(),
    reviewed_by     BIGINT,
    reviewed_at     TIMESTAMP,
    review_comment  TEXT
);

CREATE INDEX idx_workflows_tenant ON report_workflows(tenant_id);

-- =====================================================
-- 5. 审计日志表
-- =====================================================

CREATE TABLE audit_log (
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

CREATE INDEX idx_audit_tenant_time ON audit_log(tenant_id, created_at DESC);
CREATE INDEX idx_audit_user ON audit_log(user_id);

-- 审计日志防篡改：禁止更新和删除
CREATE OR REPLACE FUNCTION prevent_audit_modify() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION '审计日志不可修改或删除';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_log_no_update BEFORE UPDATE OR DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION prevent_audit_modify();

-- =====================================================
-- 6. 审计日志根哈希表（每小时生成）
-- =====================================================

CREATE TABLE audit_root_hash (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    hour_start       TIMESTAMP NOT NULL,
    root_hash       VARCHAR(64) NOT NULL,
    log_count       INTEGER NOT NULL,
    created_at      TIMESTAMP DEFAULT NOW(),
    UNIQUE(tenant_id, hour_start)
);

-- =====================================================
-- 7. 数据源配置表
-- =====================================================

CREATE TABLE data_source (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    name            VARCHAR(128) NOT NULL,
    type            VARCHAR(32) NOT NULL,
    connection_config JSONB NOT NULL,
    created_by      BIGINT,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_data_source_tenant ON data_source(tenant_id);

-- =====================================================
-- 8. 任务表
-- =====================================================

CREATE TABLE task (
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

CREATE INDEX idx_task_tenant_status ON task(tenant_id, status);
CREATE INDEX idx_task_priority ON task(priority DESC, created_at);
