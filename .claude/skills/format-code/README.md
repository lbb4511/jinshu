# format-code 技能使用指南

格式化锦书企业级报表系统所有代码。

## 用法

```bash
/format-code
```

## 功能说明

### 默认模式（全量格式化）

格式化所有代码：

1. ✅ 后端 Java 代码（Spotless + Google Java Format）
2. ✅ 前端 TypeScript/JavaScript/TSX/JSX 代码（Prettier）
3. ✅ 文档和配置文件（Markdown、YAML、JSON）

## 选项说明

| 选项 | 说明 |
|-----|------|
| `--backend-only` | 仅格式化后端 Java 代码 |
| `--frontend-only` | 仅格式化前端代码 |
| `--check` | 仅检查是否需要格式化，不修改文件 |
| `--staged` | 仅格式化 Git 暂存区的文件（用于提交前） |
| `--quiet` | 静默模式，减少输出信息 |

## 使用示例

### 格式化所有代码

```bash
/format-code
```

### 仅格式化后端

```bash
/format-code --backend-only
```

### 仅格式化前端

```bash
/format-code --frontend-only
```

### 检查是否需要格式化

```bash
/format-code --check
```

### 格式化暂存区文件（提交前）

```bash
/format-code --staged
```

## 后端格式化

使用 **Spotless** + **Google Java Format** 进行格式化。

### 格式化规则

- ✅ 缩进 4 空格
- ✅ 最大行宽 120 字符
- ✅ 统一的大括号风格
- ✅ 自动 import 排序
- ✅ 多余空行清理

### 手动运行

```bash
cd backend
./gradlew spotlessApply
```

### 仅检查

```bash
cd backend
./gradlew spotlessCheck
```

## 前端格式化

使用 **Prettier** 进行格式化。

### 格式化规则

- ✅ 缩进 2 空格
- ✅ 最大行宽 100 字符
- ✅ 单引号
- ✅ 无分号（TypeScript 不需要）
- ✅ 尾随逗号（多行时）
- ✅ 自动 import 排序

### 手动运行

```bash
cd frontend
pnpm format
```

### 仅检查

```bash
cd frontend
pnpm format:check
```

## Git 集成

### 提交前自动格式化

建议配置 Git hooks 在提交前自动格式化：

```bash
# 可以使用 husky 或类似工具
cd frontend
pnpm husky install
```

### 手动格式化暂存区

```bash
/format-code --staged
```

## IDE 配置

### VS Code

项目已包含 `.vscode/settings.json`，安装推荐插件后会自动格式化：

- Prettier - Code formatter
- ESLint
- Java Extension Pack

### IntelliJ IDEA

导入项目后，配置：

1. 启用 Google Java Format 插件
2. 设置保存时自动格式化
3. 配置 import 优化规则

## 代码规范说明

### Java 代码规范

基于 **Alibaba Java Coding Guidelines** 和 **Google Java Format**：

- 类名：`UpperCamelCase`
- 方法名：`lowerCamelCase`
- 常量名：`UPPER_SNAKE_CASE`
- 包名：全小写，用点分隔

### TypeScript 代码规范

基于 **TypeScript Standard**：

- 组件名：`UpperCamelCase`
- 函数和变量：`lowerCamelCase`
- 常量：`UPPER_SNAKE_CASE`
- 类型名：`UpperCamelCase`

## 常见问题

### 格式化后代码无法编译

格式化只是改变风格，不改变语义。如果出现问题，请检查：

- 是否有语法错误
- 是否缺少依赖

### 与其他开发者格式冲突

确保所有人使用相同的格式化工具和版本，提交前先格式化。

### 不想格式化某些文件

配置 `.prettierignore`（前端）或 Spotless 配置（后端）忽略特定文件。

## 相关文档

- [pre-commit](../pre-commit/README.md) - 提交前检查
- [frontend/.prettierrc](../../frontend/.prettierrc) - Prettier 配置
- [backend/build.gradle.kts](../../backend/build.gradle.kts) - Spotless 配置
