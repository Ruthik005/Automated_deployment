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
                setlocal enabledelayedexpansion

                echo Starting cleanup process...

                REM -----------------------------
                REM Stop and remove containers for this image
                REM -----------------------------
                echo Stopping and removing containers for image: ${IMAGE_NAME}
                
                docker ps -a -q --filter "ancestor=${IMAGE_NAME}" > temp_containers.txt 2>nul
                if exist temp_containers.txt (
                    for /f %%i in (temp_containers.txt) do (
                        echo Stopping container: %%i
                        docker stop %%i >nul 2>&1
                        echo Removing container: %%i
                        docker rm %%i >nul 2>&1
                    )
                    del temp_containers.txt
                )

                REM Also remove any container with the same name
                docker stop ${IMAGE_NAME} >nul 2>&1
                docker rm ${IMAGE_NAME} >nul 2>&1

                REM -----------------------------
                REM Remove old images except current build tag
                REM -----------------------------
                echo Cleaning old images for: ${IMAGE_NAME}
                
                docker images ${IMAGE_NAME} --format "{{.ID}} {{.Tag}}" > temp_images.txt 2>nul
                if exist temp_images.txt (
                    for /f "tokens=1,2" %%a in (temp_images.txt) do (
                        if NOT "%%b"=="${IMAGE_TAG}" (
                            echo Removing old image: %%a with tag %%b
                            docker rmi -f %%a >nul 2>&1
                        )
                    )
                    del temp_images.txt
                )

                echo Cleanup completed successfully.
                endlocal
                """
            }
        }

        stage('Build & Test in Docker') {
            steps {
                bat """
                @echo off
                echo Building Docker image: ${IMAGE_NAME}:${IMAGE_TAG}
                docker build --no-cache -t ${IMAGE_NAME}:${IMAGE_TAG} .
                """
            }
        }

        stage('Run Docker Container') {
            steps {
                bat """
                @echo off
                echo Preparing to run container...
                
                REM Stop & remove existing container silently
                docker stop ${IMAGE_NAME} >nul 2>&1
                docker rm ${IMAGE_NAME} >nul 2>&1

                echo Starting container on port ${APP_PORT}
                docker run -d -p ${APP_PORT}:${APP_PORT} --name ${IMAGE_NAME} ${IMAGE_NAME}:${IMAGE_TAG}
                
                echo Container started successfully.
                """
            }
        }

        stage('Health Check') {
            steps {
                bat """
                @echo off
                setlocal enabledelayedexpansion

                echo Starting health check...
                set RETRIES=10
                set SUCCESS=0

                :CHECK
                echo Attempting health check... Retries remaining: !RETRIES!
                
                REM Use timeout command for better reliability
                timeout /t 5 /nobreak >nul
                
                for /f "delims=" %%a in ('curl -s -u admin:admin123 http://localhost:${APP_PORT}/health 2^>nul') do (
                    set RESPONSE=%%a
                )

                echo Response received: "!RESPONSE!"

                REM Check if response contains "OK" anywhere in the response
                echo !RESPONSE! | findstr /C:"OK" >nul
                if !ERRORLEVEL! EQU 0 (
                    set SUCCESS=1
                    goto END
                ) else (
                    set /a RETRIES=!RETRIES!-1
                    if !RETRIES! GTR 0 (
                        echo Health check failed, retrying...
                        goto CHECK
                    ) else (
                        echo Health check failed after all retries!
                        echo Final response was: !RESPONSE!
                        exit /b 1
                    )
                )

                :END
                if !SUCCESS!==1 (
                    echo Health check passed successfully.
                )
                endlocal
                """
            }
        }

        stage('Push to Docker Hub') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-hub', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    bat """
                    @echo off
                    echo Logging into Docker Hub...
                    echo %DOCKER_PASS% | docker login -u %DOCKER_USER% --password-stdin

                    echo Tagging images...
                    docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${DOCKER_HUB_REPO}:${IMAGE_TAG}
                    docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${DOCKER_HUB_REPO}:latest

                    echo Pushing images to Docker Hub...
                    docker push ${DOCKER_HUB_REPO}:${IMAGE_TAG}
                    docker push ${DOCKER_HUB_REPO}:latest

                    echo Logging out for security...
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
            @echo off
            echo Performing final cleanup...
            docker system prune -f >nul 2>&1
            echo Cleanup completed.
            """
        }
        failure {
            bat """
            @echo off
            echo Pipeline failed. Checking Docker status...
            docker ps -a
            docker images
            """
        }
    }
}