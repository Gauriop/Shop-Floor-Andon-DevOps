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
                bat 'mvn -B clean compile'
            }
        }

        // Week 10: Selenium quality gate.
        // If any test fails, Package and Deploy are skipped automatically.
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
                bat "taskkill /F /IM java.exe /FI \"WINDOWTITLE eq andon*\" || echo No previous instance running"
                withEnv(['JENKINS_NODE_COOKIE=dontKillMe', 'BUILD_ID=dontKillMe']) {
                    bat "start \"andon-app\" cmd /c java -jar target\\andon-dashboard-0.1.0.jar --server.port=%PORT%"
                }
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully. Tests passed and app deployed.'
        }
        failure {
            echo 'Pipeline failed. Check Test Result and archived screenshots. Deployment was skipped if tests failed.'
        }
    }
}