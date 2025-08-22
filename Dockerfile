# ===== Stage 1: Build & Test =====
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Set working directory
WORKDIR /app

# Copy Maven files first to leverage caching
COPY pom.xml ./

# Download dependencies only (cache this layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Run tests and build the JAR
RUN mvn clean package -DskipTests=false

# ===== Stage 2: Runtime =====
FROM eclipse-temurin:21-jre-alpine

# Set working directory
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/login-ci-demo-1.0.0.jar app.jar

# Expose the application port
EXPOSE 8081

# Security hardening: use a non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Run Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]

# Optional: reduce JVM attack surface
# Use minimal memory and disable insecure options if needed
# CMD ["java", "-XX:+UseContainerSupport", "-Xmx512m", "-jar", "app.jar"]
