# 测试数据目录

## 目录结构

```
test-data/
├── excel/
│   ├── valid_100_rows.xlsx      # 正常小文件
│   ├── valid_10000_rows.xlsx    # 正常中等文件
│   ├── valid_1000000_rows.xlsx  # 百万行大文件
│   ├── empty_file.xlsx           # 空文件
│   ├── corrupted.xlsx            # 损坏文件
│   ├── wrong_format.pdf          # 格式错误
│   └── special_chars.xlsx        # 特殊字符边界
├── sql/
│   ├── tenant_1_basic_data.sql   # 租户 1 基础数据
│   └── multi_tenant_test_data.sql # 多租户隔离测试数据
└── pdf/
    └── sample_10_pages.pdf       # PDF 渲染测试
```

## 测试数据原则

1. **真实但脱敏**：数据格式与生产一致，但无真实敏感信息
2. **覆盖边界**：空文件、超大文件、特殊字符、异常格式
3. **可重复生成**：所有测试数据应有生成脚本
4. **版本控制**：小文件纳入 Git，大文件使用 Git LFS

## 数据生成工具

`scripts/generate-test-data.py` - Python 脚本生成各种规格的 Excel 测试文件
