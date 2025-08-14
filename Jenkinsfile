pipeline {
    agent any

    environment {
        IMAGE_NAME = "login-ci-demo"
        IMAGE_TAG = "${env.BUILD_NUMBER}"   // Build number tag
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build with Maven') {
            steps {
                bat "mvn clean install -DskipTests"
            }
        }

        stage('Test with Maven') {
            steps {
                bat "mvn test"
            }
        }

        stage('Publish JUnit Results') {
            steps {
                junit '**/target/surefire-reports/*.xml'
            }
        }

        stage('Build Docker Image') {
            steps {
                bat """
                REM Build with both build number and latest tags
                docker build --no-cache -t %IMAGE_NAME%:%IMAGE_TAG% -t %IMAGE_NAME%:latest .

                REM Delete old images for this project except current build and latest
                for /f "tokens=1" %%i in ('docker images %IMAGE_NAME% --format "{{.Repository}}:{{.Tag}}" ^| findstr /V ":%IMAGE_TAG%" ^| findstr /V ":latest"') do docker rmi -f %%i
                """
            }
        }

        stage('Run Docker Container') {
            steps {
                bat """
                REM Stop and remove old container if exists
                docker stop %IMAGE_NAME% 2>nul
                docker rm %IMAGE_NAME% 2>nul

                REM Run the container on 8081 to avoid Jenkins 8080 conflict
                docker run --rm -d -p 8081:8080 --name %IMAGE_NAME% %IMAGE_NAME%:%IMAGE_TAG%
                """
            }
        }
    }

    post {
        always {
            echo "Pipeline finished with status: ${currentBuild.currentResult}"
            bat "docker image prune -f"
        }
    }
}
