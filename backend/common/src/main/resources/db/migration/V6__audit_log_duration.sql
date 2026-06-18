-- =====================================================
-- 6. 审计日志增加耗时字段
-- =====================================================

ALTER TABLE audit.audit_log
    ADD COLUMN IF NOT EXISTS duration INTEGER;

COMMENT ON COLUMN audit.audit_log.duration IS '操作耗时（毫秒）';
