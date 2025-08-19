pipeline {
    agent any

    environment {
        IMAGE_NAME = "login-ci-demo"
        IMAGE_TAG = "${env.BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test in Docker') {
            steps {
                bat """
                REM Build and test inside Maven container
                docker run --rm -v %CD%:/app -w /app maven:3.9.6-eclipse-temurin-21 mvn clean test
                """
            }
        }

        stage('Build Docker Image') {
            steps {
                bat """
                REM Build final Docker image with Spring Boot app
                docker build --no-cache -t %IMAGE_NAME%:%IMAGE_TAG% -t %IMAGE_NAME%:latest .
                """
            }
        }

        stage('Run Docker Container') {
            steps {
                bat """
                REM Stop and remove old container if exists
                docker stop %IMAGE_NAME% 2>nul
                docker rm %IMAGE_NAME% 2>nul

                REM Run container on 8081
                docker run -d -p 8081:8081 --name %IMAGE_NAME% %IMAGE_NAME%:%IMAGE_TAG%
                """
            }
        }

        stage('Publish JUnit Results') {
            steps {
                junit '**/target/surefire-reports/*.xml'
            }
        }
    }

    post {
        always {
            echo "Pipeline finished with status: ${currentBuild.currentResult}"
            bat "docker image prune -f"
        }
    }
}
