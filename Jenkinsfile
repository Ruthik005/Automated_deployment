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
            steps {
                sh """
                ngrok config add-authtoken ${NGROK_AUTH_TOKEN}
                nohup ngrok http 8080 > /dev/null 2>&1 &
                sleep 5
                echo "Public URL:"
                curl -s http://localhost:4040/api/tunnels | grep -o 'https://[a-zA-Z0-9.-]*ngrok-free.app'
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
