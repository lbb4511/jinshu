-- =====================================================
-- 锦书企业级报表系统 - 数据库迁移脚本 V8
-- 功能: RBAC 五角色体系约束与角色变更审计日志
-- 设计文档: docs/03-安全与合规/06.权限控制矩阵.md
-- =====================================================

-- 1. 为 sys.users 表增加角色 CHECK 约束，限定五角色体系
ALTER TABLE sys.users
    ADD CONSTRAINT chk_user_role
        CHECK (role IN ('ADMIN', 'SECURITY_ADMIN', 'AUDITOR', 'USER', 'VIEWER'));

-- 2. 创建角色变更审计日志表
CREATE TABLE IF NOT EXISTS sys.role_change_log (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        BIGINT NOT NULL,
    target_user_id   BIGINT NOT NULL,
    operator_user_id BIGINT NOT NULL,
    old_role         VARCHAR(30),
    new_role         VARCHAR(30) NOT NULL,
    reason           VARCHAR(500),
    created_at       TIMESTAMP DEFAULT NOW(),

    CONSTRAINT chk_role_change_old_role CHECK (
        old_role IS NULL OR old_role IN ('ADMIN', 'SECURITY_ADMIN', 'AUDITOR', 'USER', 'VIEWER')
    ),
    CONSTRAINT chk_role_change_new_role CHECK (
        new_role IN ('ADMIN', 'SECURITY_ADMIN', 'AUDITOR', 'USER', 'VIEWER')
    )
);

-- 3. 创建目标用户查询索引
CREATE INDEX IF NOT EXISTS idx_role_change_log_target
    ON sys.role_change_log (target_user_id, created_at DESC);

COMMENT ON TABLE sys.role_change_log IS '用户角色变更审计日志';
COMMENT ON COLUMN sys.role_change_log.target_user_id IS '被变更角色的目标用户ID';
COMMENT ON COLUMN sys.role_change_log.operator_user_id IS '执行角色变更的操作人ID';
COMMENT ON COLUMN sys.role_change_log.old_role IS '变更前角色';
COMMENT ON COLUMN sys.role_change_log.new_role IS '变更后角色';
COMMENT ON COLUMN sys.role_change_log.reason IS '变更原因';
