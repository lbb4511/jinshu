-- =====================================================
-- 4. 补充 audit_log 表哈希链字段
-- =====================================================

ALTER TABLE audit.audit_log
    ADD COLUMN IF NOT EXISTS previous_hash VARCHAR(64);

COMMENT ON COLUMN audit.audit_log.previous_hash IS '上一条审计日志的 log_hash，构成哈希链';
