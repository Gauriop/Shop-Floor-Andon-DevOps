pipeline {
    agent any

    parameters {
        string(name: 'APP_PORT', defaultValue: '8081', description: 'Port to run the app on')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                bat 'npm install'
            }
        }

        stage('Package') {
            steps {
                bat 'echo Packaging step - archiving app files'
            }
        }

        stage('Deploy') {
            steps {
                bat '''
                    for /f "tokens=5" %%a in ('netstat -aon ^| findstr :%APP_PORT%') do taskkill /F /PID %%a
                    start /B cmd /c "set PORT=%APP_PORT% && node server.js > deploy.log 2>&1"
                '''
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: '**/deploy.log', allowEmptyArchive: true
        }
    }
}