pipeline {
    agent any

    parameters {
        string(name: 'PORT', defaultValue: '8081', description: 'Port to deploy the app on')
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'develop', url: 'https://github.com/Gauriop/Shop-Floor-Andon-DevOps.git'
            }
        }
        stage('Build') {
            steps {
                bat 'mvn clean package -DskipTests'
            }
        }
        stage('Package') {
            steps {
                bat 'echo Packaging complete - JAR ready in target/'
            }
        }
        stage('Deploy') {
            steps {
                bat "taskkill /F /IM java.exe /FI \"WINDOWTITLE eq andon*\" || echo No previous instance running"
                bat "start \"andon-app\" cmd /c java -jar target\\andon-dashboard-0.1.0.jar --server.port=%PORT%"
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully. App deployed.'
        }
        failure {
            echo 'Pipeline failed.'
        }
    }
}