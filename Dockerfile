# Use official Maven image to build the app
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the JAR
RUN mvn clean package -DskipTests

# Use smaller Java runtime for final image
FROM eclipse-temurin:21-jre

# Set working directory
WORKDIR /app

# Copy built JAR from build stage
COPY --from=build /app/target/login-ci-demo-1.0.jar app.jar

# Expose port 8080
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
