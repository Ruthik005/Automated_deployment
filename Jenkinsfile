pipeline {
    agent any

    environment {
        IMAGE_NAME = "login-ci-demo"
        IMAGE_TAG = "${env.BUILD_NUMBER}"  // Unique tag per build
        APP_PORT = "8081"
        DOCKER_HUB_REPO = "ruthik005/capstone_project"
        KUBE_DEPLOYMENT_NAME = "login-ci-demo-deployment" // The 'name' from your deployment.yaml metadata
    }

    stages {
        // NO CHANGE: This stage is exactly as you provided.
        stage('Checkout SCM') {
            steps {
                checkout scm
            }
        }

        // NO CHANGE: This stage is exactly as you provided.
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
                            )
                        )
                        REM Clean up any dangling containers and images
                        docker container prune -f >nul 2>&1
                        docker image prune -f >nul 2>&1
                        echo Cleanup completed successfully.
                        """
                    } catch (Exception e) {
                        echo "Cleanup stage had issues but continuing: ${e.getMessage()}"
                    }
                }
            }
        }

        // NO CHANGE: This stage is exactly as you provided.
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
        
        // NO CHANGE: This stage is exactly as you provided.
        stage('Security Scan with Trivy') {
            steps {
                bat """
                @echo off
                echo --- Starting Security Scan ---
                trivy image --severity CRITICAL,HIGH --exit-code 1 --ignore-unfixed ${IMAGE_NAME}:${IMAGE_TAG}
                if %ERRORLEVEL% NEQ 0 (
                    echo.
                    echo ******************************************************
                    echo * SECURITY SCAN FAILED                             *
                    echo * High or Critical vulnerabilities were found.     *
                    echo * Please review the logs above and fix them.       *
                    echo ******************************************************
                    echo.
                    exit /b 1
                )
                echo --- SECURITY SCAN PASSED ---
                """
            }
        }

        // NO CHANGE: This stage is exactly as you provided.
        stage('Push to Docker Hub') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-hub', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    bat """
                    @echo off
                    echo Logging into Docker Hub...
                    echo %DOCKER_PASS% | docker login -u %DOCKER_USER% --password-stdin
                    if %ERRORLEVEL% NEQ 0 ( echo Docker Hub login failed!; exit /b 1 )

                    echo Tagging images...
                    docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${DOCKER_HUB_REPO}:${IMAGE_TAG}
                    docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${DOCKER_HUB_REPO}:latest

                    echo Pushing images to Docker Hub...
                    docker push ${DOCKER_HUB_REPO}:${IMAGE_TAG}
                    if %ERRORLEVEL% NEQ 0 ( echo Failed to push image with build tag!; exit /b 1 )
                    
                    docker push ${DOCKER_HUB_REPO}:latest
                    if %ERRORLEVEL% NEQ 0 ( echo Failed to push latest image!; exit /b 1 )

                    echo Logging out for security...
                    docker logout
                    echo Images pushed successfully to Docker Hub.
                    """
                }
            }
        }

        // NO CHANGE: This stage is exactly as you provided.
        stage('Scan Kubernetes Manifests') {
            steps {
                bat """
                @echo off
                echo --- Scanning Kubernetes YAML files for security issues ---
                trivy config --severity HIGH,CRITICAL --exit-code 1 .
                if %ERRORLEVEL% NEQ 0 (
                    echo ******************************************************
                    echo * KUBERNETES MANIFEST SCAN FAILED              *
                    echo * High or Critical misconfigurations were found. *
                    echo ******************************************************
                    exit /b 1
                )
                echo --- Kubernetes manifest scan passed ---
                """
            }
        }

        // *** UPDATED STAGE: Added withCredentials block and --token flags ***
        stage('Deploy and Update on Kubernetes') {
            steps {
                withCredentials([string(credentialsId: 'kubernetes-token', variable: 'KUBE_TOKEN')]) {
                    bat """
                    @echo off
                    echo --- Applying configurations to ensure deployment exists ---
                    kubectl --token=%KUBE_TOKEN% apply -f deployment.yaml
                    kubectl --token=%KUBE_TOKEN% apply -f service.yaml

                    echo --- Triggering rolling update of pods with the new image ---
                    REM This command tells Kubernetes to update the deployment with the new image tag for this specific build.
                    kubectl --token=%KUBE_TOKEN% set image deployment/${KUBE_DEPLOYMENT_NAME} login-ci-demo-container=${DOCKER_HUB_REPO}:${IMAGE_TAG}
                    if %ERRORLEVEL% NEQ 0 (
                        echo Failed to set the new image on the deployment!
                        exit /b 1
                    )
                    echo --- Rolling update initiated successfully ---
                    """
                }
            }
        }
        
        // *** UPDATED STAGE: Added withCredentials block and --token flags ***
        stage('Verify Kubernetes Deployment') {
            steps {
                withCredentials([string(credentialsId: 'kubernetes-token', variable: 'KUBE_TOKEN')]) {
                    bat """
                    @echo off
                    echo --- Verifying deployment rollout status ---
                    REM This command waits for the pod updates to complete successfully.
                    kubectl --token=%KUBE_TOKEN% rollout status deployment/${KUBE_DEPLOYMENT_NAME} --timeout=2m
                    if %ERRORLEVEL% NEQ 0 (
                        echo.
                        echo ******************************************************
                        echo * DEPLOYMENT VERIFICATION FAILED                   *
                        echo * The application did not deploy successfully.     *
                        echo ******************************************************
                        exit /b 1
                    )
                    echo --- Deployment successfully verified ---
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
            // *** UPDATED BLOCK: Added withCredentials for successful status check ***
            withCredentials([string(credentialsId: 'kubernetes-token', variable: 'KUBE_TOKEN')]) {
                bat """
                @echo off
                echo --- Final Kubernetes Status ---
                kubectl --token=%KUBE_TOKEN% get deployments
                kubectl --token=%KUBE_TOKEN% get pods -o wide
                kubectl --token=%KUBE_TOKEN% get services
                """
            }
        }
        failure {
            // *** UPDATED BLOCK: Added withCredentials for easier debugging on failure ***
            withCredentials([string(credentialsId: 'kubernetes-token', variable: 'KUBE_TOKEN')]) {
                bat """
                @echo off
                echo === Docker Images on Agent ===
                docker images
                echo.
                echo === K8s Deployment Status ===
                kubectl --token=%KUBE_TOKEN% get deployment ${KUBE_DEPLOYMENT_NAME} -o yaml
                echo.
                echo === K8s Pods Status & Logs ===
                kubectl --token=%KUBE_TOKEN% describe pods -l app=login-ci-demo
                kubectl --token=%KUBE_TOKEN% logs -l app=login-ci-demo --tail=100
                """
            }
        }
    }
}

