# build-frontend 技能使用指南

构建锦书企业级报表系统前端项目。

## 用法

```bash
/build-frontend
```

## 功能说明

### 默认模式（快速类型检查）

仅运行 TypeScript 类型检查：

```bash
/build-frontend
```

等效于：

```bash
cd frontend
pnpm build  # 仅类型检查，不打包
```

## 选项说明

| 选项 | 说明 |
|-----|------|
| `--clean` | 先清理构建缓存和 Vite 缓存 |
| `--type-check` | 运行完整 TypeScript 类型检查（默认） |
| `--lint` | 运行 ESLint 代码检查 |
| `--test` | 运行单元测试 |
| `--install` | 先运行 pnpm install 安装/更新依赖 |
| `--watch` | 监听模式（不退出，持续构建） |
| `--analyze` | 生成构建包大小分析报告 |

## 使用示例

### 日常开发（快速检查）

```bash
/build-frontend
```

### 完整质量检查

```bash
/build-frontend --lint --type-check --test
```

### 发布前构建

```bash
/build-frontend --clean --lint --type-check
```

### 安装依赖并构建

```bash
/build-frontend --install --lint
```

### 分析包大小

```bash
/build-frontend --analyze
```

## 前端技术栈

| 技术 | 版本 | 说明 |
|-----|------|------|
| React | 19.x | UI 框架 |
| TypeScript | 6.x | 类型安全 |
| Vite | 8.x | 构建工具 |
| PrimeReact | 10.x | UI 组件库 |
| ECharts | 6.x | 图表库 |
| React Router | 7.x | 路由 |
| Axios | 1.8.x | HTTP 客户端 |

## 构建产物

构建产物位置：

```
frontend/dist/
├── index.html
├── assets/
│   ├── index-xxx.js
│   └── index-xxx.css
└── ...
```

## 开发模式

本技能主要用于构建，开发时请直接运行：

```bash
cd frontend
pnpm dev
```

## 代码规范

### ESLint 规则

项目使用 ESLint 进行代码检查，主要规则：

- ✅ TypeScript 严格模式
- ✅ React Hooks 规则
- ✅ 禁止未使用变量
- ✅ 强制类型注解（函数参数和返回值）

### 代码格式化

使用 Prettier 自动格式化：

```bash
cd frontend
pnpm format
```

## 常见问题

### 依赖安装慢

配置 pnpm 国内镜像：

```bash
pnpm config set registry https://registry.npmmirror.com
```

### 类型检查错误

查看具体错误：

```bash
cd frontend
pnpm tsc --noEmit
```

### 构建缓存问题

清理缓存：

```bash
/build-frontend --clean
```

或者手动删除：

```bash
cd frontend
rm -rf node_modules/.vite dist
```

## 相关文档

- [frontend/package.json](../../frontend/package.json) - 依赖和脚本
- [frontend/vite.config.ts](../../frontend/vite.config.ts) - Vite 配置
- [frontend/tsconfig.json](../../frontend/tsconfig.json) - TypeScript 配置
