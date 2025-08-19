pipeline {
    agent any

    environment {
        IMAGE_NAME = "login-ci-demo"
        IMAGE_TAG = "${env.BUILD_NUMBER}"  // Unique tag per build
        APP_PORT = "8081"
    }

    stages {
        stage('Checkout SCM') {
            steps {
                checkout scm
            }
        }

        stage('Clean Old Docker Images & Containers') {
            steps {
                bat """
                REM Stop and remove all running containers silently
                for /F "tokens=*" %%c in ('docker ps -a -q --filter "ancestor=%IMAGE_NAME%"') do (
                    docker stop %%c >nul 2>&1 || exit /b 0
                    docker rm %%c >nul 2>&1 || exit /b 0
                )

                REM Remove old images except the newly built one silently
                for /F "tokens=*" %%i in ('docker images %IMAGE_NAME% --format "{{.ID}} {{.Tag}}"') do (
                    for /F "tokens=1,2" %%a in ("%%i") do (
                        if NOT "%%b"=="%IMAGE_TAG%" docker rmi -f %%a >nul 2>&1 || exit /b 0
                    )
                )
                """
            }
        }

        stage('Build & Test in Docker') {
            steps {
                bat """
                REM Build Docker image (runs tests automatically in Maven container)
                docker build --no-cache -t %IMAGE_NAME%:%IMAGE_TAG% .
                """
            }
        }

        stage('Run Docker Container') {
            steps {
                bat """
                REM Stop & remove existing container silently
                docker stop %IMAGE_NAME% >nul 2>&1 || exit /b 0
                docker rm %IMAGE_NAME% >nul 2>&1 || exit /b 0

                REM Run container on specified port
                docker run -d -p %APP_PORT%:%APP_PORT% --name %IMAGE_NAME% %IMAGE_NAME%:%IMAGE_TAG%
                """
            }
        }

       stage('Health Check') {
    steps {
        bat """
        REM Wait ~20 seconds for the app to start
        ping 127.0.0.1 -n 21 >nul

        REM Retry loop: check health endpoint up to 5 times
        set RETRIES=5
        set SUCCESS=0
        :CHECK
        curl -u admin:admin123 -s http://localhost:%APP_PORT%/health | findstr /C:"OK"
        if %errorlevel%==0 (
            set SUCCESS=1
            goto END
        ) else (
            set /a RETRIES=%RETRIES%-1
            if %RETRIES% GTR 0 (
                ping 127.0.0.1 -n 6 >nul
                goto CHECK
            ) else (
                echo Health check failed!
                exit /b 1
            )
        )
        :END
        if %SUCCESS%==1 (
            echo Health check passed.
        )
        """
    }
}
    }

    post {
        always {
            echo "Pipeline finished with status: ${currentBuild.currentResult}"
            bat """
            REM Optional: Clean dangling images silently
            docker system prune -f >nul 2>&1 || exit /b 0
            """
        }
    }
}
