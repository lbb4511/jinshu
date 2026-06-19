-- =====================================================
-- 锦书企业级报表系统 - MQ 死信消息记录表
-- 所属 Schema: task
-- 设计文档: docs/04-模块设计/07.消息队列设计.md
-- =====================================================

CREATE TABLE IF NOT EXISTS task.mq_dead_letter (
    id              BIGSERIAL PRIMARY KEY,
    msg_id          VARCHAR(64),
    task_id         BIGINT,
    message_type    VARCHAR(32),
    source_queue    VARCHAR(128),
    error_message   TEXT,
    payload         JSONB,
    x_death_count   INTEGER DEFAULT 0,
    status          VARCHAR(16) DEFAULT 'PENDING',
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_mq_dead_letter_task
    ON task.mq_dead_letter(task_id);

CREATE INDEX IF NOT EXISTS idx_mq_dead_letter_created
    ON task.mq_dead_letter(created_at DESC);

COMMENT ON TABLE task.mq_dead_letter IS 'MQ 死信队列消息记录（消费端最终兜底）';
COMMENT ON COLUMN task.mq_dead_letter.msg_id IS '原始消息唯一 ID';
COMMENT ON COLUMN task.mq_dead_letter.x_death_count IS '进入死信队列前的死亡次数';
COMMENT ON COLUMN task.mq_dead_letter.status IS '处理状态: PENDING / PROCESSED';
