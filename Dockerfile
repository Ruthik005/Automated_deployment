# ===== Stage 1: Build & Test =====
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Copy Maven files first to leverage caching
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Run tests and build the JAR
RUN mvn clean package -DskipTests=false

# ===== Stage 2: Runtime =====
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/target/login-ci-demo-1.0.jar app.jar

# Expose application port
EXPOSE 8081

# Run Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
