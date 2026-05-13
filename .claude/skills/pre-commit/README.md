# pre-commit 技能使用指南

提交前完整检查，确保代码质量。

## 用法

```bash
/pre-commit
```

## 功能说明

### 默认模式（完整检查）

运行所有检查项：

1. ✅ 代码格式化检查
2. ✅ 后端 Java 编译检查
3. ✅ 前端 TypeScript 类型检查
4. ✅ ESLint 代码检查
5. ✅ 多租户 SQL 检查
6. ✅ 单元测试

## 选项说明

| 选项 | 说明 |
|-----|------|
| `--quick` | 快速模式，跳过慢的检查（如集成测试） |
| `--skip-tests` | 跳过所有测试 |
| `--skip-format` | 跳过代码格式化检查 |
| `--skip-lint` | 跳过代码检查 |
| `--skip-tenantsql` | 跳过多租户 SQL 检查 |

## 使用示例

### 完整检查（提交前）

```bash
/pre-commit
```

### 快速检查（赶时间时）

```bash
/pre-commit --quick --skip-tests
```

### 仅做最基本检查

```bash
/pre-commit --quick --skip-tests --skip-lint
```

## 检查项详情

### 1. 代码格式化检查 ✅

检查代码是否已格式化：

- 后端：Spotless 检查
- 前端：Prettier 检查

如果没有通过，运行：

```bash
/format-code
```

### 2. 后端编译检查 ✅

编译所有后端模块：

```bash
cd backend
./gradlew compileJava
```

### 3. 前端类型检查 ✅

运行 TypeScript 类型检查：

```bash
cd frontend
pnpm tsc --noEmit
```

### 4. ESLint 检查 ✅

运行 ESLint 检查前端代码：

```bash
cd frontend
pnpm lint
```

### 5. 多租户 SQL 检查 ✅

检查 SQL 是否符合多租户规范：

```bash
/check-tenantsql backend/api/src/main/resources/mapper/ --strict
```

### 6. 单元测试 ✅

运行后端和前端的单元测试：

```bash
cd backend
./gradlew test

cd frontend
pnpm test
```

## 检查结果

### 通过

```
╔══════════════════════════════════════════════════════════════╗
║           ✅ 所有检查通过，可以提交代码！                         ║
╚══════════════════════════════════════════════════════════════╝
```

### 失败

```
╔══════════════════════════════════════════════════════════════╗
║           ❌ 检查失败，请修复以下问题后再提交                    ║
╚══════════════════════════════════════════════════════════════╝

[代码格式化] ❌ 失败
  运行 /format-code 自动修复

[ESLint] ❌ 失败
  查看上面的错误信息
```

## 修复建议

### 格式化问题

```bash
/format-code
```

### 类型错误

查看 TypeScript 错误信息并修复。

### Lint 错误

查看 ESLint 错误信息并修复，部分可以自动修复：

```bash
cd frontend
pnpm lint --fix
```

### 多租户 SQL 问题

```bash
/check-tenantsql backend/api/src/main/resources/mapper/ --fix
```

### 测试失败

查看测试失败信息并修复代码。

## CI/CD 集成

此检查也用于 CI/CD 流水线，确保代码质量：

```yaml
# .github/workflows/ci.yml
jobs:
  check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run pre-commit checks
        run: /pre-commit
```

## 何时跳过检查

以下情况可以使用 `--skip-*` 选项：

- 紧急修复生产问题
- 仅修改文档
- 仅修改注释
- 重构代码但不改变逻辑

⚠️ **注意**: 即使跳过检查，代码也应该能正常编译和运行！

## 相关文档

- [format-code](../format-code/README.md) - 代码格式化
- [check-tenantsql](../check-tenantsql/README.md) - 多租户 SQL 检查
- [CONTRIBUTING.md](../../CONTRIBUTING.md) - 贡献指南
