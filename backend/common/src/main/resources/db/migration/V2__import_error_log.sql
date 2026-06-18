-- =====================================================
-- 锦书企业级报表系统 - 导入错误日志表
-- 所属 Schema: data
-- 设计文档: docs/04-模块设计/01.报表导入模块设计.md (§4.2)
-- =====================================================

CREATE TABLE IF NOT EXISTS data.import_error_log (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    task_id         BIGINT NOT NULL,
    report_id       BIGINT NOT NULL,
    row_no          INTEGER NOT NULL,
    column_name     VARCHAR(128),
    error_type      VARCHAR(32) NOT NULL,
    error_message   VARCHAR(512) NOT NULL,
    cell_value      TEXT,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_import_error_task
    ON data.import_error_log(task_id, row_no);

CREATE INDEX IF NOT EXISTS idx_import_error_tenant
    ON data.import_error_log(tenant_id, created_at DESC);

COMMENT ON TABLE data.import_error_log IS '报表导入错误日志（精确到行号和列名）';
COMMENT ON COLUMN data.import_error_log.error_type IS '错误类型: REQUIRED / FORMAT / TYPE / BUSINESS';
COMMENT ON COLUMN data.import_error_log.cell_value IS '原始单元格值（截断 200 字）';
