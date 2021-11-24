pipeline {
    agent any

    environment {
        LIMIT = "test"
        LOG_LEVEL = "debug"
        PROJECT = "events-a22"
        PROJECT_FOLDER = "data-collectors/${PROJECT}"
        ARTIFACT_NAME = "dc-${PROJECT}"
        DOCKER_IMAGE = "755952719952.dkr.ecr.eu-west-1.amazonaws.com/${PROJECT}"
        DOCKER_TAG = "$LIMIT-$BUILD_NUMBER"
        DATACOLLECTORS_CLIENT_SECRET = credentials("keycloak-datacollectors-secret-$LIMIT")
        KEYCLOAK_URL = "https://auth.opendatahub.testingmachine.eu"
        WRITER_URL = "https://mobility.share.opendatahub.testingmachine.eu"
        A22_CONNECTOR_URL = "http://webservices.cau-a22.org:8080/A22Data"
        A22_CONNECTOR_USR = "BN5"
        A22_CONNECTOR_PWD = credentials('a22connector_password_bn5')
    }

    stages {
        stage('Configure') {
            steps {
                sh """
                    cd ${PROJECT_FOLDER}
                    echo 'COMPOSE_PROJECT_NAME=${PROJECT}' > .env
                    echo 'DOCKER_IMAGE=${DOCKER_IMAGE}' >> .env
                    echo 'DOCKER_TAG=${DOCKER_TAG}' >> .env
                    echo 'LOG_LEVEL=${LOG_LEVEL}' >> .env
                    echo 'ARTIFACT_NAME=${ARTIFACT_NAME}' >> .env
                    echo 'authorizationUri=${KEYCLOAK_URL}/auth' >> .env
                    echo 'tokenUri=${KEYCLOAK_URL}/auth/realms/noi/protocol/openid-connect/token' >> .env 
                    echo 'clientId=odh-mobility-datacollector' >> .env
                    echo 'clientName=odh-mobility-datacollector' >> .env
                    echo 'clientSecret=${DATACOLLECTORS_CLIENT_SECRET}' >> .env
                    echo 'scope=openid' >> .env 
                    echo -n 'provenance_version=' >> .env
                    xmlstarlet sel -N pom=http://maven.apache.org/POM/4.0.0 -t -v '/pom:project/pom:version' pom.xml >> .env
                    echo '' >> .env
                    echo -n 'provenance_name=' >> .env 
                    xmlstarlet sel -N pom=http://maven.apache.org/POM/4.0.0 -t -v '/pom:project/pom:artifactId' pom.xml >> .env
                    echo '' >> .env
                    echo 'BASE_URI=${WRITER_URL}/json' >> .env
                    echo 'A22_CONNECTOR_URL=${A22_CONNECTOR_URL}' >> .env
                    echo 'A22_CONNECTOR_USR=${A22_CONNECTOR_USR}' >> .env
                    echo 'A22_CONNECTOR_PWD=${A22_CONNECTOR_PWD}' >> .env
                """
            }
        }
        stage('Test & Build') {
            steps {
                sh """
                    cd ${PROJECT_FOLDER}
                    aws ecr get-login --region eu-west-1 --no-include-email | bash
                    docker-compose --no-ansi -f infrastructure/docker-compose.build.yml build --pull
                    docker-compose --no-ansi -f infrastructure/docker-compose.build.yml push
                """
            }
        }
        stage('Deploy') {
            steps {
               sshagent(['jenkins-ssh-key']) {
                    sh """
                        (cd ${PROJECT_FOLDER}/infrastructure/ansible && ansible-galaxy install -f -r requirements.yml)
                        (cd ${PROJECT_FOLDER}/infrastructure/ansible && ansible-playbook --limit=${LIMIT} deploy.yml --extra-vars "release_name=${BUILD_NUMBER} project_name=${PROJECT}")
                    """
                }
            }
        }
    }
}
