pipeline {
    agent any

    environment {
        IMAGE_NAME = "login-ci-demo"
        IMAGE_TAG = "${env.BUILD_NUMBER}"  // Unique tag per build
        APP_PORT = "8081"
        DOCKER_HUB_REPO = "ruthik005/capstone_project"  // <-- replace with your Docker Hub repo
    }

    stages {
        stage('Checkout SCM') {
            steps {
                checkout scm
            }
        }

        stage('Clean Old Docker Images & Containers') {
            steps {
                bat """
                @echo off
                REM -----------------------------
                REM Stop and remove all running containers safely
                REM -----------------------------
                for /F "tokens=*" %%c in ('docker ps -a -q --filter "ancestor=%IMAGE_NAME%" 2^>nul') do (
                    docker stop %%c >nul 2>&1 || echo No container %%c running
                    docker rm %%c >nul 2>&1 || echo No container %%c to remove
                )

                REM -----------------------------
                REM Remove old images except the newly built one safely
                REM -----------------------------
                for /F "tokens=1,2" %%a in ('docker images %IMAGE_NAME% --format "{{.ID}} {{.Tag}}" 2^>nul') do (
                    if NOT "%%b"=="%IMAGE_TAG%" (
                        docker rmi -f %%a >nul 2>&1 || echo Image %%a not removed
                    )
                )
                """
            }
        }

        stage('Build Docker Image') {
            steps {
                bat """
                REM Build Docker image (no-cache)
                docker build --no-cache -t %IMAGE_NAME%:%IMAGE_TAG% .
                """
            }
        }

        stage('Trivy Security Scan') {
            steps {
                bat """
                REM Scan Docker image for vulnerabilities (HIGH & CRITICAL)
                "C:/ProgramData/chocolatey/bin/trivy.exe" image --severity HIGH,CRITICAL --exit-code 1 --ignore-unfixed --no-progress %IMAGE_NAME%:%IMAGE_TAG%
                """
            }
        }

        stage('Run Docker Container') {
            steps {
                bat """
                REM Stop & remove existing container safely
                docker stop %IMAGE_NAME% >nul 2>&1 || echo No running container
                docker rm %IMAGE_NAME% >nul 2>&1 || echo No container to remove

                REM Run container on specified port
                docker run -d -p %APP_PORT%:%APP_PORT% --name %IMAGE_NAME% %IMAGE_NAME%:%IMAGE_TAG%
                """
            }
        }

        stage('Health Check') {
            steps {
                bat """
                @echo off
                setlocal enabledelayedexpansion

                set RETRIES=10
                set SUCCESS=0

                :CHECK
                for /f "delims=" %%a in ('curl -s -u admin:admin123 http://localhost:%APP_PORT%/health') do (
                    set RESPONSE=%%a
                    set RESPONSE=!RESPONSE:~0,2!
                )

                echo Response received: "!RESPONSE!"

                if /i "!RESPONSE!"=="OK" (
                    set SUCCESS=1
                    goto END
                ) else (
                    set /a RETRIES=!RETRIES!-1
                    if !RETRIES! GTR 0 (
                        echo Health check failed, retrying... Remaining retries: !RETRIES!
                        ping 127.0.0.1 -n 6 >nul
                        goto CHECK
                    ) else (
                        echo Health check failed after all retries!
                        exit /b 1
                    )
                )

                :END
                if !SUCCESS!==1 (
                    echo Health check passed.
                )
                endlocal
                """
            }
        }

        stage('Push to Docker Hub') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-hub', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    bat """
                    REM Login to Docker Hub
                    echo %DOCKER_PASS% | docker login -u %DOCKER_USER% --password-stdin

                    REM Tag image for Docker Hub
                    docker tag %IMAGE_NAME%:%IMAGE_TAG% %DOCKER_HUB_REPO%:%IMAGE_TAG%
                    docker tag %IMAGE_NAME%:%IMAGE_TAG% %DOCKER_HUB_REPO%:latest

                    REM Push both tags
                    docker push %DOCKER_HUB_REPO%:%IMAGE_TAG%
                    docker push %DOCKER_HUB_REPO%:latest

                    REM Logout for security
                    docker logout
                    """
                }
            }
        }
    }

    post {
        always {
            echo "Pipeline finished with status: ${currentBuild.currentResult}"
            bat """
            REM Optional: Clean dangling images silently
            docker system prune -f >nul 2>&1 || echo No dangling images to remove
            """
        }
    }
}
