pipeline {
    agent any

    stages {
        stage('Prepare') {
            steps {
                sh 'chmod +x gradlew'
                sh './gradlew --refresh-dependencies -s clean setupDecompWorkspace'
            }
        }

        stage('Build') {
            steps {
                sh './gradlew --refresh-dependencies -s  clean build :uploadArchives'
            }
        }
    }
}
