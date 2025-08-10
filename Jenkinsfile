pipeline {
    agent any

    environment {
        DOCKER_IMAGE = "login-ci-demo"
        NGROK_AUTH_TOKEN = credentials('ngrok-auth') // Jenkins Credential ID for ngrok token
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/Ruthik005/Automated_deployment.git'
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
                bat "docker run -d -p 8080:8080 --name %DOCKER_IMAGE%-container %DOCKER_IMAGE%"
            }
        }

        stage('Install & Run ngrok') {
            steps {
                bat """
                curl -s https://bin.equinox.io/c/4VmDzA7iaHb/ngrok-stable-windows-amd64.zip -o ngrok.zip
                tar -xf ngrok.zip
                ngrok config add-authtoken %NGROK_AUTH_TOKEN%
                start /B ngrok http 8080
                """
            }
        }

        stage('Show ngrok Public URL') {
            steps {
                script {
                    // Wait for ngrok to initialize
                    sleep(time: 5, unit: 'SECONDS')
                    def jsonOutput = bat(script: 'curl -s http://localhost:4040/api/tunnels', returnStdout: true).trim()
                    // Extract just the HTTPS URL using regex
                    def matcher = (jsonOutput =~ /(https:\\/\\/[a-zA-Z0-9.-]+ngrok-free\\.app)/)
                    if (matcher.find()) {
                        def ngrokUrl = matcher.group(1).replace("\\/", "/")
                        echo "✅ Your public URL: ${ngrokUrl}"
                    } else {
                        error("Could not find ngrok public URL")
                    }
                }
            }
        }
    }

    post {
        always {
            bat "docker rm -f %DOCKER_IMAGE%-container || exit 0"
            bat "taskkill /F /IM ngrok.exe || exit 0"
        }
    }
}
