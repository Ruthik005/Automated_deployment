pipeline {
    agent any

    environment {
        IMAGE_NAME = "login-ci-demo"
        IMAGE_TAG = "latest"   // Always overwrite the same tag
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

        stage('Build Docker Image (No Cache)') {
            steps {
                bat "docker build --no-cache -t %IMAGE_NAME%:%IMAGE_TAG% ."
            }
        }

        stage('Run Docker Container') {
            steps {
                bat """
                docker stop %IMAGE_NAME% || true
                docker rm %IMAGE_NAME% || true
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
