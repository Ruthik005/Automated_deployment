pipeline {
    agent any

    environment {
        IMAGE_NAME = "login-ci-demo"
        IMAGE_TAG = "${env.BUILD_NUMBER}"
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
                docker build --no-cache -t %IMAGE_NAME%:%IMAGE_TAG% -t %IMAGE_NAME%:latest .
                docker images %IMAGE_NAME% --format "{{.ID}}" > all_images.txt
                for /F %%i in ('more +1 all_images.txt') do docker rmi -f %%i
                """
            }
        }

        stage('Run Docker Container') {
            steps {
                bat """
                docker stop %IMAGE_NAME% 2>nul
                docker rm %IMAGE_NAME% 2>nul
                docker run --rm -d -p 8081:8080 --name %IMAGE_NAME% %IMAGE_NAME%:%IMAGE_TAG%
                """
            }
        }
    }

    post {
        always {
            echo "Pipeline finished with status: ${currentBuild.currentResult}"
        }
    }
}
