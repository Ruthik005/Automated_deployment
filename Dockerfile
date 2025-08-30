# ===== Stage 1: Build the application using Maven =====
# This stage uses a Maven image to compile your Java source code
# from the 'src' directory and packages it into a .jar file.
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the Maven project file first. This is a Docker best practice
# that uses layer caching to speed up future builds.
COPY pom.xml .

# Copy the rest of your application's source code
COPY src ./src

# Run the Maven command to build the project. This command cleans the
# project, runs tests, and packages it into the final .jar file.
# The result will be in the /app/target/ directory.
RUN mvn clean package


# ===== Stage 2: Create the final, optimized runtime image =====
# This stage uses a lightweight Java Runtime Environment (JRE) which is
# smaller and more secure for running the application.
FROM eclipse-temurin:21-jre

# SECURITY FIX: Update the Operating System packages within the final image.
# This command updates the package lists and installs the latest security patches.
# The eclipse-temurin images are Debian-based, so 'apt-get' is the correct command.
RUN apt-get update && apt-get upgrade -y && rm -rf /var/lib/apt/lists/*

# Set the working directory for the runtime container
WORKDIR /app

# Copy the built .jar file from the 'build' stage into this final stage.
# The JAR was created in /app/target/ in the previous stage.
COPY --from=build /app/target/login-ci-demo-1.0.jar app.jar

# Expose port 8081 to allow network traffic to your application
EXPOSE 8081

# Set the command to run your Spring Boot application when the container starts.
# ENTRYPOINT is used so the container behaves like an executable.
ENTRYPOINT ["java", "-jar", "app.jar"]
