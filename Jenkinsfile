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

        stage('Build & Test in Docker') {
            steps {
                bat """
                REM Build Docker image (runs tests automatically in Maven container)
                docker build --no-cache -t %IMAGE_NAME%:%IMAGE_TAG% .
                """
            }
        }

        stage('Stop & Remove Old Containers') {
    steps {
        bat """
        REM Stop and remove any running containers of this project
        for /F "tokens=*" %%c in ('docker ps -a -q --filter "ancestor=%IMAGE_NAME%"') do (
            docker stop %%c 2>nul
            docker rm %%c 2>nul
        )
        """
    }
}


        stage('Remove Old Images') {
            steps {
                bat """
                REM Remove all old images except the newly built one
                for /F "tokens=*" %%i in ('docker images %IMAGE_NAME% --format "{{.ID}} {{.Tag}}"') do (
                    for /F "tokens=1,2" %%a in ("%%i") do (
                        if NOT "%%b"=="%IMAGE_TAG%" docker rmi -f %%a
                    )
                )
                """
            }
        }

        stage('Run Docker Container') {
            steps {
                bat """
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
            REM Optional: Remove dangling images
            docker image prune -f
            """
        }
    }
}
