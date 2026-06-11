pipeline {
    agent any

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
                    def props = readProperties file: "${ENV_FILE}"
                    env.IMAGE_NAME = props.IMAGE_NAME
                    env.IMAGE_TAG  = props.IMAGE_TAG
                    env.APP_PORT   = props.APP_PORT
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                echo "Building Docker image ${env.IMAGE_NAME}:${env.IMAGE_TAG}..."
                sh "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} ./high-concurrency-order-system"
            }
        }

        stage('Push to Docker Hub') {
            steps {
                echo 'Pushing to Docker Hub...'
                sh "echo ${DOCKERHUB_CREDENTIALS_PSW} | docker login -u ${DOCKERHUB_CREDENTIALS_USR} --password-stdin"
                sh "docker push ${IMAGE_NAME}:${IMAGE_TAG}"
            }
        }

        stage('Deploy with Docker Compose') {
            steps {
                echo 'Deploying...'
                sh "cp ${ENV_FILE} .env"
                sh "docker compose down || true"
                sh "docker compose up -d"
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