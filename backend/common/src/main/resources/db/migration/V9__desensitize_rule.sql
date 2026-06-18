-- =====================================================
-- 锦书企业级报表系统 - 数据库迁移脚本 V9
-- 功能: 敏感数据脱敏规则配置表
-- 设计文档: docs/03-安全与合规/04.数据加密标准.md
-- =====================================================

-- 1. 创建脱敏规则配置表
CREATE TABLE IF NOT EXISTS sys.desensitize_rule (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        BIGINT NOT NULL DEFAULT 0,
    table_name       VARCHAR(100) NOT NULL,
    column_name      VARCHAR(100) NOT NULL,
    rule_type        VARCHAR(30) NOT NULL,
    applicable_roles VARCHAR(200) NOT NULL DEFAULT 'VIEWER',
    enabled          BOOLEAN NOT NULL DEFAULT true,
    created_at       TIMESTAMP DEFAULT NOW(),
    updated_at       TIMESTAMP DEFAULT NOW(),

    UNIQUE(tenant_id, table_name, column_name)
);

-- 2. 创建租户与资源查询索引
CREATE INDEX IF NOT EXISTS idx_desensitize_rule_tenant
    ON sys.desensitize_rule (tenant_id, table_name);

COMMENT ON TABLE sys.desensitize_rule IS '脱敏规则配置表，支持运行时更新';
COMMENT ON COLUMN sys.desensitize_rule.tenant_id IS '租户ID，0 表示全局默认规则';
COMMENT ON COLUMN sys.desensitize_rule.table_name IS '表名 / 资源类型';
COMMENT ON COLUMN sys.desensitize_rule.column_name IS '列名 / 字段名';
COMMENT ON COLUMN sys.desensitize_rule.rule_type IS '脱敏规则类型：PHONE/ID_CARD/BANK_CARD/EMAIL/NAME/AMOUNT/FULL_MASK';
COMMENT ON COLUMN sys.desensitize_rule.applicable_roles IS '适用角色列表，逗号分隔，如 USER,VIEWER';
COMMENT ON COLUMN sys.desensitize_rule.enabled IS '是否启用';

-- 3. 初始化默认脱敏规则（全局）
INSERT INTO sys.desensitize_rule (tenant_id, table_name, column_name, rule_type, applicable_roles) VALUES
(0, 'users', 'email', 'EMAIL', 'USER,VIEWER'),
(0, 'users', 'display_name', 'NAME', 'USER,VIEWER'),
(0, 'users', 'phone', 'PHONE', 'USER,VIEWER'),
(0, 'users', 'id_card', 'ID_CARD', 'USER,VIEWER'),
(0, 'users', 'bank_card', 'BANK_CARD', 'USER,VIEWER')
ON CONFLICT (tenant_id, table_name, column_name) DO NOTHING;
