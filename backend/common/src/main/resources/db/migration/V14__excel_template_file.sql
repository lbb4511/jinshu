-- =====================================================
-- V14. Excel 模板文件元数据表
-- 支持模板驱动导出：上传 .xlsx 模板并关联报表
-- =====================================================

CREATE TABLE IF NOT EXISTS meta.excel_template_file (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    report_id       BIGINT,
    file_name       VARCHAR(256),
    file_path       VARCHAR(512) NOT NULL,
    file_size       BIGINT,
    created_by      BIGINT,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE meta.excel_template_file IS 'Excel 模板文件元数据表';
COMMENT ON COLUMN meta.excel_template_file.tenant_id IS '租户 ID';
COMMENT ON COLUMN meta.excel_template_file.report_id IS '关联报表 ID，可选';
COMMENT ON COLUMN meta.excel_template_file.file_name IS '原始文件名';
COMMENT ON COLUMN meta.excel_template_file.file_path IS '文件在存储系统中的绝对路径';
COMMENT ON COLUMN meta.excel_template_file.file_size IS '文件大小（字节）';
COMMENT ON COLUMN meta.excel_template_file.created_by IS '创建人 ID';

CREATE INDEX IF NOT EXISTS idx_excel_template_file_tenant_report
    ON meta.excel_template_file(tenant_id, report_id);
