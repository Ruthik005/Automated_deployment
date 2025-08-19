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

        stage('Clean Old Docker Images') {
            steps {
                bat """
                REM Remove all old images of this project
                for /F "tokens=*" %%i in ('docker images %IMAGE_NAME% --format "{{.ID}}"') do docker rmi -f %%i
                """
            }
        }

        stage('Build & Test in Docker') {
            steps {
                bat """
                REM Build Docker image (runs tests automatically in Maven container)
                docker build --no-cache -t %IMAGE_NAME%:%IMAGE_TAG% .
                """
            }
        }

        stage('Run Docker Container') {
            steps {
                bat """
                REM Stop and remove existing container if running
                docker stop %IMAGE_NAME% 2>nul
                docker rm %IMAGE_NAME% 2>nul

                REM Run container on port 8081
                docker run -d -p 8081:8081 --name %IMAGE_NAME% %IMAGE_NAME%:%IMAGE_TAG%
                """
            }
        }
    }

    post {
        always {
            echo "Pipeline finished with status: ${currentBuild.currentResult}"
            REM Remove dangling images (optional cleanup)
            bat "docker image prune -f"
        }
    }
}
