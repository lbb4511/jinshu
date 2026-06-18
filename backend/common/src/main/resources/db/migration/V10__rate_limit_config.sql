-- =====================================================
-- D48 API 限流与租户并发配额配置表
-- =====================================================

-- 限流规则配置表：支持全局/租户级/用户级/登录 IP 级规则
CREATE TABLE IF NOT EXISTS sys.rate_limit_config (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        BIGINT,
    scope            VARCHAR(32) NOT NULL,
    resource_pattern VARCHAR(255) NOT NULL DEFAULT '*',
    max_requests     INT NOT NULL,
    window_seconds   INT NOT NULL,
    enabled          BOOLEAN NOT NULL DEFAULT true,
    created_at       TIMESTAMP DEFAULT NOW(),
    updated_at       TIMESTAMP DEFAULT NOW(),
    CONSTRAINT chk_rate_limit_scope CHECK (scope IN ('USER', 'TENANT', 'LOGIN_IP'))
);

COMMENT ON TABLE sys.rate_limit_config IS 'API 限流规则配置表';
COMMENT ON COLUMN sys.rate_limit_config.scope IS '限流维度：USER-用户级，TENANT-租户级，LOGIN_IP-登录 IP 级';
COMMENT ON COLUMN sys.rate_limit_config.resource_pattern IS '资源匹配模式，* 表示通配';
COMMENT ON COLUMN sys.rate_limit_config.max_requests IS '时间窗口内允许的最大请求数';
COMMENT ON COLUMN sys.rate_limit_config.window_seconds IS '滑动窗口时长（秒）';

CREATE INDEX IF NOT EXISTS idx_rate_limit_scope_tenant ON sys.rate_limit_config(scope, tenant_id);

-- 租户并发配额表：限制单个租户同时处理的活跃请求数
CREATE TABLE IF NOT EXISTS sys.tenant_concurrency_quota (
    tenant_id       BIGINT PRIMARY KEY,
    max_concurrent  INT NOT NULL,
    enabled         BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE sys.tenant_concurrency_quota IS '租户并发请求配额表';
COMMENT ON COLUMN sys.tenant_concurrency_quota.max_concurrent IS '租户最大并发活跃请求数';

-- 默认全局限流规则（与 API 安全设计文档一致）
INSERT INTO sys.rate_limit_config (tenant_id, scope, resource_pattern, max_requests, window_seconds, enabled)
VALUES (NULL, 'USER', '*', 100, 1, true),
       (NULL, 'TENANT', '*', 1000, 1, true),
       (NULL, 'LOGIN_IP', '/auth/login', 10, 60, true);

-- 默认租户并发配额
INSERT INTO sys.tenant_concurrency_quota (tenant_id, max_concurrent, enabled)
SELECT id, 100, true
FROM sys.tenant
WHERE NOT EXISTS (SELECT 1 FROM sys.tenant_concurrency_quota);
