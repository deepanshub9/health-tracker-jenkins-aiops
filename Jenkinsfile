pipeline {
    agent any

    environment {
        IMAGE_NAME = 'deepanshub9/health-tracker-rest'
        DOCKER_REGISTRY = 'https://index.docker.io/v1/'
        DOCKER_CREDENTIALS = 'dockerhub'
        DOCKERHUB_API_URL = 'https://hub.docker.com/v2/repositories'
        DOCKERHUB_ACCESS_TOKEN = 'Your personal token'
    }

    stages {
        stage('Checkout') {
            steps {
                git url: 'https://github.com/deepanshub9/healthtracker14.git', branch: 'main'
            }
        }

        stage('Build JAR') {
            steps {
                sh 'mvn clean install -DskipTests'
            }
        }

        stage('Run Tests') {
            steps {
                sh 'mvn test -Dmaven.test.failure.ignore=true'
            }
        }

        stage('Code Coverage Report') {
            steps {
                sh 'mvn jacoco:report'
            }
        }

        stage('SonarQube Analysis') {
    environment {
        scannerHome = tool 'SonarQubeScanner'
    }
    steps {
        withSonarQubeEnv('sonarqube') {
            sh '''
                mvn sonar:sonar \
                -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
                -Dsonar.junit.reportPaths=target/surefire-reports
            '''
        }
    }
}


        stage('Build Docker Image') {
            steps {
                script {
                    def IMAGE_TAG = "${BUILD_NUMBER}"
                    docker.build("${IMAGE_NAME}:${IMAGE_TAG}")
                    docker.build("${IMAGE_NAME}:latest")
                }
            }
        }

        stage('Trivy Scan') {
            steps {
                script {
                    def IMAGE_TAG = "${BUILD_NUMBER}"
                    sh "trivy image ${IMAGE_NAME}:${IMAGE_TAG}"
                }
            }
        }

        stage('Push Docker Image') {
            steps {
                script {
                    def IMAGE_TAG = "${BUILD_NUMBER}"
                    docker.withRegistry(DOCKER_REGISTRY, DOCKER_CREDENTIALS) {
                        // Push the current build tag and the "latest" tag to Docker Hub
                        docker.image("${IMAGE_NAME}:${IMAGE_TAG}").push()
                        docker.image("${IMAGE_NAME}:latest").push()

                        sh """
                        curl -s -X GET "${DOCKERHUB_API_URL}/${IMAGE_NAME}/tags?page_size=100" | \
                        jq -r '.results | sort_by(.last_updated) | reverse | .[3:] | .[].name' | \
                        xargs -I {} curl -X DELETE "${DOCKERHUB_API_URL}/${IMAGE_NAME}/tags/{}" \
                            --header "Authorization: Bearer ${DOCKERHUB_ACCESS_TOKEN}"
                        """
                    }
                }
            }
        }

        stage('Deploy to EC2') {
            steps {
                sshagent(['ec2-ssh-credentials']) {
                    withCredentials([usernamePassword(credentialsId: 'dockerhub-creds', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        script {
                            sh """
ssh -o StrictHostKeyChecking=no ubuntu@54.82.50.60 <<EOF
docker login -u ${DOCKER_USER} -p ${DOCKER_PASS}
docker pull ${IMAGE_NAME}:latest

docker stop health-tracker-rest || true
docker rm health-tracker-rest || true
docker run -d -p 8001:8001 --name health-tracker-rest ${IMAGE_NAME}:latest
EOF
                            """
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            sh 'docker image prune -a -f'  
            cleanWs()  
        }
    }
}

