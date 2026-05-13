# run-tests 技能使用指南

运行锦书企业级报表系统的测试。

## 用法

```bash
/run-tests
```

## 功能说明

### 默认模式（单元测试）

运行后端和前端的单元测试：

1. ✅ 后端单元测试
2. ✅ 前端单元测试

## 选项说明

| 选项 | 说明 |
|-----|------|
| `--backend-only` | 仅运行后端测试 |
| `--frontend-only` | 仅运行前端测试 |
| `--unit-only` | 仅运行单元测试（默认） |
| `--integration-only` | 仅运行集成测试 |
| `--e2e-only` | 仅运行端到端测试 |
| `--watch` | 监听模式，文件变更时自动重跑 |
| `--coverage` | 生成测试覆盖率报告 |
| `--verbose` | 详细输出模式 |

## 使用示例

### 运行所有单元测试

```bash
/run-tests
```

### 仅运行后端测试

```bash
/run-tests --backend-only
```

### 仅运行前端测试

```bash
/run-tests --frontend-only
```

### 运行集成测试

```bash
/run-tests --integration-only
```

### 生成覆盖率报告

```bash
/run-tests --coverage
```

### 监听模式（开发时用）

```bash
/run-tests --watch
```

## 后端测试

### 单元测试

使用 JUnit 5 运行单元测试：

```bash
cd backend
./gradlew test
```

测试位置：`backend/*/src/test/java/`

### 集成测试

使用 Spring Boot Test 运行集成测试（需要数据库）：

```bash
cd backend
./gradlew integrationTest
```

集成测试位置：`backend/*/src/integrationTest/java/`

### 测试覆盖率

生成 JaCoCo 测试覆盖率报告：

```bash
cd backend
./gradlew jacocoTestReport
```

报告位置：`backend/*/build/reports/jacoco/test/html/`

### 覆盖率要求

| 模块 | 最低覆盖率 | 建议覆盖率 |
|-----|-----------|-----------|
| Common | 50% | 70% |
| API | 60% | 80% |
| Batch | 50% | 70% |
| Worker | 50% | 70% |

## 前端测试

### 单元测试

使用 Vitest 运行单元测试：

```bash
cd frontend
pnpm test
```

测试位置：`frontend/src/**/*.test.ts`、`frontend/src/**/*.test.tsx`

### 组件测试

使用 React Testing Library 进行组件测试：

```bash
cd frontend
pnpm test:components
```

### E2E 测试

使用 Playwright 运行端到端测试：

```bash
cd frontend
pnpm test:e2e
```

### 测试覆盖率

生成测试覆盖率报告：

```bash
cd frontend
pnpm test:coverage
```

报告位置：`frontend/coverage/`

## 测试策略

### 测试金字塔

```
        /\
       /E2E\         端到端测试（少量）
      /------\
     / 集成测试 \    集成测试（适中）
    /----------\
   /  单元测试   \   单元测试（大量）
  /--------------\
```

### 测试类型说明

| 类型 | 范围 | 速度 | 稳定性 | 数量 |
|-----|------|------|--------|------|
| 单元测试 | 单个函数/类 | 快 | 高 | 多 |
| 集成测试 | 模块间协作 | 中 | 中 | 中 |
| E2E 测试 | 完整用户流程 | 慢 | 低 | 少 |

## 测试配置

### 后端测试配置

- 测试框架: JUnit 5
- Mock 框架: Mockito
- Assertions: AssertJ
- 测试容器: TestContainers（集成测试）

### 前端测试配置

- 测试框架: Vitest
- 测试库: React Testing Library
- E2E: Playwright
- Assertions: Vitest built-in

## 测试最佳实践

### 单元测试原则

- ✅ 测试行为，不是实现细节
- ✅ 每个测试独立运行
- ✅ 清晰的 Given-When-Then 结构
- ✅ 使用有意义的测试名称
- ❌ 不要过度 mock

### 集成测试原则

- ✅ 测试模块间的交互
- ✅ 使用真实的数据库（TestContainers）
- ✅ 每个测试清理数据
- ✅ 测试关键业务流程

### E2E 测试原则

- ✅ 测试最重要的用户流程
- ✅ 不要测试边缘情况（留给单元测试）
- ✅ 使用可预测的测试数据
- ✅ 考虑测试的稳定性

## CI/CD 集成

在 CI/CD 流水线上自动运行：

```yaml
# .github/workflows/ci.yml
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run tests
        run: /run-tests --coverage
```

## 常见问题

### 集成测试失败

检查数据库等基础设施是否正在运行：

```bash
/check-health --infra-only
```

### 测试超时

增加超时时间或优化测试。

### 覆盖率不达标

先检查覆盖率报告，找出未覆盖的代码：

```bash
/run-tests --coverage
```

然后添加相应的测试。

## 相关文档

- [pre-commit](../pre-commit/README.md) - 提交前检查
- [CONTRIBUTING.md](../../CONTRIBUTING.md) - 贡献指南
