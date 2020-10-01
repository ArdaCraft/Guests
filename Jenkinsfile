pipeline {
    agent any

    stages {
        stage('Prepare') {
            steps {
                sh 'chmod +x gradlew'
            }
        }

        stage('Build') {
            steps {
                sh './gradlew --refresh-dependencies -s  clean build :uploadArchives'
            }
        }
    }
}
