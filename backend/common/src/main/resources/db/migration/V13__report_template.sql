-- =====================================================
-- V13. 报表模板表
-- 支持模板市场：系统级公共模板 + 租户私有/公开模板
-- =====================================================

CREATE TABLE IF NOT EXISTS meta.report_template (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    name            VARCHAR(128) NOT NULL,
    description     TEXT,
    category        VARCHAR(64),
    thumbnail_url   VARCHAR(512),
    layout_json     JSONB NOT NULL,
    sample_data     JSONB,
    is_public       BOOLEAN DEFAULT false,
    is_system       BOOLEAN DEFAULT false,
    status          VARCHAR(16) DEFAULT 'ACTIVE',
    created_by      BIGINT,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE meta.report_template IS '报表模板表';
COMMENT ON COLUMN meta.report_template.tenant_id IS '租户ID，0 表示系统级公共模板';
COMMENT ON COLUMN meta.report_template.name IS '模板名称';
COMMENT ON COLUMN meta.report_template.description IS '模板描述';
COMMENT ON COLUMN meta.report_template.category IS '模板分类，如 SALES、FINANCE、HR';
COMMENT ON COLUMN meta.report_template.thumbnail_url IS '缩略图 URL';
COMMENT ON COLUMN meta.report_template.layout_json IS '报表布局 JSON';
COMMENT ON COLUMN meta.report_template.sample_data IS '示例数据 JSON';
COMMENT ON COLUMN meta.report_template.is_public IS '是否公开';
COMMENT ON COLUMN meta.report_template.is_system IS '是否系统级模板，系统级模板不可修改/删除';
COMMENT ON COLUMN meta.report_template.status IS '状态：ACTIVE/INACTIVE';
COMMENT ON COLUMN meta.report_template.created_by IS '创建人ID';

CREATE INDEX IF NOT EXISTS idx_report_template_tenant_status ON meta.report_template(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_report_template_category ON meta.report_template(category);
CREATE INDEX IF NOT EXISTS idx_report_template_public_status ON meta.report_template(is_public, status);
