-- =====================================================
-- 3. 补充 task 表字段
-- 在原有 type/config 基础上补充实体映射字段
-- =====================================================

ALTER TABLE task.task
    ADD COLUMN IF NOT EXISTS report_id      BIGINT,
    ADD COLUMN IF NOT EXISTS task_type      VARCHAR(32),
    ADD COLUMN IF NOT EXISTS parent_task_id BIGINT,
    ADD COLUMN IF NOT EXISTS shard_seq      INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS shard_total    INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS result         TEXT;

COMMENT ON COLUMN task.task.report_id IS '关联报表 ID';
COMMENT ON COLUMN task.task.task_type IS '任务类型（IMPORT/EXPORT/PDF）';
COMMENT ON COLUMN task.task.parent_task_id IS '父任务 ID（分片任务使用）';
COMMENT ON COLUMN task.task.shard_seq IS '分片序号';
COMMENT ON COLUMN task.task.shard_total IS '分片总数';
COMMENT ON COLUMN task.task.result IS '任务结果（如输出文件路径）';
