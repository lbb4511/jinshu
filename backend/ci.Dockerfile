FROM gradle:jdk21

WORKDIR /app

# 只复制构建配置文件，不复制源码
COPY build.gradle.kts settings.gradle.kts gradlew gradlew.bat /app/
COPY gradle/ /app/gradle/
COPY api/build.gradle.kts /app/api/
COPY common/build.gradle.kts /app/common/
COPY batch/build.gradle.kts /app/batch/
COPY worker/build.gradle.kts /app/worker/
COPY data-sync/build.gradle.kts /app/data-sync/

# 预下载所有子项目依赖（不编译源码）
RUN gradle :api:dependencies :common:dependencies :batch:dependencies :worker:dependencies :data-sync:dependencies --no-daemon -q
