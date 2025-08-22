# ===== Stage 1: Build & Test =====
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml and source
COPY pom.xml .
COPY src ./src

# Run tests and build JAR
RUN mvn clean package

# ===== Stage 2: Runtime =====
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/target/login-ci-demo-1.0.jar app.jar

# Expose port 8081 (your Docker app port)
EXPOSE 8081

# Run Spring Boot app
ENTRYPOINT ["java", "-jar", "app.jar"]
