# build-backend 技能使用指南

构建锦书企业级报表系统后端项目。

## 用法

```bash
/build-backend
```

## 功能说明

### 默认模式（快速编译）

仅编译 `common` 和 `api` 模块，不运行测试：

```bash
/build-backend
```

等效于：

```bash
cd backend
./gradlew common:compileJava api:compileJava
```

## 选项说明

| 选项 | 说明 |
|-----|------|
| `--clean` | 先执行 clean，删除构建产物后再编译 |
| `--test` | 编译后运行单元测试 |
| `--integration-test` | 运行集成测试（需要数据库等基础设施） |
| `--package` | 编译后打包为可执行 jar 文件 |
| `--skip-checks` | 跳过所有检查（测试、代码检查等），快速构建 |
| `--parallel` | 并行构建各模块（提高速度） |
| `--all` | 构建所有模块（common + api + batch + worker + data-sync） |

## 使用示例

### 日常开发（最快）

```bash
/build-backend
```

### 完整构建（含测试）

```bash
/build-backend --clean --test
```

### 构建所有模块

```bash
/build-backend --all
```

### 打包发布

```bash
/build-backend --clean --package
```

### 并行快速构建

```bash
/build-backend --parallel --skip-checks
```

## 模块说明

后端采用 Gradle Multi-Project 结构：

| 模块 | 说明 | 是否独立运行 |
|-----|------|------------|
| `common` | 公共模块（实体、工具、异常等） | ❌ |
| `api` | API 服务（主后端） | ✅ |
| `batch` | Spring Batch 批处理服务 | ✅ |
| `worker` | MQ 消费者 + PDF 生成 | ✅ |
| `data-sync` | 数据同步服务 | ✅ |

## 构建产物

JAR 文件位置：

```
backend/api/build/libs/jinshu-report-api-1.0.0.jar
backend/batch/build/libs/jinshu-report-batch-1.0.0.jar
backend/worker/build/libs/jinshu-report-worker-1.0.0.jar
backend/data-sync/build/libs/jinshu-report-data-sync-1.0.0.jar
```

## 常见问题

### 依赖下载慢

配置国内镜像源，编辑 `~/.gradle/init.gradle`:

```groovy
allprojects {
    repositories {
        maven { url 'https://maven.aliyun.com/repository/public' }
        mavenCentral()
    }
}
```

### 内存不足

增加 Gradle 内存，编辑 `backend/gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
```

### 模块间依赖问题

先构建 common 模块，再构建其他模块：

```bash
/build-backend --clean  # 清理并重新构建所有模块
```

## 相关文档

- [backend/build.gradle.kts](../../backend/build.gradle.kts) - 根构建配置
- [backend/settings.gradle.kts](../../backend/settings.gradle.kts) - 模块配置
- [backend/gradle/libs.versions.toml](../../backend/gradle/libs.versions.toml) - 版本管理
