pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = 'truongdocker1'
        DOCKER_CREDENTIALS_ID = 'dockerhub-creds'
        IMAGE_NAME = 'bookstore-order-service'
        TAG = "${BUILD_NUMBER}"

        K8S_DEPLOYMENT = 'order-service-deployment'
        K8S_CONTAINER = 'order-service'
    }

    tools {
        maven 'Maven 3.9'
        jdk 'JDK 21'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Docker Build') {
            steps {
                script {
                    dockerImage = docker.build(
                        "${DOCKER_REGISTRY}/${IMAGE_NAME}:${TAG}",
                        "."
                    )
                }
            }
        }

        stage('Push Docker Image') {
            steps {
                script {
                    docker.withRegistry(
                        'https://index.docker.io/v1/',
                        "${DOCKER_CREDENTIALS_ID}"
                    ) {
                        dockerImage.push()
                    }
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                withCredentials([
                    usernamePassword(credentialsId: 'db-creds', usernameVariable: 'DB_USERNAME', passwordVariable: 'DB_PASSWORD')
                ]) {
                    sh '''
                export KUBECONFIG=/var/jenkins_home/.kube/config

                # Update image tag robustly, even if the workspace still has an older build tag.
                sed -i "s|image: .*${IMAGE_NAME}:.*|image: ${DOCKER_REGISTRY}/${IMAGE_NAME}:${TAG}|g" k8s/deployment.yaml

                # ConfigMap is safe to keep in Git.
                kubectl apply -f k8s/configmap.yaml

                # App secret from Jenkins Credentials. Do not apply k8s/secret.yaml with real values.
                kubectl create secret generic order-service-secret \
                  --from-literal=DB_USERNAME="$DB_USERNAME" \
                  --from-literal=DB_PASSWORD="$DB_PASSWORD" \
                  --from-literal=RABBITMQ_USERNAME="admin" \
                  --from-literal=RABBITMQ_PASSWORD="123456" \
                  --dry-run=client -o yaml | kubectl apply -f -

                # Deploy app.
                kubectl apply -f k8s/deployment.yaml
                kubectl apply -f k8s/service.yaml

                kubectl rollout status deployment/${K8S_DEPLOYMENT} --timeout=180s
                '''
                }
            }
        }
    }

    post {
        success {
            echo "Build & Deploy SUCCESS"
        }
        failure {
            echo "Build FAILED"
        }
    }
}
