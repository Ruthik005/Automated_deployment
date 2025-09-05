pipeline {
    agent any

    environment {
        IMAGE_NAME           = "login-ci-demo"
        IMAGE_TAG            = "${env.BUILD_NUMBER}"
        APP_PORT             = "8081"
        DOCKER_HUB_REPO      = "ruthik005/capstone_project"
        KUBE_DEPLOYMENT_NAME = "login-ci-demo-deployment"
        KUBE_SERVICE_NAME    = "login-ci-demo-service"
        K8S_LABEL            = "app=login-ci-demo"
    }

    stages {
        stage('Checkout SCM') {
            steps { checkout scm }
        }

        stage('Clean Old Docker Images & Containers') {
            steps {
                bat """
                @echo off
                docker stop %IMAGE_NAME% >nul 2>&1
                docker rm %IMAGE_NAME%   >nul 2>&1

                for /f "tokens=1,2" %%i in ('docker images %IMAGE_NAME% --format "{{.ID}} {{.Tag}}" 2^>nul') do (
                    if NOT "%%j"=="%IMAGE_TAG%" docker rmi -f %%i >nul 2>&1
                )

                docker container prune -f >nul 2>&1
                docker image prune -f -a >nul 2>&1
                """
            }
        }

        stage('Build & Test in Docker') {
            steps {
                bat """
                @echo off
                echo Building Docker image: %IMAGE_NAME%:%IMAGE_TAG%
                docker build --no-cache -t %IMAGE_NAME%:%IMAGE_TAG% .
                if %ERRORLEVEL% NEQ 0 exit /b 1
                """
            }
        }

        stage('Security Scan with Trivy') {
            steps {
                bat """
                @echo off
                set TRIVY_TIMEOUT=10m
                set TRIVY_CACHE_CLEAR=true
                echo Starting Trivy security scan with extended timeout...
                trivy image --timeout %TRIVY_TIMEOUT% --severity CRITICAL,HIGH --exit-code 1 --ignore-unfixed %IMAGE_NAME%:%IMAGE_TAG%
                if %ERRORLEVEL% NEQ 0 exit /b 1
                """
            }
        }

        stage('Push to Docker Hub') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-hub', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    bat """
                    @echo off
                    echo %DOCKER_PASS% | docker login -u %DOCKER_USER% --password-stdin
                    if %ERRORLEVEL% NEQ 0 exit /b 1

                    docker tag %IMAGE_NAME%:%IMAGE_TAG% %DOCKER_HUB_REPO%:%IMAGE_TAG%
                    docker tag %IMAGE_NAME%:%IMAGE_TAG% %DOCKER_HUB_REPO%:latest

                    docker push %DOCKER_HUB_REPO%:%IMAGE_TAG%
                    if %ERRORLEVEL% NEQ 0 exit /b 1
                    docker push %DOCKER_HUB_REPO%:latest
                    if %ERRORLEVEL% NEQ 0 exit /b 1

                    docker logout
                    """
                }
            }
        }

        stage('Scan Kubernetes Manifests') {
            steps {
                bat """
                @echo off
                trivy config --severity HIGH,CRITICAL --exit-code 1 .
                if %ERRORLEVEL% NEQ 0 exit /b 1
                """
            }
        }

        stage('Deploy and Update on Kubernetes') {
            steps {
                withCredentials([file(credentialsId: 'kubeconfig-file', variable: 'KCFG')]) {
                    bat """
                    @echo off
                    set "KUBECONFIG=%KCFG%"
                    kubectl config current-context
                    kubectl cluster-info

                    kubectl apply -f deployment.yaml --validate=false
                    if %ERRORLEVEL% NEQ 0 exit /b 1
                    kubectl apply -f service.yaml --validate=false
                    if %ERRORLEVEL% NEQ 0 exit /b 1

                    kubectl set image deployment/%KUBE_DEPLOYMENT_NAME% login-ci-demo-container=%DOCKER_HUB_REPO%:%IMAGE_TAG%
                    if %ERRORLEVEL% NEQ 0 exit /b 1
                    """
                }
            }
        }

        stage('Debug Pod Issues') {
            steps {
                withCredentials([file(credentialsId: 'kubeconfig-file', variable: 'KCFG')]) {
                    bat """
                    @echo off
                    set "KUBECONFIG=%KCFG%"

                    echo === Current Deployment Status ===
                    kubectl get deployment %KUBE_DEPLOYMENT_NAME% -o wide || echo Deployment not found

                    echo === Pod Status and Details ===
                    kubectl get pods -l %K8S_LABEL% -o wide

                    echo === Container Logs (last 50 lines per pod) ===
                    for /f %%p in ('kubectl get pods -l %K8S_LABEL% -o name 2^>nul') do (
                        echo --- Logs for %%p ---
                        kubectl logs %%p --tail=50
                        echo.
                    )

                    echo === Service Status ===
                    kubectl get svc %KUBE_SERVICE_NAME% -o wide
                    kubectl describe svc %KUBE_SERVICE_NAME%

                    echo === Local Test Run of Image ===
                    docker run --rm -d --name test-container -p 8082:8081 %DOCKER_HUB_REPO%:%IMAGE_TAG%
                    ping -n 20 127.0.0.1 >nul
                    curl http://localhost:8082/actuator/health || echo Health endpoint failed
                    docker stop test-container
                    """
                }
            }
        }

        stage('Verify Kubernetes Deployment') {
            steps {
                withCredentials([file(credentialsId: 'kubeconfig-file', variable: 'KCFG')]) {
                    bat """
                    @echo off
                    set "KUBECONFIG=%KCFG%"
                    ping -n 10 127.0.0.1 >nul

                    kubectl rollout status deployment/%KUBE_DEPLOYMENT_NAME% --timeout=5m
                    if %ERRORLEVEL% NEQ 0 (
                        echo DEPLOYMENT VERIFICATION FAILED
                        kubectl get pods -l %K8S_LABEL% -o wide
                        exit /b 1
                    )

                    kubectl get pods -l %K8S_LABEL% -o wide
                    kubectl get svc %KUBE_SERVICE_NAME% -o wide
                    """
                }
            }
        }
    }

    post {
        always {
            echo "Pipeline finished with status: ${currentBuild.currentResult}"
            bat "docker system prune -f >nul 2>&1"
        }
        success {
            withCredentials([file(credentialsId: 'kubeconfig-file', variable: 'KCFG')]) {
                bat """
                @echo off
                set "KUBECONFIG=%KCFG%"
                kubectl get deployments
                kubectl get pods -o wide
                kubectl get services
                """
            }
        }
        failure {
            withCredentials([file(credentialsId: 'kubeconfig-file', variable: 'KCFG')]) {
                bat """
                @echo off
                set "KUBECONFIG=%KCFG%"
                echo === FINAL DEBUG INFO ===
                kubectl get deployment %KUBE_DEPLOYMENT_NAME% -o yaml || echo no deployment
                kubectl describe pods -l %K8S_LABEL%
                kubectl logs -l %K8S_LABEL% --tail=200
                """
            }
        }
    }
}