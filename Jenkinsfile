pipeline {
    agent any

    environment {
        DOCKER_IMAGE = "login-ci-demo"
        NGROK_AUTH_TOKEN = credentials('ngrok-auth') // Add in Jenkins credentials
    }
    //jenkins file with automation of ngrok
    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'hthttps://github.com/Ruthik005/Automated_deployment.git'
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
                sh "docker run -d --name ${DOCKER_IMAGE}-container ${DOCKER_IMAGE}"
            }
        }
         stages {
        stage('Run Command') {
            steps {
                bat '''
                    echo Running my command...
                    your-command-here
                '''
            }
        }
    }
        stage('Expose via ngrok') {
            steps {
                sh """
                curl -s https://ngrok-agent.s3.amazonaws.com/ngrok.asc | sudo tee /etc/apt/trusted.gpg.d/ngrok.asc >/dev/null
                echo "deb https://ngrok-agent.s3.amazonaws.com buster main" | sudo tee /etc/apt/sources.list.d/ngrok.list
                sudo apt update && sudo apt install ngrok
                ngrok config add-authtoken ${NGROK_AUTH_TOKEN}
                ngrok http 8080 &
                sleep 5
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
