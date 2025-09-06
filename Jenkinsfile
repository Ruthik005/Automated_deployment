pipeline {
    agent any

    environment {
        IMAGE_NAME           = "login-ci-demo"
        IMAGE_TAG            = "${env.BUILD_NUMBER}"
        APP_PORT             = "8081"
        DOCKER_IMAGE         = "ruthik005/capstone_project:latest"
        KUBE_DEPLOYMENT_NAME = "login-ci-demo-deployment"
        KUBE_SERVICE_NAME    = "login-ci-demo-service"
        K8S_LABEL            = "app=login-ci-demo"
        NAMESPACE            = "ci-cd-app"
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

        stage('Build Local Docker Image') {
            steps {
                bat """
                @echo off
                echo === Building Local Docker Image ===
                docker build --no-cache -t %DOCKER_IMAGE% .
                if %ERRORLEVEL% NEQ 0 exit /b 1
                """
            }
        }

        stage('Run Local Container Test') {
            steps {
                bat """
                @echo off
                echo === Running Container Test ===
                docker run --rm -d --name test-build -p 8085:%APP_PORT% %DOCKER_IMAGE%
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
                trivy image --timeout %TRIVY_TIMEOUT% --severity CRITICAL,HIGH --exit-code 1 --ignore-unfixed %DOCKER_IMAGE%
                if %ERRORLEVEL% NEQ 0 exit /b 1
                """
            }
        }

        stage('Deploy to Local Kubernetes') {
            steps {
                bat """
                @echo off
                set "KUBECONFIG=%KUBECONFIG%"
                
                echo === Ensure Namespace Exists ===
                kubectl create namespace %NAMESPACE% --dry-run=client -o yaml | kubectl apply -f -

                echo === Applying Deployment & Service ===
                kubectl apply -f k8s/deployment-service.yaml -n %NAMESPACE%

                echo === Wait for Rollout ===
                kubectl rollout status deployment/%KUBE_DEPLOYMENT_NAME% -n %NAMESPACE%
                """
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
            bat "kubectl get pods -n %NAMESPACE%"
            bat "kubectl get svc -n %NAMESPACE%"
        }
        success {
            bat """
            @echo off
            set "KUBECONFIG=%KUBECONFIG%"
            kubectl get deployments -n %NAMESPACE%
            kubectl get pods -o wide -n %NAMESPACE%
            kubectl get services -n %NAMESPACE%
            """
        }
        failure {
            bat """
            @echo off
            set "KUBECONFIG=%KUBECONFIG%"
            echo === FINAL DEBUG INFO ===
            kubectl get deployment %KUBE_DEPLOYMENT_NAME% -n %NAMESPACE% -o yaml || echo no deployment
            kubectl describe pods -l %K8S_LABEL% -n %NAMESPACE%
            kubectl logs -l %K8S_LABEL% -n %NAMESPACE% --tail=200
            """
        }
    }
}
