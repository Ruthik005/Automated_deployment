pipeline {
    agent any

    environment {
        IMAGE_NAME = "login-ci-demo"
        IMAGE_TAG = "${env.BUILD_NUMBER}" // Unique tag per build
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

        stage('Build Docker Image and Cleanup Old') {
            steps {
                bat """
                REM Build new image with build number and latest tags
                docker build --no-cache -t %IMAGE_NAME%:%IMAGE_TAG% -t %IMAGE_NAME%:latest .

                REM Remove all old build number images except the current one
                for /f "tokens=*" %%i in ('docker images %IMAGE_NAME% --format "{{.Repository}}:{{.Tag}}" ^| findstr /R ":[0-9][0-9]*" ^| findstr /V ":%IMAGE_TAG%"') do docker rmi -f %%i
                """
            }
        }

        stage('Run Docker Container') {
            steps {
                bat """
                docker stop %IMAGE_NAME% 2>nul
                docker rm %IMAGE_NAME% 2>nul
                docker run -d -p 8081:8080 --name %IMAGE_NAME% %IMAGE_NAME%:%IMAGE_TAG%
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
