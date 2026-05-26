# syntax=docker/dockerfile:1
FROM gradle:jdk21 AS builder
WORKDIR /app
# Cache layer: copy build scripts first to cache dependency downloads
COPY --chown=gradle:gradle build.gradle* settings.gradle* gradle.properties* ./
COPY --chown=gradle:gradle gradle/ ./gradle/
RUN --mount=type=cache,target=/home/gradle/.gradle,uid=1000,gid=1000 \
    gradle dependencies --no-daemon > /dev/null 2>&1 || true
# Copy source and build
COPY --chown=gradle:gradle . .
RUN --mount=type=cache,target=/home/gradle/.gradle,uid=1000,gid=1000 \
    gradle api:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/api/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
