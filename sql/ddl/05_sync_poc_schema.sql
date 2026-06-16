-- =====================================================
-- 5. POC 阶段 schema 同步
-- 将 dev 数据库手动 ALTER 的变更代码化，确保新/旧环境一致
-- 注意：此脚本对已有数据是安全的（ALTER ... IF NOT EXISTS / DROP ... IF EXISTS）
-- =====================================================

-- 1. sys.tenant: 补充 description
ALTER TABLE sys.tenant
    ADD COLUMN IF NOT EXISTS description TEXT;

COMMENT ON COLUMN sys.tenant.description IS '租户描述';

-- 2. sys.users: 补充最后登录 IP 与密码更新时间
ALTER TABLE sys.users
    ADD COLUMN IF NOT EXISTS last_login_ip      VARCHAR(45),
    ADD COLUMN IF NOT EXISTS password_updated_at TIMESTAMP;

COMMENT ON COLUMN sys.users.last_login_ip IS '最后登录 IP';
COMMENT ON COLUMN sys.users.password_updated_at IS '密码最后更新时间';

-- 3. meta.report_metadata: 补充状态字段
ALTER TABLE meta.report_metadata
    ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'DRAFT';

COMMENT ON COLUMN meta.report_metadata.status IS '报表状态（DRAFT/PUBLISHED/ARCHIVED 等）';

-- 4. task.task: POC 阶段字段调整
--    - 增加 cancelled_at
--    - 将 config (JSONB) 迁移为 parameters (TEXT)
--    - 移除旧的 type 列（已被 task_type 替代）

ALTER TABLE task.task
    ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP;

COMMENT ON COLUMN task.task.cancelled_at IS '任务取消时间';

-- 如果存在旧的 type 列则删除（业务已使用 task_type）
ALTER TABLE task.task
    DROP COLUMN IF EXISTS type;

-- 如果存在 config 列且不存在 parameters 列，则迁移
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'task' AND table_name = 'task' AND column_name = 'config'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'task' AND table_name = 'task' AND column_name = 'parameters'
    ) THEN
        -- 新增 parameters 列
        ALTER TABLE task.task ADD COLUMN parameters TEXT;
        -- 把 JSONB 序列化后写入 TEXT
        UPDATE task.task SET parameters = config::TEXT WHERE parameters IS NULL;
        -- 删除旧列
        ALTER TABLE task.task DROP COLUMN config;
    END IF;
END $$;

-- 如果 parameters 列还不存在（新环境或上面未命中），直接创建
ALTER TABLE task.task
    ADD COLUMN IF NOT EXISTS parameters TEXT;

COMMENT ON COLUMN task.task.parameters IS '任务参数（JSON 文本）';
