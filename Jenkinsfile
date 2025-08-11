pipeline {
    agent any

    environment {
        DOCKER_IMAGE = "login-ci-demo"
        NGROK_AUTH_TOKEN = credentials('ngrok-auth') // Add in Jenkins credentials
    }

    stages {
        stage('Checkout') {
            steps {
                bat 'git clone https://github.com/Ruthik005/Automated_deployment.git .'
            }
        }

        stage('Run Unit Tests') {
            steps {
                bat 'mvn clean test'
            }
        }

        stage('Build Docker Image') {
            steps {
                bat "docker build -t %DOCKER_IMAGE% ."
            }
        }

        stage('Run Docker Container') {
            steps {
                bat "docker run -d --name %DOCKER_IMAGE%-container %DOCKER_IMAGE%"
            }
        }

        stage('Run Command') {
            steps {
                bat '''
                    echo Running my command...
                    REM Replace the below with your actual command
                    your-command-here
                '''
            }
        }

        stage('Expose via ngrok') {
            steps {
                bat """
                    curl -s https://ngrok-agent.s3.amazonaws.com/ngrok.asc > ngrok.asc
                    REM Skipping apt since Windows doesn’t have apt-get
                    ngrok config add-authtoken %NGROK_AUTH_TOKEN%
                    start /B ngrok http 8080
                    timeout /T 5
                    curl http://localhost:4040/api/tunnels
                """
            }
        }
    }

    post {
        always {
            bat "docker rm -f %DOCKER_IMAGE%-container || exit 0"
        }
    }
}
