-- =====================================================
-- 锦书报表系统 - 测试数据初始化脚本
-- =====================================================

-- -----------------------------------------------------
-- 1. 业务测试表：销售订单（供报表查询使用）
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS data.sales_orders (
    id              BIGSERIAL PRIMARY KEY,
    order_no        VARCHAR(32) NOT NULL,
    customer_name   VARCHAR(128) NOT NULL,
    product_name    VARCHAR(128) NOT NULL,
    category        VARCHAR(50),
    amount          DECIMAL(12, 2) NOT NULL,
    quantity        INTEGER NOT NULL DEFAULT 1,
    region          VARCHAR(50),
    order_date      DATE NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'completed'
);

CREATE INDEX IF NOT EXISTS idx_sales_orders_date ON data.sales_orders(order_date);
CREATE INDEX IF NOT EXISTS idx_sales_orders_region ON data.sales_orders(region);
CREATE INDEX IF NOT EXISTS idx_sales_orders_category ON data.sales_orders(category);

TRUNCATE TABLE data.sales_orders RESTART IDENTITY;

INSERT INTO data.sales_orders (order_no, customer_name, product_name, category, amount, quantity, region, order_date, status) VALUES
('SO202501001', '张三科技有限公司', '企业版 SaaS 订阅', '软件服务', 128000.00, 10, '华东', '2025-01-15', 'completed'),
('SO202501002', '李四贸易有限公司', '云服务器 ECS-8C16G', '云计算', 56000.00, 4, '华北', '2025-01-18', 'completed'),
('SO202501003', '王五制造集团', '智能报表系统', '软件服务', 256000.00, 1, '华南', '2025-01-22', 'completed'),
('SO202501004', '赵六物流股份', '数据存储包 10TB', '云计算', 24000.00, 2, '华东', '2025-02-05', 'completed'),
('SO202501005', '钱七电商平台', 'API 网关服务', '软件服务', 86000.00, 1, '西南', '2025-02-14', 'completed'),
('SO202501006', '孙八金融控股', '风控分析模块', '软件服务', 320000.00, 1, '华北', '2025-02-20', 'completed'),
('SO202501007', '周九医疗器械', '云数据库 RDS', '云计算', 48000.00, 3, '华东', '2025-03-01', 'completed'),
('SO202501008', '吴十教育机构', '在线考试系统', '软件服务', 64000.00, 2, '华中', '2025-03-08', 'completed'),
('SO202501009', '郑十一传媒公司', '视频转码服务', '云计算', 18000.00, 6, '华南', '2025-03-15', 'completed'),
('SO202501010', '陈十二零售连锁', '会员管理系统', '软件服务', 96000.00, 8, '华东', '2025-03-22', 'completed'),
('SO202501011', '张三科技有限公司', '数据备份服务', '云计算', 12000.00, 1, '华东', '2025-04-01', 'completed'),
('SO202501012', '李四贸易有限公司', '负载均衡 SLB', '云计算', 36000.00, 2, '华北', '2025-04-10', 'completed'),
('SO202501013', '王五制造集团', '工业物联网平台', '软件服务', 198000.00, 1, '华南', '2025-04-18', 'completed'),
('SO202501014', '赵六物流股份', '实时定位服务', '软件服务', 54000.00, 3, '华东', '2025-04-25', 'completed'),
('SO202501015', '钱七电商平台', '大促弹性扩容', '云计算', 72000.00, 5, '西南', '2025-05-01', 'pending');

-- -----------------------------------------------------
-- 2. 系统测试数据
-- -----------------------------------------------------

-- 数据源：指向当前 jinshu 数据库本身
INSERT INTO meta.data_source (
    tenant_id, name, type, host, port, database_name, username,
    connection_config, status, description, created_by, created_at, updated_at
) VALUES (
    1, '锦书本地数据库', 'POSTGRESQL', 'localhost', 5432, 'jinshu', 'jinshu',
    '{"driver":"org.postgresql.Driver","ssl":false}'::jsonb,
    'ACTIVE', '系统内置测试数据源，指向当前 PostgreSQL 实例', 1, NOW(), NOW()
) ON CONFLICT DO NOTHING;

-- 报表元数据：销售统计报表
INSERT INTO meta.report_metadata (
    tenant_id, name, description, category, schema_version, columns_meta,
    data_source_id, template_config, created_by, created_at, updated_at
) VALUES (
    1, '2025年销售订单统计报表', '按地区、品类汇总展示2025年销售订单数据', '销售分析',
    1,
    '[
        {"field":"region","title":"地区","type":"STRING","width":120},
        {"field":"category","title":"品类","type":"STRING","width":120},
        {"field":"order_count","title":"订单数","type":"NUMBER","width":100},
        {"field":"total_amount","title":"总金额","type":"CURRENCY","width":150},
        {"field":"total_quantity","title":"总数量","type":"NUMBER","width":100}
    ]'::jsonb,
    (SELECT id FROM meta.data_source WHERE tenant_id = 1 LIMIT 1),
    '{"chartType":"bar","groupBy":"region"}'::jsonb,
    1, NOW(), NOW()
) ON CONFLICT DO NOTHING;

-- 报表数据：预聚合的统计结果（宽表模式）
TRUNCATE TABLE data.report_data RESTART IDENTITY;

INSERT INTO data.report_data (tenant_id, report_id, row_no, data_json)
SELECT 1, id, row.row_no, row.data_json
FROM meta.report_metadata
CROSS JOIN LATERAL (VALUES
    (1, '{"region":"华东","category":"软件服务","order_count":3,"total_amount":320000.00,"total_quantity":19}'::jsonb),
    (2, '{"region":"华东","category":"云计算","order_count":2,"total_amount":72000.00,"total_quantity":5}'::jsonb),
    (3, '{"region":"华北","category":"软件服务","order_count":1,"total_amount":320000.00,"total_quantity":1}'::jsonb),
    (4, '{"region":"华北","category":"云计算","order_count":1,"total_amount":56000.00,"total_quantity":4}'::jsonb),
    (5, '{"region":"华南","category":"软件服务","order_count":2,"total_amount":454000.00,"total_quantity":2}'::jsonb),
    (6, '{"region":"华南","category":"云计算","order_count":1,"total_amount":18000.00,"total_quantity":6}'::jsonb),
    (7, '{"region":"西南","category":"软件服务","order_count":1,"total_amount":86000.00,"total_quantity":1}'::jsonb),
    (8, '{"region":"西南","category":"云计算","order_count":1,"total_amount":72000.00,"total_quantity":5}'::jsonb),
    (9, '{"region":"华中","category":"软件服务","order_count":1,"total_amount":64000.00,"total_quantity":2}'::jsonb)
) AS row(row_no, data_json)
WHERE meta.report_metadata.tenant_id = 1 AND meta.report_metadata.name = '2025年销售订单统计报表';
