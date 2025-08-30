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
                script {
                    try {
                        bat """
                        @echo off
                        echo Starting cleanup process...

                        REM Stop and remove existing container by name
                        docker stop ${IMAGE_NAME} >nul 2>&1
                        if %ERRORLEVEL% NEQ 0 echo Container ${IMAGE_NAME} was not running

                        docker rm ${IMAGE_NAME} >nul 2>&1
                        if %ERRORLEVEL% NEQ 0 echo Container ${IMAGE_NAME} was not found

                        REM Remove old images of this project (keep only current build)
                        echo Cleaning old images for: ${IMAGE_NAME}
                        for /f "tokens=1,2" %%i in ('docker images ${IMAGE_NAME} --format "{{.ID}} {{.Tag}}" 2^>nul') do (
                            if NOT "%%j"=="${IMAGE_TAG}" (
                                echo Removing old image: %%i with tag %%j
                                docker rmi -f %%i >nul 2>&1
                                if %ERRORLEVEL% EQU 0 (
                                    echo Successfully removed image %%i
                                ) else (
                                    echo Failed to remove image %%i
                                )
                            )
                        )

                        REM Clean up any dangling containers and images
                        docker container prune -f >nul 2>&1
                        docker image prune -f >nul 2>&1

                        echo Cleanup completed successfully.
                        """
                    } catch (Exception e) {
                        echo "Cleanup stage had issues but continuing: ${e.getMessage()}"
                        // Continue with pipeline even if cleanup fails
                    }
                }
            }
        }

        stage('Build & Test in Docker') {
            steps {
                bat """
                @echo off
                echo Building Docker image: ${IMAGE_NAME}:${IMAGE_TAG}
                docker build --no-cache -t ${IMAGE_NAME}:${IMAGE_TAG} .
                if %ERRORLEVEL% NEQ 0 (
                    echo Docker build failed!
                    exit /b 1
                )
                echo Docker image built successfully.
                """
            }
        }

        // =================================================================
        // NEW STAGE: Security Scan with Trivy
        // This stage will scan the newly built image and fail the pipeline
        // if any HIGH or CRITICAL vulnerabilities are found.
        // =================================================================
        stage('Security Scan with Trivy') {
            steps {
                bat """
                @echo off
                echo --- Starting Security Scan ---
                trivy image --severity CRITICAL,HIGH --exit-code 1 --ignore-unfixed ${IMAGE_NAME}:${IMAGE_TAG}
                if %ERRORLEVEL% NEQ 0 (
                    echo.
                    echo ******************************************************
                    echo * SECURITY SCAN FAILED                *
                    echo * High or Critical vulnerabilities were found.    *
                    echo * Please review the logs above and fix them.      *
                    echo ******************************************************
                    echo.
                    exit /b 1
                )
                echo.
                echo ******************************************************
                echo * SECURITY SCAN PASSED                *
                echo * No High or Critical vulnerabilities found.    *
                echo ******************************************************
                echo.
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
                if %ERRORLEVEL% NEQ 0 (
                    echo Failed to start container!
                    exit /b 1
                )
                
                echo Container started successfully.
                """
            }
        }

        stage('Health Check') {
            steps {
                bat """
                @echo off
                REM -----------------------------
                REM Robust Health Check Script
                REM -----------------------------
                setlocal enabledelayedexpansion

                set RETRIES=10
                set SUCCESS=0

                :CHECK
                for /f "delims=" %%a in ('curl -s -u admin:admin123 http://localhost:${APP_PORT}/health 2^>nul') do (
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
                        echo Final response was: !RESPONSE!
                        docker logs ${IMAGE_NAME}
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
                    @echo off
                    echo Logging into Docker Hub...
                    echo %DOCKER_PASS% | docker login -u %DOCKER_USER% --password-stdin
                    if %ERRORLEVEL% NEQ 0 (
                        echo Docker Hub login failed!
                        exit /b 1
                    )

                    echo Tagging images...
                    docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${DOCKER_HUB_REPO}:${IMAGE_TAG}
                    docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${DOCKER_HUB_REPO}:latest

                    echo Pushing images to Docker Hub...
                    docker push ${DOCKER_HUB_REPO}:${IMAGE_TAG}
                    if %ERRORLEVEL% NEQ 0 (
                        echo Failed to push image with build tag!
                        exit /b 1
                    )
                    
                    docker push ${DOCKER_HUB_REPO}:latest
                    if %ERRORLEVEL% NEQ 0 (
                        echo Failed to push latest image!
                        exit /b 1
                    )

                    echo Logging out for security...
                    docker logout
                    echo Images pushed successfully to Docker Hub.
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
            echo Final cleanup completed.
            """
        }
        success {
            echo "Pipeline completed successfully!"
            bat """
            @echo off
            echo Current Docker images:
            docker images ${IMAGE_NAME}
            echo Current running containers:
            docker ps --filter "name=${IMAGE_NAME}"
            """
        }
        failure {
            echo "Pipeline failed. Gathering debug information..."
            bat """
            @echo off
            echo === Docker Status ===
            docker ps -a
            echo.
            echo === Docker Images ===
            docker images
            echo.
            echo === Container Logs ===
            docker logs ${IMAGE_NAME} 2>nul || echo No container logs available
            echo.
            echo === System Resources ===
            docker system df
            """
        }
    }
}
