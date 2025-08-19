# Stage 1: Build the app with Maven
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom and source
COPY pom.xml .
COPY src ./src

# Build JAR without running tests
RUN mvn clean package -DskipTests

# Stage 2: Use lightweight Java runtime
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built JAR from the previous stage
COPY --from=build /app/target/login-ci-demo-1.0.jar app.jar

# Expose Docker port 8081
EXPOSE 8081

# Run the Spring Boot app
ENTRYPOINT ["java", "-jar", "app.jar"]
