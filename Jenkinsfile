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
        KUBECONFIG           = "${env.USERPROFILE}\\.kube\\config"  // Use default config
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
                bat '''
                @echo off
                echo === Building Docker Image ===
                docker build --no-cache -t %IMAGE_NAME%:%IMAGE_TAG% .

                if %ERRORLEVEL% NEQ 0 exit /b 1

                echo === Running Container Test ===
                docker run --rm -d --name test-build -p 8085:8081 %IMAGE_NAME%:%IMAGE_TAG%

                echo === Waiting for App Startup ===
                ping -n 30 127.0.0.1 >nul

                echo === Health Check ===
                curl http://localhost:8085/actuator/health || (echo Health check failed & exit /b 1)

                docker stop test-build || true
                docker rm test-build || true
                '''
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

        stage('Verify Kubernetes Connection') {
            steps {
                bat """
                @echo off
                echo === Testing Kubernetes Connection ===
                kubectl cluster-info
                if %ERRORLEVEL% NEQ 0 (
                    echo Kubernetes connection failed - skipping K8s stages
                    exit /b 0
                )
                kubectl get nodes
                """
            }
        }

        stage('Deploy and Update on Kubernetes') {
            when {
                expression {
                    try {
                        bat(script: 'kubectl cluster-info', returnStatus: true) == 0
                    } catch (Exception e) {
                        return false
                    }
                }
            }
            steps {
                bat """
                @echo off
                echo === Deploying to Kubernetes ===
                kubectl apply -f deployment.yaml
                kubectl apply -f service.yaml
                kubectl set image deployment/login-ci-demo-deployment login-ci-demo-container=ruthik005/capstone_project:${BUILD_NUMBER}
                """
            }
        }

        stage('Debug Pod Issues') {
            when {
                expression {
                    try {
                        bat(script: 'kubectl cluster-info', returnStatus: true) == 0
                    } catch (Exception e) {
                        return false
                    }
                }
            }
            steps {
                bat """
                @echo off
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
                kubectl get svc %KUBE_SERVICE_NAME% -o wide || echo Service not found
                kubectl describe svc %KUBE_SERVICE_NAME% || echo Service not found
                """
            }
        }

        stage('Verify Kubernetes Deployment') {
            when {
                expression {
                    try {
                        bat(script: 'kubectl cluster-info', returnStatus: true) == 0
                    } catch (Exception e) {
                        return false
                    }
                }
            }
            steps {
                bat """
                @echo off
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
            script {
                try {
                    bat """
                    @echo off
                    kubectl get deployments
                    kubectl get pods -o wide
                    kubectl get services
                    """
                } catch (Exception e) {
                    echo "Kubernetes not accessible for success summary"
                }
            }
        }
        failure {
            script {
                try {
                    bat """
                    @echo off
                    echo === FINAL DEBUG INFO ===
                    kubectl get deployment %KUBE_DEPLOYMENT_NAME% -o yaml || echo no deployment
                    kubectl describe pods -l %K8S_LABEL%
                    kubectl logs -l %K8S_LABEL% --tail=200
                    """
                } catch (Exception e) {
                    echo "Kubernetes not accessible for failure debugging"
                }
            }
        }
    }
}