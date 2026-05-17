# ai-services

> Python 辅助服务模块 —— AI 报表生成、数据科学分析、运维脚本

## 适用范围

✅ **允许**

- AI 报表生成（LLM 调用、Prompt 编排、自然语言转 SQL）
- 数据科学分析（Pandas / NumPy / Scikit-learn）
- 运维与数据脚本（一次性数据迁移、定时任务原型）

❌ **禁止**（参见 [决策 007](../docs/01-立项与架构/03.技术选型决策记录.md#决策-007引入-python-作为-ai数据科学辅助语言)）

- 业务核心逻辑（报表导入/导出、PDF 生成、审计日志）
- 直连业务数据库写操作（必须通过 Java API）
- 替代 Java 实现已有功能

## 目录结构

```
ai-services/
├── README.md                # 本文档
├── pyproject.toml           # 依赖与构建配置（uv / poetry）
├── .python-version          # Python 版本锁定
├── src/
│   └── jinshu_ai/
│       ├── __init__.py
│       ├── llm/             # LLM 报表生成
│       ├── analytics/       # 数据科学分析
│       └── scripts/         # 运维脚本
└── tests/
```

## 环境要求

- Python 3.12+
- uv（推荐）或 poetry
- 与 Java 服务通过 HTTP/gRPC 通信（部署方式待评估）

## 快速开始

```bash
# 安装 uv
curl -LsSf https://astral.sh/uv/install.sh | sh

# 同步依赖
uv sync

# 运行测试
uv run pytest

# 代码规范检查
uv run ruff check .
uv run mypy src
```

## 开发规范

| 项 | 规范 |
|---|------|
| 代码风格 | `ruff` (line-length=120) |
| 类型检查 | `mypy --strict` |
| 测试框架 | `pytest` + `pytest-cov`，核心模块覆盖率 ≥ 80% |
| 依赖管理 | `pyproject.toml`，禁止 `requirements.txt` |
| 日志 | `structlog`，JSON 格式输出 |

## 状态

🚧 **骨架阶段** —— 待编写设计文档（`docs/04-模块设计/` 下）后再开始实质开发，遵守"设计先行"约束。
