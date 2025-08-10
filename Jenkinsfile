pipeline {
    agent any

    environment {
        DOCKER_IMAGE = "login-ci-demo"
        NGROK_AUTH_TOKEN = credentials('ngrok-auth') // Jenkins credential ID
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/Ruthik005/Automated_deployment.git'
            }
        }

        stage('Run Unit Tests') {
            steps {
                sh 'mvn clean test'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh "docker build -t ${DOCKER_IMAGE} ."
            }
        }

        stage('Run Docker Container') {
            steps {
                sh "docker run -d -p 8080:8080 --name ${DOCKER_IMAGE}-container ${DOCKER_IMAGE}"
            }
        }

        stage('Install ngrok') {
            steps {
                sh """
                if ! command -v ngrok &> /dev/null
                then
                    curl -s https://ngrok-agent.s3.amazonaws.com/ngrok.asc | sudo tee /etc/apt/trusted.gpg.d/ngrok.asc >/dev/null
                    echo "deb https://ngrok-agent.s3.amazonaws.com buster main" | sudo tee /etc/apt/sources.list.d/ngrok.list
                    sudo apt update && sudo apt install -y ngrok
                fi
                """
            }
        }

        stage('Expose via ngrok') {
            environment {
        NGROK_AUTH_TOKEN = credentials('ngrok-auth') // Replace with your Jenkins credential ID
    }
    steps {
        bat """
        ngrok config add-authtoken %NGROK_AUTH_TOKEN%
        start /B ngrok http 8080
        ping -n 6 127.0.0.1 > nul
        curl http://localhost:4040/api/tunnels
        """
    }
        }
    }

    post {
        always {
            sh "docker rm -f ${DOCKER_IMAGE}-container || true"
        }
    }
}
