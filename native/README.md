# native

> Rust 高性能/系统级模块 —— 高性能 Excel 解析、WASM 前端组件、CLI 工具

## 适用范围

✅ **允许**（参见 [决策 008](../docs/01-立项与架构/03.技术选型决策记录.md#决策-008引入-rust-作为高性能系统级辅助语言)）

- 高性能 Excel 解析（calamine 等）
- 编译为 WASM 嵌入前端的高性能组件（大数据虚拟滚动、表格渲染）
- 分发给租户的 CLI 工具与 SDK

❌ **禁止**

- 业务编排逻辑（仍由 Java 主导）
- 审计日志、租户拦截器等强业务约束模块
- 替代 Java 已实现的稳定功能（仅在性能瓶颈验证后才考虑替换）

## 目录结构（Cargo Workspace）

```
native/
├── README.md                # 本文档
├── Cargo.toml               # workspace 根
├── rust-toolchain.toml      # 工具链版本锁定
├── crates/
│   ├── excel-parser/        # 高性能 Excel 解析（可编译为 .so 给 Java JNI）
│   ├── wasm-grid/           # WASM 前端组件
│   └── cli/                 # 租户 CLI 工具
└── target/                  # 构建产物（gitignore）
```

## 环境要求

- Rust 1.80+ (stable)
- Cargo
- 可选：`wasm-pack`（WASM 构建）、`cargo-make`（统一构建脚本）

## 快速开始

```bash
# 安装 Rust
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh

# 构建所有 crate
cargo build --workspace

# 运行测试
cargo test --workspace

# 代码规范检查
cargo fmt --all -- --check
cargo clippy --workspace --all-targets -- -D warnings
```

## 开发规范

| 项 | 规范 |
|---|------|
| 代码风格 | `rustfmt` 默认配置 |
| Lint | `clippy` 严格模式（warnings 视为错误） |
| 测试框架 | 内建 `#[test]` + `criterion`（性能基准） |
| 依赖管理 | workspace 统一版本，禁止子 crate 独立升级 |
| 安全审计 | `cargo audit` CI 集成 |

## 与 Java 的集成方式

部署方式待评估（参见决策 008），候选：

1. **JNI / Project Panama 内嵌**：编译 `.so/.dll`，Java 直接调用（性能最优，集成复杂）
2. **独立微服务**：通过 HTTP/gRPC 通信（解耦，跨网络延迟）
3. **WASM 嵌入前端**：直接编译为 `.wasm`，前端 JS 调用
4. **独立 CLI 二进制**：单文件分发，租户本地运行

## 状态

🚧 **骨架阶段** —— 待编写设计文档（`docs/04-模块设计/` 下）后再开始实质开发，遵守"设计先行"约束。
