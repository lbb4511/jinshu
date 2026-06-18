-- =====================================================
-- 锦书企业级报表系统 - 数据库迁移脚本 V7
-- 变更说明: data_source.connection_config 改为 TEXT 类型，
--           以支持 AES-256-GCM 字段级加密后的密文存储。
-- 注意: 已有数据会从 JSONB 自动 cast 为 TEXT；
--       生产环境上线后需运行数据重加密任务将明文转为密文。
-- =====================================================

ALTER TABLE meta.data_source
    ALTER COLUMN connection_config TYPE TEXT;

COMMENT ON COLUMN meta.data_source.connection_config IS '数据源连接配置（JSON 字符串，已 AES-256-GCM 加密）';
