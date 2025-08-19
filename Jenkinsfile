pipeline {
    agent any

    environment {
        IMAGE_NAME = "login-ci-demo"
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        APP_PORT = "8081"
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
                REM Stop and remove any running container of this project
                for /F "tokens=*" %%c in ('docker ps -a -q --filter "ancestor=%IMAGE_NAME%"') do (
                    docker stop %%c
                    docker rm %%c
                )

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
                docker run -d -p %APP_PORT%:%APP_PORT% --name %IMAGE_NAME% %IMAGE_NAME%:%IMAGE_TAG%
                """
            }
        }

        stage('Health Check') {
            steps {
                bat """
                REM Wait ~20 seconds for app to start
                ping 127.0.0.1 -n 21 >nul

                REM Check health endpoint
                curl -s http://localhost:%APP_PORT%/health | findstr /C:"OK"
                if errorlevel 1 (
                    echo Health check failed!
                    exit /b 1
                ) else (
                    echo Health check passed.
                )
                """
            }
        }
    }

    post {
        always {
            echo "Pipeline finished with status: ${currentBuild.currentResult}"
            bat """
            REM Remove dangling images (optional cleanup)
            docker image prune -f
            """
        }
    }
}
