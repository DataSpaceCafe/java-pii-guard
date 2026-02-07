FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

COPY pom.xml ./
COPY src ./src
COPY config ./config
COPY docs ./docs

RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

COPY --from=builder /workspace/target/java-pii-guard.jar /app/app.jar
COPY config /app/config

ENV CONFIG_PATH=/app/config/config.yaml

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
