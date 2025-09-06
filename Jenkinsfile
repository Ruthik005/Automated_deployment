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
        KUBECONFIG           = "${env.USERPROFILE}\\.kube\\jenkins-kubeconfig.yaml"
        TRIVY_TIMEOUT        = "10m"
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
                echo === Building Docker Image ===
                docker build --no-cache -t %IMAGE_NAME%:%IMAGE_TAG% .
                if %ERRORLEVEL% NEQ 0 exit /b 1

                echo === Running Container Test ===
                docker run --rm -d --name test-build -p 8085:%APP_PORT% %IMAGE_NAME%:%IMAGE_TAG%
                ping -n 15 127.0.0.1 >nul
                curl http://localhost:8085/actuator/health || echo Health check failed
                docker stop test-build
                """
            }
        }

        stage('Security Scan with Trivy') {
            steps {
                bat """
                @echo off
                echo === Running Trivy Scan ===
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
                echo === Scanning Kubernetes YAML with Trivy ===
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
                    kubectl config use-context docker-desktop
                    kubectl config current-context
                    kubectl cluster-info

                    echo === Applying Deployment and Service ===
                    kubectl apply -f deployment.yaml --validate=false
                    if %ERRORLEVEL% NEQ 0 exit /b 1

                    kubectl apply -f service.yaml --validate=false
                    if %ERRORLEVEL% NEQ 0 exit /b 1

                    echo === Updating Image ===
                    kubectl set image deployment/%KUBE_DEPLOYMENT_NAME% login-ci-demo-container=%DOCKER_HUB_REPO%:%IMAGE_TAG%
                    if %ERRORLEVEL% NEQ 0 exit /b 1
                    """
                }
            }
        }

        stage('Debug Pod Issues') {
            steps {
                bat """
                @echo off
                set "KUBECONFIG=%KUBECONFIG%"

                echo === Deployment Status ===
                kubectl get deployment %KUBE_DEPLOYMENT_NAME% -o wide || echo Deployment not found

                echo === Pod Status ===
                kubectl get pods -l %K8S_LABEL% -o wide

                echo === Pod Logs ===
                for /f %%p in ('kubectl get pods -l %K8S_LABEL% -o name 2^>nul') do (
                    echo --- Logs for %%p ---
                    kubectl logs %%p --tail=50
                    echo.
                )

                echo === Service Info ===
                kubectl get svc %KUBE_SERVICE_NAME% -o wide
                kubectl describe svc %KUBE_SERVICE_NAME%
                """
            }
        }

        stage('Verify Kubernetes Deployment') {
            steps {
                bat """
                @echo off
                set "KUBECONFIG=%KUBECONFIG%"
                ping -n 10 127.0.0.1 >nul

                echo === Verifying Rollout ===
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

    post {
        always {
            echo "Pipeline finished with status: ${currentBuild.currentResult}"
            cleanWs()
            bat "docker system prune -f >nul 2>&1"
        }
        success {
            bat """
            @echo off
            set "KUBECONFIG=%KUBECONFIG%"
            kubectl get deployments
            kubectl get pods -o wide
            kubectl get services
            """
        }
        failure {
            bat """
            @echo off
            set "KUBECONFIG=%KUBECONFIG%"
            echo === FINAL DEBUG INFO ===
            kubectl get deployment %KUBE_DEPLOYMENT_NAME% -o yaml || echo no deployment
            kubectl describe pods -l %K8S_LABEL%
            kubectl logs -l %K8S_LABEL% --tail=200
            """
        }
    }
}
