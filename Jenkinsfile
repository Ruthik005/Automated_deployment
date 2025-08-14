pipeline {
    agent any

    environment {
        IMAGE_NAME = "login-ci-demo"
        IMAGE_TAG = "latest"   // Always overwrite same image
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
                // Build without --no-cache so it reuses layers and overwrites 'latest'
                bat "docker build -t %IMAGE_NAME%:%IMAGE_TAG% ."
                // Optional: remove old dangling images immediately
                bat "docker image prune -f"
            }
        }

        stage('Run Docker Container') {
            steps {
                // Stop and remove container if running, then run the new one
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
        }
    }
}
