pipeline {
    agent any

    options {
        disableConcurrentBuilds()
    }

    parameters {
        string(name: 'PORT', defaultValue: '8081', description: 'Port for the jar deployment (Week 8)')
        string(name: 'DOCKER_USER', defaultValue: 'gauriic20', description: 'Docker Hub username')
        string(name: 'CONTAINER_PORT', defaultValue: '8083', description: 'Host port for the Docker container deployment')
    }

    environment {
        IMAGE_NAME     = 'andon-dashboard'
        IMAGE_VERSION  = "1.0.${env.BUILD_NUMBER}"
        CONTAINER_NAME = 'andon-app-ci'
    }

    stages {

        stage('Checkout') {
            steps {
                git branch: 'develop', url: 'https://github.com/Gauriop/Shop-Floor-Andon-DevOps.git'
            }
        }

        stage('Stop Previous App') {
            steps {
                bat 'powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort %PORT% -State Listen -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }; exit 0"'
            }
        }

        stage('Build') {
            steps {
                bat 'mvn -B clean compile'
            }
        }

        stage('Test') {
            steps {
                bat 'mvn -B test'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
                    archiveArtifacts artifacts: 'target/selenium-screenshots/*.png', allowEmptyArchive: true
                }
            }
        }

        stage('Package') {
            steps {
                bat 'mvn -B package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('Deploy') {
            steps {
                powershell '''
                $ws = $env:WORKSPACE
                $jar = Join-Path $ws "target\\andon-dashboard-0.1.0.jar"
                $log = Join-Path $ws "app.log"
                $cmd = "cmd /c java -jar `"$jar`" --server.port=$env:PORT > `"$log`" 2>&1"
                Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{ CommandLine = $cmd; CurrentDirectory = $ws } | Out-Null
                '''
            }
        }

        stage('Docker Build') {
            steps {
                bat "docker version --format \"Docker server: {{.Server.Version}}\""
                bat "docker build -t ${params.DOCKER_USER}/${env.IMAGE_NAME}:${env.IMAGE_VERSION} -t ${params.DOCKER_USER}/${env.IMAGE_NAME}:latest ."
                bat "docker images ${params.DOCKER_USER}/${env.IMAGE_NAME}"
            }
        }

        stage('Docker Push') {
            steps {
                withEnv(["DOCKER_CONFIG=${env.WORKSPACE}\\docker-config"]) {
                    withCredentials([usernamePassword(credentialsId: 'dockerhub-creds',
                                                      usernameVariable: 'DH_USER',
                                                      passwordVariable: 'DH_PASS')]) {
                        bat 'if not exist "%DOCKER_CONFIG%" mkdir "%DOCKER_CONFIG%"'
                        bat 'docker login -u %DH_USER% -p %DH_PASS%'
                        bat "docker push ${params.DOCKER_USER}/${env.IMAGE_NAME}:${env.IMAGE_VERSION}"
                        bat "docker push ${params.DOCKER_USER}/${env.IMAGE_NAME}:latest"
                    }
                }
            }
            post {
                always {
                    withEnv(["DOCKER_CONFIG=${env.WORKSPACE}\\docker-config"]) {
                        bat 'docker logout || exit /b 0'
                    }
                    bat "if exist \"${env.WORKSPACE}\\docker-config\" rmdir /s /q \"${env.WORKSPACE}\\docker-config\""
                }
            }
        }

        stage('Deploy Container') {
            steps {
                bat "docker rm -f ${env.CONTAINER_NAME} || exit /b 0"
                bat "docker run -d --name ${env.CONTAINER_NAME} -p ${params.CONTAINER_PORT}:8081 ${params.DOCKER_USER}/${env.IMAGE_NAME}:${env.IMAGE_VERSION}"
            }
        }

        stage('Verify Container') {
            steps {
                powershell """
                \$url = "http://localhost:${params.CONTAINER_PORT}/events"
                \$ok = \$false
                for (\$i = 1; \$i -le 15; \$i++) {
                    try {
                        \$r = Invoke-WebRequest -Uri \$url -UseBasicParsing -TimeoutSec 5
                        if (\$r.StatusCode -eq 200) { \$ok = \$true; break }
                    } catch {
                        Start-Sleep -Seconds 3
                    }
                }
                docker ps --filter "name=${env.CONTAINER_NAME}"
                if (\$ok) {
                    Write-Output "HEALTH CHECK PASSED: \$url returned 200"
                } else {
                    Write-Output "HEALTH CHECK FAILED"
                    docker logs ${env.CONTAINER_NAME}
                    exit 1
                }
                """
            }
        }
    }

    post {
        success {
            echo "Commit-to-container complete. Image ${params.DOCKER_USER}/${env.IMAGE_NAME}:${env.IMAGE_VERSION} pushed and running on port ${params.CONTAINER_PORT}."
        }
        failure {
            echo 'Pipeline failed. Check Test Result, archived screenshots and the Docker stage output. Later stages are skipped on failure.'
        }
    }
}