pipeline {
    agent any
    tools {
        maven 'Maven3'
    }
    environment {
        DOCKERHUB_CREDENTIALS = credentials('dockerhub-credentials')
        ENV_FILE = '/var/jenkins_home/.env'
    }
    stages {
        stage('Checkout') {
            steps {
                echo 'Cloning repository...'
                checkout scm
            }
        }
        stage('Load Env') {
            steps {
                script {
                    def envContent = readFile("${ENV_FILE}")
                    envContent.split('\n').each { line ->
                        line = line.trim()
                        if (line && !line.startsWith('#')) {
                            def idx = line.indexOf('=')
                            if (idx > 0) {
                                def key   = line.substring(0, idx).trim()
                                def value = line.substring(idx + 1).trim()
                                env."${key}" = value
                            }
                        }
                    }
                }
            }
        }
        stage('Build') {
            steps {
                echo 'Running Maven build...'
                dir('high-concurrency-order-system') {
                    sh 'mvn clean package -B -DskipTests'
                }
            }
        }
        stage('Build Docker Image') {
            steps {
                echo "Building ${env.IMAGE_NAME}:${env.IMAGE_TAG}..."
                sh "docker build -t ${env.IMAGE_NAME}:${env.IMAGE_TAG} ./high-concurrency-order-system"
            }
        }
        stage('Push to Docker Hub') {
            steps {
                echo 'Pushing to Docker Hub...'
                sh "echo ${DOCKERHUB_CREDENTIALS_PSW} | docker login -u ${DOCKERHUB_CREDENTIALS_USR} --password-stdin"
                sh "docker push ${env.IMAGE_NAME}:${env.IMAGE_TAG}"
            }
        }
        stage('Deploy with Docker Compose') {
            steps {
                echo 'Deploying...'
                sh "cp ${ENV_FILE} .env"
                sh "docker-compose down || true"
                sh "docker-compose up -d"
            }
        }
    }
    post {
        success {
            echo 'Pipeline succeeded!'
        }
        failure {
            echo 'Pipeline failed!'
        }
        always {
            sh "docker logout"
        }
    }
}