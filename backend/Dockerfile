FROM gradle:8-jdk21 AS builder
WORKDIR /app
COPY --chown=gradle:gradle . .
RUN gradle api:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/api/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
