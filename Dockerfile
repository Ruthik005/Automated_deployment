# ===== Stage 1: Build the application using Maven =====
# This stage compiles your Java code and creates the .jar file.
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Copy the Maven project file first to leverage Docker's layer caching
COPY pom.xml .
# Copy the rest of your source code
COPY src ./src

# Run Maven to build the project. This creates the .jar file.
RUN mvn clean package -DskipTests


# ===== Stage 2: Create the final, smaller runtime image =====
# This stage takes the .jar from the 'build' stage and prepares it for running.
FROM eclipse-temurin:21-jre

# Update OS packages in the final image for security.
RUN apt-get update && apt-get upgrade -y && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# ** NEW **: Create a non-root user and group
RUN groupadd --gid 1001 appuser && \
    useradd --uid 1001 --gid 1001 -m -s /bin/bash appuser

# Copy the .jar file and set ownership to the new user
COPY --from=build --chown=appuser:appuser /app/target/login-ci-demo-1.0.jar app.jar

# ** NEW **: Switch to the non-root user
USER appuser

# Expose the port your application runs on
EXPOSE 8081

# Command to run your application
ENTRYPOINT ["java", "-jar", "app.jar"]

