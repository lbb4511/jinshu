-- =====================================================
-- V12. 补充任务结果文件元数据字段
-- 用于支持大文件分片下载，记录导出/生成任务的实际输出文件信息
-- =====================================================

ALTER TABLE task.task
    ADD COLUMN IF NOT EXISTS result_file_path VARCHAR(1024),
    ADD COLUMN IF NOT EXISTS result_file_size BIGINT,
    ADD COLUMN IF NOT EXISTS result_file_name VARCHAR(512);

COMMENT ON COLUMN task.task.result_file_path IS '结果文件绝对路径';
COMMENT ON COLUMN task.task.result_file_size IS '结果文件大小（字节）';
COMMENT ON COLUMN task.task.result_file_name IS '结果文件下载名称';

CREATE INDEX IF NOT EXISTS idx_task_result_file_path ON task.task(result_file_path);
