# ===== Stage 1: Build the application using Maven =====
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# ===== Stage 2: Runtime image =====
FROM eclipse-temurin:21-jre

RUN apt-get update && apt-get upgrade -y && rm -rf /var/lib/apt/lists/*

WORKDIR /app

RUN groupadd --gid 1001 appuser && \
    useradd --uid 1001 --gid 1001 -m -s /bin/bash appuser

COPY --from=build --chown=appuser:appuser /app/target/login-ci-demo-1.0.jar app.jar

USER appuser

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
