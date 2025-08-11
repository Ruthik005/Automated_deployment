pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build with Maven') {
            steps {
                bat "mvn clean install"
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
                bat "docker build -t login-ci-demo:latest ."
            }
        }

        stage('Run Docker Container') {
            steps {
                bat "docker run --rm login-ci-demo:latest"
            }
        }

        stage('Email Notification') {
            steps {
                emailext(
                    to: 'your-email@example.com',
                    subject: "Jenkins Build - ${currentBuild.currentResult}",
                    body: "Build completed with status: ${currentBuild.currentResult}"
                )
            }
        }
    }
}
