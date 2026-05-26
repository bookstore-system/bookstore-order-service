pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = 'truongdocker1'
        DOCKER_CREDENTIALS_ID = 'dockerhub-creds'
        IMAGE_NAME = 'bookstore-user-service'
        TAG = "${BUILD_NUMBER}"

        K8S_DEPLOYMENT = 'user-service-deployment'
        K8S_CONTAINER = 'user-service'
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
                    usernamePassword(credentialsId: 'db-creds', usernameVariable: 'DB_USERNAME', passwordVariable: 'DB_PASSWORD'),
                    file(credentialsId: 'jwt-private-pem', variable: 'JWT_PRIVATE_PEM'),
                    file(credentialsId: 'jwt-public-pem', variable: 'JWT_PUBLIC_PEM'),
                    string(credentialsId: 'cloudinary-cloud-name', variable: 'CLOUDINARY_CLOUD_NAME'),
                    string(credentialsId: 'cloudinary-api-key', variable: 'CLOUDINARY_API_KEY'),
                    string(credentialsId: 'cloudinary-api-secret', variable: 'CLOUDINARY_API_SECRET'),
                    string(credentialsId: 'redis-password', variable: 'SPRING_DATA_REDIS_PASSWORD')
                ]) {
                    sh '''
                export KUBECONFIG=/var/jenkins_home/.kube/config

                # Update image tag robustly, even if the workspace still has an older build tag.
                sed -i "s|image: .*${IMAGE_NAME}:.*|image: ${DOCKER_REGISTRY}/${IMAGE_NAME}:${TAG}|g" k8s/deployment.yaml

                # ConfigMap is safe to keep in Git.
                kubectl apply -f k8s/configmap.yaml

                # App secret from Jenkins Credentials. Do not apply k8s/secret.yaml with real values.
                kubectl create secret generic user-service-secret \
                  --from-literal=DB_USERNAME="$DB_USERNAME" \
                  --from-literal=DB_PASSWORD="$DB_PASSWORD" \
                  --from-literal=CLOUDINARY_ENABLED=true \
                  --from-literal=CLOUDINARY_CLOUD_NAME="$CLOUDINARY_CLOUD_NAME" \
                  --from-literal=CLOUDINARY_API_KEY="$CLOUDINARY_API_KEY" \
                  --from-literal=CLOUDINARY_API_SECRET="$CLOUDINARY_API_SECRET" \
                  --from-literal=SPRING_DATA_REDIS_PASSWORD="$SPRING_DATA_REDIS_PASSWORD" \
                  --dry-run=client -o yaml | kubectl apply -f -

                # JWT signing key pair. user-service needs private.pem + public.pem.
                kubectl create secret generic jwt-keys \
                  --from-file=private.pem="$JWT_PRIVATE_PEM" \
                  --from-file=public.pem="$JWT_PUBLIC_PEM" \
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
